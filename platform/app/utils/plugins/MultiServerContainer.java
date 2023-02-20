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
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.tuple.Pair;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.stream.IOResult;
import akka.stream.SourceRef;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamRefs;
import akka.util.ByteString;
import models.MidataId;
import models.Plugin;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.exceptions.AppException;
import utils.messaging.AccountWipeMessage;
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
		   ErrorReporter.report("MultiServerContainer", null, e);
		} finally {
		  ServerTools.endRequest();
		  ActionRecorder.end(path, st);
		}
	
	}
	
	@Override
    void doAction(DeployAction msg) throws AppException {
		
    	if (msg.clusterNode == null) {
    		CurrentDeployStatus status = getDeployStatus(msg.pluginId, false);
    		if (status == null) {
    			status = getDeployStatus(msg.pluginId, true);
    			status.actors = new ArrayList<ActorRef>();
    			broadcast(msg.newPhase(DeployPhase.COUNT, getSelf()));  
    			getContext().getSystem().scheduler().scheduleOnce(
    					   Duration.ofSeconds(5),
    					   getSelf(), msg, getContext().dispatcher(), getSender());    			
    		} else {   		
    		    status.reports = new HashMap<String, String>();
    		    
    		    if (msg.exportedData != null) {
    		    	Flow<ByteString, ByteString, ArrayList<SourceRef>> flow = 
    		    			Flow.of(ByteString.class)
    		    			.mapMaterializedValue(nu -> new ArrayList<SourceRef>());
    		    	
    	            for (ActorRef actorRef : status.actors) {    	                  		    	 
    		    	  flow = flow.alsoToMat(StreamRefs.sourceRef(), (al, ref) -> { al.add(ref); return al; });
    	            }
    	            
    	            Sink<ByteString, ArrayList<SourceRef>> sink = flow.toMat(Sink.ignore(), Keep.left());

    	            ArrayList<SourceRef> sources = (ArrayList<SourceRef>) msg.
    	            		exportedData.getSource().runWith(sink, getContext().getSystem());
    	                	            
    	            int i=0;
                    for (ActorRef actorRef : status.actors) {
                    	System.out.println("USE SOURCE REF"+i);
	    		    	actorRef.tell(msg.forward("*", sources.get(i)), getSender());
	    		    	i++;
	    		    }
                                                                                   				    		    	
    		    } else {
	    		    for (ActorRef actorRef : status.actors) {
	    		    	actorRef.tell(msg.forward("*"), getSender());
	    		    }
    		    }
    		    
    		}
    	} else if (msg.status == DeployPhase.REPORT_COUNT) {    		
    		CurrentDeployStatus status = getDeployStatus(msg.pluginId, false);
    		System.out.println("GOT REPORT COUNT "+status.actors.size());
    		status.numStarted++;    
    		status.actors.add(getSender());
    	} else if (msg.status == DeployPhase.FINISHED || msg.status == DeployPhase.FINISH_AUDIT) {
			statusMap.remove(msg.pluginId);		
    	} else if (msg.status.isReport()) {
    		CurrentDeployStatus status = getDeployStatus(msg.pluginId, false);
    		if (msg.success) {
    			status.numFinished++;
    		} else status.numFailed++;
    		if (msg.report != null) status.reports.putAll(msg.report);
    		    		    		
    		if (status.numFailed + status.numFinished >= status.numStarted) {
    		  msg.replyTo.tell(msg.response(status.numFailed == 0 && status.numFinished > 0, status.reports), getSelf());
    		}
    		
    		
    	} else {
    		
    		targetActor.tell(msg.forward(clusterNode), getSender());
    	}
	}
	
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
			}
			return result;
	}
    
  
    
    private void broadcast(DeployAction action) {
    	
		Instances.sendDeploy(action.forward("*"), getSelf());
	}

	
		
}

