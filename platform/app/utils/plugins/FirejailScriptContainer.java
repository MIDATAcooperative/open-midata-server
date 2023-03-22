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
import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.ActorRef;
import akka.stream.IOResult;
import akka.stream.SourceRef;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import models.Plugin;
import utils.InstanceConfig;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class FirejailScriptContainer extends AbstractScriptContainer {

	 private String globalBaseDirectory;
		
		@Override
		public void preStart() throws Exception {		
			super.preStart();
			globalBaseDirectory = InstanceConfig.getInstance().getConfig().getString("visualizations.path")+"/scripts";
		}

		@Override
		void doAction(DeployAction msg) throws AppException {		
			if (msg.status == DeployPhase.IMPORT_SCRIPTS) {
				Plugin plugin = Plugin.getById(msg.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));		
				if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
										
				File baseDir = new File(globalBaseDirectory);
				
				doDownload(msg, baseDir, plugin.filename, msg.exportedData);
			} else if (msg.status == DeployPhase.WIPE_SCRIPT) {
				Plugin plugin = Plugin.getById(msg.pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken", "hasScripts"));		
				if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
										
				File baseDir = new File(globalBaseDirectory+"/"+plugin.filename);
				
				doWipe(msg, baseDir);
			}
			
		}
		
		private String getScriptsArchive(String filename) {
			return "scripts-"+filename+".tar";
		}
		
		public void doDownload(final DeployAction action, final File baseDir, final String filename, SourceRef<ByteString> data) {
			File dest = new File(baseDir+"/"+filename);
			if (!dest.exists()) dest.mkdir();
			
			final ActorRef sender = getSender();	
			Sink<ByteString, CompletionStage<IOResult>> result = FileIO.toFile(new File(dest.getAbsolutePath()+"/"+getScriptsArchive(filename)));
			
			CompletionStage<IOResult> io = data.getSource().runWith(result, getContext().getSystem());
			io.thenRun(() -> this.doActivate(sender, action, dest, filename));
		}
		
		public void doActivate(ActorRef sender, DeployAction action, File deploymentDir, String filename) {
			
			List<String> cmd = new ArrayList<String>();
			cmd.add("/bin/tar");		
			cmd.add("-xf");
			cmd.add(deploymentDir.getAbsolutePath()+"/"+getScriptsArchive(filename));
			cmd.add("-C");
			cmd.add(".");
			process(deploymentDir, cmd);
						
			cmd.clear();
			cmd.add("rm");
			cmd.add(getScriptsArchive(filename));
			process(deploymentDir, cmd);
			
			cmd.clear();
			cmd.add("/bin/chmod");
			cmd.add("-R");
			cmd.add("755");		
			cmd.add(".");
			Pair<Boolean, String> result = process(deploymentDir, cmd);
			result(sender, action, DeployPhase.REPORT_IMPORT_SCRIPTS, result);
		}
		
       public void doWipe(DeployAction action, File deploymentDir) {
			
			List<String> cmd = new ArrayList<String>();
			cmd.add("/bin/rm");		
			cmd.add("-rf");
			cmd.add(deploymentDir.getAbsolutePath());
			
			process(deploymentDir, cmd);
			
			result(action, DeployPhase.REPORT_WIPE_SCRIPT, true, "");
		}

}
