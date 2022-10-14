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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.ActorRef;
import akka.actor.Props;
import models.MidataId;
import models.Plugin;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.exceptions.AppException;
import utils.stats.ActionRecorder;
import utils.sync.Instances;

public class MultiServerContainer extends AbstractContainer {

	
	private String clusterNode;
	private ActorRef targetActor;
	
	static Props props(Class targetClass, final ActorRef targetActor) {	    
	    return Props.create(targetClass, () -> new MultiServerContainer(targetActor));
	}
	
	public MultiServerContainer(ActorRef targetActor) {
		this.targetActor = targetActor;
	}
	
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		clusterNode = Instances.getClusterInstanceName();
	}
	
	void deploy(DeployAction msg) {
		String path = "PluginDeployment/multi";
		long st = ActionRecorder.start(path);
		try {
			defaultMessages(msg);
		} catch (AppException e) {			
		   ErrorReporter.report("CDNContainer", null, e);
		} finally {
		  ServerTools.endRequest();
		  ActionRecorder.end(path, st);
		}
	
	}
	
	@Override
    void doAction(DeployAction msg) throws AppException {
		
    	if (msg.clusterNode == null) {
    		DeployStatus status = getDeployStatus(msg.pluginId, false);
    		if (status == null) {
    			status = getDeployStatus(msg.pluginId, true);
    			broadcast(msg.newPhase(DeployPhase.COUNT, getSelf()));
    		}    		
    		status.reports = new HashMap<String, String>();
    		broadcast(msg);
    	} else if (msg.status == DeployPhase.REPORT_COUNT) {
    		DeployStatus status = getDeployStatus(msg.pluginId, false);
    		status.numStarted++;    		
    	} else if (msg.status.isReport()) {
    		DeployStatus status = getDeployStatus(msg.pluginId, false);
    		if (msg.success) {
    			status.numFinished++;
    		} else status.numFailed++;
    		if (msg.report != null) status.reports.putAll(msg.report);
    		
    		if (status.numFailed + status.numFinished >= status.numStarted) {
    		  msg.replyTo.tell(msg.response(status.numFailed == 0 && status.numFinished > 0, status.reports), getSelf());
    		}
    		
    		if (msg.status == DeployPhase.FINISHED) {
    			statusMap.remove(status.plugin._id);
    		}
    	} else {
    		targetActor.tell(msg.forward(clusterNode), getSelf());
    	}
	}
    
  
    
    private void broadcast(DeployAction action) {
		Instances.sendDeploy(action.forward("*"), getSelf());
	}

	
		
}

