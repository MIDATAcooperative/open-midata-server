package utils.plugins;

import models.Plugin;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ExternCDNContainer extends AbstractExternContainer {

		
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		serviceUrl = InstanceConfig.getInstance().getInternalCDNUrl();
	}
	
	@Override
	void doAction(DeployAction msg) throws AppException {		
		if (msg.status == DeployPhase.IMPORT_CDN) {
			Plugin plugin = Plugin.getById(msg.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));		
			if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
									
			processWithInput(plugin.filename, "deploy", null, msg, DeployPhase.REPORT_IMPORT_CDN);			
						
		} else if (msg.status == DeployPhase.WIPE_CDN) {
			Plugin plugin = Plugin.getById(msg.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));		
			if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
									
			process(plugin.filename, "wipe", null, msg, DeployPhase.REPORT_WIPE_CDN);
		}
		
	}
			
}
