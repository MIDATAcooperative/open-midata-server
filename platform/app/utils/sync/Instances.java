package utils.sync;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
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
		instanceURIs = Play.application().configuration().getStringList("servers");
		actorSystem.actorOf(Props.create(InstanceSync.class), "instanceSync");		
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
		if (broadcast == null) {
			List<String> urls = new ArrayList<String>();
		    for (String url : instanceURIs) { urls.add("akka.tcp://midata@"+url+"/user/instanceSync"); }
		    broadcast = actorSystem.actorOf(new BroadcastGroup(urls).props(), "broadcast");
		}
		return broadcast;
	}
	
	/**
	 * send a clear cache message to all application servers
	 */
	public static void cacheClear(String collection, MidataId entry) {		
		getBroadcast().tell(new ReloadMessage(collection, entry), ActorRef.noSender());
	}

}

/**
 * A "reload cache" message
 *
 */
class ReloadMessage {
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