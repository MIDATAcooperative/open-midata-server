package utils.messaging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Subscription;
import org.hl7.fhir.dstu3.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.dstu3.model.Subscription.SubscriptionChannelType;

import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.SubscriptionData;
import models.User;
import models.enums.ConsentStatus;
import models.enums.MessageReason;
import models.enums.UserStatus;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.SubscriptionResourceProvider;
import utils.sync.Instances;

/**
 * Do notifications for FHIR subscriptions
 *
 */
public class SubscriptionProcessor extends AbstractActor {

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(SubscriptionTriggered.class, this::processSubscription)	      
			   .build();
	}
	
	void processSubscription(SubscriptionTriggered triggered) {
		try {		
			List<SubscriptionData> allMatching = null;
			if (triggered.getApp() != null) {
			  Plugin pl = Plugin.getById(triggered.getApp());
			  if (pl == null) return;
			  for (SubscriptionData dat : pl.defaultSubscriptions) {
				  if (dat.format.equals(triggered.getType())) {
					  if (allMatching == null) allMatching = new ArrayList<SubscriptionData>(2);
					  allMatching.add(dat);
				  }
			  }
			  if (allMatching == null) return;
			} else {
			  allMatching = SubscriptionData.getByOwnerAndFormat(triggered.affected, triggered.type, SubscriptionData.ALL);
			}
			for (SubscriptionData subscription : allMatching) {				
				if (subscription.active && (subscription.content == null || subscription.content.equals(triggered.getEventCode())) && checkNotExpired(subscription)) {							
					Subscription fhirSubscription = SubscriptionResourceProvider.subscription(subscription);
					SubscriptionChannelComponent channel = fhirSubscription.getChannel();
					if (channel.getType().equals(SubscriptionChannelType.RESTHOOK)) {					
						processRestHook(subscription._id, triggered, channel);
					} else if (channel.getType().equals(SubscriptionChannelType.MESSAGE)) {
						String endpoint = channel.getEndpoint();
						if (endpoint != null && endpoint.startsWith("node://")) processApplication(subscription, triggered, channel);
					} else if (channel.getType().equals(SubscriptionChannelType.EMAIL)) {
						processEmail(subscription, triggered, channel);
					}
					
				}
			}
		} catch (Exception e) {
			ErrorReporter.report("Subscriptions", null, e);
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
	   //System.out.println("CALLING REST HOOK!");
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
		   System.out.println("A");
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
	}
	
	void processApplication(SubscriptionData subscription, SubscriptionTriggered triggered, SubscriptionChannelComponent channel) throws InternalServerException {
		final String nodepath = InstanceConfig.getInstance().getConfig().getString("node.path");
		Plugin plugin = Plugin.getById(subscription.app);
		if (plugin == null) return;

		String endpoint = channel.getEndpoint();
		
		String cmd = endpoint.substring("node://".length());
		String visPath =  InstanceConfig.getInstance().getConfig().getString("visualizations.path")+"/"+plugin.filename+"/"+cmd;
		
		String handle = ServiceHandler.decrypt(subscription.session);
		
		if (handle == null) {
			SubscriptionData.setError(subscription._id, "Background service key expired");
		}
		
		User user = User.getById(subscription.owner, Sets.create("status", "role", "language"));
		
		if (user==null || user.status.equals(UserStatus.DELETED) || user.status.equals(UserStatus.BLOCKED)) return;
						
		SpaceToken tk = null;
		if (plugin.type.equals("mobile") || plugin.type.equals("service")) {
			Set<MobileAppInstance> mais = MobileAppInstance.getByApplicationAndOwner(plugin._id, user._id, Sets.create("status"));
			if (mais.isEmpty()) return;
			MidataId appInstanceId = null;
			for (MobileAppInstance mai : mais) {
				if (mai.status.equals(ConsentStatus.ACTIVE)) { appInstanceId = mai._id; break; }
			}
			if (appInstanceId == null) return;			
			tk = new SpaceToken(handle, appInstanceId, subscription.owner, user.getRole(), null, subscription.app, subscription.owner);			
		} else {
			tk = new SpaceToken(handle, subscription.instance, subscription.owner, user.getRole(), null, null, subscription.owner);
		}
		
		String token = tk.encrypt();
		
		final String lang = user.language != null ? user.language : InstanceConfig.getInstance().getDefaultLanguage();
		final String id = triggered.getResourceId() != null ? triggered.getResourceId().toString() : "-";
		try {
		  System.out.println("Build process...");
		  Process p = new ProcessBuilder(nodepath, visPath, token, lang, "http://localhost:9001", subscription.owner.toString(), id).redirectError(Redirect.INHERIT).start();
		  System.out.println("Output...");
		  PrintWriter out = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));		  
		  out.println(triggered.resource);
		  out.close();
		  System.out.println("Output done...");
		  InputStreamCollector result = new InputStreamCollector(p.getInputStream());
		  result.start();
		  System.out.println("Input...");
		  p.waitFor();
		  System.out.println("Wait for finished...");
		  result.join();
		  System.out.println("Wait for input...");
		  System.out.println(result.getResult());
		  String r = result.getResult();
		  if (r != null && r.length() >0) {
			 getSender().tell(new MessageResponse(r, p.exitValue()), getSelf());  
		  } else {
			 getSender().tell(new MessageResponse(null, p.exitValue()), getSelf());
		  } 
		  		    
		  System.out.println("Response sended");
		  return;
		  
		} catch (InterruptedException e) {
			
		} catch (IOException e2) {
			
		}
		getSender().tell(new MessageResponse(null,-1), getSelf());
	}	
}