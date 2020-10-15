package utils.plugins;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import models.MidataId;
import play.libs.ws.WSClient;
import utils.exceptions.AppException;

public class DeploymentManager {

private static ActorSystem system;
	
	private static ActorRef deployer;
	
	public static void init(ActorSystem system1) {
		system = system1;	
		
		deployer = system.actorOf(Props.create(PluginDeployment.class), "pluginDeployment");
	   			
	}
	
	public static DeploymentReport deploy(MidataId plugin, MidataId executor, boolean doDelete) throws AppException {
		DeploymentReport report = new DeploymentReport();
		report._id = plugin;
		report.init();
		report.status.add(DeployPhase.SCEDULED);
		report.sceduled = System.currentTimeMillis();
		report.add();
		
		deployer.tell(new DeployAction(plugin, executor, doDelete ? DeployPhase.COORDINATE_DELETE : DeployPhase.COORDINATE), ActorRef.noSender());
		
		return report;
	}
	
	public static void deploy(DeployAction action, ActorRef source) {
		deployer.tell(action, source);
	}
}
