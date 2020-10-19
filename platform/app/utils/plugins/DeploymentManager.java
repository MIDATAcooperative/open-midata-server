package utils.plugins;

import java.io.File;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import models.MidataId;
import models.Plugin;
import play.libs.ws.WSClient;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class DeploymentManager {

private static ActorSystem system;
	
	private static ActorRef deployer;
	
	public static void init(ActorSystem system1) {
		system = system1;	
		
		deployer = system.actorOf(Props.create(PluginDeployment.class).withDispatcher("pinned-dispatcher"), "pluginDeployment");
	   			
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
	
	public static boolean hasUserDeployment(MidataId pluginId) throws AppException {
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl"));
		if (plugin == null || plugin.repositoryUrl==null) return false;
		String deployLocation =  InstanceConfig.getInstance().getConfig().getString("visualizations.path");
		String targetDir = deployLocation+"/"+plugin.filename;
		File test = new File(targetDir);
		if (test.exists()) return true;
		test = new File(deployLocation+"/../plugin_active/"+plugin.filename);
		if (test.exists()) return true;
		return false;
		
	}
}
