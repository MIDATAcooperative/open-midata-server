package utils.messaging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Subscription;
import org.hl7.fhir.dstu3.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.dstu3.model.Subscription.SubscriptionChannelType;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

import static akka.pattern.PatternsCS.ask;
import controllers.AutoRun.ImportResult;
import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Model;
import models.Plugin;
import models.Record;
import models.Space;
import models.SubscriptionData;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.MessageReason;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.SubscriptionResourceProvider;
import utils.sync.Instances;

/**
 * Manager for FHIR Subscriptions to Resource Changes
 *
 */
public class SubscriptionManager {

	protected static WSClient ws;
	private static ActorSystem system;
	
	private static ActorRef subscriptionChecker;
	
	public static void init(ActorSystem system1, WSClient ws1) {
		system = system1;
	   	ws = ws1;
	   	
	   	subscriptionChecker = system.actorOf(Props.create(SubscriptionChecker.class), "subscriptionChecker");
	}
	
	
	public static void resourceChange(Consent consent) {	
		AccessLog.log("Resource change: Consent");
		subscriptionChecker.tell(new ResourceChange("fhir/Consent", consent), ActorRef.noSender());							
	}
	
	public static void resourceChange(Record record) {	
		AccessLog.log("Resource change: "+record.format);
		subscriptionChecker.tell(new ResourceChange(record.format, record), ActorRef.noSender());							
	}
	
	public static String messageToProcess(MidataId executor, MidataId app, String eventCode, String bundleJSON, boolean doasync) {
		AccessLog.log("Process Message: user="+executor.toString()+" app="+app.toString()+" async="+doasync);
		if (doasync) {
			subscriptionChecker.tell(new ProcessMessage(executor, app, eventCode, bundleJSON), ActorRef.noSender());
			return null;
		} else {
			CompletableFuture<Object> answer = ask(subscriptionChecker, new ProcessMessage(executor, app, eventCode, bundleJSON), 1000*60).toCompletableFuture();
			Object obj = answer.join();
			if (obj instanceof MessageResponse) {
				MessageResponse res = (MessageResponse) obj;
				if (res.getErrorcode() != 0) throw new InternalErrorException("App execution failed with return code "+res.getErrorcode());
				return res.getResponse();
			}
			return obj.toString();
		}
	}
	
	public static void activateSubscriptions(MidataId userId, Plugin plugin) throws AppException {
	    if (plugin.defaultSubscriptions != null) {
	    	for (SubscriptionData dat : plugin.defaultSubscriptions) {
	    		if (!dat.format.equals("fhir/MessageHeader")) {
	    			SubscriptionData newdata = new SubscriptionData();
	    			newdata._id = new MidataId();
	    			newdata.format = dat.format;
	    			newdata.active = true;
	    			newdata.app = plugin._id;
	    			newdata.content = dat.content;
	    			newdata.endDate = dat.endDate;
	    			newdata.fhirSubscription = dat.fhirSubscription;
	    			newdata.format = dat.format;
	    			newdata.lastUpdated = System.currentTimeMillis();
	    			newdata.owner = userId;
	    			newdata.add();
	    		}
	    	}
	    }
	}
	
	public static void deactivateSubscriptions(MidataId userId, Plugin app, MidataId currentlyDeactivating) throws AppException {
	  if (app.type.equals("mobile") || app.type.equals("service")) {
	    Set<MobileAppInstance> mais = MobileAppInstance.getByApplicationAndOwner(app._id, userId, Sets.create("_id", "status"));
	    for (MobileAppInstance mai : mais) {
		    if (mai.status.equals(ConsentStatus.ACTIVE) && !mai._id.equals(currentlyDeactivating)) return;
	    }
	  } else {
	     Set<Space> spaces = Space.getByOwnerVisualization(userId, app._id, Sets.create("_id"));
	     if (spaces.size() > 1) return;
	  }
	  
	  List<SubscriptionData> data = SubscriptionData.getByOwner(userId, CMaps.map("app", app), SubscriptionData.ALL);
	  for (SubscriptionData dat : data) {
		  SubscriptionData.setOff(dat._id);
	  }
	}
	
	public static void subscriptionChange(SubscriptionData subscriptionData) {
		Instances.cacheClear("SubscriptionData", subscriptionData.owner);
		//subscriptionChecker.tell(new SubscriptionChange(subscriptionData.owner), ActorRef.noSender());
	}	
	
	public static void subscriptionChangeLocal(MidataId owner) {
		subscriptionChecker.tell(new SubscriptionChange(owner), ActorRef.noSender());
	}
	    
}

/**
 * Message: A resource has changed
 *
 */
class ResourceChange {
	
	/**
	 * Type of resource that changed. (example: fhir/Consent )
	 */
	final String type;
	
	/**
	 * The resource that changed
	 */
	final Model resource;
	
	ResourceChange(String type, Model resource) {
		this.type = type;
		this.resource = resource;
	}

	public String getType() {
		return type;
	}

	public Model getResource() {
		return resource;
	}			
}

/**
 * A FHIR Message should be processed
 * @author alexander
 *
 */
class ProcessMessage {
	
	final String message;
	
	final MidataId executor;
	
	final MidataId app;
	
	final String eventCode;
	
	ProcessMessage(MidataId executor, MidataId app, String eventCode, String message) {
		this.app = app;
		this.executor = executor;
		this.message = message;
		this.eventCode = eventCode;
	}

	public String getMessage() {
		return message;
	}

	public MidataId getExecutor() {
		return executor;
	}

	public MidataId getApp() {
		return app;
	}

	public String getEventCode() {
		return eventCode;
	}
	
	
}

class MessageResponse {
	final String response;
	
