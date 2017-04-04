package utils.messaging;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import play.libs.Akka;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.RecordManager;


public class Messager {

	private static ActorRef mailSender;
	
	public static void init() {
		mailSender = Akka.system().actorOf(Props.create(MailSender.class), "mailSender");
	}
	
	public static void sendTextMail(String email, String fullname, String subject, String content) {
		mailSender.tell(new Message(email, fullname, subject, content), ActorRef.noSender());
	}
}

class MailSender extends UntypedActor {
			
	public MailSender() {							    
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		try {
		if (message instanceof Message) {
			Message msg = (Message) message;
			MailUtils.sendTextMail(msg.getReceiverEmail(), msg.getReceiverName(), msg.getSubject(), msg.getText());
		} else {
		    unhandled(message);
	    }	
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			RecordManager.instance.clear();
			AccessLog.newRequest();	
		}
	}
	
}