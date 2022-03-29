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

package utils.sync;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import akka.actor.Props;
import akka.cluster.Cluster;
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
import utils.plugins.DeployAction;
import utils.plugins.DeploymentManager;
import utils.stats.ActionRecorder;

/**
 * Synchronization between multiple application servers for changes on cached data like plugins or content type definitions
 *
 */
public class Instances {

	private static ActorSystem actorSystem;
	private static List<String> instanceURIs;
	private static ActorRef broadcast;
	private static String selfName;
					
	/**
	 * Initialize synchronization
	 */
	public static void init() {
		actorSystem = ActorSystem.create("midata", InstanceConfig.getInstance().getConfig().getConfig("midata").withFallback(InstanceConfig.getInstance().getConfig()));
		//instanceURIs = Play.application().configuration().getStringList("servers");
		actorSystem.actorOf(Props.create(InstanceSync.class).withDispatcher("quick-work-dispatcher"), "instanceSync");	
				
		actorSystem.actorOf(Props.create(ClusterMonitor.class).withDispatcher("quick-work-dispatcher"), "midataClusterMonitor");
		
		Iterable<String> routeesPaths = Collections.singletonList("/user/instanceSync");				
		broadcast = actorSystem.actorOf(
		    new ClusterRouterGroup(new BroadcastGroup(routeesPaths),
		        new ClusterRouterGroupSettings(Integer.MAX_VALUE, routeesPaths,
		            true, Sets.create())).props().withDispatcher("quick-work-dispatcher"), "broadcast");
		
		selfName = Cluster.get(actorSystem).selfAddress().host().get();
	}
	
	public static ActorSystem system() {
		return actorSystem;
	}
	
	public static String getClusterInstanceName() {
		return selfName;
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
		AccessLog.log("broadcast reload collection=", collection);
		getBroadcast().tell(new ReloadMessage(collection, entry), ActorRef.noSender());
	}
	
	public static void retrieveKey() {
		getBroadcast().tell(new ReloadMessage("aeskey", null), ActorRef.noSender());
	}

	public static void sendKey(byte[] aeskey) {
		getBroadcast().tell(new KeyMessage(aeskey), ActorRef.noSender());
	}
	
	public static void sendDeploy(DeployAction deployAction, ActorRef source) { 
		getBroadcast().tell(deployAction, source);
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
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4044888802031833L;
	
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
	      .match(DeployAction.class, this::deploy)
	      .build();
	}
	

	public void reload(ReloadMessage msg) throws Exception {
		String path = "InstanceSync/reload";
		long st = ActionRecorder.start(path);
		try {		
		   AccessLog.log("Received Reload Message: ", msg.toString());		  
		   if (msg.collection.equals("plugin")) {
			   Plugin.cacheRemove(msg.entry);
		   } else if (msg.collection.equals("content")) {
			   RecordGroup.load();
			   ContentCode.reset();
		   } else if (msg.collection.equals("SubscriptionData")) {			 
			   SubscriptionManager.subscriptionChangeLocal(msg.entry);			  
		   } else if (msg.collection.equals("aeskey")) {
			   ServiceHandler.shareKey();
		   }
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {			
			AccessLog.newRequest();	
			ActionRecorder.end(path, st);
		}
	}
	
	public void setKey(KeyMessage msg) {
		String path = "InstanceSync/setKey";
		long st = ActionRecorder.start(path);
		ServiceHandler.setKey(msg.aeskey);
		ActionRecorder.end(path, st);
	}
	
	public void deploy(DeployAction deploy) {
		String path = "InstanceSync/deploy";
		long st = ActionRecorder.start(path);
		DeploymentManager.deploy(deploy, getSender());
		ActionRecorder.end(path, st);
	}
	
}