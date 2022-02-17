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

package controllers;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.BSONObject;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.mongodb.MongoGridFSException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinPool;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import controllers.admin.Administration;
import models.KeyRecoveryProcess;
import models.MidataId;
import models.PersistedSession;
import models.Plugin;
import models.Space;
import models.SubscriptionData;
import models.User;
import models.enums.UserRole;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.db.FileStorage;
import utils.exceptions.AppException;
import utils.largerequests.UnlinkedBinary;
import utils.messaging.MailSenderType;
import utils.messaging.MailUtils;
import utils.messaging.MessageResponse;
import utils.messaging.SubscriptionProcessor;
import utils.messaging.SubscriptionTriggered;
import utils.sync.Instances;

/**
 * Automatically run plugins once a day for auto import of records
 *
 */
public class AutoRun extends APIController {

		

	private static ActorRef managerSingleton;
	private static ActorRef manager;
	
	/**
	 * initialize import job launcher
	 */
	public static void init() {
		
		//manager = Akka.system().actorOf(Props.create(ImportManager.class), "manager");
		
		final ClusterSingletonManagerSettings settings =
				  ClusterSingletonManagerSettings.create(Instances.system());
		
		managerSingleton = Instances.system().actorOf(ClusterSingletonManager.props(Props.create(ImportManager.class).withDispatcher("medium-work-dispatcher"), PoisonPill.getInstance(), settings), "autoimport");
		
		final ClusterSingletonProxySettings proxySettings =
			    ClusterSingletonProxySettings.create(Instances.system());

		
		manager = Instances.system().actorOf(ClusterSingletonProxy.props("user/autoimport", proxySettings).withDispatcher("medium-work-dispatcher"), "autoimportProxy");
		
		
	}
	
	/**
	 * shutdown job launcher
	 */
	//public static void shutdown() {
	//	if (importer != null) importer.cancel();
	//}
	
	/**
	 * manually trigger import. Only for testing.
	 * @return
	 * @throws AppException
	 */
	public Result run() throws AppException {		  	     		
		manager.tell(new StartImport(), ActorRef.noSender());		
		return ok();
	}
	
	/**
	 * request to run plugin for a specific space
	 *
	 */
	public static class ImportRequest implements Serializable {
		
		private static final long serialVersionUID = 6535855157383731993L;
		
		private final MidataId autorunner;
		private final Space space;
		private final String handle;
		
		/**
		 * Contruct import request
		 * @param autorunner id of executing "user". (There is one "autorun" user in the database)
		 * @param space (id of space to run plugin)
		 */
		public ImportRequest(String handle, MidataId autorunner, Space space) {
			this.handle = handle;
			this.autorunner = autorunner;
			this.space = space;
		}
		
		/**
		 * Retrieve id of autorun user
		 * @return id
		 */
		public MidataId getAutorunner() {
			return autorunner;
		}
		
		/**
		 * Retrieve space to run plugin
		 * @return id of space
		 */
		public Space getSpace() {
			return space;
		}	
		
		public String getHandle() {
			return handle;
		}
		
		public String toString() {
			return "import space="+space._id;
		}
	}
	
	/**
	 * response from import plugin for a space
	 *
	 */
	public static class ImportResult implements Serializable {
	
		private static final long serialVersionUID = 2863510695436070968L;
		
		private final int exitCode;
		private final String message;
		private final String plugin;

		/**
		 * Construct import result
		 * @param exitCode
		 */
		public ImportResult(int exitCode, String message, String plugin) {
			this.exitCode = exitCode;
			this.message = message;
			this.plugin = plugin;
		}
		
		/**
		 * Retrieve exit code of plugin
		 * @return
		 */
		public int getExitCode() {
			return exitCode;
		}

		/**
		 * Get error message
		 * @return
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * Returns internal name of plugin
		 * @return
		 */
		public String getPlugin() {
			return plugin;
		}
						
		
		
	}
	
