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
import models.enums.MessageChannel;
import models.enums.MessageReason;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class Messager {

	private static ActorRef mailSender;
	
	private static ActorRef smsSender;
		
	private static ActorSystem system;
	
	public static void init(ActorSystem system1) {
		system = system1;
		mailSender = system.actorOf(Props.create(MailSender.class).withDispatcher("medium-work-dispatcher"), "mailSender");
		smsSender = system.actorOf(Props.create(SMSSender.class).withDispatcher("medium-work-dispatcher"), "smsSender");
	}
	
	public static void sendTextMail(String email, String fullname, String subject, String content) {	
		AccessLog.log("trigger send text mail to="+email);
		mailSender.tell(new Message(email, fullname, subject, content), ActorRef.noSender());
	}
	
	public static void sendSMS(String phone, String text) {
		AccessLog.log("trigger send SMS to="+phone);
		smsSender.tell(new SMS(phone, text), ActorRef.noSender());
	}

	public static boolean sendMessage(MidataId sourcePlugin, MessageReason reason, String code, Set targets, String defaultLanguage, Map<String, String> replacements) throws AppException {
		return sendMessage(sourcePlugin, reason, code, targets, defaultLanguage, replacements, MessageChannel.EMAIL);
	}
	
	public static boolean sendMessage(MidataId sourcePlugin, MessageReason reason, String code, Set targets, String defaultLanguage, Map<String, String> replacements, MessageChannel channel) throws AppException {
		if (targets == null || targets.isEmpty()) return false;
		Plugin plugin = Plugin.getById(sourcePlugin, Sets.create("predefinedMessages", "name"));
		if (plugin.predefinedMessages != null) {
		  replacements.put("plugin-name", plugin.name);
		  replacements.put("midata-portal-url", "https://" + InstanceConfig.getInstance().getPortalServerDomain());
		  return sendMessage(plugin.predefinedMessages, reason, code, targets, defaultLanguage, replacements, channel);
		}
		return false;
	}
	
	public static boolean sendMessage(Map<String, MessageDefinition> messageDefinitions, MessageReason reason, String code, Set targets, String defaultLanguage, Map<String, String> replacements, MessageChannel channel) throws AppException {
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
		
		sendMessage(msg, footers, targets, defaultLanguage, replacements, channel);
		
		return true;
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, Set targets, String defaultLanguage, Map<String, String> replacements, MessageChannel channel) throws AppException {	
		for (Object target : targets) {
			if (target instanceof MidataId) {
				MidataId userId = (MidataId) target;
				User user = User.getById(userId, Sets.create("email", "firstname", "lastname", "language"));
				if (user != null) sendMessage(messageDefinition, footers, user, replacements, channel);
			} else if (target instanceof String) {
				sendMessage(messageDefinition, footers, target.toString(), null, defaultLanguage, replacements, channel);
			}
			
		}
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, User member, Map<String, String> replacements, MessageChannel channel) {		
		String email = member.email;
		if (email == null) return;
		String fullname = member.firstname+" "+member.lastname;
		String subject = messageDefinition.title.get(member.language);
		if (subject == null) subject = messageDefinition.title.get(InstanceConfig.getInstance().getDefaultLanguage());
		String content = messageDefinition.text.get(member.language);
		if (content == null) content = messageDefinition.text.get(InstanceConfig.getInstance().getDefaultLanguage());
		
		if (subject==null) subject = "No subject";
		if (content == null) return;
		
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
		String phone = member.mobile;
		
		if (phone == null) phone = member.phone;
		if (phone == null) channel = MessageChannel.EMAIL;
		
		if (channel.equals(MessageChannel.SMS)) {
		   Messager.sendSMS(phone, content);
		} else {
		   Messager.sendTextMail(email, fullname, subject, content);
		}
	}
	
	public static void sendMessage(MessageDefinition messageDefinition, Map<String, String> footers, String email, String fullname, String language, Map<String, String> replacements, MessageChannel channel) {				

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
		
		if (channel.equals(MessageChannel.SMS)) {
		  Messager.sendSMS(email, content);
		} else {
		  Messager.sendTextMail(email, fullname, subject, content);
		}
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
		    AccessLog.logStart("jobs", "send email");
			if (!InstanceConfig.getInstance().getInstanceType().disableMessaging()) {			  
			  MailUtils.sendTextMail(MailSenderType.USER, msg.getReceiverEmail(), msg.getReceiverName(), msg.getSubject(), msg.getText());
			}			
		} catch (Exception e) {
			ErrorReporter.report("Messager (EMail)", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
}

class SMSSender extends AbstractActor {
	
	public SMSSender() {							    
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(SMS.class, this::sendSMS)	      
	      .build();
	}
		
	public void sendSMS(SMS msg) throws Exception {
		try {	
			AccessLog.logStart("jobs", "send SMS");
			if (!InstanceConfig.getInstance().getInstanceType().disableMessaging()) {			  
			   SMSUtils.sendSMS(msg.getPhone(), msg.getText());
			}			
		} catch (Exception e) {
			ErrorReporter.report("Messager (SMS)", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
}