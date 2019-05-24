package utils.fhir;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;

public class FHIRVersionConvert {

	public final static int MODE_RENAME = 0;
	public final static int MODE_IDENTIFIER_TO_STRING = 1;
	public final static int MODE_STRING_TO_ANNOTATIONS = 2;
	
	public final static Date FHIR_R4_RELEASE = new Date(1558602161933l);
	
	public static boolean doConvert(MidataId id) {
		return id.getCreationDate().before(FHIR_R4_RELEASE);
	}
	
	public static void rename(Object in, String oldName, String newName, String...path) {
	  change(in, path, 0, oldName, newName, MODE_RENAME);	
	}
	
	public static void rename(Object in, int mode, String oldName, String newName, String...path) {
		  change(in, path, 0, oldName, newName, mode);	
	}
	
	protected static Object read(Object obj, String path[]) {		
		for (int i=0;i<path.length;i++) {
	    	if (obj == null) return null;
	    	if (obj instanceof BasicBSONObject) {
	    		obj = ((BasicBSONObject) obj).get(path[i]);
	    	} else if (obj instanceof BasicBSONList) {
	    		if (((BasicBSONList) obj).isEmpty()) return null;
	    	}
	    }
		return obj;
	}
	
	public static void change(Object in, String[] path, int idx, String oldName, String newName, int mode) {
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
		    } else if (obj.containsField(oldName)) {
		    	switch (mode) {
		    	case MODE_RENAME :
		    		obj.put(newName, obj.get(oldName));
					obj.removeField(oldName);
					break;
		    	case MODE_IDENTIFIER_TO_STRING:
		    		Object old = obj.get(oldName);
		    		if (old != null && old instanceof BasicBSONObject) {
		    			obj.put(newName, ((BasicBSONObject) old).get("value"));
		    			obj.removeField(oldName);
		    		}
		    		break;
		    	case MODE_STRING_TO_ANNOTATIONS:
		    		Object old_string = obj.get(oldName);
		    		if (old_string != null && old_string instanceof String) {
		    			obj.removeField(oldName);
		    			BasicBSONList lst = new BasicBSONList();
		    			BasicBSONObject entry = new BasicBSONObject();
		    			lst.add(entry);
		    			entry.append("text", old_string);
		    			obj.put(newName, entry);
		    		}
		    		break;
		    	}
				
			}
		}
	}
}
