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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import akka.actor.AbstractActor;
import models.MidataId;
import models.Plugin;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ExternPluginDeployment extends AbstractActor {

	private Map<MidataId, DeployStatus> statusMap = new HashMap<MidataId, DeployStatus>();
	//private String clusterNode;
	
	@Inject
	WSClient ws;	
	
	String serviceUrl;
	
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		serviceUrl = InstanceConfig.getInstance().getInternalBuilderUrl();
		//clusterNode = Instances.getClusterInstanceName();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(DeployAction.class, this::deploy)			   
			   .build();
	}
	
	public void process(String pluginName, String command, String repo, DeployAction action, DeployPhase next) {
		 AccessLog.log("Execute command "+command.toString());
		 		 
		 WSRequest holder = ws.url(serviceUrl);

	     String post ="";
		 try {
			post = "action="+command+"&name="+URLEncoder.encode(pluginName, "UTF-8");		
	        if (repo != null) post += "&repo="+URLEncoder.encode(repo, "UTF-8");
		 } catch (UnsupportedEncodingException e) {}
		 //holder = holder.setAuth(app.consumerKey, app.consumerSecret);
			
		CompletionStage<WSResponse> promise = holder.setContentType("application/x-www-form-urlencoded; charset=utf-8").post(post);
		promise.thenAccept(response -> {							
			final String body = response.getBody();
			final int status = response.getStatus();
			result(action, next, status == 200, body);					
	    });
	}
	
	
	
	private DeployStatus getDeployStatus(MidataId plugin, boolean create) {
		DeployStatus result = statusMap.get(plugin);		
		if (create) {
			result = new DeployStatus();
			statusMap.put(plugin, result);
		}
		return result;
	}
	
	
	
	private boolean result(DeployAction action, DeployPhase type, boolean success, String result) {
		getSender().tell(new DeployAction(action, "all", type, result, success), getSelf());
		return success;
	}
	
	private void toSelf(DeployAction action, DeployPhase type) {
		getSelf().tell(new DeployAction(action, type), getSender());
	}
	
	private void finished(Plugin plugin) {
		AccessLog.log("finished");
	}
	
	private void checkFail(DeployAction action, DeployStatus status) throws AppException {
		if (!action.success) {
			status.report.status.add(DeployPhase.FAILED);
			status.report.finsihed = System.currentTimeMillis();
			//status.numFailed++;
			status.report.add();
		}
		status.report.add();
		System.out.println("failed");
	}
	
	public void deploy(DeployAction action) throws AppException {		
		MidataId pluginId = action.pluginId;
		try {
		AccessLog.logStart("jobs", "DEPLOY "+action.status+" "+pluginId);
		
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl","repositoryToken"));		
		if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
					
		String repo = plugin.repositoryUrl;
		String filename = plugin.filename;
	
		if (plugin.repositoryToken != null) {
			if (repo.startsWith("https://")) repo = "https://"+plugin.repositoryToken+"@"+repo.substring("https://".length());
		}
		
		switch(action.status) {
		case COORDINATE:
			DeployStatus status = getDeployStatus(pluginId, true);
			status.plugin = plugin;
			status.report = new DeploymentReport();
			status.report._id = pluginId;
			status.report.init();
			status.report.status.add(DeployPhase.SCEDULED);
			status.report.status.add(DeployPhase.COORDINATE);
			status.report.sceduled = System.currentTimeMillis();
			status.report.clusterNodes.add(action.clusterNode);
			status.report.add();
			toSelf(action, DeployPhase.CHECKOUT);
					
			break;
			
		case COORDINATE_DELETE:
			status = getDeployStatus(pluginId, true);
			status.plugin = plugin;
			status.report = new DeploymentReport();
			status.report._id = pluginId;
			status.report.init();			
			status.report.sceduled = System.currentTimeMillis();
			status.report.add();
			process(filename, "delete", null, action, DeployPhase.DELETE);
			
			break;
		case DELETE:
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.AUDIT);
			status.report.auditReport.put(action.clusterNode, action.report);
			status.report.add();
			status.plugin.repositoryDate = 0;
			Plugin.set(status.plugin._id, "repositoryDate", status.plugin.repositoryDate);
			statusMap.remove(status.plugin._id);
			break;
		case CHECKOUT:
			process(filename, "clone", repo, action, DeployPhase.REPORT_CHECKOUT);
			break;
		case INSTALL:
			process(filename, "ci", repo, action, DeployPhase.REPORT_INSTALL);			
			break;
		case AUDIT:
			process(filename, "audit", repo, action, DeployPhase.REPORT_AUDIT);						
			break;
		case COMPILE:
			process(filename, "build", repo, action, DeployPhase.REPORT_COMPILE);			
			break;
		case PUBLISH:
			process(filename, "publish", repo, action, DeployPhase.FINISHED);					
			break;		
		case REPORT_CHECKOUT:
			status = getDeployStatus(pluginId, false);	
			status.report.status.add(DeployPhase.CHECKOUT);
			status.report.checkoutReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			if (action.success) toSelf(action, DeployPhase.INSTALL);
			break;			
		case REPORT_INSTALL:
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.INSTALL);
			status.report.installReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			if (action.success) toSelf(action, DeployPhase.AUDIT);
			break;
		case REPORT_AUDIT:
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.AUDIT);
			status.report.auditReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			if (action.success) toSelf(action, DeployPhase.COMPILE);
			break;
		case REPORT_COMPILE:	
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.COMPILE);
			status.report.buildReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			if (action.success) toSelf(action, DeployPhase.PUBLISH);
			break;		
		case FINISHED:
			status = getDeployStatus(pluginId, false);
			
            status.report.status.add(DeployPhase.FINISHED);
			status.report.finsihed = System.currentTimeMillis();
			status.report.add();
			status.plugin.repositoryDate = System.currentTimeMillis();
			Plugin.set(status.plugin._id, "repositoryDate", status.plugin.repositoryDate);
			statusMap.remove(status.plugin._id);
			
			break;		
		}
		} finally {
			ServerTools.endRequest();
		}
	}
}
