/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.messaging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.bson.BSONObject;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;

import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import controllers.Plugins;
import akka.actor.ActorRef;
import models.APSNotExistingException;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.SubscriptionData;
import models.TestPluginCall;
import models.User;
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
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.SubscriptionResourceProvider;
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
		try {		
			List<SubscriptionData> allMatching = null;
		
			allMatching = SubscriptionData.getByOwnerAndFormat(triggered.affected, triggered.type, SubscriptionData.ALL);
				
			boolean anyAnswered = false;
			
			for (SubscriptionData subscription : allMatching) {	
				System.out.println("ok:"+subscription.active+" "+subscription.content+" "+triggered.getEventCode());
				boolean answered = false;
				if (subscription.active && (subscription.content == null || subscription.content.equals("MessageHeader") || subscription.content.equals(triggered.getEventCode())) && checkNotExpired(subscription)) {
					if (triggered.getType().equals("fhir/MessageHeader") && (!triggered.getApp().equals(subscription.app))) continue;
					
					
					//System.out.println("ok3");
					Subscription fhirSubscription = SubscriptionResourceProvider.subscription(subscription);
					SubscriptionChannelComponent channel = fhirSubscription.getChannel();
					
					Stats.startRequest();
					Stats.setPlugin(subscription.app);
					//System.out.println("type="+channel.getType().toString());
					if (channel.getType().equals(SubscriptionChannelType.RESTHOOK)) {					
						processRestHook(subscription._id, triggered, channel);
					} else if (channel.getType().equals(SubscriptionChannelType.MESSAGE)) {
						String endpoint = channel.getEndpoint();
						if (endpoint != null && endpoint.startsWith("node://")) {
							answered = processApplication(subscription, triggered, channel) || answered;
							if (answered) anyAnswered = true;
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
						getSender().tell(new MessageResponse("No action",-1, app), getSelf());
						anyAnswered = true;
					}
				}
								
			}
			
			if (!anyAnswered) {
				String app = triggered.getApp() != null ? triggered.getApp().toString() : null;
				getSender().tell(new MessageResponse("No action",-1, app), getSelf());
			}
			
		} catch (Exception e) {			
			ErrorReporter.report("Subscriptions", null, e);
			getSender().tell(new MessageResponse("Exception: "+e.toString(),-1, null), getSelf());
			
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
	   System.out.println("CALLING REST HOOK!");
	   CompletionStage<WSResponse> response = request.execute("POST");
	   response.whenComplete((out, exception) -> {
		   //System.out.println("COMPLETE REST HOOK");
		   String error = null;
		   if (out != null && out.getStatus() >= 400) {
			   error = "status "+out.getStatus();
		   }
		   if (exception != null) {
			   if (exception instanceof CompletionException) exception = exception.getCause();
			   error = exception.toString();
		   }
		   //System.out.println("A");
		   if (error != null) {
			   try {
				   SubscriptionData.setError(subscription, new Date().toString()+": "+error);
			   } catch (Exception e) {
				   System.out.println("ERROR");
				   ErrorReporter.report("SubscriptionManager", null, e);
			   }
		   }		   
	   });
	}
	
	void processEmail(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws AppException {
		Map<String,String> replacements = new HashMap<String, String>();
		System.out.println("process email type="+triggered.getType()+"  "+triggered.getEventCode());
		if (triggered.getType().equals("fhir/MessageHeader")) {
		  Messager.sendMessage(subscription.app, MessageReason.PROCESS_MESSAGE, triggered.getEventCode(), Collections.singleton(subscription.owner), null, replacements);			
		} else {
		  Messager.sendMessage(subscription.app, MessageReason.RESOURCE_CHANGE, triggered.getType(), Collections.singleton(subscription.owner), null, replacements);
		}
		getSender().tell(new MessageResponse(null,0,"internal:mail"), getSelf());
	}
	
	void processSMS(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws AppException {
		Map<String,String> replacements = new HashMap<String, String>();
		System.out.println("process sms type="+triggered.getType()+"  "+triggered.getEventCode());
		if (triggered.getType().equals("fhir/MessageHeader")) {
		  Messager.sendMessage(subscription.app, MessageReason.PROCESS_MESSAGE, triggered.getEventCode(), Collections.singleton(subscription.owner), null, replacements, MessageChannel.SMS);			
		} else {
		  Messager.sendMessage(subscription.app, MessageReason.RESOURCE_CHANGE, triggered.getType(), Collections.singleton(subscription.owner), null, replacements, MessageChannel.SMS);
		}
		getSender().tell(new MessageResponse(null,0,"internal:sms"), getSelf());
	}
	
	boolean processApplication(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws AppException {
		System.out.println("prcApp app="+subscription.app);
		
		Plugin plugin = Plugin.getById(subscription.app);
		if (plugin == null) {
			subscription.disable();
			return false;
		}
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
			return true;
		}
		
		User user = null;
		
		if (!plugin.type.equals("analyzer") && !plugin.type.equals("external") && !plugin.type.equals("endpoint")) {
			user = User.getById(subscription.owner, Sets.create("status", "role", "language", "developer"));
			//System.out.println("prcApp4");
			if (user==null || user.status.equals(UserStatus.DELETED) || user.status.equals(UserStatus.BLOCKED)) {
				subscription.disable();
				return false;
			}
		}
        final ActorRef sender = getSender();						
		SpaceToken tk = null;
		if (plugin.type.equals("mobile") || plugin.type.equals("service") || plugin.type.equals("analyzer") || plugin.type.equals("external")) {
			AccessLog.log("RIGHT PATH");
			System.out.println("RIGHT PATH "+plugin.name);
			Set<MobileAppInstance> mais = MobileAppInstance.getByApplicationAndOwner(plugin._id, subscription.owner, Sets.create("status"));
			if (mais.isEmpty()) return false;
			MidataId appInstanceId = null;
			for (MobileAppInstance mai : mais) {
				if (mai.status.equals(ConsentStatus.ACTIVE)) { appInstanceId = mai._id; break; }
			}
			if (appInstanceId == null) return false;	
			//AccessLog.log("HANDLE="+handle);

			if (subscription.app == null) throw new NullPointerException(); 
			tk = new SpaceToken(handle, appInstanceId, subscription.owner, user != null ? user.getRole() : UserRole.ANY, null, subscription.app, subscription.owner);			
			AccessLog.log("HANDLEPOST="+tk.handle+" space="+tk.spaceId.toString()+" app="+tk.pluginId);
			
			runProcess(getSender(), plugin, triggered, subscription, user, tk.encrypt(), endpoint);
		} else if (plugin.type.equals("oauth2")) {
			System.out.println("NEW OAUTH2 - 1");
			try {
				KeyManager.instance.continueSession(handle, subscription.owner);
               			
				BSONObject oauthmeta = RecordManager.instance.getMeta(subscription.owner, subscription.instance, "_oauth");
				if (oauthmeta != null) {
					System.out.println("NEW OAUTH2 - 2");
					if (oauthmeta.get("refreshToken") != null) {
						tk = new SpaceToken(handle, subscription.instance, subscription.owner, user.getRole(), null, null, subscription.owner);
						final String token = tk.encrypt();
						System.out.println("NEW OAUTH2 - 3");
						Plugin plugin2 = Plugin.getById(plugin._id, Sets.create("type", "filename", "name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "tokenExchangeParams"));
						final User user1 = user;
						Plugins.requestAccessTokenOAuth2FromRefreshToken(handle, subscription.owner, plugin2, subscription.instance.toString(), oauthmeta.toMap()).thenAcceptAsync(success1 -> {
							    boolean success = (Boolean) success1;
								if (success) {
									System.out.println("NEW OAUTH2 - 4");							
									runProcess(sender, plugin, triggered, subscription, user1, token, endpoint);
								} else {
									System.out.println("NEW OAUTH2 - 4B");
									sender.tell(new MessageResponse("OAuth 2 failed",-1, plugin.filename), getSelf());	
								}
						});
					} else {
						sender.tell(new MessageResponse("OAuth 2 no refresh token",-1, plugin.filename), getSelf());						
					}
				} else {
					sender.tell(new MessageResponse("OAuth 2 no data",-1, plugin.filename), getSelf());
				}	
			} catch (APSNotExistingException e) {
				subscription.disable();
				sender.tell(new MessageResponse("Space no longer existing - disabled",-1, plugin.filename), getSelf());
			} finally {
				ServerTools.endRequest();
			}
		} else {
			//AccessLog.log("BPART HANDLE="+handle);
			tk = new SpaceToken(handle, subscription.instance, subscription.owner, user.getRole(), null, null, subscription.owner);
			runProcess(getSender(), plugin, triggered, subscription, user, tk.encrypt(), endpoint);
		}
		return true;
	}	
	
	private void runProcess(ActorRef sender, Plugin plugin, SubscriptionTriggered triggered, SubscriptionData subscription, User user, String token, String endpoint) {
		try {
		String cmd = endpoint.substring("node://".length());
		String visDir = InstanceConfig.getInstance().getConfig().getString("visualizations.path");
		String visPath =  visDir+"/"+plugin.filename+"/"+cmd;
		final String lang = (user != null && user.language != null) ? user.language : InstanceConfig.getInstance().getDefaultLanguage();
		final String id = triggered.getResourceId() != null ? triggered.getResourceId().toString() : "-";
		final String nodepath = InstanceConfig.getInstance().getConfig().getString("node.path");
		boolean testing = InstanceConfig.getInstance().getInstanceType().getDebugFunctionsAvailable() && (plugin.status.equals(PluginStatus.DEVELOPMENT) || plugin.status.equals(PluginStatus.BETA));
		//System.out.println("prcApp5");
		if (testing) {
			plugin = Plugin.getById(plugin._id, Plugin.ALL_DEVELOPER);
			if (plugin.debugHandle != null) {
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
				testcall.add();
				
				getContext().getSystem().scheduler().scheduleOnce(
					      Duration.ofSeconds(1),
					      getSelf(), new RecheckMessage(testcall._id, 0), getContext().dispatcher(), getSender());
				return;
				//getSender().tell(new MessageResponse(null,0), getSelf());				
			}
		}				
		//System.out.println("prcApp6");
		
		  System.out.println("Build process...");		  
		  Process p = new ProcessBuilder("/usr/bin/firejail","--quiet","--whitelist="+visDir,nodepath, visPath, token, lang, "http://localhost:9001", subscription.owner.toString(), id).redirectError(Redirect.INHERIT).start();
		  //System.out.println("Output...");
		  PrintWriter out = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));		  
		  out.println(triggered.resource);
		  out.close();
		  //System.out.println("Output done...");
		  InputStreamCollector result = new InputStreamCollector(p.getInputStream());
		  result.start();
		  //System.out.println("Input...");
		  p.waitFor();
		  //System.out.println("Wait for finished...");
		  result.join();
		  System.out.println("Wait for input...");
		  System.out.println(result.getResult());
		  String r = result.getResult();
		  
		  Stats.finishRequest(TRIGGER, triggered.getDescription(), null, ""+p.exitValue(), Collections.emptySet());
		  
		  if (r != null && r.length() >0) {
			 sender.tell(new MessageResponse(r, p.exitValue(), plugin.filename), getSelf());  
		  } else {
			 sender.tell(new MessageResponse(null, p.exitValue(), plugin.filename), getSelf());
		  } 
		  		    
		  System.out.println("Response sended");		 		  
		} catch (Exception e) {			
			sender.tell(new MessageResponse("Failed: "+e.toString(),-1, plugin.filename), getSelf());	
		} 				
	}
	
	void answerDebugCall(RecheckMessage msg) {
		try {
			TestPluginCall call = TestPluginCall.getById(msg.id);
			if (call != null) {
				if (call.answerStatus != TestPluginCall.NOTANSWERED) {
					TestPluginCall.delete(call.handle, call._id);
					getSender().tell(new MessageResponse(call.answer, call.answerStatus, null), getSelf());
				} else {
					if (msg.count < 50) {
						getContext().getSystem().scheduler().scheduleOnce(
							      Duration.ofSeconds(1),
							      getSelf(), new RecheckMessage(msg.id, msg.count + 1), getContext().dispatcher(), getSender());
					
					}
				}
			}
		} catch (Exception e) {}
	}
}

class RecheckMessage {
	public final MidataId id;
	public int count;
	public RecheckMessage(MidataId id, int count) {
		this.id = id;
		this.count = count;
	}
}