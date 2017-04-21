package utils.messaging;

import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import models.MessageDefinition;
import models.MidataId;
import models.Plugin;
import models.User;
import models.enums.MessageReason;
import play.libs.Akka;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class Messager {

	private static ActorRef mailSender;
	
	public static void init() {
		mailSender = Akka.system().actorOf(Props.create(MailSender.class), "mailSender");
	}
	
	public static void sendTextMail(String email, String fullname, String subject, String content) {
		mailSender.tell(new Message(email, fullname, subject, content), ActorRef.noSender());
	}
	
	public static void sendMessage(MidataId sourcePlugin, MessageReason reason, String code, Set<MidataId> targets, Map<String, String> replacements) throws AppException {
		Plugin plugin = Plugin.getById(sourcePlugin, Sets.create("predefinedMessages", "name"));
		if (plugin.predefinedMessages != null) {
		  replacements.put("plugin-name", plugin.name);
		  replacements.put("midata-instance", "https://" + InstanceConfig.getInstance().getPortalServerDomain());
		  sendMessage(plugin.predefinedMessages, reason, code, targets, replacements);
		}
	}
	
	public static void sendMessage(Map<String, MessageDefinition> messageDefinitions, MessageReason reason, String code, Set<MidataId> targets, Map<String, String> replacements) throws AppException {
		if (targets.isEmpty()) return;
		
		MessageDefinition msg = null; 
		if (code != null) msg = messageDefinitions.get(reason.toString()+"_"+code);
		if (msg == null) msg = messageDefinitions.get(reason.toString());
		if (msg == null) return;
		
		Map<String, String> footers = null;
		Plugin commonPlugin = Plugin.getById(RuntimeConstants.instance.commonPlugin, Sets.create("predefinedMessages"));
		if (commonPlugin.predefinedMessages != null) {
		  MessageDefinition footerDefs = commonPlugin.predefinedMessages.get(reason.toString());
		  if (footerDefs != null) footers = footerDefs.text;
		}
		
		sendMessage(msg, footers, targets, replacements);			
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, Set<MidataId> targets, Map<String, String> replacements) throws AppException {	
		for (MidataId userId : targets) {
			User user = User.getById(userId, Sets.create("email", "firstname", "lastname", "language"));
			if (user != null) sendMessage(messageDefinition, footers, user, replacements);
		}
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, User member, Map<String, String> replacements) {				
		String email = member.email;
		String fullname = member.firstname+" "+member.lastname;
		String subject = messageDefinition.title.get(member.language);
		if (subject == null) subject = messageDefinition.title.get("EN");
		String content = messageDefinition.text.get(member.language);
		if (content == null) content = messageDefinition.text.get("EN");
		
		if (footers != null) {
			String footer = footers.get(member.language);
			if (footer == null) footer = footers.get("EN");
			if (footer != null) content += "\n"+footer;
		}
		replacements.put("firstname", member.firstname);
		replacements.put("lastname", member.lastname);
		replacements.put("email", member.email);
		
		for (Map.Entry<String, String> replacement : replacements.entrySet()) {
			String key = "<"+replacement.getKey()+">";
		    subject = subject.replaceAll(key, replacement.getValue());
		    content = content.replaceAll(key, replacement.getValue());
		}
		
		Messager.sendTextMail(email, fullname, subject, content);
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