package utils.messaging;

import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import models.MessageDefinition;
import models.MidataId;
import models.Plugin;
import models.User;
import models.enums.MessageReason;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class Messager {

	private static ActorRef mailSender;
		
	private static ActorSystem system;
	
	public static void init(ActorSystem system1) {
		system = system1;
		mailSender = system.actorOf(Props.create(MailSender.class), "mailSender");
	}
	
	public static void sendTextMail(String email, String fullname, String subject, String content) {		
		mailSender.tell(new Message(email, fullname, subject, content), ActorRef.noSender());
	}
	
	public static boolean sendMessage(MidataId sourcePlugin, MessageReason reason, String code, Set targets, String defaultLanguage, Map<String, String> replacements) throws AppException {
		if (targets == null || targets.isEmpty()) return false;
		Plugin plugin = Plugin.getById(sourcePlugin, Sets.create("predefinedMessages", "name"));
		if (plugin.predefinedMessages != null) {
		  replacements.put("plugin-name", plugin.name);
		  replacements.put("midata-portal-url", "https://" + InstanceConfig.getInstance().getPortalServerDomain());
		  return sendMessage(plugin.predefinedMessages, reason, code, targets, defaultLanguage, replacements);
		}
		return false;
	}
	
	public static boolean sendMessage(Map<String, MessageDefinition> messageDefinitions, MessageReason reason, String code, Set targets, String defaultLanguage, Map<String, String> replacements) throws AppException {
		if (targets.isEmpty()) return false;
		
		MessageDefinition msg = null; 
		if (code != null) msg = messageDefinitions.get(reason.toString()+"_"+code);
		if (msg == null) msg = messageDefinitions.get(reason.toString());
		if (msg == null) return false;
		
		Map<String, String> footers = null;
		Plugin commonPlugin = Plugin.getById(RuntimeConstants.instance.commonPlugin, Sets.create("predefinedMessages"));
		if (commonPlugin.predefinedMessages != null) {
		  MessageDefinition footerDefs = commonPlugin.predefinedMessages.get(reason.toString());
		  if (footerDefs != null) footers = footerDefs.text;
		}
		
		sendMessage(msg, footers, targets, defaultLanguage, replacements);
		
		return true;
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, Set targets, String defaultLanguage, Map<String, String> replacements) throws AppException {	
		for (Object target : targets) {
			if (target instanceof MidataId) {
				MidataId userId = (MidataId) target;
				User user = User.getById(userId, Sets.create("email", "firstname", "lastname", "language"));
				if (user != null) sendMessage(messageDefinition, footers, user, replacements);
			} else if (target instanceof String) {
				sendMessage(messageDefinition, footers, target.toString(), null, defaultLanguage, replacements);
			}
			
		}
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, User member, Map<String, String> replacements) {				
		String email = member.email;
		if (email == null) return;
		String fullname = member.firstname+" "+member.lastname;
		String subject = messageDefinition.title.get(member.language);
		if (subject == null) subject = messageDefinition.title.get(InstanceConfig.getInstance().getDefaultLanguage());
		String content = messageDefinition.text.get(member.language);
		if (content == null) content = messageDefinition.text.get(InstanceConfig.getInstance().getDefaultLanguage());
		
		if (footers != null) {
			String footer = footers.get(member.language);
			if (footer == null) footer = footers.get(InstanceConfig.getInstance().getDefaultLanguage());
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
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, String email, String fullname, String language, Map<String, String> replacements) {				

		String subject = messageDefinition.title.get(language);
		if (subject == null) subject = messageDefinition.title.get(InstanceConfig.getInstance().getDefaultLanguage());
		String content = messageDefinition.text.get(language);
		if (content == null) content = messageDefinition.text.get(InstanceConfig.getInstance().getDefaultLanguage());
		
		if (footers != null) {
			String footer = footers.get(language);
			if (footer == null) footer = footers.get(InstanceConfig.getInstance().getDefaultLanguage());
			if (footer != null) content += "\n"+footer;
		}
		String names[] = fullname == null ? new String[0] : fullname.split(" ");
		
		if (names.length>1) {
		  replacements.put("firstname", names[0]);
		  replacements.put("lastname", names[names.length-1]);
		} else if (names.length == 1) {
		  replacements.put("firstname", "");
		  replacements.put("lastname", names[0]);
		} else {
		  replacements.put("firstname", "");
		  replacements.put("lastname", "");	
		}
		replacements.put("email", email);
		
		for (Map.Entry<String, String> replacement : replacements.entrySet()) {
			String key = "<"+replacement.getKey()+">";
		    subject = subject.replaceAll(key, replacement.getValue());
		    content = content.replaceAll(key, replacement.getValue());
		}
		
		Messager.sendTextMail(email, fullname, subject, content);
	}
}

class MailSender extends AbstractActor {
			
	public MailSender() {							    
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(Message.class, this::receiveMessage)	      
	      .build();
	}
		
	public void receiveMessage(Message msg) throws Exception {
		try {		
			if (!InstanceConfig.getInstance().getInstanceType().disableMessaging()) {			  
			  MailUtils.sendTextMail(msg.getReceiverEmail(), msg.getReceiverName(), msg.getSubject(), msg.getText());
			}			
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
}