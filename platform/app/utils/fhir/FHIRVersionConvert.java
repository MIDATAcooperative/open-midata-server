/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;

public class FHIRVersionConvert {

	public final static int MODE_RENAME = 0;	
	public final static int MODE_STRING_TO_ANNOTATIONS = 2;
	
	public final static int CONVERT_RELATED = 1;
	
	public final static Date FHIR_R4_RELEASE = new Date(1558602161933l);
	
	private final static String[] emptyString = new String[0];
	
	public static boolean doConvert(MidataId id) {
		return id.getCreationDate().before(FHIR_R4_RELEASE);
	}
	
	public static void rename(Object in, String oldName, String newName, String...path) {
	  String old[] = oldName.split("\\.");
	  for (BasicBSONObject obj : select(in, path)) {
		 rename(obj, old, newName); 
	  }	  
	}
			
	public static void nest(Object in, String oldName, String newName, String nested, String...path) {
		String old[] = oldName.split("\\.");
		for (BasicBSONObject obj : select(in, path)) {
		   stringToComplex(obj, old, newName, nested); 
		}	
	}
		
	protected static Object read(Object obj, String path) {		
		return read(obj, new String[] { path });
	}
	
	protected static Object read(Object obj, String path[]) {		
		for (int i=0;i<path.length;i++) {
	    	if (obj == null) return null;
	    	if (obj instanceof BasicBSONObject) {
	    		obj = ((BasicBSONObject) obj).get(path[i]);
	    	} else if (obj instanceof BasicBSONList) {
	    		if (((BasicBSONList) obj).isEmpty()) return null;
	    		((BasicBSONList) obj).get(0);
	    	}
	    }
		return obj;
	}
	
	protected static void addToArray(Object obj, String name, Object content) {
		if (obj == null) return;
		if (obj instanceof BasicBSONObject) {
			Object array = ((BasicBSONObject) obj).get(name);
			if (array != null && array instanceof BasicBSONList) {
				((BasicBSONList) array).add(content);
			} else {
				BasicBSONList lst = new BasicBSONList();
				lst.add(content);
				((BasicBSONObject) obj).put(name, lst);
		    }
		}
	}
	
	protected static boolean has(Object obj, String name, String value) {
		if (obj instanceof BasicBSONObject) {
			Object val = ((BasicBSONObject) obj).get(name);
			if (val != null && val.equals(value)) return true;
		}
		return false;
	}
	
	protected static BasicBSONList getList(Object obj, String name) {
		if (obj instanceof BasicBSONObject) {
			Object target = ((BasicBSONObject) obj).get(name);
			if (target != null && target instanceof BasicBSONList) return (BasicBSONList) target;
		}
		return null;
	}
	
	protected static List<BasicBSONObject> select(Object in, String[] path) {
		return select(in, path, 0);
	}
	
	protected static List<BasicBSONObject> select(Object in, String[] path, int idx) {
		List<BasicBSONObject> result = Collections.emptyList();
		if (in == null) return result;
		
		if (in instanceof BasicBSONList) {
		  	BasicBSONList list = ((BasicBSONList) in);
		  	for (Object member : list) {
		  		List<BasicBSONObject> part = select(member, path, idx);
		  		if (result.isEmpty()) result = part; 
		  		else if (!part.isEmpty()) result.addAll(part);
		  	}
		}
		
		if (in instanceof BasicBSONObject) {
			BasicBSONObject obj = ((BasicBSONObject) in);
		    if (idx < path.length) {
			   Object target = obj.get(path[idx]);
			   List<BasicBSONObject> part = select(target, path, idx+1);
			   if (result.isEmpty()) result = part;
			   else if (!part.isEmpty()) result.addAll(part);			   
		    } else {
		    	if (result.isEmpty()) result = new ArrayList<BasicBSONObject>();
		    	result.add(obj);
		    			    	
		    }
		}
		
		return result;		    			
	}
	
	public static void rename(BasicBSONObject obj, String oldName[], String newName) {
		Object old = read(obj, oldName);
		if (old != null) {
    		obj.put(newName, old);
			obj.removeField(oldName[0]);
		}
	}
	
	public static void stringToComplex(BasicBSONObject obj, String[] oldName, String newName, String nestedName) {
		Object old_string = read(obj, oldName);
		if (old_string != null && old_string instanceof String) {
			obj.removeField(oldName[0]);		    			
			BasicBSONObject entry = new BasicBSONObject();		    			
			entry.append(nestedName, old_string);		    			
			addToArray(obj, newName, entry);
		}
	}
	
	/*public static void change(Object in, String[] path, int idx, String oldName[], String newName, int mode) {
		if (in == null) return;
		
		if (in instanceof BasicBSONList) {
		  	BasicBSONList list = ((BasicBSONList) in);
		  	for (Object member : list) {
		  		change(member, path, idx, oldName, newName, mode);
		  	}
		}
		
		if (in instanceof BasicBSONObject) {
			BasicBSONObject obj = ((BasicBSONObject) in);
		    if (idx < path.length) {
			   Object target = obj.get(path[idx]);
			   change(target, path, idx+1, oldName, newName, mode);
		    } else if (obj.containsField(oldName[0])) {
		    	switch (mode) {
		    	case MODE_RENAME :
		    		Object old = read(obj, oldName);
		    		if (old != null) {
			    		obj.put(newName, old);
						obj.removeField(oldName[0]);
		    		}
					break;		    	
		    	case MODE_STRING_TO_ANNOTATIONS:
		    		Object old_string = read(obj, oldName);
		    		if (old_string != null && old_string instanceof String) {
		    			obj.removeField(oldName[0]);		    			
		    			BasicBSONObject entry = new BasicBSONObject();		    			
		    			entry.append("text", old_string);		    			
		    			addToArray(obj, newName, entry);
		    		}
		    		break;
		    	}		    			    	
				
			}
		}
	}*/
		
	public static void convertRelated(Object in) {		
		if (in instanceof BasicBSONObject) {		
			BasicBSONObject obj = ((BasicBSONObject) in);
		  
		  	BasicBSONList related = getList(obj, "related");
		  	if (related != null) {		  		
		  		for (Object elem : related) {
		  			if (has(elem, "type", "has-member")) {		  			
		  				addToArray(obj, "hasMember", read(elem, "target"));
		  			}
		  			if (has(elem, "type", "derived-from")) {
		  				addToArray(obj, "derivedFrom", read(elem, "target"));
		  			}
		  		}		    			
		  	}
		}
	}
	
	public static void convertCodeToCodesystem(Object in, String field, String system) {
		if (in == null) return;
		
		if (in instanceof BasicBSONObject) {
			BasicBSONObject obj = ((BasicBSONObject) in);
		    Object oldVal = read(obj, field);
		    if (oldVal instanceof String) {
		    	BasicBSONObject coding = new BasicBSONObject("system", system);
		    	coding.append("code", oldVal);
		    	BasicBSONList arr = new BasicBSONList();
		    	arr.add(coding);
		    	BasicBSONObject cc = new BasicBSONObject("coding", arr);
		    	obj.put(field, cc);			
			}
		}
	}
}
