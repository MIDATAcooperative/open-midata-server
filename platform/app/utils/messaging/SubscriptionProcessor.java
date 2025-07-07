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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.bson.BSONObject;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import controllers.Plugins;
import models.APSNotExistingException;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.SubscriptionData;
import models.TestPluginCall;
import models.User;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.MessageChannel;
import models.enums.MessageReason;
import models.enums.PluginStatus;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.fhir.SubscriptionResourceProvider;
import utils.stats.ActionRecorder;
import utils.stats.Stats;
import utils.sync.Instances;

/**
 * Do notifications for FHIR subscriptions
 *
 */
public class SubscriptionProcessor extends AbstractActor {
	
	public final static String TRIGGER = "EVENT";
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(SubscriptionTriggered.class, this::processSubscription)
			   .match(RecheckMessage.class, this::answerDebugCall)
			   .build();
	}
	
	void processSubscription(SubscriptionTriggered triggered) {
		String path = "SubscriptionProcessor/processSubscription";
		long st = ActionRecorder.start(path);
		try {		
			AccessLog.logStart("jobs", triggered.toString());
			List<SubscriptionData> allMatching = null;
		
			if (triggered.getById()!=null) {
			  allMatching = Collections.singletonList(SubscriptionData.getById(triggered.getById(), SubscriptionData.ALL));
			} else {
			  allMatching = SubscriptionData.getByOwnerAndFormat(triggered.affected, triggered.type, SubscriptionData.ALL);
			}
				
			boolean anyAnswered = false;
			AccessLog.log("found "+allMatching.size()+" possible subscriptions");
			for (SubscriptionData subscription : allMatching) {	
				AccessLog.log("ok:"+subscription.active+" "+subscription.content+" "+triggered.getEventCode());				
				boolean answered = false;
				if (subscription.active && (subscription.content == null || subscription.content.equals("MessageHeader") || subscription.content.equals(triggered.getEventCode())) && checkNotExpired(subscription)) {
					if (triggered.getType().equals("fhir/MessageHeader") && (!triggered.getApp().equals(subscription.app))) {
						AccessLog.log("trigger app="+triggered.getApp()+" subscription app="+subscription.app);
						continue;
					}
									
					Subscription fhirSubscription = SubscriptionResourceProvider.subscription(subscription);
					SubscriptionChannelComponent channel = fhirSubscription.getChannel();
					
					Stats.startRequest();
					Stats.setPlugin(subscription.app);
					
					//System.out.println("type="+channel.getType().toString());
					if (channel.getType().equals(SubscriptionChannelType.RESTHOOK)) {
						if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), 1), getSelf());
						AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.RESTHOOK).withActor(null, subscription.owner).withModifiedActor(null, triggered.getSourceOwner()).withApp(subscription.app));
						processRestHook(subscription._id, triggered, channel);
					} else if (channel.getType().equals(SubscriptionChannelType.MESSAGE)) {
						String endpoint = channel.getEndpoint();
						if (endpoint != null && endpoint.startsWith("node://")) {							
							answered = processApplication(subscription, triggered, channel) || answered;
							if (answered) anyAnswered = true;
						} else if (endpoint != null && endpoint.startsWith("app://")) {
							answered = true;
							anyAnswered = true;
							getSender().tell(new MessageResponse("Can only forward FHIR messages with destination",-1, triggered.getApp() != null ? triggered.getApp().toString() : null), getSelf());
						}
					} else if (channel.getType().equals(SubscriptionChannelType.EMAIL)) {
						
						processEmail(subscription, triggered, channel);
						answered = true;
						anyAnswered = true;
					} else if (channel.getType().equals(SubscriptionChannelType.SMS)) {
						processSMS(subscription, triggered, channel);
						answered = true;
						anyAnswered = true;
					}
					Stats.finishRequest(TRIGGER, triggered.getDescription(), null, "200", Collections.emptySet());
					//System.out.println("ok4");
			
					if (!answered) {
						//System.out.println("SEND DEFAULT ANSWER");
						String app = triggered.getApp() != null ? triggered.getApp().toString() : null;
						getSender().tell(new MessageResponse("No matching subscription",-1, app), getSelf());
						anyAnswered = true;
					}
				}
								
			}
			
			if (!anyAnswered) {
				String app = triggered.getApp() != null ? triggered.getApp().toString() : null;
				getSender().tell(new MessageResponse("No matching subscription",-1, app), getSelf());
			}
			
		} catch (Exception e) {			
			ErrorReporter.report("Subscriptions", null, e);
			getSender().tell(new MessageResponse("Exception while processing subscription: "+e.toString(),-1, null), getSelf());
			
		} finally {
			if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
	
	boolean checkNotExpired(SubscriptionData data) throws AppException {
		if (data.endDate != null && data.endDate.before(new Date())) {
			SubscriptionData.setOff(data._id);			
			data.active = false;
			Instances.cacheClear("SubscriptionData", data.owner);			
			return false;
		}
		return true;
	} 
	
	void processRestHook(MidataId subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) {
	   WSRequest request = SubscriptionManager.ws.url(channel.getEndpoint());
	   for (StringType header : channel.getHeader()) {
		   String str = header.getValue();
		   int p = str.indexOf(':');
		   if (p > 0) request.addHeader(str.substring(0, p).trim(), str.substring(p+1));
	   }
	   AccessLog.log("Calling Rest Hook");
	   final ActorRef sender = getSender();
	   final MidataId eventId = AuditManager.instance.convertLastEventToAsync();
	   CompletionStage<WSResponse> response = request.execute("POST");
	   response.whenComplete((out, exception) -> {
		   AuditManager.instance.resumeAsyncEvent(eventId);
		   String error = null;
		   if (out != null && out.getStatus() >= 400) {
			   error = "status "+out.getStatus();
		   }
		   if (exception != null) {
			   if (exception instanceof CompletionException) exception = exception.getCause();
			   error = exception.toString();
		   }
		   
		   if (error != null) {
			   try {
				   SubscriptionData.fail(subscription);
				   SubscriptionData.setError(subscription, new Date().toString()+": "+error);
				   AuditManager.instance.fail(400, error, "error.plugin");
			   } catch (Exception e) {
				   AccessLog.logException("Error during Subscription.setError", e);				   
				   ErrorReporter.report("SubscriptionManager", null, e);
			   } finally {
				   ServerTools.endRequest();
			   }
		   } else try { AuditManager.instance.success(); } catch (AppException e) {}	
		   
		   if (triggered.getTransactionId()!=null) sender.tell(new TriggerCountMessage(triggered.getTransactionId(), -1), ActorRef.noSender());
	   });
	}
	
	void processEmail(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws AppException {
		Map<String,String> replacements = new HashMap<String, String>();
		if (triggered.getParams()!=null) replacements.putAll(triggered.getParams());
		AccessLog.log("process email type="+triggered.getType()+"  "+triggered.getEventCode());
		Set targets = Collections.singleton(subscription.owner);
		String endpoint = channel.getEndpoint();
		if (endpoint!=null && endpoint.trim().length()>0) targets = Collections.singleton(endpoint); 
		if (triggered.getType().equals("fhir/MessageHeader")) {
		  String ev = triggered.getEventCode();
		  //if (ev.indexOf(":")>=0) ev = ev.substring(0,ev.indexOf(":"));
		  AccessLog.log("send ev="+ev+" ow="+subscription.owner+" app="+subscription.app);		  
		  Messager.sendMessage(subscription.app, MessageReason.PROCESS_MESSAGE, ev, targets, null, replacements);			
		} else {
		  Messager.sendMessage(subscription.app, MessageReason.RESOURCE_CHANGE, triggered.getType(), targets, null, replacements);
		}
		getSender().tell(new MessageResponse(null,0,"internal:mail"), getSelf());
	}
	
	void processSMS(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws AppException {
		Map<String,String> replacements = new HashMap<String, String>();
		if (triggered.getParams()!=null) replacements.putAll(triggered.getParams());
		AccessLog.log("process sms type="+triggered.getType()+"  "+triggered.getEventCode());
		Set targets = Collections.singleton(subscription.owner);
		String endpoint = channel.getEndpoint();
		if (endpoint!=null && endpoint.trim().length()>0) targets = Collections.singleton(endpoint); 
		if (triggered.getType().equals("fhir/MessageHeader")) {
		  String ev = triggered.getEventCode();
		  //if (ev.indexOf(":")>=0) ev = ev.substring(0,ev.indexOf(":"));
		  Messager.sendMessage(subscription.app, MessageReason.PROCESS_MESSAGE, ev, targets, null, replacements, MessageChannel.SMS);			
		} else {
		  Messager.sendMessage(subscription.app, MessageReason.RESOURCE_CHANGE, triggered.getType(), targets, null, replacements, MessageChannel.SMS);
		}
		getSender().tell(new MessageResponse(null,0,"internal:sms"), getSelf());
	}
	
	boolean processApplication(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws AppException {
		AccessLog.log("prcApp app="+subscription.app);
		try {
		Plugin plugin = Plugin.getById(subscription.app);
		if (plugin == null) {
			subscription.disable();
			return false;
		}
		if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), 1), getSelf());
		AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.SCRIPT_INVOCATION).withActor(null, subscription.owner).withModifiedActor(null, triggered.getSourceOwner()).withApp(subscription.app));
		//System.out.println("prcApp2");
		String endpoint = channel.getEndpoint();		
		
		//System.out.println("prcApp3");
		//AccessLog.log("sub session="+subscription.session);
		String handle = ServiceHandler.decrypt(subscription.session);
		
		if (handle == null) {
			SubscriptionData.setError(subscription._id, "Background service key expired");
			getSender().tell(new MessageResponse("Service key expired",-1, plugin.filename), getSelf());
			try {
				throw new InternalServerException("error.internal", "Missing service key subscription: "+subscription._id);
			} catch (InternalServerException e) {
				ErrorReporter.report("Subscription-Processor", null, e);
			}
			
			AuditManager.instance.fail(500, "Service key expired", "error.missing.token");
			if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
			return true;
		}
		
		User user = null;
		
		if (!plugin.type.equals("analyzer") && !plugin.type.equals("external") && !plugin.type.equals("broker") && !plugin.type.equals("endpoint")) {
			user = User.getById(subscription.owner, Sets.create("status", "role", "language", "developer"));
			//System.out.println("prcApp4");
			if (user==null || user.status.isDeleted() || user.status.equals(UserStatus.BLOCKED)) {
				subscription.disable();
				AuditManager.instance.fail(400, "Subscription owner bad status", "error.unknown.user");
				if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
				return false;
			}
		}
        final ActorRef sender = getSender();						
		SpaceToken tk = null;
		if (plugin.type.equals("mobile") || plugin.type.equals("service") || plugin.type.equals("analyzer") || plugin.type.equals("external") || plugin.type.equals("broker")) {
			AccessLog.log("RIGHT PATH");
			System.out.println("RIGHT PATH "+plugin.name);
			Set<MobileAppInstance> mais = MobileAppInstance.getByApplicationAndOwner(plugin._id, subscription.owner, Sets.create("status", "dateOfCreation"));
			if (mais.isEmpty()) {
				AuditManager.instance.fail(400, "No application instance", "error.unknown.consent");
				if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
				return false;
			}
			MidataId appInstanceId = null;
			Date best = null;
			for (MobileAppInstance mai : mais) {
				if (mai.status.equals(ConsentStatus.ACTIVE)) {
					if (appInstanceId == null || (mai.dateOfCreation != null && mai.dateOfCreation.after(best))) {
					  appInstanceId = mai._id;
					  best = mai.dateOfCreation;
					}
				}
			}
			if (appInstanceId == null) {
				AuditManager.instance.fail(400, "No application instance", "error.unknown.consent");
				if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
				return false;	
			}
			//AccessLog.log("HANDLE="+handle);

			if (subscription.app == null) throw new NullPointerException(); 
			tk = new SpaceToken(handle, appInstanceId, subscription.owner, user != null ? user.getRole() : UserRole.ANY, null, subscription.app, subscription.owner, triggered.getUserGroupId());			
			AccessLog.log("HANDLEPOST="+tk.handle+" space="+tk.spaceId.toString()+" app="+tk.pluginId);
			
			runProcess(getSender(), plugin, triggered, subscription, user, tk.encrypt(), endpoint);
			
		} else if (plugin.type.equals("oauth2")) {
			System.out.println("NEW OAUTH2 - 1");
			try {
				KeyManager.instance.continueSession(handle, subscription.owner);
               	AccessContext context = ContextManager.instance.createSessionForDownloadStream(subscription.owner, UserRole.MEMBER);	
				BSONObject oauthmeta = RecordManager.instance.getMeta(context, subscription.instance, "_oauth");
				if (oauthmeta != null) {
					System.out.println("NEW OAUTH2 - 2");
					if (oauthmeta.get("refreshToken") != null) {
						tk = new SpaceToken(handle, subscription.instance, subscription.owner, user.getRole(), null, null, subscription.owner, triggered.getUserGroupId());
						final String token = tk.encrypt();
						System.out.println("NEW OAUTH2 - 3");
						Plugin plugin2 = Plugin.getById(plugin._id, Sets.create("type", "filename", "name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "refreshTkExchangeParams"));
						final User user1 = user;
						final MidataId eventId = AuditManager.instance.convertLastEventToAsync();
						Plugins.requestAccessTokenOAuth2FromRefreshToken(handle, subscription.owner, plugin2, subscription.instance.toString(), oauthmeta.toMap()).thenAcceptAsync(success1 -> {
							    AuditManager.instance.resumeAsyncEvent(eventId);
							    boolean success = (Boolean) success1;
								if (success) {
									System.out.println("NEW OAUTH2 - 4");							
									runProcess(sender, plugin, triggered, subscription, user1, token, endpoint);
								} else {
									System.out.println("NEW OAUTH2 - 4B");
									sender.tell(new MessageResponse("OAuth 2 failed",-1, plugin.filename), getSelf());
									AuditManager.instance.fail(400, "OAuth 2 failed", "error.missing.token");
									if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
								}
								try { AuditManager.instance.success(); } catch (AppException e) {}
						});
					} else {
						SubscriptionData.fail(subscription._id);
						sender.tell(new MessageResponse("OAuth 2 no refresh token",-1, plugin.filename), getSelf());
						AuditManager.instance.fail(400, "OAuth 2 no refresh token", "error.missing.token");
						if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
					}
				} else {
					SubscriptionData.fail(subscription._id);
					sender.tell(new MessageResponse("OAuth 2 no data",-1, plugin.filename), getSelf());
					AuditManager.instance.fail(400, "OAuth 2 no data", "error.missing.consent_accept");
					if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
				}	
			} catch (APSNotExistingException e) {
				subscription.disable();
				sender.tell(new MessageResponse("Space no longer existing - disabled",-1, plugin.filename), getSelf());
				AuditManager.instance.fail(400, "No application instance", "error.missing.plugin");
				if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
			} 
		} else {
			//AccessLog.log("BPART HANDLE="+handle);
			tk = new SpaceToken(handle, subscription.instance, subscription.owner, user.getRole(), null, null, subscription.owner, triggered.getUserGroupId());
			runProcess(getSender(), plugin, triggered, subscription, user, tk.encrypt(), endpoint);
		}
		AuditManager.instance.success();
		return true;
		} finally {
			ServerTools.endRequest();
		}
	}	
	
	private void runProcessExternal(ActorRef sender, Plugin plugin, final SubscriptionTriggered triggered, SubscriptionData subscription, User user, String token, String endpoint) {
  		String type = triggered.getType();
		if (type.startsWith("fhir/")) type = type.substring("fhir/".length());
	
	   final String lang = (user != null && user.language != null) ? user.language : InstanceConfig.getInstance().getDefaultLanguage();
	   final String id = triggered.getResourceId() != null ? type+"/"+triggered.getResourceId().toString()+"/_history/"+triggered.resourceVersion : "-";
	   WSRequest request = SubscriptionManager.ws.url(InstanceConfig.getInstance().getInternalScriptingUrl());
	   
	   request.addQueryParameter("token", token);
	   request.addQueryParameter("lang", lang);
	   request.addQueryParameter("backend", "https://"+InstanceConfig.getInstance().getPlatformServer()); //"http://localhost:9001");
	   request.addQueryParameter("owner", subscription.owner.toString());
	   request.addQueryParameter("id", id);
	   //final ActorRef sender = getSender();	   
	   AccessLog.log("Calling Rest Hook");
	   //final MidataId eventId = AuditManager.instance.convertLastEventToAsync();
	   CompletionStage<WSResponse> response = request.execute("POST");
	   response.whenComplete((out, exception) -> {
		   
		   String error = null;
		   if (out != null && out.getStatus() >= 400) {
			   error = "status "+out.getStatus();
		   }
		   if (exception != null) {
			   if (exception instanceof CompletionException) exception = exception.getCause();
			   error = exception.toString();
		   }
		   
		   if (error != null) {
			   try {
				   SubscriptionData.fail(subscription._id);
				   SubscriptionData.setError(subscription._id, new Date().toString()+": "+error);
				   AuditManager.instance.fail(400, error, "error.plugin");
				   ErrorReporter.reportPluginProblem("subscription script", null, new PluginException(plugin._id, "error.plugin", error));
			   } catch (Exception e) {
				   AccessLog.logException("Error during Subscription.setError", e);				   
				   ErrorReporter.report("SubscriptionManager", null, e);
			   } finally {
				   ServerTools.endRequest();
			   }
		   } else {
			   try { subscription.ok(); } catch (Exception e) {}
			   Stats.finishRequest(TRIGGER, triggered.getDescription(), null, "0", Collections.emptySet());
			   String r = out.getBody();
			   if (r != null && r.length() >0) {
				 sender.tell(new MessageResponse(r, 0, plugin.filename), getSelf());  
			   } else {
					  
					  /*if (p.exitValue()==1) {
						  response = "Script not found";
						  AuditManager.instance.fail(400, "Script error", "error.plugin");
					  }*/
					 sender.tell(new MessageResponse("", 0, plugin.filename), getSelf());
				  } 
		   }
		   if (triggered.getTransactionId()!=null) sender.tell(new TriggerCountMessage(triggered.getTransactionId(), -1), ActorRef.noSender());
		   
	   });
	}
	
	private void reportPluginProblem(Plugin plugin, String error) {
	  try {
		throw new PluginException(plugin._id, "error.plugin", error);
	  } catch (PluginException e) {
	    ErrorReporter.report("Script execution", null, e);
	  }
	}

	private void runProcess(ActorRef sender, Plugin plugin, SubscriptionTriggered triggered, SubscriptionData subscription, User user, String token, String endpoint) {
		boolean testing = InstanceConfig.getInstance().getInstanceType().getDebugFunctionsAvailable() && (plugin.status.equals(PluginStatus.DEVELOPMENT) || plugin.status.equals(PluginStatus.BETA));
		if (!testing && InstanceConfig.getInstance().getInternalScriptingUrl() != null) {
			runProcessExternal(sender, plugin, triggered, subscription, user, token, endpoint);
			return;
		} 
		try {
		String cmd = endpoint.substring("node://".length());
		String visDir = InstanceConfig.getInstance().getConfig().getString("visualizations.path")+"/scripts";
		String visPath =  visDir+"/"+plugin.filename+"/"+cmd;
		final String lang = (user != null && user.language != null) ? user.language : InstanceConfig.getInstance().getDefaultLanguage();
		
		String type = triggered.getType();
		if (type.startsWith("fhir/")) type = type.substring("fhir/".length());
		
		final String id = triggered.getResourceId() != null ? type+"/"+triggered.getResourceId().toString()+"/_history/"+triggered.resourceVersion : "-";
		final String nodepath = InstanceConfig.getInstance().getConfig().getString("node.path");
		
		//System.out.println("prcApp5");
		if (testing) {
			plugin = Plugin.getById(plugin._id, Plugin.ALL_DEVELOPER);
			if (plugin.debugHandle != null) {
				MidataId eventId = AuditManager.instance.convertLastEventToAsync();
				TestPluginCall testcall = new TestPluginCall();
				testcall._id = new MidataId();
				testcall.handle = plugin.debugHandle;
				testcall.lang = lang;
				testcall.owner = subscription.owner.toString();
				testcall.path = visPath;
				testcall.resource = triggered.resource;
				testcall.token = token;
				testcall.resourceId = id;				
				testcall.answer = null;
				testcall.answerStatus = TestPluginCall.NOTANSWERED;		
				testcall.transactionId = triggered.transactionId;
				testcall.add();
				
				getContext().getSystem().scheduler().scheduleOnce(
					      Duration.ofSeconds(1),
					      getSelf(), new RecheckMessage(testcall._id, eventId, 0), getContext().dispatcher(), getSender());
				return;
				//getSender().tell(new MessageResponse(null,0), getSelf());				
			}
		}				
		//System.out.println("prcApp6");
		  String token1 = token;
		  String token2 = "";
		  if (token.length() > 3000) {
			  token1 = token.substring(0, 3000);
			  token2 = "token:"+token.substring(3000);
		  }
		  AccessLog.log("Build process...");
		  //AccessLog.log("/usr/bin/firejail --quiet --whitelist="+visDir+" "+nodepath+" "+visPath+" "+token1+" "+lang+" http://localhost:9001 "+subscription.owner.toString()+" "+id+" "+token2);
		  Process p = new ProcessBuilder("/usr/bin/firejail","--quiet","--whitelist="+visDir,nodepath, visPath, token1, lang, "http://localhost:9001", subscription.owner.toString(), id, token2).redirectError(Redirect.PIPE).start();
		  //System.out.println("Output...");
		  PrintWriter out = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));		  
		  out.println(triggered.resource);
		  out.close();
		  //System.out.println("Output done...");
		  InputStreamCollector result = new InputStreamCollector(p.getInputStream());
		  result.start();
		  InputStreamCollector errors = new InputStreamCollector(p.getErrorStream());
		  errors.start();
		  //System.out.println("Input...");
		  p.waitFor();
		  //System.out.println("Wait for finished...");
		  AccessLog.log("Wait for input...");
		  result.join();
		  errors.join();
		
		  String r = result.getResult();
		  String err = errors.getResult();
		  AccessLog.log("result='"+r+"'");
		  AccessLog.log("errors='"+err+"'");
		  
		  
		  Stats.finishRequest(TRIGGER, triggered.getDescription(), null, ""+p.exitValue(), Collections.emptySet());
		  subscription.ok();
		  if (r != null && r.length() >0) {
			 if (p.exitValue() != 0) {
				 if (err == null || err.trim().length()==0) err = "Status "+p.exitValue()+", script provided no error message.";
				 reportPluginProblem(plugin, err);
				 
			 }
			 sender.tell(new MessageResponse(r, p.exitValue(), plugin.filename), getSelf());  
		  } else {
			  String response = "";
			  if (p.exitValue()!=0) {
				  response = "Script execution error";
				  if (err == null || err.trim().length()==0) err = "Status "+p.exitValue()+", script provided no error message.";
				  reportPluginProblem(plugin, err);
				  AuditManager.instance.fail(400, "Script error", "error.plugin");
			  }
			 sender.tell(new MessageResponse(response, p.exitValue(), plugin.filename), getSelf());
		  } 
		  if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());	    
		  AccessLog.log("Response sended");		 		  
		} catch (Exception e) {		
			try { SubscriptionData.fail(subscription._id); } catch (Exception e2) {}
			sender.tell(new MessageResponse("Failed: "+e.toString(),-1, plugin.filename), getSelf());	
			reportPluginProblem(plugin, "Failed: "+e.toString());
			AuditManager.instance.fail(500, "Script error", "error.internal");
			if (triggered.getTransactionId()!=null) getSender().tell(new TriggerCountMessage(triggered.getTransactionId(), -1), getSelf());
		} 				
	}
	
	void answerDebugCall(RecheckMessage msg) {
		String path = "SubscriptionProcessor/answerDebugCall";
		long st = ActionRecorder.start(path);
		AccessLog.logStart("jobs", "debug recheck");
		try {
			TestPluginCall call = TestPluginCall.getById(msg.id);
			if (call != null) {
				if (call.answerStatus != TestPluginCall.NOTANSWERED) {
					AuditManager.instance.resumeAsyncEvent(msg.eventId);
					TestPluginCall.delete(call.handle, call._id);
					getSender().tell(new MessageResponse(call.answer, call.answerStatus, null), getSelf());
					if (call.answerStatus != 0) {
						AuditManager.instance.fail(400, "error during debug", "error.plugin");
					} else {
						AuditManager.instance.success();
					}
					if (call.transactionId!=null) getSender().tell(new TriggerCountMessage(call.transactionId, -1), getSelf());
				} else {
					if (msg.count < 50) {
						getContext().getSystem().scheduler().scheduleOnce(
							      Duration.ofSeconds(1),
							      getSelf(), new RecheckMessage(msg.id, msg.eventId, msg.count + 1), getContext().dispatcher(), getSender());
					
					} else {
						AuditManager.instance.resumeAsyncEvent(msg.eventId);
						AuditManager.instance.fail(500, "error during debug", "error.timeout");
					}
				}
			}
		} catch (Exception e) {}
		finally {
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
}

class RecheckMessage {
	public final MidataId id;
	public final MidataId eventId;
	public int count;
	public RecheckMessage(MidataId id, MidataId eventId, int count) {
		this.id = id;
		this.eventId = eventId;
		this.count = count;
	}
}