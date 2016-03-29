package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import play.Play;
import play.mvc.Http;
import utils.mails.MailUtils;
import views.txt.mails.welcome;

public class ErrorReporter {

	private static String bugReportEmail = Play.application().configuration().getString("errorreports.targetemail");
	private static String bugReportName = Play.application().configuration().getString("errorreports.targetname");
	private static volatile long lastReport = 0;
	
	public static void report(String fromWhere, Http.Context ctx, Exception e) {
		long now = System.currentTimeMillis();
		if (now - lastReport < 1000 * 60) return;
		lastReport = now;
		String path = "none";
		String user = "none";
		if (ctx != null) {
		   path = "["+ctx.request().method()+"] "+ctx.request().host()+ctx.request().path();
		   user = ctx.session().get("role")+" "+ctx.session().get("id");
		} 
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(new Date());
		if (e!=null) AccessLog.logException("Uncatched Exception:", e);
		String txt = "Time:"+timeStamp+"\nInterface: "+fromWhere+"\nSession: "+user+"\nPath: "+path+"\n\n"+AccessLog.getReport();
		MailUtils.sendTextMail(bugReportEmail, bugReportName, "Error Report: "+path, txt);
	}
}
