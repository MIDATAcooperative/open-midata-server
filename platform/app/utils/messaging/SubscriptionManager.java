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

import static akka.pattern.PatternsCS.ask;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.ConsistentHashingPool;
import akka.routing.ConsistentHashingRouter.ConsistentHashMapper;
import akka.routing.RoundRobinPool;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import models.Consent;
import models.HPUser;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Model;
import models.Plugin;
import models.Record;
import models.ServiceInstance;
import models.Space;
import models.SubscriptionData;
import models.User;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.ws.WSClient;
import scala.Option;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.PluginLoginCache;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;
import utils.fhir.MidataConsentResourceProvider;
import utils.fhir.FHIRServlet;
import utils.fhir.SubscriptionResourceProvider;
import utils.stats.ActionRecorder;
import utils.stats.Stats;
import utils.sync.Instances;

/**
 * Manager for FHIR Subscriptions to Resource Changes
 *
 */
public class SubscriptionManager {

	protected static WSClient ws;
	private static ActorSystem system;
	
	private static ActorRef subscriptionChecker;
	private static ActorRef accountWiper;
	
	public static void init(ActorSystem system1, WSClient ws1) {
		system = system1;
	   	ws = ws1;
	   	
	   	subscriptionChecker = system.actorOf(Props.create(SubscriptionChecker.class).withDispatcher("medium-work-dispatcher"), "subscriptionChecker");
	   	accountWiper = system.actorOf(Props.create(AccountWiper.class).withDispatcher("medium-work-dispatcher"), "accountWiper");
	}
	
	
	public static void resourceChange(AccessContext context, Consent consent) {	
		AccessLog.log("Resource change: Consent");
		
		MidataConsentResourceProvider prov = MidataConsentResourceProvider.getInstance(); 
		try {
		    String resource = prov.serialize(prov.readConsentFromMidataConsent(context, consent, consent.type != ConsentType.STUDYRELATED));
		    
		    AccessLog.log("CONSENT RES CHANGE: "+resource);		   
		    resourceChange(context, new ResourceChange("fhir/Consent", consent, false, resource, consent.owner));
		} catch (AppException e) {
			ErrorReporter.report("Subscripion processing", null, e);
		}
											
	}
		
	
	public static void resourceChange(Record record) {	
		AccessLog.log("Resource change: "+record.format);
		subscriptionChecker.tell(new ResourceChange(record.format, record, record.owner.equals(RuntimeConstants.instance.publicUser), record.owner), ActorRef.noSender());							
	}
	
	public static void resourceChange(AccessContext context, ResourceChange change) {
		SubscriptionBuffer buf = context.getRequestCache().getSubscriptionBuffer();
		if (buf != null) buf.add(change);
		else resourceChange(change);							
	}
	
	public static void resourceChange(ResourceChange change) {			
		subscriptionChecker.tell(change, ActorRef.noSender());							
	}
	
	public static void accountWipe(AccessContext context, MidataId userId) throws AppException {
		User user = User.getByIdAlsoDeleted(userId, User.ALL_USER_INTERNAL);
		if (user != null) {
			if (!user.status.isDeleted()) {
			  user.status = UserStatus.DELETED;
			  user.set("status", user.status);
			}
			
			accountWiper.tell(new AccountWipeMessage(userId, KeyManager.instance.currentHandle(context.getAccessor()), context.getAccessor(), 0, AuditManager.instance.convertLastEventToAsync(), user.status == UserStatus.FAKE), ActorRef.noSender());
		}
	}
	
