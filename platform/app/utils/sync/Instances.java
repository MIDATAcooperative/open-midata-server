package utils.sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.routing.ClusterRouterGroup;
import akka.cluster.routing.ClusterRouterGroupSettings;
import akka.routing.BroadcastGroup;
import models.ContentCode;
import models.MidataId;
import models.Plugin;
import models.RecordGroup;
import play.Play;
import play.libs.Akka;
import utils.AccessLog;
import utils.ErrorReporter;

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
		actorSystem = ActorSystem.create("midata", Play.application().configuration().getConfig("midata").underlying().withFallback(Play.application().configuration().underlying()));
		//instanceURIs = Play.application().configuration().getStringList("servers");
		actorSystem.actorOf(Props.create(InstanceSync.class), "instanceSync");	
				
		Iterable<String> routeesPaths = Collections.singletonList("/user/instanceSync");				
		broadcast = actorSystem.actorOf(
		    new ClusterRouterGroup(new BroadcastGroup(routeesPaths),
		        new ClusterRouterGroupSettings(Integer.MAX_VALUE, routeesPaths,
		            true, null)).props(), "broadcast");
	}
	
	public static ActorSystem system() {
		return actorSystem;
	}
	
	/**
	 * Shutdown synchronization
	 */
	public static void shutdown() {
		actorSystem.shutdown();  
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

/**
 * Actor for instance synchronization
 *
 */
class InstanceSync extends UntypedActor {
	
	public InstanceSync() {							    
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		try {
		if (message instanceof ReloadMessage) {
			AccessLog.log("Received Reload Message");
		   ReloadMessage msg = (ReloadMessage) message;
		   if (msg.collection.equals("plugin")) {
			   Plugin.cacheRemove(msg.entry);
		   } else if (msg.collection.equals("content")) {
			   RecordGroup.load();
			   ContentCode.reset();
		   }
		} else {
		    unhandled(message);
	    }	
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {			
			AccessLog.newRequest();	
		}
	}
	
}