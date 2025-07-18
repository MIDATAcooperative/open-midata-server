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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
		int id = ident.get().size()*2;
		if (id > "                                            ".length()) {
			System.out.println(getReport());			
		}
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.println(txt);
		}
	}
	
	public static void log(String txt, String txt2) {
		int id = ident.get().size()*2;
		if (id > "                                            ".length()) {
			System.out.println(getReport());			
		}
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.println(txt2);
		}
	}
	
	public static void log(String txt, String txt2, String txt3) {
		int id = ident.get().size()*2;
		if (id > "                                            ".length()) {
			System.out.println(getReport());			
		}
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.print(txt2);
			c.println(txt3);
		}
	}
	
	public static void log(String txt, String txt2, String txt3, String txt4) {
		int id = ident.get().size()*2;
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.print(txt2);
			c.print(txt3);
			c.println(txt4);
		}
	}
	
	public static void log(String txt, String txt2, String txt3, String txt4, String txt5) {
		int id = ident.get().size()*2;
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.print(txt2);
			c.print(txt3);
			c.print(txt4);
			c.println(txt5);
		}
	}
	
	public static void log(String txt, String txt2, String txt3, String txt4, String txt5, String txt6) {
		int id = ident.get().size()*2;
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.print(txt2);
			c.print(txt3);
			c.print(txt4);
			c.print(txt5);
			c.println(txt6);
		}
	}
	
	public static void log(String txt, String txt2, String txt3, String txt4, String txt5, String txt6, String txt7) {
		int id = ident.get().size()*2;
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.print(txt2);
			c.print(txt3);
			c.print(txt4);
			c.print(txt5);
			c.print(txt6);
			c.println(txt7);
		}
	}
	
	public static void log(String txt, String txt2, String txt3, String txt4, String txt5, String txt6, String txt7, String txt8) {
		int id = ident.get().size()*2;
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			c.print(txt);
			c.print(txt2);
			c.print(txt3);
			c.print(txt4);
			c.print(txt5);
			c.print(txt6);
			c.print(txt7);
			c.println(txt8);
		}
	}
	
	public static void log(String... txt) {
		int id = ident.get().size()*2;
		String msg = "                                            ".substring(0,id);	
		if (logForMail) {
			LogContext c = msgs.get();
			c.print(msg);
			for (int i=0;i<txt.length-1;i++) c.print(txt[i]);			
			c.println(txt[txt.length-1]);
		}
	}
		
	
	/**
	 * log an access to an APS
	 * @param aps the APS to be accessed
	 * @param who the person trying to access the APS
	 * @param lvl the security level of the APS
	 */
	public static void apsAccess(MidataId aps, MidataId who, APSSecurityLevel lvl) {
		String msg = "Access APS:"+(aps != null ? aps.toString() : "null")+" from user:"+(who!=null?who.toString():"null")+" security:"+lvl.toString();
		log("Access APS:",(aps != null ? aps.toString() : "null")," from user:",(who!=null?who.toString():"null")," security:",lvl.toString());		
	}
	
	
	/**
	 * log a query to be executed
	 * @param properties restrictions of query
	 * @param fields fields to be queries
	 */
	public static void logQuery(MidataId aps, Map<String,Object> properties, Set<String> fields) {	 
		
	   StringBuilder s = new StringBuilder(500);
	   
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
	   log("Query:",s.toString());
	}
	
	/**
	 * log an DB access
	 * @param msg the message to be logged
	 */
	public static void logDB(String msg) { 	   
	   log("DB:",msg);
	}
	public static void logDB(String msg, String p1) { 	   
	   log("DB:",msg, p1);
	}
	public static void logDB(String msg, String p1, String p2) { 	   
	   log("DB:",msg, p1, p2);
	}
	public static void logDB(String msg, String p1, String p2, String p3) { 	   
	   log("DB:",msg, p1, p2, p3);
	}
	public static void logDB(String msg, String p1, String p2, String p3, String p4) { 	   
	   log("DB:",msg, p1, p2, p3, p4);
	}
	public static void logDB(String msg, String p1, String p2, String p3, String p4, String p5) { 	   
	   log("DB:",msg, p1, p2, p3, p4, p5);
	}
	public static void logDB(String msg, String p1, String p2, String p3, String p4, String p5, String p6, String p7) { 	   
		   log("DB:",msg, p1, p2, p3, p4, p5, p6, p7);
	}
	
	
	private static ThreadLocal<Deque<Long>> ident = new ThreadLocal<Deque<Long>>() {
        @Override protected Deque<Long> initialValue() {
            return new ArrayDeque<Long>();
        }
	};
	
	private static ThreadLocal<Stack<String>> logpath = new ThreadLocal<Stack<String>>() {
        @Override protected Stack<String> initialValue() {
        	Stack<String> result = new Stack<String>();
        	result.add("");
            return result;
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
		ident.get().clear();		
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
		ident.get().push(System.currentTimeMillis());
	}
	public static void logBegin(String txt, String txt2) {
		log(txt, txt2);		
		ident.get().push(System.currentTimeMillis());
	}
	public static void logBegin(String txt, String txt2, String txt3) {
		log(txt, txt2, txt3);		
		ident.get().push(System.currentTimeMillis());
	}
	public static void logBegin(String txt, String txt2, String txt3, String txt4) {
		log(txt, txt2, txt3, txt4);		
		ident.get().push(System.currentTimeMillis());
	}
	public static void logBegin(String txt, String txt2, String txt3, String txt4, String txt5) {
		log(txt, txt2, txt3, txt4, txt5);		
		ident.get().push(System.currentTimeMillis());
	}
	public static void logBegin(String txt, String txt2, String txt3, String txt4, String txt5, String txt6) {
		log(txt, txt2, txt3, txt4, txt5, txt6);		
		ident.get().push(System.currentTimeMillis());
	}
	public static void logBegin(String... txt) {
		log(txt);		
		ident.get().push(System.currentTimeMillis());
	}
	
	private static String path() {
		Stack<String> path = logpath.get();
		return path.peek();
	}
	
	public static void logBeginPath(String path, String extra) {				
		logBegin("start ",path.toUpperCase()," on ",path(),(extra!=null?(": "+extra):""));
		logpath.get().add(path()+"/"+path);		
	}
	
	public static void logEndPath(String result) {				 		
		String lp = logpath.get().pop();
		int i = lp.lastIndexOf('/');
		logEnd("end ",lp.substring(i+1).toUpperCase()," on ",lp,(result!=null?(" with "+result):""));
	}
	
	public static void logPath(String result) {		
		String lp = path();		
		log(lp,": ",result);
	}
	
	public static String lp() {
		return path();
	}
	
	/**
	 * log the end of an operation that was started using logBegin()
	 * @param txt a message to be logged
	 */
	public static void logEnd(String txt) {
		long ts = System.currentTimeMillis() - ident.get().pop();			
		log(txt, " ts=", Long.toString(ts));			
	}
	public static void logEnd(String txt, String txt2) {
		long ts = System.currentTimeMillis() - ident.get().pop();		
		log(txt, txt2, " ts=", Long.toString(ts));			
	}
	public static void logEnd(String txt, String txt2, String txt3) {
		long ts = System.currentTimeMillis() - ident.get().pop();
		log(txt, txt2, txt3, " ts=", Long.toString(ts));			
	}
	public static void logEnd(String txt, String txt2, String txt3, String txt4) {
		long ts = System.currentTimeMillis() - ident.get().pop();
		log(txt, txt2, txt3, txt4, " ts=", Long.toString(ts));			
	}
	public static void logEnd(String txt, String txt2, String txt3, String txt4, String txt5) {
		long ts = System.currentTimeMillis() - ident.get().pop();
		log(txt, txt2, txt3, txt4, txt5, " ts=", Long.toString(ts));			
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
		 log("Decrypt Failure Record=", record.toString());
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
			head = new StringWriter(500);		
			writer = new PrintWriter(head);
		}
		
		void print(String part) {
		   len += part.length();
		   writer.print(part);
		}
		
		void println(String line) {
			len += line.length();
			if (len > LOGSIZELIMIT) {
				writer.close();				
				if (tail == null) {
					tail = new StringWriter(1024);					
					writer = new PrintWriter(tail);
					len = 0;
				} else {
					cleared++;					
					tail = new StringWriter(1024);					
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
