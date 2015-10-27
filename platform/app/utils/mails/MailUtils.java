package utils.mails;

import play.Play;

import com.typesafe.plugin.MailerPlugin;
import com.typesafe.plugin.MailerAPI;

/**
 * function for sending mails
 *
 */
public class MailUtils {

	public static void sendTextMail(String email, String fullname, String subject, Object content) {
		MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
		mail.setSubject(subject);
		mail.setRecipient(fullname +"<" + email + ">");
		mail.setFrom(Play.application().configuration().getString("smtp.from"));	
		mail.send(content.toString());
	}
}
