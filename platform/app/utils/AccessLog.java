package utils;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import models.enums.APSSecurityLevel;
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
	private static boolean logToFile = InstanceConfig.getInstance().getInstanceType().getLogToFile();
	
	/**
	 * keep log in memory (during request) for mail bug reports 
	 */
	private static boolean logForMail = true;
	
	private static int LOGSIZELIMIT = 1024 * 100;
	
	/**
	 * add a line of text to the log
	 * @param txt the line to be logged
	 */
	public static void log(String txt) {
		String msg = "                                            ".substring(0,ident.get())+txt;
		//if (logToFile) 
		Logger.debug(msg);
		if (logForMail) msgs.get().println(msg);
	}
	
	/**
	 * log an access to an APS
	 * @param aps the APS to be accessed
	 * @param who the person trying to access the APS
	 * @param lvl the security level of the APS
	 */
	public static void apsAccess(MidataId aps, MidataId who, APSSecurityLevel lvl) {
		String msg = "Access APS:"+(aps != null ? aps.toString() : "null")+" from user:"+(who!=null?who.toString():"null")+" security:"+lvl.toString();
		log(msg);		
	}
	
	
	/**
	 * log a query to be executed
	 * @param properties restrictions of query
	 * @param fields fields to be queries
	 */
	public static void logQuery(MidataId aps, Map<String,Object> properties, Set<String> fields) {	 
		
	   StringBuilder s = new StringBuilder();
	   
	   s.append("aps="+aps.toString());
	   for (Map.Entry<String,Object> entry : properties.entrySet()) {
		   Object v = entry.getValue();
		   s.append(",");
		   s.append(entry.getKey());
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
	   //if (logToFile) Logger.debug(msg);
	   if (logForMail) log(msg);
	}
	
	/**
	 * log an DB access
	 * @param msg the message to be logged
	 */
	public static void logDB(String msg) {
	   String msg1 = "                                            ".substring(0,ident.get())+"DB:"+msg; 
	   //if (logToFile) Logger.debug(msg1);
	   if (logForMail) log(msg1);
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
		if (logToFile) {
		  Logger.debug(getReport());
		}
		ident.set(0);
		LogContext context = msgs.get();
		if (context != null) {
			context.writer.close();
			try {
			  context.head.close();
			} catch (IOException e) {}
			try {
			  if (context.tail != null) context.tail.close();			  
			} catch (IOException e) {}
			context.head = null;
			context.tail = null;
		}
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
	public static void decryptFailure(MidataId record) {
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
		StringWriter head;
		StringWriter tail;
		int len = 0;
		int cleared = 0;
		
		LogContext() {
			head = new StringWriter();		
			writer = new PrintWriter(head);
		}
		
		void println(String line) {
			len += line.length();
			if (len > LOGSIZELIMIT) {
				writer.close();				
				if (tail == null) {
					tail = new StringWriter();					
					writer = new PrintWriter(tail);
					len = 0;
				} else {
					cleared++;
					tail = new StringWriter();					
					writer = new PrintWriter(tail);
					len = 0;
					writer.println("...(skipping "+cleared+" blocks)...");
				}
			}
			writer.println(line);
		}
		
		public String toString() {
			if (tail == null) return head.toString();
			else return head.toString() + tail.toString();
		}
	}
	
	/**
	 * retrieve the log of the current request for error reporting
	 * @return
	 */
	public static String getReport() {
		return msgs.get().toString();
	}
}
