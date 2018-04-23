package utils.messaging;

import java.util.Date;

import com.typesafe.plugin.MailerAPI;
import com.typesafe.plugin.MailerPlugin;

import play.Play;

/**
 * function for sending mails
 *
 */
public class MailUtils {

	/**
	 * Sends an email in text format
	 * @param email target email address
	 * @param fullname name of recipient
	 * @param subject title of email
	 * @param content content of email
	 */
	public static void sendTextMail(String email, String fullname, String subject, Object content) {
		System.out.println("Start send mail to "+email+" at "+new Date().toString());
		MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
		mail.setSubject(subject);
		mail.setRecipient(fullname +"<" + email + ">");
		mail.setFrom(Play.application().configuration().getString("smtp.from"));	
		mail.send(content.toString());
		System.out.println("End send mail to "+email);
		System.out.flush();
	}
}
