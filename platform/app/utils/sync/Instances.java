package utils.sync;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.cluster.routing.ClusterRouterGroup;
import akka.cluster.routing.ClusterRouterGroupSettings;
import akka.routing.BroadcastGroup;
import models.ContentCode;
import models.MidataId;
import models.Plugin;
import models.RecordGroup;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.messaging.ServiceHandler;
import utils.messaging.SubscriptionManager;

/**
 * Synchronization between multiple application servers for changes on cached data like plugins or content type definitions
 *
 */
public class Instances {

	private static ActorSystem actorSystem;
	private static List<String> instanceURIs;
	private static ActorRef broadcast;
					
	/**
	 * Initialize synchronization
	 */
	public static void init() {
		actorSystem = ActorSystem.create("midata", InstanceConfig.getInstance().getConfig().getConfig("midata").withFallback(InstanceConfig.getInstance().getConfig()));
		//instanceURIs = Play.application().configuration().getStringList("servers");
		actorSystem.actorOf(Props.create(InstanceSync.class), "instanceSync");	
				
		actorSystem.actorOf(Props.create(ClusterMonitor.class), "midataClusterMonitor");
		
		Iterable<String> routeesPaths = Collections.singletonList("/user/instanceSync");				
		broadcast = actorSystem.actorOf(
		    new ClusterRouterGroup(new BroadcastGroup(routeesPaths),
		        new ClusterRouterGroupSettings(Integer.MAX_VALUE, routeesPaths,
		            true, Sets.create())).props(), "broadcast");
	}
	
	public static ActorSystem system() {
		return actorSystem;
	}
	
	/**
	 * Shutdown synchronization
	 */
	public static CompletionStage<Done> shutdown() {
		return CoordinatedShutdown.get(actorSystem).runAll(CoordinatedShutdown.jvmExitReason());
		//actorSystem.terminate();  
		//return actorSystem.getWhenTerminated();
	}
	
	/**
	 * Get actor to broadcast to all application servers
	 */
	protected static ActorRef getBroadcast() {				
		return broadcast;
	}
	
	/**
	 * send a clear cache message to all application servers
	 */
	public static void cacheClear(String collection, MidataId entry) {	
		AccessLog.log("broadcast reload message");
		getBroadcast().tell(new ReloadMessage(collection, entry), ActorRef.noSender());
	}
	
	public static void retrieveKey() {
		getBroadcast().tell(new ReloadMessage("aeskey", null), ActorRef.noSender());
	}

	public static void sendKey(byte[] aeskey) {
		getBroadcast().tell(new KeyMessage(aeskey), ActorRef.noSender());
	}
}

/**
 * A "reload cache" message
 *
 */
class ReloadMessage implements Serializable {
	
	private static final long serialVersionUID = -1541876300975690426L;
	
	final String collection;
	final MidataId entry;
	
	public ReloadMessage(String collection, MidataId entry) {
		this.collection = collection;
		this.entry = entry;
	}
}

class KeyMessage implements Serializable {
	
	final byte[] aeskey;	
	
	public KeyMessage(byte[] aeskey) {
		this.aeskey = aeskey;		
	}
	
}

/**
 * Actor for instance synchronization
 *
 */
class InstanceSync extends AbstractActor {
	
	public InstanceSync() {							    
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(ReloadMessage.class, this::reload)	
	      .match(KeyMessage.class, this::setKey)
	      .build();
	}
	

	public void reload(ReloadMessage msg) throws Exception {
		try {		
		   AccessLog.log("Received Reload Message: "+msg.toString());		  
		   if (msg.collection.equals("plugin")) {
			   Plugin.cacheRemove(msg.entry);
		   } else if (msg.collection.equals("content")) {
			   RecordGroup.load();
			   ContentCode.reset();
		   } else if (msg.collection.equals("SubscriptionData")) {
			   System.out.println("A");
			   SubscriptionManager.subscriptionChangeLocal(msg.entry);
			   System.out.println("B");
		   } else if (msg.collection.equals("aeskey")) {
			   ServiceHandler.shareKey();
		   }
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {			
			AccessLog.newRequest();	
		}
	}
	
	public void setKey(KeyMessage msg) {
		ServiceHandler.setKey(msg.aeskey);
	}
	
}