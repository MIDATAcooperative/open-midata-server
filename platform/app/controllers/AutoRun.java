package controllers;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import models.Admin;
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
import utils.db.FileStorage;
import utils.exceptions.AppException;
import utils.largerequests.UnlinkedBinary;
import utils.messaging.MailUtils;
import utils.messaging.MessageResponse;
import utils.messaging.ServiceHandler;
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
		
		managerSingleton = Instances.system().actorOf(ClusterSingletonManager.props(Props.create(ImportManager.class), PoisonPill.getInstance(), settings), "autoimport");
		
		final ClusterSingletonProxySettings proxySettings =
			    ClusterSingletonProxySettings.create(Instances.system());

		
		manager = Instances.system().actorOf(ClusterSingletonProxy.props("user/autoimport", proxySettings), "autoimportProxy");
		
		
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
	}
	
	/**
	 * response from import plugin for a space
	 *
	 */
	public static class ImportResult implements Serializable {
	
		private static final long serialVersionUID = 2863510695436070968L;
		
		private final int exitCode;

		/**
		 * Construct import result
		 * @param exitCode
		 */
		public ImportResult(int exitCode) {
			this.exitCode = exitCode;
		}
		
		/**
		 * Retrieve exit code of plugin
		 * @return
		 */
		public int getExitCode() {
			return exitCode;
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
	
	public static class SendEndReport implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6171207037975268555L;
		
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
			    	KeyManager.instance.continueSession(request.handle);
			    	MidataId autorunner = request.autorunner;
			    	Space space = request.space;
			        
			    	final String nodepath = InstanceConfig.getInstance().getConfig().getString("node.path");
					final String visPath = InstanceConfig.getInstance().getConfig().getString("visualizations.path");
							    	
			    	final Plugin plugin = Plugin.getById(space.visualization, Sets.create("type", "filename", "name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "tokenExchangeParams"));					
					User tuser = User.getById(space.owner, Sets.create("language", "role"));					
					SpaceToken token = new SpaceToken(request.handle, space._id, space.owner, tuser.role, null, null, autorunner);
					final String lang = tuser.language != null ? tuser.language : InstanceConfig.getInstance().getDefaultLanguage();
					final String tokenstr = token.encrypt();
					final String owner = space.owner.toString();
					final ActorRef sender = getSender();
					if (tuser.role.equals(UserRole.DEVELOPER) || tuser.role.equals(UserRole.ADMIN)) {
						sender.tell(new ImportResult(0), getSelf());
						return;
					}
		
					if (plugin.type != null && plugin.type.equals("oauth2")) {
						BSONObject oauthmeta = RecordManager.instance.getMeta(autorunner, space._id, "_oauth");
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
											  sender.tell(new ImportResult(p.exitValue()), getSelf());
											} catch (InterruptedException e) {
												sender.tell(new ImportResult(-2), getSelf());
											}
										} else {
											sender.tell(new ImportResult(-1), getSelf());
										}
									} catch (IOException e) {
										ErrorReporter.report("Autorun-Service", null, e);
									}
								});
		
							} else {
								sender.tell(new ImportResult(-1), getSelf());
							}
						} else {}
					} else if (plugin.type != null && plugin.type.equals("oauth1")) {
						BSONObject oauthmeta = RecordManager.instance.getMeta(autorunner, space._id, "_oauth1");
						if (oauthmeta != null) {
							AccessLog.log("OAuth 1");
							AccessLog.log(nodepath+" "+visPath+"/"+plugin.filename+"/server.js"+" "+tokenstr+" "+lang);
							Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr, lang).inheritIO().start();
							try {
							  p.waitFor();
							  sender.tell(new ImportResult(p.exitValue()), getSelf());
							} catch (InterruptedException e) {
								sender.tell(new ImportResult(-2), getSelf());
							}
						} else {
							sender.tell(new ImportResult(-1), getSelf());
						}
					} else {
					
						Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr, lang).inheritIO().start();
						try {
						  p.waitFor();
						  sender.tell(new ImportResult(p.exitValue()), getSelf());
						} catch (InterruptedException e) {
						  sender.tell(new ImportResult(-2), getSelf());
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

		private final Router workerRouter;
		private final ActorRef processor;
		private final int nrOfWorkers;
		private int numberSuccess = 0;
		private int numberFailure = 0;
		private static Cancellable importer;
		private static Cancellable endReport;
		
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
		
		/**
		 * Constructor
		 */
		public ImportManager() {
			this.nrOfWorkers = 1; // For Testing if less performance warnings occur
			
		    List<Routee> routees = new ArrayList<Routee>();
		    for (int i = 0; i < nrOfWorkers; i++) {
		      ActorRef r = getContext().actorOf(Props.create(Importer.class));
		      getContext().watch(r);
		      routees.add(new ActorRefRoutee(r));
		    }
		    workerRouter = new Router(new RoundRobinRoutingLogic(), routees);	
		    		    
		    processor = this.context().actorOf(new RoundRobinPool(2).props(Props.create(SubscriptionProcessor.class)), "subscriptionProcessor");
		}
		
		
		
		@Override
		public void postStop() throws Exception {
			
			super.postStop();
			
			importer.cancel();
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
						
		}


		@Override
		public Receive createReceive() {
		    return receiveBuilder()
		      .match(StartImport.class, this::startImport)
		      .match(ImportResult.class, this::processResult)
		      .match(MessageResponse.class, this::processResultNew)
		      .match(SendEndReport.class, this::reportEnd)
		      .build();
		}
		
		public void startImport(StartImport message) throws Exception {
			
			if (!reportSend) reportEnd();
			
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
			reportSend = false;
			errors = new StringBuffer();
			
			endReport = getContext().system().scheduler().scheduleOnce(
	                Duration.ofHours(3),
	                manager, new SendEndReport(),
	                Instances.system().dispatcher(), null);	
			
			try {
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
				MidataId autorunner = RuntimeConstants.instance.autorunService;
				String handle = KeyManager.instance.login(1000l*60l*60l*23l, false);
				KeyManager.instance.unlock(autorunner, null);
				Set<Space> autoImports = Space.getAll(CMaps.map("autoImport", true), Sets.create("_id", "owner", "visualization"));
				
				for (Space space : autoImports) {										    
			       workerRouter.route(new ImportRequest(handle, autorunner, space), getSelf());
			    }
				countOldImports = autoImports.size();
				AccessLog.log("Done scheduling old autoimport size="+autoImports.size());
				
				List<SubscriptionData> datas = SubscriptionData.getAllActiveFormat("time", SubscriptionData.ALL);
				countNewImports = datas.size();
				for (SubscriptionData data : datas) {
					processor.tell(new SubscriptionTriggered(data.owner, data.app, "time", null, null, null), getSelf());
				}
				
				AccessLog.log("Done scheduling new autoimport size="+datas.size());
				endScheduling = System.currentTimeMillis();
			} catch (Exception e) {
				ErrorReporter.report("Autorun-Service", null, e);	
				throw e;
			} finally {
				ServerTools.endRequest();				
			}

		}
		
		public void reportEnd(SendEndReport msg) {
			reportEnd();
		}
		
		public void reportEnd() {
			if (reportSend) return;
			
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
			report += "# Success messages: "+numberSuccess+" \n\n";
			report += "# Open Recovery Processes: "+openRecoveries+" \n\n";
			report += errors.toString();
			
			String email = InstanceConfig.getInstance().getConfig().getString("errorreports.targetemail");
			String fullname = InstanceConfig.getInstance().getConfig().getString("errorreports.targetname");
			String server = InstanceConfig.getInstance().getPlatformServer();
			MailUtils.sendTextMail(email, fullname, "Autoimport "+server, report);
			
			if (endReport != null) {
				endReport.cancel();
				endReport = null;				
			}
		}
		
		public void processResult(ImportResult result) throws Exception {							
				if (result.exitCode == 0) numberSuccess++; else {
					numberFailure++;
					errors.append(result.exitCode+" error (old)\n");
				}
				AccessLog.log("Autoimport success="+numberSuccess+" fail="+numberFailure);
				if (numberSuccess+numberFailure >= countOldImports + countNewImports) reportEnd();
		}
		
		public void processResultNew(MessageResponse result) throws Exception {							
			if (result.getErrorcode() == 0) numberSuccess++; else {
				numberFailure++;
				String msg = (result.getResponse() != null && result.getResponse().length()<1024) ? result.getResponse() : "error (new)";
				errors.append(result.getErrorcode()+" "+msg+"\n");
			}
			AccessLog.log("Autoimport success="+numberSuccess+" fail="+numberFailure);
			if (numberSuccess+numberFailure >= countOldImports + countNewImports) reportEnd();
	    }
		
	}
	
	private static int nextExecutionInSeconds(int hour, int minute){
		return Seconds.secondsBetween(
				new DateTime(),
				nextExecution(hour, minute)
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
}
