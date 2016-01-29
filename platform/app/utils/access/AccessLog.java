package utils.access;


import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import play.Logger;

/**
 * Currently only used for debugging 
 *
 */
public class AccessLog {

	public static boolean detailedLog = true;
	
	public static void apsAccess(ObjectId aps, ObjectId who) {
		Logger.debug("Access APS:"+(aps != null ? aps.toString() : "null")+" from user:"+(who!=null?who.toString():"null"));
	}
	
	public static void debug(String txt) {
		Logger.debug("                                            ".substring(0,ident)+txt);
	}
	
	public static void logQuery(Map<String,Object> properties, Set<String> fields) {
	   StringBuilder s = new StringBuilder();
	   boolean first = true;
	   for (String key : properties.keySet()) {
		   Object v = properties.get(key);
		   if (first) first = false; else s.append(",");
		   s.append(key);
		   s.append("=");
		   s.append(v != null ? v.toString() : "null");		   
	   }
	   s.append(" (");
	   for (String key : fields) {
		   s.append(key);
		   s.append(" ");
	   }
	   s.append(")");
	   Logger.debug("                                            ".substring(0,ident)+"Query:"+s.toString());
	}
	
	public static void logDB(String msg) {
	   Logger.debug("                                            ".substring(0,ident)+"DB:"+msg);
	}
	
	private static int ident = 0;
	
	public static void logBegin(String txt) {
		Logger.debug("                                            ".substring(0,ident)+txt);
		ident += 2;
	}
	
	public static void logEnd(String txt) {
		ident -= 2;
		Logger.debug("                                            ".substring(0,ident)+txt);		
	}
	
	/*
	public static void logLocalQuery(ObjectId aps, Map<String,Object> properties, Set<String> fields) {
		   StringBuilder s = new StringBuilder();
		   for (String key : properties.keySet()) {
			   Object v = properties.get(key);
			   s.append(key);
			   s.append("=");
			   s.append(v != null ? v.toString() : "null");
			   s.append(",");
		   }
		   for (String key : fields) {
			   s.append(key);
			   s.append(" ");
		   }
		   Logger.debug("Local Query:"+aps.toString()+" : "+s.toString());
	}
	
	public static void lookupSingle(ObjectId aps, ObjectId record, Map<String,Object> properties) {
		StringBuilder s = new StringBuilder();
		for (String key : properties.keySet()) {
			   s.append(key);
			   s.append("=");
			   s.append(properties.get(key).toString());
			   s.append(",");
		}
		Logger.debug("Single Lookup: APS="+aps.toString()+" Record="+record.toString()+" Query="+s.toString());
	}
	
	public static void logMap(Map properties) {
		StringBuilder s = new StringBuilder();
		for (Object key : properties.keySet()) {
			   s.append(key.toString());
			   s.append("=");
			   s.append(properties.get(key).toString());
			   s.append(",");
		}
		Logger.debug("TEST: "+s.toString());
	}
	
	public static void identified(ObjectId aps, ObjectId record) {
		  Logger.debug("Identified Record APS="+aps.toString()+" Record="+record.toString());
	}
	*/
	public static void decryptFailure(ObjectId record) {
		 Logger.debug("Decrypt Failure Record="+record.toString());
	}
}
