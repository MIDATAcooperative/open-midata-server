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

package utils.messaging;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.text.StringEscapeUtils;
import org.hazlewood.connor.bottema.emailaddress.EmailAddressCriteria;
import org.hazlewood.connor.bottema.emailaddress.EmailAddressParser;

import com.typesafe.config.Config;

import akka.dispatch.Mailbox;
import models.MidataId;
import models.Plugin;
import play.api.libs.mailer.SMTPConfiguration;
import play.api.libs.mailer.SMTPMailer;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import scala.Option;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;
import com.typesafe.config.ConfigFactory;

/**
 * function for sending mails
 *
 */
public class MailUtils {

	private Map<MailSenderType, MailerClient> mailerClient;
	
	private Config config;
	
	@Inject
	public MailUtils(Config config) {
		this.config = config;
		this.mailerClient = new HashMap<MailSenderType, MailerClient>();
		this.mailerClient.put(MailSenderType.USER, createInstance("play.mailer.user"));
		this.mailerClient.put(MailSenderType.STATUS, createInstance("play.mailer.status"));
		this.mailerClient.put(MailSenderType.BULK, createInstance("play.mailer.bulk"));					
	}
		
	private MailerClient createInstance(String name) {
		System.out.println(name);
		Config sub = config.getConfig(name);
		SMTPConfiguration conf = new SMTPConfiguration(
				sub.getString("host"), 
				sub.getInt("port") , 
				sub.getBoolean("ssl"), 
				sub.getBoolean("tls"), 
				false /*sub.getBoolean("tlsRequired")*/, Option.apply(sub.getString("user")), Option.apply(sub.getString("password")), false, Option.empty(), Option.empty(), sub, false);
        return new SMTPMailer(conf);		
	}
	
	private MailerClient createInstance(SMTPConfig smtp) {

		SMTPConfiguration conf = new SMTPConfiguration(smtp.host, smtp.port, smtp.ssl, smtp.tls, false, Option.apply(smtp.user), Option.apply(smtp.password), false, Option.empty(), Option.empty(), ConfigFactory.empty(), false);
		return new SMTPMailer(conf);	
	}
	
	private static MailUtils instance;
	
	public static void setInstance(MailUtils instance1) {
		instance = instance1;
	}
	
	/**
	 * Sends an email in text format
	 * @param email target email address
	 * @param fullname name of recipient
	 * @param subject title of email
	 * @param content content of email
	 */
	public static void sendTextMail(MailSenderType sender, String email, String fullname, String subject, Object content, MidataId smtpFromApp) {		
	  sendTextMail(sender, email, fullname, subject, content, null, smtpFromApp);
	}
	public static void sendTextMail(MailSenderType sender, String email, String fullname, String subject, Object content, String htmlFrame, MidataId smtpFromApp) {		
		if (instance == null) throw new NullPointerException("MailUtils not initialized");
		SMTPConfig config = null;
		if (smtpFromApp != null) {
			try {
			  Plugin pl = Plugin.getById(smtpFromApp, Sets.create("smtp", "status"));
		      if (pl != null && pl.smtp != null && pl.status.isUsable()) config = pl.smtp;
			} catch (InternalServerException e) {}
		}
		instance.sendEmail(sender, config, email, htmlFrame, fullname, subject, content);
	}
	
	public static void sendTextMailAsync(MailSenderType sender, String email, String fullname, String subject, Object content) {
		sendTextMailAsync(sender, email, fullname, subject, content, null);
	}
	
	public static void sendTextMailAsync(MailSenderType sender, String email, String fullname, String subject, Object content, MidataId smtpFromApp) {
		Messager.sendTextMail(email, fullname, subject, content.toString(), null, null, sender, smtpFromApp);
	}
					 
