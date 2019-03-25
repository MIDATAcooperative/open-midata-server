package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import models.MidataId;
import models.Plugin;
import play.mvc.Http;
import utils.auth.PortalSessionToken;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
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
		     user = tsk.getRole().toString()+" "+tsk.getOwnerId().toString();
		   } 
		} else path = "[internal] "+InstanceConfig.getInstance().getPortalServerDomain();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
		if (e!=null) AccessLog.logException("Uncatched Exception:", e);
		String txt = "Instance: "+InstanceConfig.getInstance().getPortalServerDomain()+"\nTime:"+timeStamp+"\nInterface: "+fromWhere+"\nPortal Session: "+user+"\nPath: "+path+"\n\n"+AccessLog.getReport();
		MailUtils.sendTextMail(bugReportEmail, bugReportName, "Error Report: "+path, txt);
		if (e!=null) Stats.addComment("Error: "+e.getClass().getName()+": "+e.getMessage());
	}
	
	public static void reportPluginProblem(String fromWhere, Http.Context ctx, PluginException e) {
		try {
			MidataId pluginId = e.getPluginId();
			if (pluginId==null) {
				report(fromWhere, ctx, e);
				return;
			}
			
			Plugin plg = Plugin.getById(pluginId, Sets.create("creatorLogin", "name", "filename", "sendReports"));
			if (plg==null || plg.creatorLogin==null) {
				report(fromWhere, ctx, e);
				return;
			}
	
			String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
			String txt = "Dear Developer,\n\non "+timeStamp+"\nthe plugin/app called '"+plg.name+"' (internal: '"+plg.filename+"')\non the MIDATA instance at '"+InstanceConfig.getInstance().getPortalServerDomain()+"'\nhas caused this error:\n\n"+e.getMessage()+"\n\nThis is an automated email send by the MIDATA platform.\nYou can turn off reporting for this application in the application settings.";				
			
			if (plg.sendReports) {						
				MailUtils.sendTextMail(plg.creatorLogin, plg.creatorLogin, "Error Report: ["+plg.name+"] "+InstanceConfig.getInstance().getPortalServerDomain(), txt);
			} 
			
			MailUtils.sendTextMail(bugReportEmail, bugReportName, "Error Report: ["+plg.name+"] "+InstanceConfig.getInstance().getPortalServerDomain(), txt);
			if (e!=null) Stats.addComment("Error: "+e.getClass().getName()+": "+e.getMessage());
		} catch (InternalServerException e2) {
			report(fromWhere, ctx, e2);
		}
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
		     user = tsk.getRole().toString()+" "+tsk.getOwnerId().toString();
		   } 
		} else path = "[internal] "+InstanceConfig.getInstance().getPortalServerDomain();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
		
		String txt = "Instance: "+InstanceConfig.getInstance().getPortalServerDomain()+"\nTime:"+timeStamp+"\nInterface: "+fromWhere+"\nPortal Session: "+user+"\nPath: "+path+"\nExecution Time: "+duration+"ms\n\n"+AccessLog.getReport();
		MailUtils.sendTextMail(bugReportEmail, bugReportName, "Bad Performance: "+path, txt);		
	}
}
