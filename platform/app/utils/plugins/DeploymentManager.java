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
		
		if (InstanceConfig.getInstance().getInternalBuilderUrl() != null) {
		  deployer = system.actorOf(Props.create(ExternPluginDeployment.class).withDispatcher("pinned-dispatcher"), "pluginDeployment");
		} else {
		  deployer = system.actorOf(Props.create(PluginDeployment.class).withDispatcher("pinned-dispatcher"), "pluginDeployment");
		}
	   			
	}
	
	public static WSClient getWsClient() {
		return ws;
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
		if (plugin.repositoryDate==0) return false;
		return true;
		
		/*String deployLocation =  InstanceConfig.getInstance().getConfig().getString("visualizations.path");
		String targetDir = deployLocation+"/"+plugin.filename;
		File test = new File(targetDir);
		if (test.exists()) return true;
		test = new File(deployLocation+"/../plugin_active/"+plugin.filename);
		if (test.exists()) return true;
		return false;*/
		
	}
}
