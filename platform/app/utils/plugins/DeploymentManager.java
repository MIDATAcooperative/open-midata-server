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
	private static WSClient ws;
	
	public static void init(WSClient ws1, ActorSystem system1) {
		system = system1;	
		ws = ws1;
		
		
		/*
		if (InstanceConfig.getInstance().getInternalBuilderUrl() != null) {
			//buildContainer = getContext().actorOf(Props.create(ExternPluginDeployment.class).withDispatcher("pinned-dispatcher"), "pluginDeployment");
		} else {
			ActorRef localBuildContainer = getContext().actorOf(Props.create(FirejailBuildContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");
			scriptContainer = getContext().actorOf(Props.create(FirejailScriptContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");
			cdnContainer = 
		}*/
		
		ActorRef localBuildContainer = system.actorOf(Props.create(FirejailBuildContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");
		ActorRef localCDNContainer = system.actorOf(Props.create(FilesystemCDNContainer.class).withDispatcher("pinned-dispatcher"), "pluginCDN");
		ActorRef localScriptContainer = system.actorOf(Props.create(FirejailScriptContainer.class).withDispatcher("pinned-dispatcher"), "pluginScripts");
		
		deployer = system.actorOf(DeployCoordinator.props(localBuildContainer, localScriptContainer, localCDNContainer).withDispatcher("medium-work-dispatcher"), "pluginDeployment");
			   		
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
