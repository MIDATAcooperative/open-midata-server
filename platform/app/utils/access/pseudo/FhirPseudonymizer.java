/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access.pseudo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import utils.access.DBRecord;
import utils.access.Feature_Pseudonymization;
import utils.exceptions.AppException;

public class FhirPseudonymizer {

	public static FhirPseudonymizer forSTU3() {
	  	return stu3;
	}
	
	public static FhirPseudonymizer forR4() {
	  	return r4;
	}
	
	private static FhirPseudonymizer stu3 = new FhirPseudonymizer();
	private static FhirPseudonymizer r4 = new FhirPseudonymizer();
	
	private static PseudonymizeOperation subject = new PseudonymizeOperation(new String[] { "subject" }, PseudonymizeAction.REFERENCE);
	private static PseudonymizeOperation patient = new PseudonymizeOperation(new String[] { "patient" }, PseudonymizeAction.REFERENCE);
	
	Map<String, List<PseudonymizeOperation>> catalog = new HashMap<String, List<PseudonymizeOperation>>();
			
	public FhirPseudonymizer reset(String resource) {
		getResource(resource).clear();		
		return this;
	}
	
	public FhirPseudonymizer pseudonymizeReference(String resource, String... path) {		
		getResource(resource).add(new PseudonymizeOperation(path, PseudonymizeAction.REFERENCE));
		return this;
	}
	
	public FhirPseudonymizer hideIfPseudonymized(String resource, String... path) {
		getResource(resource).add(new PseudonymizeOperation(path, PseudonymizeAction.REMOVE));
		return this;
	}
	
	private List<PseudonymizeOperation> getResource(String resource) {
		List<PseudonymizeOperation> ops = catalog.get("fhir/"+resource);
		if (ops == null) {
			ops = new ArrayList<PseudonymizeOperation>();
			catalog.put("fhir/"+resource, ops);
		}
		return ops;
	}
	
	public void pseudonymize(DBRecord rec) throws AppException {
		String format = (String) rec.meta.get("format");
		List<PseudonymizeOperation> ops = catalog.get(format);
		if (rec.data != null && rec.data instanceof BasicBSONObject) {
			if (ops != null) {			
				for (PseudonymizeOperation op : ops) {
					apply(rec, rec.data, op, 0);
				}			
			}
		  
			pseudonymizeContained(rec, (BasicBSONObject) rec.data);
		}
		pseudonymizeAllReferences(rec, rec.data);
		
	}
	
	public void pseudonymizeContained(DBRecord rec, BasicBSONObject base) throws AppException {
		Object allContained = base.get("contained");
		if (allContained != null && allContained instanceof BasicBSONList) {
			BasicBSONList lst = (BasicBSONList) allContained;
			for (int i=0;i<lst.size();i++) {
				Object entry = lst.get(i);
				if (entry instanceof BasicBSONObject) {
					BasicBSONObject obj = (BasicBSONObject) entry;
					String type = obj.getString("resourceType");
					if (type != null) {
						apply(rec, obj, FhirPseudonymizer.patient, 0);
						apply(rec, obj, FhirPseudonymizer.subject, 0);
						List<PseudonymizeOperation> ops = catalog.get("fhir/"+type);
						if (ops != null) {			
							for (PseudonymizeOperation op : ops) {
								apply(rec, obj, op, 0);
							}			
						}
					}
					
					pseudonymizeContained(rec, obj);
				}
			}
		}		
	}
	
	public void pseudonymizeAllReferences(DBRecord rec, Object base) throws AppException {
	    if (base == null) return;
        if (base instanceof BasicBSONList) {       
            BasicBSONList lst = (BasicBSONList) base;
            for (int i=0;i<lst.size();i++) {
                Object entry = lst.get(i);
                pseudonymizeAllReferences(rec, entry);
            }
	   } else if (base instanceof BasicBSONObject) {
            BasicBSONObject obj = (BasicBSONObject) base;
            if (obj.containsField("reference")) {
                Object t = obj.get("reference");
                if (t != null && t instanceof String) {
                    String p = Feature_Pseudonymization.pseudonymizeRefOrNull(rec.context, (String) t);
                    if (p != null) {
                        obj.put("reference", p);
                        obj.remove("display");
                    }
                }
            }
            for (Object v : obj.values()) pseudonymizeAllReferences(rec, v);                    
        }       
    }
	
	public BasicBSONObject replaceReference(DBRecord rec, BasicBSONObject ref) throws AppException {		
		if (ref.get("reference") instanceof String) {
			String reference = ref.getString("reference");
			MidataId id = rec.context.getOwner();
			if (reference.indexOf("Patient/"+id.toString())>=0) {
				ref.put("reference", "Patient/"+rec.context.getOwnerPseudonymized().toString());
				ref.put("display", rec.context.getOwnerName());
			}
		}
		return ref;
	}
	
	public void apply(DBRecord rec, BSONObject data, PseudonymizeOperation op, int idx) throws AppException {
		Object v = data.get(op.path[idx]);
		if (v == null) return;
		if (idx == op.path.length - 1) {
			switch(op.action) {
			case REMOVE: data.removeField(op.path[idx]);break;
			case REFERENCE: 
				if (v instanceof BasicBSONObject) {
					data.put(op.path[idx], replaceReference(rec, (BasicBSONObject) v));
				} else if (v instanceof BasicBSONList) {
					BasicBSONList lst = (BasicBSONList) v;
					for (int i=0;i<lst.size();i++) {
						Object entry = lst.get(i);
						if (entry instanceof BSONObject) {
							lst.put(i, replaceReference(rec, (BasicBSONObject) entry));
						}
					}	
				}
			}
		} else {
			if (v instanceof BasicBSONObject) {
				apply(rec, data, op, idx+1);
			} else if (v instanceof BasicBSONList) {
				BasicBSONList list = (BasicBSONList) v;
				for (Object entry : list) {
					if (entry instanceof BSONObject) {
						apply(rec, (BSONObject) entry, op, idx+1);
					}
				}
			}
		}
	}
}

class PseudonymizeOperation {
	String[] path;
	PseudonymizeAction action;
	
	PseudonymizeOperation(String[] path, PseudonymizeAction action) {
		this.path = path;
		this.action = action;
	}
	
}

enum PseudonymizeAction {
	REFERENCE,
	REMOVE
}
