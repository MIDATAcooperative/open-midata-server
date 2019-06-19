package utils.fhir;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import utils.access.op.Condition;

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
	  change(in, path, 0, oldName.split("\\."), newName, MODE_RENAME);	
	}
			
	public static void rename(Object in, int mode, String oldName, String newName, String...path) {
		  change(in, path, 0, oldName.split("\\."), newName, mode);	
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
			}
		} else {
			BasicBSONList lst = new BasicBSONList();
			lst.add(content);
			((BasicBSONList) obj).put(name, lst);
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
		if (obj instanceof BasicBSONList) {
			Object target = ((BasicBSONList) obj).get(name);
			if (target != null && target instanceof BasicBSONList) return (BasicBSONList) target;
		}
		return null;
	}
	
	public static void change(Object in, String[] path, int idx, String oldName[], String newName, int mode) {
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
	}
		
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
