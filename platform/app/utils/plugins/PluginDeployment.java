package utils.plugins;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import models.MidataId;
import models.Plugin;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.messaging.InputStreamCollector;
import utils.messaging.SubscriptionTriggered;
import utils.sync.Instances;
import org.apache.commons.lang3.tuple.Pair;

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
		 System.out.println("Execute command...");
		 for (String str : command) System.out.print(str);
		 System.out.println();
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
			  System.out.println("Wait for input...");
			  System.out.println(result.getResult());	
			  int exit = p.exitValue();
			  System.out.println("EXIT VALUE = "+exit);
			  return Pair.of(exit==0, result.getResult());
		 } catch (IOException e) {
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
		cmd.add("/usr/local/bin/npm");
		cmd.add("ci");		
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doAudit(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/local/bin/npm");
		cmd.add("audit");		
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doBuild(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/local/bin/npm");
		cmd.add("run");
		cmd.add("prod:build");
		return process(targetDir, cmd);
	}
	
	public Pair<Boolean, String> doDelete(File baseDir, String filename) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/bin/rm");
		cmd.add("-rf");
		cmd.add(filename);
		return process(baseDir, cmd);
	}
	
	public Pair<Boolean, String> doActivate(File baseDir, String filename) {
		File dest = new File(baseDir+"/plugin_active/"+filename);
		if (!dest.exists()) dest.mkdir();
		File dist = new File(baseDir+"/plugin_active/"+filename+"/dist");
		if (!dist.exists()) dist.mkdir();
		List<String> cmd = new ArrayList<String>();
		cmd.add("/bin/cp");
		cmd.add("-r");
		cmd.add(filename+"/dist");
		cmd.add("../plugin_active/"+filename);
		return process(baseDir, cmd);	
	}
	
	private DeployStatus getDeployStatus(MidataId plugin, boolean create) {
		DeployStatus result = statusMap.get(plugin);		
		if (result == null && create) {
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
		System.out.println("finished");
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
		System.out.println(action.status+" DEPLOY "+pluginId);
		
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl","repositoryToken"));		
		if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
			
		String deployLocation =  InstanceConfig.getInstance().getConfig().getString("visualizations.path");
		String targetDir = deployLocation+"/"+plugin.filename;
		File baseDir = new File(deployLocation);
		File target = new File(targetDir);
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
			if (target.exists() && target.isDirectory()) {
				cont = result(action, DeployPhase.REPORT_CHECKOUT, doGitPull(target));
			} else {
				cont = result(action, DeployPhase.REPORT_CHECKOUT, doGitClone(baseDir, repo, plugin.filename));
			}
			if (cont) toSelf(action, DeployPhase.INSTALL);					
			break;
		case INSTALL:
			if (result(action, DeployPhase.REPORT_INSTALL, doInstall(target))) toSelf(action, DeployPhase.AUDIT); 
			break;
		case AUDIT:
			if (result(action, DeployPhase.REPORT_AUDIT, doAudit(target))) toSelf(action, DeployPhase.COMPILE);
			break;
		case COMPILE:
			result(action, DeployPhase.REPORT_COMPILE, doBuild(target));
			break;
		case PUBLISH:
			result(action, DeployPhase.FINISHED, doActivate(baseDir, plugin.filename));			
			break;
		case STARTED:
			status = getDeployStatus(pluginId, false);
			status.numStarted++;
			status.report.clusterNodes.add(action.clusterNode);
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
			if (action.success) status.numReady++;
			if (status.numReady >= status.numStarted) {
				broadcast(action, DeployPhase.PUBLISH);
			}
			break;		
		case FINISHED:
			status = getDeployStatus(pluginId, false);
			if (action.success) status.numFinished++;			
            if (status.numFinished >= status.numStarted) {
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
