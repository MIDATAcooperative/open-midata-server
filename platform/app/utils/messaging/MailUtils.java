package utils.messaging;

import java.util.Date;

import javax.inject.Inject;

import com.typesafe.config.Config;

import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;

/**
 * function for sending mails
 *
 */
public class MailUtils {

	private MailerClient mailerClient;
	private Config config;
	
	@Inject
	public MailUtils(MailerClient mailerClient, Config config) {
		this.mailerClient = mailerClient;
		this.config = config;
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
	public static void sendTextMail(String email, String fullname, String subject, Object content) {		
		if (instance == null) throw new NullPointerException("MailUtils not initialized");
		instance.sendEmail(email, fullname, subject, content);
	}
	
		
		  

	public void sendEmail(String email, String fullname, String subject, Object content) {
		System.out.println("Start send mail to "+email+" at "+new Date().toString());
		
		if (email==null) return;
		
		Email mail = new Email();
		    
		if (fullname!=null) fullname = fullname.replace(">", "").replace("<", "").replace("\"", "").replace("\\", "").replace("'", "").replace(",", " ").replace(";", " ");		
		mail.setSubject(subject);
		if (fullname != null) mail.addTo(fullname +"<" + email + ">"); else mail.addTo(email);
		mail.setFrom(config.getString("smtp.from"));	
		mail.setBodyText(content.toString());
		    
		mailerClient.send(mail);
		System.out.println("End send mail to "+email);
		System.out.flush();
	}
		
}
