package utils.messaging;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import models.Consent;
import models.MidataId;
import models.Model;
import models.SubscriptionData;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.collections.Sets;
import utils.exceptions.AppException;
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
	 * Type of resource that has changed
	 */
	final String type;
	
	/**
	 * The resource that was changed
	 */
	final Model resource;
	
	SubscriptionTriggered(MidataId affected, String type, Model resource) {
		this.affected = affected;
		this.type = type;
		this.resource = resource;
	}

	public MidataId getAffected() {
		return affected;
	}

	public String getType() {
		return type;
	}

	public Model getResource() {
		return resource;
	}
			
}

/**
 * Listen to resource changes and propagate to corresponding processor
 *
 */
class SubscriptionChecker extends AbstractActor {

	private Set<MidataId> withSubscription;
	private ActorRef processor;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(ResourceChange.class, this::resourceChange)
			   .match(SubscriptionChange.class, this::subscriptionChange)
			   .build();
	}
		
	@Override
	public void preStart() throws Exception {
		super.preStart();
		System.out.println("Subscription Checker startup");
		List<SubscriptionData> allsubsriptions = SubscriptionData.getAllActive(Sets.create("owner"));
		withSubscription = new HashSet<MidataId>();
		for (SubscriptionData sub : allsubsriptions) withSubscription.add(sub.owner);
		
		processor = this.context().actorOf(new RoundRobinPool(5).props(Props.create(SubscriptionProcessor.class)), "subscriptionProcessor");
	}

	void resourceChange(ResourceChange change) {		
		Set<MidataId> affected = new HashSet<MidataId>();
		
		if (change.getResource() instanceof Consent) {
			Consent consent = (Consent) change.getResource();
					
			if (consent.owner != null) affected.add(consent.owner);
			if (consent.authorized != null) affected.addAll(consent.authorized);
		}
		
		for (MidataId affectedUser : affected) {
			if (withSubscription.contains(affectedUser)) {
				
				SubscriptionTriggered trigger = new SubscriptionTriggered(affectedUser, change.type, change.resource);				
				processor.tell(trigger, getSelf());
			}
		}
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
			List<SubscriptionData> allMatching = SubscriptionData.getByOwnerAndFormat(triggered.affected, triggered.type, SubscriptionData.ALL);
			
			for (SubscriptionData subscription : allMatching) {				
				if (subscription.active && checkNotExpired(subscription)) {							
					Subscription fhirSubscription = SubscriptionResourceProvider.subscription(subscription);
					SubscriptionChannelComponent channel = fhirSubscription.getChannel();
					if (channel.getType().equals(SubscriptionChannelType.RESTHOOK)) {					
						processRestHook(subscription._id, triggered, channel);
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
	
}