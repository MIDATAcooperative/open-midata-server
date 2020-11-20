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

package utils;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import models.enums.APSSecurityLevel;
import play.Logger;
import play.Logger.ALogger;
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
	
	private static final ALogger application = Logger.of("application");
	private static final ALogger index = Logger.of("index");
	private static final ALogger jobs = Logger.of("jobs");
	
	/**
	 * add a line of text to the log
	 * @param txt the line to be logged
	 */
	public static void log(String txt) {
		String msg = "                                            ".substring(0,ident.get())+txt;	
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
	   log("Query:"+s.toString());
	}
	
	/**
	 * log an DB access
	 * @param msg the message to be logged
	 */
	public static void logDB(String msg) { 	   
	   log("DB:"+msg);
	}
	
	private static ThreadLocal<Integer> ident = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() {
            return 0;
        }
	};
	
	private static ThreadLocal<String> logpath = new ThreadLocal<String>() {
        @Override protected String initialValue() {
            return "";
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
		LogContext context = msgs.get();
		if (context != null && logToFile) {		  
		  String report = context.toString();
		  if (report.length()>0) context.context.debug(report);
		}
		ident.set(0);		
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
	
	public static void logStart(String context, String operation) {
		LogContext ctx = msgs.get();
		if ("index".equals(context)) ctx.context = index;
		else if ("jobs".equals(context)) ctx.context = jobs;
		else ctx.context = application;
		
		ctx.println(operation);
	}
	
	/**
	 * log the beginning of an operation that will be ended using logEnd()
	 * @param txt a message to be logged
	 */
	public static void logBegin(String txt) {
		log(txt);		
		ident.set(ident.get() + 2);
	}
	
	public static void logBeginPath(String path, String extra) {				
		logBegin("start "+path.toUpperCase()+" on "+logpath.get()+(extra!=null?(": "+extra):""));
		logpath.set(logpath.get()+"/"+path);		
	}
	
	public static void logEndPath(String result) {		
		String lp = logpath.get();
		int i = lp.lastIndexOf('/');
		logpath.set(lp.substring(0, i));
		logEnd("end "+lp.substring(i+1).toUpperCase()+" on "+lp+(result!=null?(" with "+result):""));
	}
	
	public static void logPath(String result) {		
		String lp = logpath.get();		
		log(lp+": "+result);
	}
	
	public static String lp() {
		return logpath.get();
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
		if (logToFile) msgs.get().context.error(msg, e);
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
		 if (logToFile) msgs.get().context.error("", e);
	}
	
	private static class LogContext {
		PrintWriter writer;
		StringWriter head;
		StringWriter tail;
		int len = 0;
		int cleared = 0;
		ALogger context = jobs;
		
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
