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

import java.text.SimpleDateFormat;
import java.util.Date;

import models.Developer;
import models.MidataId;
import models.Plugin;
import models.enums.UserStatus;
import play.mvc.Http.Request;
import utils.auth.PortalSessionToken;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.messaging.MailSenderType;
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
	public static void report(String fromWhere, Request request, Exception e) {
		long now = System.currentTimeMillis();
		if (now - lastReport < 1000 * 60) return;
		lastReport = now;
		String path = "none";
		String user = "none";
		if (request != null) {
		   path = "["+request.method()+"] "+request.host()+request.path();
		   PortalSessionToken tsk = PortalSessionToken.session();
		   if (tsk != null) {
		     user = tsk.getRole().toString()+" "+tsk.getOwnerId().toString();
		   } 
		} else path = "[internal] "+InstanceConfig.getInstance().getPortalServerDomain();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
		if (e!=null) AccessLog.logException("Uncatched Exception:", e);
		String txt = "Instance: "+InstanceConfig.getInstance().getPortalServerDomain()+"\nTime:"+timeStamp+"\nInterface: "+fromWhere+"\nPortal Session: "+user+"\nPath: "+path+"\n\n"+AccessLog.getReport();
		MailUtils.sendTextMailAsync(MailSenderType.STATUS, bugReportEmail, bugReportName, "Error Report: "+path, txt);
		if (e!=null) Stats.addComment("Error: "+e.getClass().getName()+": "+e.getMessage());
	}
	
	public static void reportPluginProblem(String fromWhere, Request request, PluginException e) {
		try {
			MidataId pluginId = e.getPluginId();
			if (pluginId==null) {
				report(fromWhere, request, e);
				return;
			}
			
			Plugin plg = Plugin.getById(pluginId, Sets.create("creatorLogin", "name", "filename", "sendReports", "developerTeam"));
			if (plg==null || plg.creatorLogin==null) {
				report(fromWhere, request, e);
				return;
			}
	
			String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
			String txt = "Dear Developer,\n\non "+timeStamp+"\nthe plugin/app called '"+plg.name+"' (internal: '"+plg.filename+"')\non the MIDATA instance at '"+InstanceConfig.getInstance().getPortalServerDomain()+"'\nhas caused this error:\n\n"+e.getMessage()+"\n\nThis is an automated email send by the MIDATA platform.\nYou can turn off reporting for this application in the application settings.";				
			
			if (plg.sendReports) {						
				MailUtils.sendTextMailAsync(MailSenderType.STATUS, plg.creatorLogin, plg.creatorLogin, "Error Report: ["+plg.name+"] "+InstanceConfig.getInstance().getPortalServerDomain(), txt);
				
				if (plg.developerTeam != null) {
					for (MidataId teamMember : plg.developerTeam) {
						Developer dev = Developer.getById(teamMember, Developer.ALL_USER);
						if (dev != null && (dev.status == UserStatus.ACTIVE || dev.status == UserStatus.NEW)) {
							MailUtils.sendTextMailAsync(MailSenderType.STATUS, dev.email, dev.email, "Error Report: ["+plg.name+"] "+InstanceConfig.getInstance().getPortalServerDomain(), txt);
						}
					}
				}
			} 
									
			MailUtils.sendTextMailAsync(MailSenderType.STATUS, bugReportEmail, bugReportName, "Error Report: ["+plg.name+"] "+InstanceConfig.getInstance().getPortalServerDomain(), txt);
			Stats.addComment("Error: "+e.getClass().getName()+": "+e.getMessage());
		} catch (InternalServerException e2) {
			report(fromWhere, request, e2);
		}
	}
	
	public static void reportPerformance(String fromWhere, Request request, long duration) {
		long now = System.currentTimeMillis();
		if (now - lastReport < 1000 * 60) return;
		lastReport = now;
		String path = "none";
		String user = "none";
		if (request != null) {
		   path = "["+request.method()+"] "+request.host()+request.path();
		   PortalSessionToken tsk = PortalSessionToken.session();
		   if (tsk != null) {
		     user = tsk.getRole().toString()+" "+tsk.getOwnerId().toString();
		   } 
		} else path = "[internal] "+InstanceConfig.getInstance().getPortalServerDomain();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
		
		String txt = "Instance: "+InstanceConfig.getInstance().getPortalServerDomain()+"\nTime:"+timeStamp+"\nInterface: "+fromWhere+"\nPortal Session: "+user+"\nPath: "+path+"\nExecution Time: "+duration+"ms\n\n"+AccessLog.getReport();
		MailUtils.sendTextMailAsync(MailSenderType.STATUS, bugReportEmail, bugReportName, "Bad Performance: "+path, txt);		
	}
}
