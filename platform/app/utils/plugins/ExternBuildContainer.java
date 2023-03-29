package utils.plugins;

import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.ActorRef;
import akka.stream.SourceRef;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamRefs;
import akka.util.ByteString;
import models.MidataId;
import models.Plugin;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ExternBuildContainer extends AbstractExternContainer {

				
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		serviceUrl = InstanceConfig.getInstance().getInternalBuilderUrl();
	}

	public void doAction(DeployAction action) throws AppException {	
		
		MidataId pluginId = action.pluginId;
	
		AccessLog.logStart("jobs", "DEPLOY "+action.status+" "+pluginId);
		
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken"));		
		if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
		String filename = plugin.filename;
		/*
		String targetDir1 = globalBuildBaseDirectory+"/"+plugin.filename;
		File targetRepo = new File(targetDir1);
		if (plugin.repositoryDirectory != null && plugin.repositoryDirectory.trim().length()>0) {
			if (!plugin.repositoryDirectory.startsWith("/")) targetDir1 += "/";
			targetDir1 += plugin.repositoryDirectory;
		}
		File baseDir = new File(globalBuildBaseDirectory);
		File targetCompile = new File(targetDir1);
		*/
		String repo = plugin.repositoryUrl;
	
		if (plugin.repositoryToken != null) {
			if (repo.startsWith("https://")) repo = "https://"+plugin.repositoryToken+"@"+repo.substring("https://".length());
		}
		
		switch(action.status) {
		
		case DELETE:
			process(filename, "delete", repo, action, DeployPhase.REPORT_DELETE);
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
		case AUDITFIX:
			process(filename, "auditfix", repo, action, DeployPhase.REPORT_AUDIT);			
			break;
		case COMPILE:
			process(filename, "build", repo, action, DeployPhase.REPORT_COMPILE);
			break;
		case EXPORT_TO_CDN:
			processWithResult(filename, "export_cdn", repo, action, DeployPhase.REPORT_EXPORT_TO_CDN);			
			break;
		case EXPORT_SCRIPTS:
			processWithResult(filename, "export_scripts", repo, action, DeployPhase.REPORT_EXPORT_SCRIPTS);
			break;
		}
	
	}
	
	
	
	private String getCDNArchive(String filename) {
		return "cdn-"+filename+".tar";
	}
	
	private String getScriptArchive(String filename) {
		return "script-"+filename+".tar";
	}
			
	
}
