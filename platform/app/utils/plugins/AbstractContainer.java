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

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.AbstractActor.Receive;
import models.MidataId;
import models.Plugin;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.exceptions.AppException;
import utils.messaging.InputStreamCollector;
import utils.stats.ActionRecorder;

public abstract class AbstractContainer extends AbstractActor {

	protected Map<MidataId, CurrentDeployStatus> statusMap = new HashMap<MidataId, CurrentDeployStatus>();
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(DeployAction.class, this::deploy)			   
			   .build();
	}
	
	void deploy(DeployAction msg) {
		String path = "PluginDeployment/"+getClass().getName();
		long st = ActionRecorder.start(path);
		try {
			defaultMessages(msg);
		} catch (AppException e) {			
		   ErrorReporter.report(getClass().getName(), null, e);
		} finally {
		  ServerTools.endRequest();
		  ActionRecorder.end(path, st);
		}
	
	}
	
	public void defaultMessages(DeployAction msg) throws AppException {
		if (msg.status == DeployPhase.COUNT) {
			  result(msg, DeployPhase.REPORT_COUNT, true, null);
			} else {
			  doAction(msg);
			}
	}
	
	abstract void doAction(DeployAction msg) throws AppException;
	
	
	
	  protected CurrentDeployStatus getDeployStatus(MidataId plugin, boolean create) throws AppException {
			CurrentDeployStatus result = statusMap.get(plugin);		
			if (create) {
				result = new CurrentDeployStatus();
				result.tasks = new ArrayDeque<DeployPhase>();
				statusMap.put(plugin, result);
				result.report = new DeploymentReport();
				result.report._id = plugin;
				result.report.init();
				result.report.done.add(DeployPhase.SCEDULED);
				result.report.done.add(DeployPhase.COORDINATE);
				result.report.sceduled = System.currentTimeMillis();
				result.report.add();
			}
			return result;
		}
	
	
	  
	  boolean result(DeployAction action, DeployPhase type, Pair<Boolean, String> result) {		    
			getSender().tell(action.response(type, result.getLeft(), result.getRight()), getSelf());
			return result.getLeft();
		}
	  
	  boolean result(ActorRef sender, DeployAction action, DeployPhase type, Pair<Boolean, String> result) {		    
			sender.tell(action.response(type, result.getLeft(), result.getRight()), getSelf());
			return result.getLeft();
		}
	  
	  boolean result(DeployAction action, DeployPhase type, boolean success, String report) {		  
			getSender().tell(action.response(type, success, report), getSelf());
			return success;
		}
}

class CurrentDeployStatus {
	public Plugin plugin;
	public DeploymentReport report;
	public int numStarted;
	public int numReady;
	public int numFinished;
	public int numFailed;
	public Map<String, String> reports;
	public Queue<DeployPhase> tasks;
	public MidataId auditEvent;
	
	public List<ActorRef> actors;
	
}
