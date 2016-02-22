package utils.access;


import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;

import org.bson.types.ObjectId;
import play.Logger;
import utils.exceptions.AuthException;

/**
 * Currently only used for debugging 
 *
 */
public class AccessLog {

	public static boolean detailedLog = true;
	
	public static void apsAccess(ObjectId aps, ObjectId who, APSSecurityLevel lvl) {
		Logger.debug("Access APS:"+(aps != null ? aps.toString() : "null")+" from user:"+(who!=null?who.toString():"null")+" security:"+lvl.toString());
	}
	
	public static void debug(String txt) {
		Logger.debug("                                            ".substring(0,ident.get())+txt);
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
	   Logger.debug("                                            ".substring(0,ident.get())+"Query:"+s.toString());
	}
	
	public static void logDB(String msg) {
	   Logger.debug("                                            ".substring(0,ident.get())+"DB:"+msg);
	}
	
	private static ThreadLocal<Integer> ident = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() {
            return 0;
        }
	};
	
	public static void logBegin(String txt) {
		Logger.debug("                                            ".substring(0,ident.get())+txt);
		ident.set(ident.get() + 2);
	}
	
	public static void logEnd(String txt) {
		ident.set(ident.get() - 2);
		if (ident.get() < 0) ident.set(0);
		Logger.debug("                                            ".substring(0,ident.get())+txt);		
	}
	

	public static void decryptFailure(ObjectId record) {
		 Logger.debug("Decrypt Failure Record="+record.toString());
	}
	
	public static void decryptFailure(AuthException e) {
		 Logger.error("APS Access failure", e);
	}
}
