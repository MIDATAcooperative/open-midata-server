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

import akka.stream.IOResult;
import akka.stream.SourceRef;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamRefs;
import akka.util.ByteString;
import models.MidataId;
import models.Plugin;
import play.core.Paths;
import scala.reflect.io.Path;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.stats.ActionRecorder;

public class FirejailBuildContainer extends AbstractBuildContainer {

		
	private String globalBuildBaseDirectory;
	
	@Override
	public void preStart() throws Exception {		
		super.preStart();
		globalBuildBaseDirectory = InstanceConfig.getInstance().getConfig().getString("visualizations.path")+"/staging";
	}

	public void doAction(DeployAction action) throws AppException {	
		
		MidataId pluginId = action.pluginId;
	
		AccessLog.logStart("jobs", "DEPLOY "+action.status+" "+pluginId);
		
		Plugin plugin = Plugin.getById(pluginId, Sets.create("filename", "repositoryUrl", "repositoryDirectory", "repositoryToken"));		
		if (plugin.filename.indexOf(".")>=0 || plugin.filename.indexOf("/") >=0 || plugin.filename.indexOf("\\")>=0) return;
					
		String targetDir1 = globalBuildBaseDirectory+"/"+plugin.filename;
		File targetRepo = new File(targetDir1);
		if (plugin.repositoryDirectory != null && plugin.repositoryDirectory.trim().length()>0) {
			if (!plugin.repositoryDirectory.startsWith("/")) targetDir1 += "/";
			targetDir1 += plugin.repositoryDirectory;
		}
		File baseDir = new File(globalBuildBaseDirectory);
		File targetCompile = new File(targetDir1);
		String repo = plugin.repositoryUrl;
	
		if (plugin.repositoryToken != null) {
			if (repo.startsWith("https://")) repo = "https://"+plugin.repositoryToken+"@"+repo.substring("https://".length());
		}
		
		switch(action.status) {
		
		case DELETE:
			result(action, DeployPhase.REPORT_DELETE, doDelete(baseDir, plugin.filename));
			break;
		case CHECKOUT:			
			
			if (targetRepo.exists() && targetRepo.isDirectory()) {
				result(action, DeployPhase.REPORT_CHECKOUT, doGitPull(targetRepo));
			} else {
				result(action, DeployPhase.REPORT_CHECKOUT, doGitClone(baseDir, repo, plugin.filename));
			}						
			break;
		case INSTALL:
			result(action, DeployPhase.REPORT_INSTALL, doInstall(targetCompile)); 
			break;
		case AUDIT:
			result(action, DeployPhase.REPORT_AUDIT, doAudit(targetCompile));
			break;
		case AUDITFIX:
			result(action, DeployPhase.REPORT_AUDIT, doAuditFix(targetCompile));
			break;
		case COMPILE:
			result(action, DeployPhase.REPORT_COMPILE, doBuild(targetCompile));
			break;
		case EXPORT_TO_CDN:
			Pair<String, SourceRef<ByteString>> r = doExportToCDN(baseDir, targetCompile, plugin.filename);
			if (r.getRight() != null) {
				getSender().tell(action.response(DeployPhase.REPORT_EXPORT_TO_CDN, r.getRight()), getSelf());
			} else {
				result(action, DeployPhase.REPORT_EXPORT_TO_CDN, false, r.getLeft());
			}
			break;
		case EXPORT_SCRIPTS:
			Pair<String, SourceRef<ByteString>> r2 = doExportScripts(baseDir, targetCompile, plugin.filename);
			if (r2.getRight() != null) {
				getSender().tell(action.response(DeployPhase.REPORT_EXPORT_SCRIPTS, r2.getRight()), getSelf());
			} else {
				result(action, DeployPhase.REPORT_EXPORT_SCRIPTS, false, r2.getLeft());
			}
			break;
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
	
	public Pair<Boolean, String> doAuditFix(File targetDir) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/npm");
		cmd.add("audit");
		cmd.add("fix");	
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
		cmd.add(getCDNArchive(filename));
		cmd.add(getScriptArchive(filename));
		process(baseDir, cmd);
		cmd.clear();
		cmd.add("/bin/rm");
		cmd.add("-rf");
		cmd.add(filename);
		return process(baseDir, cmd);
	}
	
	private String getCDNArchive(String filename) {
		return "cdn-"+filename+".tar";
	}
	
	private String getScriptArchive(String filename) {
		return "script-"+filename+".tar";
	}
	
	public Pair<String, SourceRef<ByteString>> doExportToCDN(File baseDir, File compileDir, String filename) {
		
				
		List<String> cmd = new ArrayList<String>();
		cmd.add("/bin/tar");		
		cmd.add("-cvf");
		cmd.add(getCDNArchive(filename));
		cmd.add("-C");
		cmd.add(filename+"/dist");
		cmd.add(".");
		Pair<Boolean, String> r1 = process(baseDir, cmd);
		
		if (r1.getLeft()) {
			System.out.println("AAAAA1");
			Source<ByteString, CompletionStage<IOResult>> result = FileIO.fromFile(new File(baseDir.getAbsolutePath()+"/"+getCDNArchive(filename)));
			System.out.println("AAAAA2");
			SourceRef<ByteString> ref = result.runWith(StreamRefs.sourceRef(), getContext().getSystem());
			System.out.println("AAAAA3");
			return Pair.of(null, ref);
		} else {
			return Pair.of(r1.getRight(), null);
		}
					
	}
	
	public Pair<String, SourceRef<ByteString>> doExportScripts(File baseDir, File compileDir, String filename) {
		
		List<String> cmd = new ArrayList<String>();
		cmd.add("/bin/tar");		
		cmd.add("-cvf");
		cmd.add(getScriptArchive(filename));		
		cmd.add("-C");
		cmd.add(filename);
		cmd.add(".");
		Pair<Boolean, String> r1 = process(baseDir, cmd);
		
		if (r1.getLeft()) {
			System.out.println("AAAAA1");
			Source<ByteString, CompletionStage<IOResult>> result = FileIO.fromFile(new File(baseDir.getAbsolutePath()+"/"+getScriptArchive(filename)));
			System.out.println("AAAAA2");
			SourceRef<ByteString> ref = result.runWith(StreamRefs.sourceRef(), getContext().getSystem());
			System.out.println("AAAAA3");
			return Pair.of(null, ref);
		} else {
			return Pair.of(r1.getRight(), null);
		}
	}
	
	
}
