package utils;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;

import org.bson.types.ObjectId;
import play.Logger;
import utils.exceptions.AuthException;

/**
 * Intra-Request logging
 *
 */
public class AccessLog {

	/**
	 * log more detailed?
	 */
	public static boolean detailedLog = true;
	
	/**
	 * log into activator log file
	 */
	private static boolean logToFile = true;
	
	/**
	 * keep log in memory (during request) for mail bug reports 
	 */
	private static boolean logForMail = true;
	
	/**
	 * add a line of text to the log
	 * @param txt the line to be logged
	 */
	public static void log(String txt) {
		String msg = "                                            ".substring(0,ident.get())+txt;
		if (logToFile) Logger.debug(msg);
		if (logForMail) msgs.get().writer.println(msg);
	}
	
	/**
	 * log an access to an APS
	 * @param aps the APS to be accessed
	 * @param who the person trying to access the APS
	 * @param lvl the security level of the APS
	 */
	public static void apsAccess(ObjectId aps, ObjectId who, APSSecurityLevel lvl) {
		String msg = "Access APS:"+(aps != null ? aps.toString() : "null")+" from user:"+(who!=null?who.toString():"null")+" security:"+lvl.toString();
		log(msg);		
	}
	
	
	/**
	 * log a query to be executed
	 * @param properties restrictions of query
	 * @param fields fields to be queries
	 */
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
	   String msg = "                                            ".substring(0,ident.get())+"Query:"+s.toString();
	   if (logToFile) Logger.debug(msg);
	   if (logForMail) msgs.get().writer.println(msg);
	}
	
	/**
	 * log an DB access
	 * @param msg the message to be logged
	 */
	public static void logDB(String msg) {
	   String msg1 = "                                            ".substring(0,ident.get())+"DB:"+msg; 
	   if (logToFile) Logger.debug(msg1);
	   if (logForMail) msgs.get().writer.println(msg1);
	}
	
	private static ThreadLocal<Integer> ident = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() {
            return 0;
        }
	};
	
	private static ThreadLocal<LogContext> msgs = new ThreadLocal<LogContext>() {
        @Override protected LogContext initialValue() {
            return new LogContext();
        }
	};
	
	/**
	 * start a new quest. Clears the currently stored information.
	 */
	public static void newRequest() {
		ident.set(0);
		msgs.set(new LogContext());
	}
	
	/**
	 * log the beginning of an operation that will be ended using logEnd()
	 * @param txt a message to be logged
	 */
	public static void logBegin(String txt) {
		log(txt);		
		ident.set(ident.get() + 2);
	}
	
	/**
	 * log the end of an operation that was started using logBegin()
	 * @param txt a message to be logged
	 */
	public static void logEnd(String txt) {
		ident.set(ident.get() - 2);
		if (ident.get() < 0) ident.set(0);
		log(txt);			
	}
	
	/**
	 * log an exception that happend during execution
	 * @param msg a message to be logged
	 * @param e the exception to be logged
	 */
	public static void logException(String msg, Exception e) {
		if (logForMail) {
			msgs.get().writer.println(msg);
			e.printStackTrace(msgs.get().writer);
		}
		if (logToFile) Logger.error(msg, e);
	}
	
    /**
     * log an APS decryption failure
     * @param record the record that could not be decrypted
     */
	public static void decryptFailure(ObjectId record) {
		 log("Decrypt Failure Record="+record.toString());
	}
	
	/**
	 * log an AuthException
	 * @param e the exception to be logged
	 */
	public static void decryptFailure(AuthException e) {		
		 log("APS Access failure");
		 if (logForMail) e.printStackTrace(msgs.get().writer);
		 if (logToFile) Logger.error("", e);
	}
	
	private static class LogContext {
		PrintWriter writer;
		StringWriter out;
		
		LogContext() {
			out = new StringWriter();
			writer = new PrintWriter(out);
		}
	}
	
	/**
	 * retrieve the log of the current request for error reporting
	 * @return
	 */
	public static String getReport() {
		return msgs.get().out.toString();
	}
}