	/**
	 * Start import message.
	 * At this time there are no parameters to pass
	 *
	 */
	public static class StartImport implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1459275213447427228L;
		
	}
	
	public static class StartIntradayImport implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 9140583433375781798L;
		
	}
	
	public static class SendEndReport implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6171207037975268555L;
		
	}
	
	public static class ImportTick implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3167900067513655444L;
		
		
	}
	
	/**
	 * Akka actor that runs the plugin of a specific space
	 *
	 */
	public static class Importer extends AbstractActor {

		@Override
		public Receive createReceive() {
		    return receiveBuilder()
		      .match(ImportRequest.class, this::doImport)	      
		      .build();
		}
				
		public void doImport(ImportRequest request) throws Exception {		    
		    	try {	
		    		AccessLog.logStart("jobs", request.toString());
			    	KeyManager.instance.continueSession(request.handle);
			    	MidataId autorunner = request.autorunner;
			    	AccessContext context = ContextManager.instance.createSessionForDownloadStream(autorunner, UserRole.ANY);
			    	Space space = request.space;
			        
			    	final String nodepath = InstanceConfig.getInstance().getConfig().getString("node.path");
					final String visPath = InstanceConfig.getInstance().getConfig().getString("visualizations.path");
					final ActorRef sender = getSender();		    	
			    	final Plugin plugin = Plugin.getById(space.visualization, Sets.create("type", "filename", "name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "tokenExchangeParams"));
			    	if (plugin==null) {
						sender.tell(new ImportResult(-1, "Plugin not existing", null), getSelf());
						return;
					}
			    	
					User tuser = User.getById(space.owner, Sets.create("language", "role"));		
					if (tuser == null) {
						sender.tell(new ImportResult(0, "Ignore autoimport for deleted user", plugin.filename), getSelf());
						return;
					}
					SpaceToken token = new SpaceToken(request.handle, space._id, space.owner, tuser.role, null, null, autorunner);
					final String lang = tuser.language != null ? tuser.language : InstanceConfig.getInstance().getDefaultLanguage();
					final String tokenstr = token.encrypt();
					final String owner = space.owner.toString();
					
					if (tuser.role.equals(UserRole.DEVELOPER) || tuser.role.equals(UserRole.ADMIN)) {
						sender.tell(new ImportResult(0, "Ignore autoimport for developer/admin", plugin.filename), getSelf());
						return;
					}
		
					if (plugin.type != null && plugin.type.equals("oauth2")) {
						BSONObject oauthmeta = RecordManager.instance.getMeta(context, space._id, "_oauth");
						if (oauthmeta != null) {
							if (oauthmeta.get("refreshToken") != null) {							                        				
								Plugins.requestAccessTokenOAuth2FromRefreshToken(request.handle, autorunner, plugin, space._id.toString(), oauthmeta.toMap()).thenAcceptAsync(success1 -> {
									try{
									    boolean success = (Boolean) success1;
										AccessLog.log("Auth:"+success);
										if (success) {
											AccessLog.log(nodepath+" "+visPath+"/"+plugin.filename+"/server.js"+" "+tokenstr+" "+lang+" "+owner);
											Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr, lang, "http://localhost:9001", owner).inheritIO().start();
											try {
											  p.waitFor();
											  sender.tell(new ImportResult(p.exitValue(), null, plugin.filename), getSelf());
											} catch (InterruptedException e) {
												sender.tell(new ImportResult(-2, "Interrupted", plugin.filename), getSelf());
											}
										} else {
											sender.tell(new ImportResult(-1, "Authorization failed (OAuth2)", plugin.filename), getSelf());
										}
									} catch (IOException e) {
										ErrorReporter.report("Autorun-Service", null, e);
									}
								});
		
							} else {
								sender.tell(new ImportResult(-1, "No refresh token available (OAuth2)", plugin.filename), getSelf());
							}
						} else {}
					} else if (plugin.type != null && plugin.type.equals("oauth1")) {
						BSONObject oauthmeta = RecordManager.instance.getMeta(context, space._id, "_oauth1");
						if (oauthmeta != null) {
							AccessLog.log("OAuth 1");
							AccessLog.log(nodepath+" "+visPath+"/"+plugin.filename+"/server.js"+" "+tokenstr+" "+lang);
							Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr, lang).inheritIO().start();
							try {
							  p.waitFor();
							  sender.tell(new ImportResult(p.exitValue(), null, plugin.filename), getSelf());
							} catch (InterruptedException e) {
								sender.tell(new ImportResult(-2, "Interrupted", plugin.filename), getSelf());
							}
						} else {
							sender.tell(new ImportResult(-1, "No oauth1 info available", plugin.filename), getSelf());
						}
					} else {
					
						Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr, lang).inheritIO().start();
						try {
						  p.waitFor();
						  sender.tell(new ImportResult(p.exitValue(), null, plugin.filename), getSelf());
						} catch (InterruptedException e) {
						  sender.tell(new ImportResult(-2, "Interrupted", plugin.filename), getSelf());
						}
					
					}
		    	} catch (Exception e) {
		    		ErrorReporter.report("Autorun-Service", null, e);	
		    		throw e;
		    	} finally {
		    		ServerTools.endRequest();		    		
		    	}		    
		}
		
	}
	
	/**
	 * Akka actor that manages the import process
	 *
	 */
	public static class ImportManager extends AbstractActor {

		public final static int PARALLEL = 2;
		private final Router workerRouter;
		private final ActorRef processor;
		private final int nrOfWorkers;
		private int numberSuccess = 0;
		private int numberFailure = 0;
		private static Cancellable importer;
		private static Cancellable intradayImporter;
		private static Cancellable endReport;
		private static Cancellable speedControl;
		
		private long startTime = 0;
		private long startRemoveUnlinkedFiles = 0;
		private long startCreateDatabaseStats = 0;
		private long startAutoimport = 0;
		private long endScheduling = 0;
		private int countUnlinkedFiles = 0;
		private int errorCount = 0;
		private int countOldImports = 0;
		private int countNewImports = 0;
		private int openRecoveries = 0;
		private boolean reportSend = true;
		private StringBuffer errors;
		
		String handle;
		List<SubscriptionData> datas;
		Iterator<SubscriptionData> datasIt;
		List<Space> autoImports;
		Iterator<Space> autoImportsIt;
		//Set<MidataId> done;
		
		private int countSlow = 0;
		private int isSlow = 0;
		
		/**
		 * Constructor
		 */
		public ImportManager() {
			this.nrOfWorkers = PARALLEL; // For Testing if less performance warnings occur
			
		    List<Routee> routees = new ArrayList<Routee>();
		    for (int i = 0; i < nrOfWorkers; i++) {
		      ActorRef r = getContext().actorOf(Props.create(Importer.class).withDispatcher("slow-work-dispatcher"));
		      getContext().watch(r);
		      routees.add(new ActorRefRoutee(r));
		    }
		    workerRouter = new Router(new RoundRobinRoutingLogic(), routees);	
		    		    
		    processor = this.context().actorOf(new RoundRobinPool(PARALLEL).props(Props.create(SubscriptionProcessor.class).withDispatcher("slow-work-dispatcher")), "subscriptionProcessor");
		}
		
		
		
		@Override
		public void postStop() throws Exception {
			
			super.postStop();
			
			importer.cancel();
			intradayImporter.cancel();
			if (speedControl != null) speedControl.cancel();
			if (endReport != null) endReport.cancel();
		}



		@Override
		public void preStart() throws Exception {			
			super.preStart();
			
			importer = getContext().system().scheduler().schedule(
	                Duration.ofSeconds(nextExecutionInSeconds(4, 0)),
	                Duration.ofHours(24),
	                manager, new StartImport(),
	                Instances.system().dispatcher(), null);
				
			intradayImporter = getContext().system().scheduler().schedule(
	                Duration.ofSeconds(nextExecution30InSeconds()),
	                Duration.ofMinutes(30),
	                manager, new StartIntradayImport(),
	                Instances.system().dispatcher(), null);
		}


		@Override
		public Receive createReceive() {
		    return receiveBuilder()
		      .match(StartImport.class, this::startImport)
		      .match(StartIntradayImport.class, this::startIntradayImport)
		      .match(ImportResult.class, this::processResult)
		      .match(MessageResponse.class, this::processResultNew)
		      .match(SendEndReport.class, this::reportEnd)
		      .match(ImportTick.class, this::importTick)
		      .build();
		}
		
		public void startImport(StartImport message) throws Exception {
			
			if (!reportSend) reportEnd();
			
			if (speedControl != null) {
				speedControl.cancel();
				speedControl = null;
			}
			
			startTime = 0;
			startRemoveUnlinkedFiles = 0;
			startCreateDatabaseStats = 0;
			startAutoimport = 0;
			endScheduling = 0;
			countUnlinkedFiles = 0;
			errorCount = 0;
			countOldImports = 0;
			countNewImports = 0;
			openRecoveries = 0;
			numberSuccess = 0;
			numberFailure = 0;
			isSlow = 0;
			countSlow = 0;
			reportSend = false;
			handle = null;
			datas = null;
			datasIt = null;
			autoImports = null;
			autoImportsIt = null;
		    //done = null;
			errors = new StringBuffer();
			
			endReport = getContext().system().scheduler().scheduleOnce(
	                Duration.ofHours(3),
	                manager, new SendEndReport(),
	                Instances.system().dispatcher(), null);	
			
			try {
				AccessLog.logStart("jobs", "start import");
			    startTime = System.currentTimeMillis();
				AccessLog.log("Removing expired sessions...");
				PersistedSession.deleteExpired();
				
				startRemoveUnlinkedFiles = System.currentTimeMillis();
				AccessLog.log("Removing unlinked files...");
				try {
				   List<UnlinkedBinary> files = UnlinkedBinary.getExpired();
				   countUnlinkedFiles = files.size();
				   for (UnlinkedBinary file : files) {
					   try {
						 FileStorage.delete(file._id.toObjectId());
					   } catch (MongoGridFSException e) {}					   
					   file.delete();
				   }
				} catch (AppException e) {
					ErrorReporter.report("remove unlinked files", null, e);
					errorCount++;
				}
				
				startCreateDatabaseStats = System.currentTimeMillis();
				AccessLog.log("Creating database statistics...");
				try {
				   Administration.createStats();
				   openRecoveries = (int) KeyRecoveryProcess.count();
				} catch (AppException e) {
					ErrorReporter.report("stats service", null, e);
					errorCount++;
				}
				
				startAutoimport = System.currentTimeMillis();
				AccessLog.log("Starting Autoimport...");
				
				handle = KeyManager.instance.login(1000l*60l*60l*23l, false);
				MidataId autorunner = RuntimeConstants.instance.autorunService;
				KeyManager.instance.unlock(autorunner, null);
				autoImports = new ArrayList<Space>(Space.getAll(CMaps.map("autoImport", true), Sets.create("_id", "owner", "visualization")));
				autoImportsIt = autoImports.iterator();
				
				countOldImports = autoImports.size();
				AccessLog.log("Done scheduling old autoimport size="+autoImports.size());
				
				datas = SubscriptionData.getAllActiveFormat("time", SubscriptionData.ALL);
				datasIt = datas.iterator();
				//done = new HashSet<MidataId>();
				countNewImports = datas.size();
				
				
				speedControl = getContext().system().scheduler().schedule(Duration.ofSeconds(10),
		                Duration.ofSeconds(10),
		                manager, new ImportTick(),
		                Instances.system().dispatcher(), null);	
				
				for (int i=0;i<PARALLEL;i++) {
					importTick();
				}
				
				AccessLog.log("Done scheduling new autoimport size="+datas.size());
				endScheduling = System.currentTimeMillis();
				
				if (countOldImports + countNewImports == 0) reportEnd();
			} catch (Exception e) {
				ErrorReporter.report("Autorun-Service", null, e);	
				throw e;
			} finally {
				ServerTools.endRequest();				
			}

		}
		
public void startIntradayImport(StartIntradayImport message) throws Exception {
			if (autoImportsIt != null || datasIt != null) return;
				
			if (speedControl != null) {
				speedControl.cancel();
				speedControl = null;
			}
			
			if (!reportSend) {
				reportEnd();
			}
			
			startTime = 0;
			startRemoveUnlinkedFiles = 0;
			startCreateDatabaseStats = 0;
			startAutoimport = 0;
			endScheduling = 0;
			countUnlinkedFiles = 0;
			errorCount = 0;
			countOldImports = 0;
			countNewImports = 0;
			openRecoveries = 0;
			numberSuccess = 0;
			numberFailure = 0;
			isSlow = 0;
			countSlow = 0;
			reportSend = true;
			handle = null;
			datas = null;
			datasIt = null;
			autoImports = null;
			autoImportsIt = null;
		    //done = null;
			errors = new StringBuffer();
			
			try {
				AccessLog.logStart("jobs", "start intraday import");								
				datas = SubscriptionData.getAllActiveFormat("time/30m", SubscriptionData.ALL);
				datasIt = datas.iterator();
				//done = new HashSet<MidataId>();
				countNewImports = datas.size();
				
				if (countNewImports > 0) {
								
					speedControl = getContext().system().scheduler().schedule(Duration.ofSeconds(10),
			                Duration.ofSeconds(10),
			                manager, new ImportTick(),
			                Instances.system().dispatcher(), null);	
					
					for (int i=0;i<PARALLEL;i++) {
						importTick();
					}
				}
				
				AccessLog.log("Done scheduling new autoimport size="+datas.size());
									
			} catch (Exception e) {
				ErrorReporter.report("Autorun-Service", null, e);	
				throw e;
			} finally {
				ServerTools.endRequest();				
			}

		}
		
		public void importTick(ImportTick msg) {
						
			if (isSlow<2) { isSlow++;return; }				
									
			boolean startedSome = false;
			for (int i=0;i<PARALLEL;i++) {
				if (importTick()) startedSome = true;
			}
			
			if (startedSome) {
				AccessLog.logStart("jobs", "import tick slow="+countSlow);				
				countSlow++;
				ServerTools.endRequest();
			}
		}
		
		public boolean importTick() {
			AccessLog.logStart("jobs", "import tick");
			//boolean foundone = false;
			if (autoImportsIt != null && autoImportsIt.hasNext()) {
				Space space = autoImportsIt.next();
				MidataId autorunner = RuntimeConstants.instance.autorunService;
				
				isSlow = 0;
				workerRouter.route(new ImportRequest(handle, autorunner, space), getSelf());
				
				return true;
			} else while (datasIt!=null && datasIt.hasNext()) {
				SubscriptionData data = datasIt.next();
				//if (!done.contains(data.owner)) {
				//	  done.add(data.owner);
					  //foundone = true;
					  isSlow = 0;
					  processor.tell(new SubscriptionTriggered(data._id, data.owner, data.app, data.format, null, null, null, null, null), getSelf());
					  return true;
				//}
			}		
			autoImportsIt = null;
			datasIt = null;
			AccessLog.log("autoimport nothing to start left slow="+countSlow);						
			
			ServerTools.endRequest();
			return false;
		}
		
		public void reportEnd(SendEndReport msg) {
			reportEnd();
		}
		
		public void reportEnd() {			
			
			AccessLog.logStart("jobs", "report auto-import end");
			
			if (speedControl != null) {
				speedControl.cancel();
				speedControl = null;
			}
			
			if (reportSend) {
				ServerTools.endRequest();		
				return;
			}
			
			reportSend = true;
			long end = System.currentTimeMillis();
			String report = "Cleaning up sessions :"+(startRemoveUnlinkedFiles-startTime)+" ms\n";
			report += "Cleaning up unused file uploads: "+(startCreateDatabaseStats-startRemoveUnlinkedFiles)+" ms\n";
			report += "Create database statistics: "+(startAutoimport-startCreateDatabaseStats)+" ms\n";
			report += "Schedule auto-imports: "+(endScheduling-startAutoimport)+" ms\n";
			report += "Execute auto-import: "+(end-endScheduling)+" ms\n";
			report += "--------------------\n";
			report += "Total time for service: "+(end-startTime)+" ms\n";
			report += "\n\n";
			report += "# Old style auto-imports: "+countOldImports+" \n";
			report += "# New style auto-imports: "+countNewImports+" \n";			
			report += "# Errors during scheduling: "+errorCount+" \n";
			report += "# Errors during import: "+numberFailure+" \n";
			report += "# Success messages: "+numberSuccess+" \n";
			report += "# Slow imports: "+countSlow+" \n\n";
			report += "# Open Recovery Processes: "+openRecoveries+" \n\n";
			report += errors.toString();
						
			handle = null;
			datas = null;
			datasIt = null;
			autoImports = null;
			autoImportsIt = null;
		    //done = null;
			
			String email = InstanceConfig.getInstance().getConfig().getString("errorreports.targetemail");
			String fullname = InstanceConfig.getInstance().getConfig().getString("errorreports.targetname");
			String server = InstanceConfig.getInstance().getPlatformServer();
			MailUtils.sendTextMail(MailSenderType.STATUS, email, fullname, "Autoimport "+server, report);									
			
			if (endReport != null) {
				endReport.cancel();
				endReport = null;				
			}
			
			ServerTools.endRequest();		
		}
		
		public void processResult(ImportResult result) throws Exception {	
			    AccessLog.logStart("jobs", "received auto import result (old)");
				if (result.exitCode == 0) numberSuccess++; else {
					numberFailure++;
					String msg = (result.getMessage() != null && result.getMessage().length()<1024) ? result.getMessage() : "error";
					errors.append(result.exitCode+" "+(result.getPlugin()!=null?result.getPlugin():"")+" "+msg+" (old)\n");
				}
				AccessLog.log("Autoimport success="+numberSuccess+" fail="+numberFailure);
				
				if (numberSuccess+numberFailure >= countOldImports + countNewImports) reportEnd();
				else importTick();
		}
		
		public void processResultNew(MessageResponse result) throws Exception {
			AccessLog.logStart("jobs", "received auto import result");
			if (result.getErrorcode() == 0) numberSuccess++; else {
				numberFailure++;
				String msg = (result.getResponse() != null && result.getResponse().length()<1024) ? result.getResponse() : "error (new)";
				errors.append(result.getErrorcode()+" "+(result.getPlugin()!=null?result.getPlugin():"")+" "+msg+"\n");
			}
			AccessLog.log("Autoimport success="+numberSuccess+" fail="+numberFailure);
			if (numberSuccess+numberFailure >= countOldImports + countNewImports) reportEnd();
			else importTick();
	    }
		
	}
	
	public static int nextExecutionInSeconds(int hour, int minute){
		return Seconds.secondsBetween(
				new DateTime(),
				nextExecution(hour, minute)
				).getSeconds();
	}
	
	public static int nextExecution30InSeconds(){
		return Seconds.secondsBetween(
				new DateTime(),
				nextExecution30()
				).getSeconds();
	}

	private static DateTime nextExecution(int hour, int minute){
		DateTime next = new DateTime()
		.withHourOfDay(hour)
		.withMinuteOfHour(minute)
		.withSecondOfMinute(0)
		.withMillisOfSecond(0);

		return (next.isBeforeNow())
				? next.plusHours(24)
						: next;
	}
	
	private static DateTime nextExecution30(){
		DateTime next = new DateTime()		
		.withMinuteOfHour(15)
		.withSecondOfMinute(0)
		.withMillisOfSecond(0);

		return (next.isBeforeNow())
				? (next.plusMinutes(30).isBeforeNow() ? next.plusMinutes(60) : next.plusMinutes(30) ) 
				: next;
	}
}