	final int errorcode;
	
	MessageResponse(String response, int errorcode) {
		this.response = response;
		this.errorcode = errorcode;
	}

	public String getResponse() {
		return response;
	}
	
	
	
	public int getErrorcode() {
		return errorcode;
	}

	public String toString() {
		return response;
	}
		
}

/**
 * Message: A subscription of a user has changed
 *
 */
class SubscriptionChange {
	
	/**
	 * The id of the user of which the subscription changed
	 */
	final MidataId owner;
	
	public SubscriptionChange(MidataId owner) {
		this.owner = owner;
	}

	public MidataId getOwner() {
		return owner;
	}
		
}

/**
 * Message: A subscription of a user has been triggered by a resource change
 *
 */
class SubscriptionTriggered {
	/**
	 * id of user who is owner of subscription
	 */
	final MidataId affected;
	
	/**
	 * app of triggered subscription 
	 */
	final MidataId app;
	
	/**
	 * Type of resource that has changed
	 */
	final String type;
	
	/**
	 * The resource that was changed
	 */
	final String resource;
	
	/**
	 * Optional event code
	 */
	final String eventCode;
	
	SubscriptionTriggered(MidataId affected, MidataId app, String type, String eventCode, String resource) {
		this.affected = affected;
		this.app = app;
		this.type = type;
		this.resource = resource;
		this.eventCode = eventCode;
	}

	public MidataId getAffected() {
		return affected;
	}
		
	public MidataId getApp() {
		return app;
	}

	public String getType() {
		return type;
	}

	public String getResource() {
		return resource;
	}

	public String getEventCode() {
		return eventCode;
	}
	
	
			
}

/**
 * Listen to resource changes and propagate to corresponding processor
 *
 */
class SubscriptionChecker extends AbstractActor {

	private Set<MidataId> withSubscription;
	//private Set<MidataId> appWithSubscription;
	private ActorRef processor;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(ResourceChange.class, this::resourceChange)
			   .match(ProcessMessage.class, this::processMessage)
			   .match(SubscriptionChange.class, this::subscriptionChange)
			   .match(MessageResponse.class, this::messageResponse)
			   .build();
	}
		
	@Override
	public void preStart() throws Exception {
		super.preStart();
		System.out.println("Subscription Checker startup");
		List<SubscriptionData> allsubsriptions = SubscriptionData.getAllActive(Sets.create("owner", "app"));
		withSubscription = new HashSet<MidataId>();
		for (SubscriptionData sub : allsubsriptions) withSubscription.add(sub.owner);
		//appWithSubscription = new HashSet<MidataId>();
		//for (SubscriptionData sub : allsubsriptions) if (sub.app != null) appWithSubscription.add(sub.app);
		
		processor = this.context().actorOf(new RoundRobinPool(5).props(Props.create(SubscriptionProcessor.class)), "subscriptionProcessor");
	}

	void resourceChange(ResourceChange change) {		
		Set<MidataId> affected = new HashSet<MidataId>();
		String resource = null;
		String content = null;
		if (change.getResource() instanceof Consent) {
			Consent consent = (Consent) change.getResource();
					
			if (consent.owner != null) affected.add(consent.owner);
			if (consent.authorized != null) affected.addAll(consent.authorized);
			
			resource = consent.fhirConsent.toString();
			content = "Consent";
		} else if (change.getResource() instanceof Record) {
			Record record = (Record) change.getResource();
			content = record.content;
			affected.add(record.owner);
			try {
			  Set<MidataId> alsoAffected = RecordManager.instance.findAllSharingAps(record.owner, record);
			  RecordManager.instance.clear();
			  if (!alsoAffected.isEmpty()) {
			    Set<Consent> affectedConsents = Consent.getActiveByIdsAndOwner(alsoAffected, record.owner, Sets.create("_id", "authorized", "entityType"));
			    for (Consent c : affectedConsents) {
			    	if (c.entityType == null || c.entityType.equals(EntityType.USER)) {
			    	  affected.addAll(c.authorized);
			    	} else {
			    	  affected.addAll(c.authorized);
			    	  for (MidataId authGroup : c.authorized) {
			    		  Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByGroup(authGroup);
			    		  for (UserGroupMember ugm : ugms) affected.add(ugm.member);
			    	  }
			    	}
			    }
			  }
			} catch (AppException e) {}
		}
		
		for (MidataId affectedUser : affected) {
			if (withSubscription.contains(affectedUser)) {
				
				SubscriptionTriggered trigger = new SubscriptionTriggered(affectedUser, null, change.type, content, resource);				
				processor.tell(trigger, getSelf());
			}
		}
	}
	
	void processMessage(ProcessMessage message) {
		SubscriptionTriggered trigger = new SubscriptionTriggered(message.executor, message.getApp(), "fhir/MessageHeader", message.getEventCode(), message.getMessage());				
		processor.forward(trigger, getContext());
	}
	
	void messageResponse(MessageResponse message) {
		getSender().tell(message, getSelf());
	}
	
	void subscriptionChange(SubscriptionChange change) {
		try {
			MidataId owner = change.getOwner();
			if (SubscriptionData.existsActiveByOwner(owner)) {
				withSubscription.add(owner);
			} else {
				withSubscription.remove(owner);
			}
		} catch (Exception e) {
			ErrorReporter.report("SubscriptionChecker", null, e);
		}
	}
		
}

/**
 * Do notifications for FHIR subscriptions
 *
 */
class SubscriptionProcessor extends AbstractActor {

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
		
		
		try {
		  System.out.println("Build process...");
		  Process p = new ProcessBuilder(nodepath, visPath).redirectError(Redirect.INHERIT).start();
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