package utils.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.tuple.Pair;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.stream.IOResult;
import org.apache.pekko.stream.SourceRef;
import org.apache.pekko.stream.javadsl.FileIO;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import models.Plugin;
import play.libs.ws.WSClient;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ExternScriptContainer extends AbstractExternContainer {

		
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		serviceUrl = InstanceConfig.getInstance().getInternalScriptingUrl();
	}
	
	@Override
	void doAction(DeployAction msg) throws AppException {		
		if (msg.status == DeployPhase.IMPORT_SCRIPTS) {
			Plugin plugin = Plugin.getById(msg.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));		
			if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
									
			processWithInput(plugin.filename, "deploy", null, msg, DeployPhase.REPORT_IMPORT_SCRIPTS);			
						
		} else if (msg.status == DeployPhase.WIPE_SCRIPT) {
			Plugin plugin = Plugin.getById(msg.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));		
			if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
									
			process(plugin.filename, "wipe", null, msg, DeployPhase.REPORT_WIPE_SCRIPT);
		}
		
	}
			
}
