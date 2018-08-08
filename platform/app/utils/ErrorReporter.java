package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import play.mvc.Http;
import utils.auth.PortalSessionToken;
import utils.messaging.MailUtils;
import utils.stats.Stats;

/**
 * Sends error reports
 *
 */
public class ErrorReporter {

	private static String bugReportEmail = InstanceConfig.getInstance().getConfig().getString("errorreports.targetemail");
	private static String bugReportName = InstanceConfig.getInstance().getConfig().getString("errorreports.targetname");
	private static volatile long lastReport = 0;
	
	/**
	 * Report an exception
	 * @param fromWhere name of API
	 * @param ctx http context or null
	 * @param e Exception to be reported
	 */
	public static void report(String fromWhere, Http.Context ctx, Exception e) {
		long now = System.currentTimeMillis();
		if (now - lastReport < 1000 * 60) return;
		lastReport = now;
		String path = "none";
		String user = "none";
		if (ctx != null) {
		   path = "["+ctx.request().method()+"] "+ctx.request().host()+ctx.request().path();
		   PortalSessionToken tsk = PortalSessionToken.session();
		   if (tsk != null) {
		     user = tsk.getRole().toString()+" "+tsk.getUserId().toString();
		   } 
		} else path = "[internal] "+InstanceConfig.getInstance().getPortalServerDomain();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
		if (e!=null) AccessLog.logException("Uncatched Exception:", e);
		String txt = "Instance: "+InstanceConfig.getInstance().getPortalServerDomain()+"\nTime:"+timeStamp+"\nInterface: "+fromWhere+"\nPortal Session: "+user+"\nPath: "+path+"\n\n"+AccessLog.getReport();
		MailUtils.sendTextMail(bugReportEmail, bugReportName, "Error Report: "+path, txt);
		if (e!=null) Stats.addComment("Error: "+e.getClass().getName()+": "+e.getMessage());
	}
	
	public static void reportPerformance(String fromWhere, Http.Context ctx, long duration) {
		long now = System.currentTimeMillis();
		if (now - lastReport < 1000 * 60) return;
		lastReport = now;
		String path = "none";
		String user = "none";
		if (ctx != null) {
		   path = "["+ctx.request().method()+"] "+ctx.request().host()+ctx.request().path();
		   PortalSessionToken tsk = PortalSessionToken.session();
		   if (tsk != null) {
		     user = tsk.getRole().toString()+" "+tsk.getUserId().toString();
		   } 
		} else path = "[internal] "+InstanceConfig.getInstance().getPortalServerDomain();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
		
		String txt = "Instance: "+InstanceConfig.getInstance().getPortalServerDomain()+"\nTime:"+timeStamp+"\nInterface: "+fromWhere+"\nPortal Session: "+user+"\nPath: "+path+"\nExecution Time: "+duration+"ms\n\n"+AccessLog.getReport();
		MailUtils.sendTextMail(bugReportEmail, bugReportName, "Bad Performance: "+path, txt);		
	}
}