	public void sendEmail(MailSenderType sender, SMTPConfig smtp, String email, String htmlFrame, String fullname, String subject, Object content) {
		System.out.println("Start send mail to "+email+" at "+new Date().toString());
		
		if (email==null) return;
		
		Email mail = new Email();		    	
		mail.setSubject(subject);
		mail.addTo(getMailboxFromAddressAndDisplay(email, fullname));
		if (smtp != null) {
		  mail.setFrom(smtp.from);	
		} else {
		  mail.setFrom(config.getString("play.mailer."+sender.toString().toLowerCase()+".from"));
		}
		
		if (sender == MailSenderType.STATUS) {
		  mail.setBodyText(content.toString());
		} else {		
		  mail.setBodyHtml(getHTMLVersionFromText(content.toString(), htmlFrame));
		  mail.setBodyText(getTextOnlyVersion(content.toString()));
		}
		
		if (smtp != null) {
			createInstance(smtp).send(mail);
		} else {
		    mailerClient.get(sender).send(mail);
		}
		System.out.println("End send mail to "+email);
		System.out.flush();
	}
	
	public static String getHTMLVersionFromText(String text, String htmlFrame) {
		StringBuilder result = new StringBuilder();
		String escaped = StringEscapeUtils.escapeHtml4(text);
		//escaped = escaped.replaceAll("\n\\*\s", "\n</p><p>");
		escaped = escaped.replaceAll("\\_\\_([^\\n_*]+?)\\_\\_", "<i>$1</i>");
		escaped = escaped.replaceAll("\\*\\*([^\\n_*]+?)\\*\\*", "<b>$1</b>");
		
		// Bullets
		escaped = escaped.replaceAll("\n", "←→");
		escaped = escaped.replaceAll("→\\*\s*(.+?)←","<ul><li>$1</li></ul>");
		escaped = escaped.replaceAll("<\\/ul><ul>", "");
		escaped = escaped.replaceAll("←→", "\n</p><p>");
		escaped = escaped.replaceAll("[←→]", "");
		
		// Newer rule for links, replace http so that the second rule for links does not match
		escaped = escaped.replaceAll("\\[(.+?)\\|http(.+)\\]", "<a href=\"qqx$2\">$1</a>");
		escaped = escaped.replaceAll("\\{\\}", "</span>");
		escaped = escaped.replaceAll("\\{(.+?)\\}", "<span style=\"$1\">");
		
		// Old rule for links
		int p = escaped.indexOf("https://");
		while (p>=0) {
			char[] chars = escaped.toCharArray();
			int e = p+8;
			while (e<chars.length && !Character.isWhitespace(chars[e])) e++;
			result.append(escaped.substring(0, p)+"<a href=\""+escaped.substring(p, e)+"\">"+escaped.substring(p,e)+"</a>");
			escaped = escaped.substring(e);
			p = escaped.indexOf("https://");
		}
		// Finish newer rule for links
		escaped = escaped.replaceAll("qqx", "http");  
		
		result.append(escaped);
		
		if (htmlFrame == null || htmlFrame.trim().length() == 0) {
		  return "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>"+result.toString()+"</p></body></html>";
		}
		return "<!DOCTYPE html>"+htmlFrame.replace("{{message}}", result.toString());
		
	}
	
	public static String getTextOnlyVersion(String text) {
	   return text.replaceAll("\\_\\_", "").replaceAll("\\*\\*", "").replaceAll("\\[(.+?)\\|http(.+)\\]", "$1 ( http$2 )").replaceAll("\\{\\}", "").
		  replaceAll("\\{(.+?)\\}", "");
	}
	
	public static String getAddressFromMailbox(String mailbox) {
		String adr[] = EmailAddressParser.getAddressParts(mailbox, EmailAddressCriteria.RECOMMENDED, true);
		return adr[1]+"@"+adr[2];
	}
	
	public static String getDisplayFromMailbox(String mailbox) {
		return EmailAddressParser.getPersonalName(mailbox, EmailAddressCriteria.RECOMMENDED, true);		
	}
	
	public static String getMailboxFromAddressAndDisplay(String email, String fullname) {
		if (fullname!=null) fullname = fullname.replace(">", "").replace("<", "").replace("\"", "").replace("\\", "").replace("'", "").replace(",", " ").replace(";", " ");		
		if (fullname != null) return (fullname +"<" + getAddressFromMailbox(email) + ">"); else return email;		
	}
		
}