	public static String messageToProcess(MidataId executor, MidataId app, String eventCode, String destination, String fhirVersion, String bundleJSON, Map<String,String> params, boolean doasync) {
		AccessLog.log("Process Message: user="+executor.toString()+" app="+app.toString()+" async="+doasync);
		if (doasync) {
			subscriptionChecker.tell(new ProcessMessage(executor, app, eventCode, destination, fhirVersion, bundleJSON, params), ActorRef.noSender());
			return null;
		} else {
			CompletableFuture<Object> answer = ask(subscriptionChecker, new ProcessMessage(executor, app, eventCode, destination, fhirVersion, bundleJSON, params), 1000*60).toCompletableFuture();
			Object obj = answer.join();
			if (obj instanceof MessageResponse) {
				MessageResponse res = (MessageResponse) obj;
				AccessLog.log("Received MessageResponse with errorcode="+res.getErrorcode());				
				if (res.getErrorcode() != 0) {
					if (Stats.enabled) Stats.addComment(res.getErrorcode()+": "+res.getResponse());
					throw new InternalErrorException("App execution failed with return code "+res.getErrorcode()+": "+res.getResponse());
				}
				return res.getResponse();
			}
			AccessLog.log("Received non MessageResponse answer");
			return obj.toString();
		}
	}
	
	public static void activateSubscriptions(MidataId userId, Plugin plugin, MidataId instance, boolean cleanold) throws AppException {
		AccessLog.log("Check Default Subscriptions:"+plugin.defaultSubscriptions);
	    if (plugin.defaultSubscriptions != null) {
	
	    	if (cleanold) {
		    	List<SubscriptionData> data = SubscriptionData.getByOwner(userId, CMaps.map("app", plugin._id), SubscriptionData.ALL);
		    		    	
		    	for (SubscriptionData dat : data) {
		    		dat.delete();
		    	}
	    	}
	    	
	    	for (SubscriptionData dat : plugin.defaultSubscriptions) {
	    		//if (!dat.format.equals("fhir/MessageHeader")) {
	    		    AccessLog.log("Add Subscription format="+dat.format);
	    			SubscriptionData newdata = new SubscriptionData();
	    			newdata._id = new MidataId();
	    			newdata.format = dat.format;
	    			newdata.active = true;
	    			newdata.app = plugin._id;
	    			newdata.instance = instance;
	    			newdata.content = dat.content;
	    			newdata.endDate = dat.endDate;
	    			newdata.fhirSubscription = dat.fhirSubscription;
	    			newdata.format = dat.format;
	    			newdata.lastUpdated = System.currentTimeMillis();
	    			newdata.owner = userId;
	    			newdata.session = ServiceHandler.encrypt(KeyManager.instance.currentHandle(userId));
	    			if (newdata.session == null) throw new NullPointerException();
	    			newdata.add();
	    			SubscriptionManager.subscriptionChange(newdata);
	    			if (newdata.format.equals("init")) { 
	    				subscriptionChecker.tell(new SubscriptionTriggered(userId, plugin._id, "init", "init", null, null, null, null, userId, null), ActorRef.noSender());
	    			}
	    		//}
	    	}
	    	
	    }
	}
	
	public static void deactivateSubscriptions(MidataId userId, Plugin app, MidataId currentlyDeactivating) throws AppException {
	  AccessLog.log("deactiveSubscription: user="+userId+" plugin="+app._id+" instance="+currentlyDeactivating);
	  if (app.type.equals("mobile") || app.type.equals("service") || app.type.equals("analyzer") || app.type.equals("external")) {
	    Set<MobileAppInstance> mais = MobileAppInstance.getByApplicationAndOwner(app._id, userId, Sets.create("_id", "status"));
	    for (MobileAppInstance mai : mais) {
		    if (mai.status.equals(ConsentStatus.ACTIVE) && !mai._id.equals(currentlyDeactivating)) return;
	    }
	  } else {
	     Set<Space> spaces = Space.getByOwnerVisualization(userId, app._id, Sets.create("_id"));
	     if (spaces.size() > 1) return;
	  }
	  
	  List<SubscriptionData> data = SubscriptionData.getByOwner(userId, CMaps.map("app", app._id), SubscriptionData.ALL);
	  AccessLog.log("deactivating: "+data.size());
	  for (SubscriptionData dat : data) {
		  dat.delete();//SubscriptionData.setOff(dat._id);
	  }
	}
	
