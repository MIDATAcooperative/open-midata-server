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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.AbstractActor;
import models.MidataId;
import models.Plugin;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.messaging.InputStreamCollector;
import utils.sync.Instances;

public class PluginDeployment extends AbstractActor {

	private Map<MidataId, DeployStatus> statusMap = new HashMap<MidataId, DeployStatus>();
	private String clusterNode;
	
	
	
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		clusterNode = Instances.getClusterInstanceName();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(DeployAction.class, this::deploy)			   
			   .build();
	}
	
	public Pair<Boolean, String> process(File targetDir, List<String> command) {
		 AccessLog.log("Execute command "+command.toString());
		 System.out.println(command.toString());
		 try {
			  Process p = new ProcessBuilder(command).directory(targetDir).redirectErrorStream(true).start();
			  //System.out.println("Output...");
			  /*PrintWriter out = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));		  
			  out.println(triggered.resource);
			  out.close();*/
			  //System.out.println("Output done...");
			  InputStreamCollector result = new InputStreamCollector(p.getInputStream());
			  result.start();
			  //System.out.println("Input...");
			  p.waitFor();
			  //System.out.println("Wait for finished...");
			  result.join();
			  AccessLog.log("Wait for input...");
			  AccessLog.log(result.getResult());	
			  int exit = p.exitValue();
			  AccessLog.log("EXIT VALUE = "+exit);
			  return Pair.of(exit==0, result.getResult());
		 } catch (IOException e) {
			 e.printStackTrace();
			 return Pair.of(false, "IO Exception");
		 } catch (InterruptedException e2) {
			 return Pair.of(false, "Interrupted Exception");
		 }		 
	}
	
	public Pair<Boolean, String> doGitClone(File targetDir, String repo, String internalName) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/git");
		cmd.add("clone");
		cmd.add(repo);
		cmd.add(internalName);
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doGitPull(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/git");
		cmd.add("pull");		
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doInstall(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/firejail");
		cmd.add("--quiet");
		cmd.add("--whitelist="+targetDir.getAbsolutePath());		
		cmd.add("npm");
		cmd.add("ci");		
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doAudit(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/npm");
		cmd.add("audit");		
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doBuild(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/firejail");
		cmd.add("--quiet");
		cmd.add("--whitelist="+targetDir.getAbsolutePath());		
		cmd.add("npm");
		cmd.add("run");
		cmd.add("prod:build");
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doDelete(File baseDir, String filename) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/bin/rm");
		cmd.add("-rf");
		cmd.add("../plugin_active/"+filename);
		process(baseDir, cmd);
		cmd.clear();
		cmd.add("/bin/rm");
		cmd.add("-rf");
		cmd.add(filename);
		return process(baseDir, cmd);
	}
	
	public Pair<Boolean, String> doActivate(File baseDir, File compileDir, String filename) {
		File dest = new File(baseDir+"/../plugin_active/"+filename);
		if (!dest.exists()) dest.mkdir();
		// Keep dist directory deployment
		File dist = new File(baseDir+"/../plugin_active/"+filename+"/dist");
		if (!dist.exists()) dist.mkdir();
				
		List<String> cmd = new ArrayList<String>();
		cmd.add("/bin/cp");
		cmd.add("-r");
		cmd.add(compileDir.getAbsolutePath()+"/dist/.");
		cmd.add("../plugin_active/"+filename+"/dist");
		process(baseDir, cmd);
		cmd.clear();
		cmd.add("/bin/chmod");
		cmd.add("-R");
		cmd.add("755");		
		cmd.add("../plugin_active/"+filename);
		return process(baseDir, cmd);	
	}
	
	private DeployStatus getDeployStatus(MidataId plugin, boolean create) {
		DeployStatus result = statusMap.get(plugin);		
		if (create) {
			result = new DeployStatus();
			statusMap.put(plugin, result);
		}
		return result;
	}
	
	private void broadcast(DeployAction action, DeployPhase type) {
		Instances.sendDeploy(new DeployAction(action, type), getSelf());
	}
	
	private boolean result(DeployAction action, DeployPhase type, Pair<Boolean, String> result) {
		getSender().tell(new DeployAction(action, clusterNode, type, result.getRight(), result.getLeft()), getSelf());
		return result.getLeft();
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
			status.numFailed++;
			status.report.add();
		}
		status.report.add();
		System.out.println("failed");
	}
	
	public void deploy(DeployAction action) throws AppException {		
		MidataId pluginId = action.pluginId;
		try {
		AccessLog.logStart("jobs", "DEPLOY "+action.status+" "+pluginId);
		
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken"));		
		if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
			
		String deployLocation =  InstanceConfig.getInstance().getConfig().getString("visualizations.path");
		String targetDir1 = deployLocation+"/"+plugin.filename;
		File targetRepo = new File(targetDir1);
		if (plugin.repositoryDirectory != null && plugin.repositoryDirectory.trim().length()>0) {
			if (!plugin.repositoryDirectory.startsWith("/")) targetDir1 += "/";
			targetDir1 += plugin.repositoryDirectory;
		}
		File baseDir = new File(deployLocation);
		File targetCompile = new File(targetDir1);
		String repo = plugin.repositoryUrl;
	
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
			status.report.add();
			broadcast(action, DeployPhase.CHECKOUT);
			break;
			
		case COORDINATE_DELETE:
			status = getDeployStatus(pluginId, true);
			status.plugin = plugin;
			status.report = new DeploymentReport();
			status.report._id = pluginId;
			status.report.init();			
			status.report.sceduled = System.currentTimeMillis();
			status.report.add();
			broadcast(action, DeployPhase.DELETE);
			break;
		case DELETE:
			doDelete(baseDir, plugin.filename);
			break;
		case CHECKOUT:
			result(action, DeployPhase.STARTED, Pair.of(true, null));			
			boolean cont = true;
			if (targetRepo.exists() && targetRepo.isDirectory()) {
				cont = result(action, DeployPhase.REPORT_CHECKOUT, doGitPull(targetRepo));
			} else {
				cont = result(action, DeployPhase.REPORT_CHECKOUT, doGitClone(baseDir, repo, plugin.filename));
			}
			if (cont) toSelf(action, DeployPhase.INSTALL);					
			break;
		case INSTALL:
			if (result(action, DeployPhase.REPORT_INSTALL, doInstall(targetCompile))) toSelf(action, DeployPhase.AUDIT); 
			break;
		case AUDIT:
			if (result(action, DeployPhase.REPORT_AUDIT, doAudit(targetCompile))) toSelf(action, DeployPhase.COMPILE);
			break;
		case COMPILE:
			result(action, DeployPhase.REPORT_COMPILE, doBuild(targetCompile));
			break;
		case PUBLISH:
			result(action, DeployPhase.FINISHED, doActivate(baseDir, targetCompile, plugin.filename));			
			break;
		case STARTED:
			status = getDeployStatus(pluginId, false);
			status.numStarted++;
			status.report.clusterNodes.add(action.clusterNode);
			AccessLog.log("STARTED: numStarted="+status.numStarted+" numReady="+status.numReady+" numFailed="+status.numFailed+" numFinished="+status.numFinished);
			break;
		case REPORT_CHECKOUT:
			status = getDeployStatus(pluginId, false);	
			status.report.status.add(DeployPhase.CHECKOUT);
			status.report.checkoutReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			break;			
		case REPORT_INSTALL:
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.INSTALL);
			status.report.installReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			break;
		case REPORT_AUDIT:
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.AUDIT);
			status.report.auditReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			break;
		case REPORT_COMPILE:	
			status = getDeployStatus(pluginId, false);
			status.report.status.add(DeployPhase.COMPILE);
			status.report.buildReport.put(action.clusterNode, action.report);
			checkFail(action, status);
			if (action.success) {
				status.numReady++;
				AccessLog.log("COMPILE_SUCCESS: numStarted="+status.numStarted+" numReady="+status.numReady+" numFailed="+status.numFailed+" numFinished="+status.numFinished);
			} else {
				AccessLog.log("COMPILE_NO_SUCCESS: numStarted="+status.numStarted+" numReady="+status.numReady+" numFailed="+status.numFailed+" numFinished="+status.numFinished);
			}
			if (status.numReady >= status.numStarted) {
				AccessLog.log("DO-PUBLISH!");
				broadcast(action, DeployPhase.PUBLISH);
			} else {
				AccessLog.log("DO NOT-PUBLISH!");
			}
			break;		
		case FINISHED:
			status = getDeployStatus(pluginId, false);
			if (action.success) {
				status.numFinished++;
				AccessLog.log("FINISHED_SUCCESS: numStarted="+status.numStarted+" numReady="+status.numReady+" numFailed="+status.numFailed+" numFinished="+status.numFinished);
			} else {
				AccessLog.log("FINISHED_NO_SUCCESS: numStarted="+status.numStarted+" numReady="+status.numReady+" numFailed="+status.numFailed+" numFinished="+status.numFinished);
			}
            if (status.numFinished >= status.numStarted) {
            	AccessLog.log("ALL OK");
            	status.report.status.add(DeployPhase.FINISHED);
				status.report.finsihed = System.currentTimeMillis();
				status.report.add();
				status.plugin.repositoryDate = System.currentTimeMillis();
				Plugin.set(status.plugin._id, "repositoryDate", status.plugin.repositoryDate);
				statusMap.remove(status.plugin._id);
			}
            checkFail(action, status);
			break;		
		}
		} finally {
			ServerTools.endRequest();
		}
	}
}

class DeployStatus {
	public Plugin plugin;
	public DeploymentReport report;
	public int numStarted;
	public int numReady;
	public int numFinished;
	public int numFailed;
}
