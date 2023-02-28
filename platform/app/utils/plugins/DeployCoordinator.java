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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.ActorRef;
import akka.actor.Props;
import models.MidataId;
import models.Plugin;
import models.enums.AuditEventType;
import models.enums.DeploymentStatus;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.stats.ActionRecorder;
import utils.sync.Instances;

public class DeployCoordinator extends AbstractContainer {

	private ActorRef buildContainer;
	private ActorRef scriptContainer;
	private ActorRef cdnContainer;
	private List<DeployAction> waiting;
	private boolean running;
	
	static Props props(final ActorRef buildContainer, ActorRef scriptContainer, ActorRef cdnContainer) {	    
	    return Props.create(DeployCoordinator.class, () -> new DeployCoordinator(buildContainer, scriptContainer, cdnContainer));
	}
	
	public DeployCoordinator(ActorRef buildContainer, ActorRef scriptContainer, ActorRef cdnContainer) {
		this.buildContainer = buildContainer;
		this.scriptContainer = scriptContainer;
		this.cdnContainer = cdnContainer;
		this.waiting = new ArrayList<DeployAction>();
	}
	
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		
		try {
			Set<Plugin> failed = Plugin.getAll(CMaps.map("deployStatus", DeploymentStatus.RUNNING), Sets.create("_id", "deployStatus"));
			for (Plugin pl : failed) {
				Plugin.set(pl._id, "deployStatus", DeploymentStatus.FAILED);			
			}
		} catch (AppException e) {
			ErrorReporter.report("deploy", null, e);
		}
		/*
		if (InstanceConfig.getInstance().getInternalBuilderUrl() != null) {
			//buildContainer = getContext().actorOf(Props.create(ExternPluginDeployment.class).withDispatcher("pinned-dispatcher"), "pluginDeployment");
		} else {
			ActorRef localBuildContainer = getContext().actorOf(Props.create(FirejailBuildContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");
			scriptContainer = getContext().actorOf(Props.create(FirejailScriptContainer.class).withDispatcher("pinned-dispatcher"), "pluginBuilder");
			cdnContainer = 
		}*/

	}
	
	public void scedule(DeployAction action, CurrentDeployStatus status) throws AppException {
		if (!running) {
			running = true;
			newPhase(action, status.tasks.poll());
		} else {
			waiting.add(action);
		}
	}
	
	public void next() throws AppException {
		
		if (!waiting.isEmpty()) {
			DeployAction action = waiting.remove(0);
			CurrentDeployStatus status = getDeployStatus(action.pluginId, false);
			newPhase(action, status.tasks.poll());
		
		} else {
			running = false;
		
		}
	}
	
	public void deploy(DeployAction action) {	
		String path = "PluginDeployment/coordinator";
		long st = ActionRecorder.start(path);
		try {
			Plugin plugin = Plugin.getById(action.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));
			if (plugin==null || plugin.filename == null) return;
			if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
			
		switch(action.status) {
		case COORDINATE:
			
			CurrentDeployStatus status = getDeployStatus(action.pluginId, true);
			status.tasks.add(DeployPhase.CHECKOUT);
			status.tasks.add(DeployPhase.INSTALL);
			status.tasks.add(DeployPhase.AUDIT);
			status.tasks.add(DeployPhase.COMPILE);
			if (plugin.hasScripts) status.tasks.add(DeployPhase.EXPORT_SCRIPTS);
			status.tasks.add(DeployPhase.EXPORT_TO_CDN);	
			status.tasks.add(DeployPhase.DELETE);	
			status.tasks.add(DeployPhase.FINISHED);
		    status.report.planned.addAll(status.tasks);
		    AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.PLUGIN_DEPLOYED).withActorUser(action.userId).withApp(action.pluginId));
		    Plugin.set(action.pluginId, "deployStatus", DeploymentStatus.RUNNING);
		    status.auditEvent = AuditManager.instance.convertLastEventToAsync();
			scedule(action, status);
			break;
			
		case COORDINATE_DELETE:
			
			status = getDeployStatus(action.pluginId, true);
			status.tasks.add(DeployPhase.DELETE);	
			status.tasks.add(DeployPhase.FINISH_AUDIT);
			status.report.planned.addAll(status.tasks);
			AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.PLUGIN_UNDEPLOYED).withActorUser(action.userId).withApp(action.pluginId).withMessage("delete"));			
		    status.auditEvent = AuditManager.instance.convertLastEventToAsync();
		    scedule(action, status);
			break;
		case COORDINATE_AUDIT:
			status = getDeployStatus(action.pluginId, true);
			status.tasks.add(DeployPhase.CHECKOUT);
			status.tasks.add(DeployPhase.INSTALL);
			status.tasks.add(DeployPhase.AUDIT);	
			status.tasks.add(DeployPhase.DELETE);	
			status.tasks.add(DeployPhase.FINISH_AUDIT);
			status.report.planned.addAll(status.tasks);
			scedule(action, status);
			break;
		case COORDINATE_AUDIT_FIX:
			status = getDeployStatus(action.pluginId, true);
			status.tasks.add(DeployPhase.CHECKOUT);
			status.tasks.add(DeployPhase.INSTALL);
			status.tasks.add(DeployPhase.AUDITFIX);
			status.tasks.add(DeployPhase.COMPILE);
			status.tasks.add(DeployPhase.DELETE);	
			status.tasks.add(DeployPhase.FINISH_AUDIT);
			status.report.planned.addAll(status.tasks);
			scedule(action, status);
			break;
		case COORDINATE_DEPLOY:
			status = getDeployStatus(action.pluginId, true);
			status.tasks.add(DeployPhase.EXPORT_TO_CDN);
			if (plugin.hasScripts) status.tasks.add(DeployPhase.EXPORT_SCRIPTS);
			status.tasks.add(DeployPhase.FINISHED);
			status.report.planned.addAll(status.tasks);
			Plugin.set(action.pluginId, "deployStatus", DeploymentStatus.RUNNING);
			AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.PLUGIN_DEPLOYED).withActorUser(action.userId).withApp(action.pluginId).withMessage("deploy only"));
		    status.auditEvent = AuditManager.instance.convertLastEventToAsync();
		    scedule(action, status);
			break;
		case COORDINATE_WIPE:
			status = getDeployStatus(action.pluginId, true);
			status.tasks.add(DeployPhase.WIPE_SCRIPT);
			status.tasks.add(DeployPhase.WIPE_CDN);			
			status.tasks.add(DeployPhase.DELETE);	
			status.tasks.add(DeployPhase.FINISH_AUDIT);
			status.report.planned.addAll(status.tasks);
			AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.PLUGIN_DEPLOYED).withActorUser(action.userId).withApp(action.pluginId).withMessage("wipe"));
			Plugin.set(action.pluginId, "deployStatus", DeploymentStatus.READY);
		    status.auditEvent = AuditManager.instance.convertLastEventToAsync();
		    scedule(action, status);
			break;
					
		case REPORT_CHECKOUT:
			status = getDeployStatus(action.pluginId, false);				
			status.report.clusterNodes.addAll(action.report.keySet());
			status.report.checkoutReport.putAll(action.report);			
			if (checkFail(action, status, DeployPhase.CHECKOUT)) newPhase(action, status.tasks.poll());
			break;			
		case REPORT_INSTALL:
			status = getDeployStatus(action.pluginId, false);			
			status.report.clusterNodes.addAll(action.report.keySet());
			status.report.installReport.putAll(action.report);
			if (checkFail(action, status, DeployPhase.INSTALL)) newPhase(action, status.tasks.poll());
			break;
		case REPORT_AUDIT:
			status = getDeployStatus(action.pluginId, false);						
			if (action.report != null && !action.report.isEmpty()) {
			   status.report.clusterNodes.addAll(action.report.keySet());
			   status.report.auditReport.putAll(action.report);
			   String auditReport = action.report.values().iterator().next();
			   int s1 = auditReport.indexOf("\nfound ");
			   if (s1<0) s1 = auditReport.indexOf("\nfixed ");
			   if (s1 > 0) {
			     auditReport = auditReport.substring(s1+1);
			     int s2 = auditReport.indexOf("\n");
			     auditReport = auditReport.substring(0, s2);
			     plugin.repositoryAuditDate = System.currentTimeMillis();
			     plugin.repositoryRisks = auditReport;
			     Plugin.set(plugin._id, "repositoryRisks", plugin.repositoryRisks);
			     Plugin.set(plugin._id, "repositoryAuditDate", plugin.repositoryAuditDate);
			   }
			}			
			if (checkFail(action, status, DeployPhase.AUDIT)) {}
			newPhase(action, status.tasks.poll());
			break;
		case REPORT_COMPILE:	
			status = getDeployStatus(action.pluginId, false);			
			status.report.clusterNodes.addAll(action.report.keySet());
			status.report.buildReport.putAll(action.report);
			if (checkFail(action, status, DeployPhase.COMPILE)) newPhase(action, status.tasks.poll());			
			break;
		case REPORT_EXPORT_TO_CDN:
			status = getDeployStatus(action.pluginId, false);
			
			if (checkFail(action, status, DeployPhase.EXPORT_TO_CDN)) {
				newPhase(action, DeployPhase.IMPORT_CDN);
			
			}
			break;
		case REPORT_IMPORT_CDN:
			status = getDeployStatus(action.pluginId, false);
		    if (checkFail(action, status, DeployPhase.IMPORT_CDN)) newPhase(action, status.tasks.poll());
			break;
		case REPORT_EXPORT_SCRIPTS:
			status = getDeployStatus(action.pluginId, false);			
			if (checkFail(action, status, DeployPhase.EXPORT_SCRIPTS)) {
				newPhase(action, DeployPhase.IMPORT_SCRIPTS);
				
			}
			break;
		case REPORT_IMPORT_SCRIPTS:
			status = getDeployStatus(action.pluginId, false);
		    if (checkFail(action, status, DeployPhase.IMPORT_SCRIPTS)) newPhase(action, status.tasks.poll());
			break;
		case REPORT_DELETE:
			status = getDeployStatus(action.pluginId, false);
		    if (checkFail(action, status, DeployPhase.DELETE)) newPhase(action, status.tasks.poll());
			break;
		case REPORT_WIPE_CDN:
			status = getDeployStatus(action.pluginId, false);
		    if (checkFail(action, status, DeployPhase.WIPE_CDN)) newPhase(action, status.tasks.poll());
			break;
		case REPORT_WIPE_SCRIPT:
			status = getDeployStatus(action.pluginId, false);
		    if (checkFail(action, status, DeployPhase.WIPE_SCRIPT)) newPhase(action, status.tasks.poll());
			break;
		case FINISH_AUDIT:
			status = getDeployStatus(action.pluginId, false);
			status.report.done.add(DeployPhase.FINISH_AUDIT);
			status.report.finsihed = System.currentTimeMillis();								
			statusMap.remove(action.pluginId);
			buildContainer.tell(action.forward("*"), getSender());
			cdnContainer.tell(action.forward("*"), getSender());
			scriptContainer.tell(action.forward("*"), getSender());
            if (checkFail(action, status, DeployPhase.FINISH_AUDIT)) next();
			break;	
		case FINISHED:
			status = getDeployStatus(action.pluginId, false);
			status.report.done.add(DeployPhase.FINISHED);
			status.report.finsihed = System.currentTimeMillis();					
			Plugin.set(action.pluginId, "repositoryDate", System.currentTimeMillis());
			Plugin.set(action.pluginId, "deployStatus", DeploymentStatus.DONE);
			statusMap.remove(action.pluginId);
			buildContainer.tell(action.forward("*"), getSender());
			cdnContainer.tell(action.forward("*"), getSender());
			scriptContainer.tell(action.forward("*"), getSender());
            if (checkFail(action, status, DeployPhase.FINISHED)) next();
			break;	
		case COUNT:
			System.out.println("RESPOND TO COUNT");
			 result(action, DeployPhase.REPORT_COUNT, true, null);
			 break;
		case CHECKOUT:
		case INSTALL:
		case AUDIT:
		case AUDITFIX:
		case COMPILE:						
		case EXPORT_TO_CDN:
		case EXPORT_SCRIPTS:
		case DELETE:
			buildContainer.tell(action, getSender());
			break;
					
		case IMPORT_CDN:
		case WIPE_CDN:
			cdnContainer.tell(action, getSender());
			break;
			
		case IMPORT_SCRIPTS:
		case WIPE_SCRIPT:
			scriptContainer.tell(action, getSender());
			break;
									
		}
		} catch (AppException e) {			
		  ErrorReporter.report("DeployCoordinator", null, e);
		} finally {
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}

	private boolean checkFail(DeployAction action, CurrentDeployStatus status, DeployPhase last) throws AppException {
		
		if (!action.success && action.status != DeployPhase.REPORT_AUDIT) {
			if (last!=null) status.report.failed.add(last);
			//status.report.done.add(DeployPhase.FAILED);
			status.report.finsihed = System.currentTimeMillis();	
			status.report.add();
			Plugin.set(action.pluginId, "deployStatus", DeploymentStatus.FAILED);
			AuditManager.instance.resumeAsyncEvent(status.auditEvent);
			AuditManager.instance.fail(400, last.toString(), "error.failed");
			next();
			return false;
		} else {
			status.report.done.add(last);
			
			if (last == DeployPhase.FINISHED) {
				AuditManager.instance.resumeAsyncEvent(status.auditEvent);
				AuditManager.instance.success();
			}
		}
		status.report.add();
	    return true;
	}

	@Override
	void doAction(DeployAction msg) throws AppException {
		// TODO Auto-generated method stub
		
	}

	
	  void newPhase(DeployAction action, DeployPhase type) throws AppException {
		    System.out.println("XXXXXX START NEW="+type);
	    	if (type != null) {
	    		ActorRef ref = buildContainer;
	    		
	    		if (type ==	DeployPhase.IMPORT_CDN || type == DeployPhase.WIPE_CDN) ref = cdnContainer;
	    		else if (type == DeployPhase.IMPORT_SCRIPTS || type == DeployPhase.WIPE_SCRIPT) ref = scriptContainer;
	    		else if (type == DeployPhase.FINISHED || type == DeployPhase.FINISH_AUDIT) ref = getSelf();
	    			    			    		
	    		ref.tell(action.newPhase(type, getSelf()), getSelf());	
	    	} else {
	    		CurrentDeployStatus status = getDeployStatus(action.pluginId, false);
				if (status != null) {
		            status.report.done.add(DeployPhase.FINISHED);
					status.report.finsihed = System.currentTimeMillis();
					status.report.add();					
					statusMap.remove(action.pluginId);
				}
				next();
	    	}
	  }
}
