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

package utils.plugins;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import models.MidataId;
import models.Plugin;
import play.libs.ws.WSClient;
import utils.InstanceConfig;
import utils.access.IndexSupervisor;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.sync.Instances;

public class DeploymentManager {

private static ActorSystem system;
	
	private static ActorRef deployer;
	private static WSClient ws;
	
	public static void init(WSClient ws1, ActorSystem system1) {
		system = system1;	
		ws = ws1;
		
		ActorRef buildContainer = null;
		ActorRef CDNContainer = null;
		ActorRef scriptContainer = null;
				
	    if (InstanceConfig.getInstance().getInternalBuilderUrl()!=null) {
	    	buildContainer = system.actorOf(Props.create(ExternBuildContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");
	    } else {
	    	buildContainer = system.actorOf(Props.create(FirejailBuildContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");	
	    }

	    if (InstanceConfig.getInstance().getInternalCDNUrl()!=null) {
	    	CDNContainer = system.actorOf(Props.create(ExternCDNContainer.class).withDispatcher("pinned-dispatcher"), "pluginCDN");
	    } else {
	    	ActorRef localCDNContainer = system.actorOf(Props.create(FilesystemCDNContainer.class).withDispatcher("pinned-dispatcher"), "pluginCDN");
	    	CDNContainer = system.actorOf(MultiServerContainer.props(MultiServerContainer.class, localCDNContainer), "globalCDN");
	    }

	    if (InstanceConfig.getInstance().getInternalScriptingUrl()!=null) {
	    	scriptContainer = system.actorOf(Props.create(ExternScriptContainer.class).withDispatcher("pinned-dispatcher"), "pluginScripts");
	    } else {
	    	ActorRef localScriptContainer = system.actorOf(Props.create(FirejailScriptContainer.class).withDispatcher("pinned-dispatcher"), "pluginScripts");
	    	scriptContainer = system.actorOf(MultiServerContainer.props(MultiServerContainer.class, localScriptContainer), "globalScripts");
	    }
										
		deployer = system.actorOf(DeployCoordinator.props(buildContainer, scriptContainer, CDNContainer).withDispatcher("medium-work-dispatcher"), "pluginDeployment");
					
	}
	
	public static WSClient getWsClient() {
		return ws;
	}
	
	public static DeploymentReport deploy(MidataId plugin, MidataId executor, String action) throws AppException {
		DeploymentReport report = new DeploymentReport();
		report._id = plugin;
		report.init();
		report.done.add(DeployPhase.SCEDULED);
		report.sceduled = System.currentTimeMillis();
		report.add();
		
		DeployPhase task = DeployPhase.COORDINATE;
		
		if ("delete".equals(action)) task = DeployPhase.COORDINATE_DELETE;
		else if ("wipe".equals(action)) task = DeployPhase.COORDINATE_WIPE;
		else if ("audit".equals(action)) task = DeployPhase.COORDINATE_AUDIT;
		else if ("auditfix".equals(action)) task = DeployPhase.COORDINATE_AUDIT_FIX;
		else if ("redeploy".equals(action)) task = DeployPhase.COORDINATE_DEPLOY;
		
		deployer.tell(new DeployAction(plugin, executor, task), ActorRef.noSender());
		
		return report;
	}
	
	public static void deploy(DeployAction action, ActorRef source) {
		deployer.tell(action, source);
	}
	
	public static boolean hasUserDeployment(MidataId pluginId) throws AppException {
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl"));
		if (plugin == null || plugin.repositoryUrl==null) return false;
		if (plugin.repositoryDate==0) return false;
		return true;						
	}
}