	public static void deactivateSubscriptions(MidataId userId, MidataId instance) throws InternalServerException {
		  AccessLog.log("deactiveSubscription: user="+userId+" instance="+instance);
		  
		  List<SubscriptionData> data = SubscriptionData.getByOwner(userId, CMaps.map("instance", instance).map("active", true), SubscriptionData.ALL);
		  AccessLog.log("deactivating: "+data.size());
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
	
	public static void answer(String ref, int status, String content) {
		system.provider().resolveActorRef(ref).tell(new MessageResponse(content, status, null), ActorRef.noSender());
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
	
	final String fhir;
	
	final boolean isPublic;
	
	final MidataId resourceOwner;
	
	ResourceChange(String type, Model resource, boolean isPublic, MidataId resourceOwner) {
		this.type = type;
		this.resource = resource;
		this.fhir = null;
		this.isPublic = isPublic;
		this.resourceOwner = resourceOwner;
	}
	
	ResourceChange(String type, Model resource, boolean isPublic, String fhir, MidataId resourceOwner) {
		this.type = type;
		this.resource = resource;
		this.fhir = fhir;
		this.isPublic = isPublic;
		this.resourceOwner = resourceOwner;
	}

	public String getType() {
		return type;
	}

	public Model getResource() {
		return resource;
	}
	
	public String getFhir() {
		return fhir;
	}
	
	public String toString() {
		return type;
	}
	
	public boolean isPublic() {
		return isPublic;
	}
	
	public MidataId getResourceOwner() {
		return resourceOwner;
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
	
	final String fhirVersion;
	
	final Map<String, String> params;
	
	final String destination;
	
	ProcessMessage(MidataId executor, MidataId app, String eventCode, String destination, String fhirVersion, String message, Map<String, String> params) {
		this.app = app;
		this.executor = executor;
		this.message = message;
		this.eventCode = eventCode;
		this.fhirVersion = fhirVersion;
		this.params = params;
		this.destination = destination;
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

	public String getFhirVersion() {
		return fhirVersion;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public String getDestination() {
		return destination;
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
		
	public String toString() {
		return owner.toString();
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
			   .match(SubscriptionTriggered.class, this::forwardSubscription)
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
		
		final ConsistentHashMapper hashMapper =
			    new ConsistentHashMapper() {
			      @Override
			      public Object hashKey(Object message) {
			        if (message instanceof SubscriptionTriggered) {
			          return ((SubscriptionTriggered) message).affected.toString();
			        } else if (message instanceof RecheckMessage) {
				      return ((RecheckMessage) message).id.toString();
				    } else{
			          return null;
			        }
			      }
			    };

		
		 processor = this.context().actorOf(new ConsistentHashingPool(6).withHashMapper(hashMapper).withVirtualNodesFactor(10).props(Props.create(SubscriptionProcessor.class).withDispatcher("slow-work-dispatcher")), "subscriptionProcessor");
		 //processor = this.context().actorOf(new RoundRobinPool(5).props(Props.create(SubscriptionProcessor.class).withDispatcher("slow-work-dispatcher")), "subscriptionProcessor");
	}

	void resourceChange(ResourceChange change) {	
		String path = "SubscriptionChecker/resourceChange";
		long st = ActionRecorder.start(path);
		
		AccessLog.logStart("jobs", "resource change: "+change);
		try {
		Set<MidataId> affected = new HashSet<MidataId>();
		String resource = null;
		MidataId resourceId = null;
		String resourceVersion = null;
		MidataId sourceOwner = null;
		String content = null;
		if (change.getResource() instanceof Consent) {
			Consent consent = (Consent) change.getResource();
			sourceOwner = consent.owner;
			if (consent.owner != null) affected.add(consent.owner);
			if (consent.authorized != null) affected.addAll(consent.authorized);
			if (consent.observers != null) {
				for (MidataId appId : consent.observers) {
					try {
					  Set<ServiceInstance> instances = ServiceInstance.getByApp(appId, Sets.create("_id","executorAccount","status"));
					  for (ServiceInstance instance : instances) if (instance.status == UserStatus.ACTIVE) affected.add(instance.executorAccount);
					} catch (InternalServerException e) {
					  AccessLog.logException("subscription processing", e);
					  ErrorReporter.report("Subscripion processing", null, e);
					}
				}
								
			}
			resource = change.getFhir();
			resourceVersion = MidataConsentResourceProvider.getInstance().getVersion(consent);
			content = "Consent";
		} else if (change.getResource() instanceof Record) {
			Record record = (Record) change.getResource();
			content = record.content;
			sourceOwner = record.owner;
			resourceId = record._id;
			resourceVersion = record.version;
			affected.add(record.owner);
			
			if (change.isPublic() && change.getType().equals("fhir/ResearchStudy")) {
				AccessLog.log("change is public research study");
				List<SubscriptionData> allData = SubscriptionData.getAllActiveFormat("fhir/ResearchStudy", Sets.create("owner"));
				for (SubscriptionData dat : allData) affected.add(dat.owner);
			}
						
			Set<Consent> apis = Consent.getAllByOwner(record.owner, CMaps.map("status", ConsentStatus.ACTIVE).map("type", ConsentType.API), Sets.create("authorized", "_id", "sharingQuery", "type"), 0);			
			if (!apis.isEmpty()) {
				AccessContext context = ContextManager.instance.createSessionForDownloadStream(record.owner, UserRole.ANY);
				for (Consent consent : apis) {
					try {									 
					  if (RecordManager.instance.checkRecordInAccessQuery(context, record, consent)) {					
						  AccessLog.log("found matching external API consent:"+consent._id);
						  affected.addAll(consent.authorized);
					  } else {
						  AccessLog.log("non matching external API consent:"+consent._id);
					  }
					} catch (Exception e) {
					  ErrorReporter.report("Subscripion processing", null, e);
					  AccessLog.logException("subscription processing", e);
					}
				}
			} else AccessLog.log("no external APIs found.");				
			
			/* TODO : The following section is not broken it should just be performance optimized somehow.
			 * It determines which user accounts subscriptions need to be triggered by a resource change.
			 * Without this section only the account owner of the changed resource is notified.
			 */
			/*
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
			*/
		}
		AccessLog.log("total possible affected users="+affected.size());
		for (MidataId affectedUser : affected) {
			if (withSubscription.contains(affectedUser)) {
				AccessLog.log("trigger subscriptions for user="+affectedUser.toString());
				SubscriptionTriggered trigger = new SubscriptionTriggered(affectedUser, null, change.type, content, null, resource, resourceId, null, sourceOwner, resourceVersion);				
				processor.tell(trigger, getSelf());
			}
		}
		} catch (Exception e) {
			ErrorReporter.report("Subscripion processing", null, e);
			AccessLog.logException("subscription processing", e);
		} finally {
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
	
	void processMessage(ProcessMessage message) {
		String path = "SubscriptionChecker/processMessage";
		long st = ActionRecorder.start(path);
		
		if (message.getDestination() != null) {		
			AccessLog.logStart("jobs", "message with destination: "+message.getDestination());
			try {
			  Plugin sender = Plugin.getById(message.getApp());
			  MidataId targetApp = getTargetApp(message);			  
			  if (targetApp == null) {
				  AccessLog.log("App not found");
				  messageResponse(new MessageResponse("Target app not found.", 400, null));
				  return;
			  }
			  MidataId targetUser = getTargetUser(message, message.executor, targetApp);			  
			  if (targetUser == null) {
				  AccessLog.log("User not found");
				  messageResponse(new MessageResponse("User not found.", 400, null));
				  return;
			  }			  
			  SubscriptionTriggered trigger = new SubscriptionTriggered(targetUser, targetApp, "fhir/MessageHeader", message.getEventCode()+":"+sender.filename, message.getFhirVersion(), message.getMessage(), null, message.getParams(), message.getExecutor(), null);
			  processor.forward(trigger, getContext());
			} catch (AppException e) {
				messageResponse(new MessageResponse("Error", 500, null));
			} finally {
				ServerTools.endRequest();
			}
		} else {
		  SubscriptionTriggered trigger = new SubscriptionTriggered(message.executor, message.getApp(), "fhir/MessageHeader", message.getEventCode(), message.getFhirVersion(), message.getMessage(), null, message.getParams(), message.getExecutor(), null);
		  processor.forward(trigger, getContext());
		}
		
		ActionRecorder.end(path, st);
	}
	
	private MidataId getTargetUser(ProcessMessage message, MidataId currentUserId, MidataId appId) throws AppException {
		String dest = message.getDestination();
		if (dest.startsWith("patient://")) {
		   User user = Member.getByEmail(dest.substring("patient://".length()), Sets.create("_id"));
		   return user != null ? user._id : null;
		} else if (dest.startsWith("practitioner://")) {
			User user = HPUser.getByEmail(dest.substring("practitioner://".length()), Sets.create("_id"));
			return user != null ? user._id : null;
		} else if (dest.startsWith("Patient/")) {
			User user = Member.getById(MidataId.from(dest.substring("Patient/".length())), Sets.create("_id"));
			return user != null ? user._id : null;
		} else if (dest.startsWith("Practitioner/")) {
			User user = HPUser.getById(MidataId.from(dest.substring("Practitioner/".length())), Sets.create("_id"));
			return user != null ? user._id : null;
		} else if (dest.startsWith("service://")) {
			//String appName = dest.substring("service://".length());
			Set<MobileAppInstance> insts = MobileAppInstance.getActiveByApplicationAndOwner(appId, currentUserId, MobileAppInstance.APPINSTANCE_ALL);
			for (MobileAppInstance appInst : insts) {
				if (appInst.type == ConsentType.API) {
					Set<MidataId> executors = appInst.authorized;
					if (!executors.isEmpty()) {
						MidataId executor = executors.iterator().next();
						return executor;
					}
				}
			}
		}
		return null;
	}
	
	private MidataId getTargetApp(ProcessMessage message) throws AppException {
		Plugin pl = Plugin.getById(message.getApp());		
		if (pl == null || pl.defaultSubscriptions == null) return null;		
		List<SubscriptionData> def = pl.defaultSubscriptions;
		for (SubscriptionData data : def) {			
			if (data.format.equals("fhir/MessageHeader") && data.content.equals(message.eventCode)) {				
				Subscription fhirSubscription = SubscriptionResourceProvider.subscription(data);				
				if (fhirSubscription.getChannel().getType().equals(SubscriptionChannelType.MESSAGE) && fhirSubscription.getChannel().getEndpoint().startsWith("app://")) {					
					String appname = fhirSubscription.getChannel().getEndpoint().substring("app://".length());
					Plugin target = PluginLoginCache.getByFilename(appname);
					if (target != null) return target._id;
				}
			}
		}
		return null;
	}
	
	void messageResponse(MessageResponse message) {
		getSender().tell(message, getSelf());
	}
	
	void subscriptionChange(SubscriptionChange change) {
		String path = "SubscriptionChecker/subscriptionChange";
		long st = ActionRecorder.start(path);
		
		AccessLog.logStart("jobs", "subscription change: "+change);
		try {
			MidataId owner = change.getOwner();
			if (SubscriptionData.existsActiveByOwner(owner)) {
				withSubscription.add(owner);
			} else {
				withSubscription.remove(owner);
			}
		} catch (Exception e) {
			ErrorReporter.report("SubscriptionChecker", null, e);
		} finally {
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
	
	void forwardSubscription(SubscriptionTriggered triggered) {
		processor.tell(triggered, getSelf());
	}
		
}

