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

import com.typesafe.config.Config;

import play.api.libs.mailer.SMTPConfiguration;
import play.api.libs.mailer.SMTPMailer;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import scala.Option;

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
	public static void sendTextMail(MailSenderType sender, String email, String fullname, String subject, Object content) {		
		if (instance == null) throw new NullPointerException("MailUtils not initialized");
		instance.sendEmail(sender, email, fullname, subject, content);
	}
	
		
		  

	public void sendEmail(MailSenderType sender, String email, String fullname, String subject, Object content) {
		System.out.println("Start send mail to "+email+" at "+new Date().toString());
		
		if (email==null) return;
		
		Email mail = new Email();
		    
		if (fullname!=null) fullname = fullname.replace(">", "").replace("<", "").replace("\"", "").replace("\\", "").replace("'", "").replace(",", " ").replace(";", " ");		
		mail.setSubject(subject);
		if (fullname != null) mail.addTo(fullname +"<" + email + ">"); else mail.addTo(email);
		mail.setFrom(config.getString("play.mailer."+sender.toString().toLowerCase()+".from"));	
		mail.setBodyText(content.toString());
		    
		mailerClient.get(sender).send(mail);
		System.out.println("End send mail to "+email);
		System.out.flush();
	}
		
}
