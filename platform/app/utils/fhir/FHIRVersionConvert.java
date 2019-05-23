package utils.fhir;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

public class FHIRVersionConvert {

	public final static int MODE_RENAME = 0;
	public final static int MODE_IDENTIFIER_TO_STRING = 1;	
	
	public static void rename(Object in, String oldName, String newName, String...path) {
	  change(in, path, 0, oldName, newName, MODE_RENAME);	
	}
	
	public static void rename(Object in, int mode, String oldName, String newName, String...path) {
		  change(in, path, 0, oldName, newName, mode);	
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
		    	
		    	}
				
			}
		}
	}
}
