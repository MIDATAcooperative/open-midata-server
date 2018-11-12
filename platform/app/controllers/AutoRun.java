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

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import controllers.admin.Administration;
import models.Admin;
import models.MidataId;
import models.PersistedSession;
import models.Plugin;
import models.Space;
import models.User;
import models.enums.UserRole;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
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
		
		managerSingleton = Instances.system().actorOf(ClusterSingletonManager.props(Props.create(ImportManager.class), null, settings), "autoimport");
		
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
				
		public void doImport(ImportRequest message) throws Exception {		    
		    	try {
			    	ImportRequest request = (ImportRequest) message;
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
					if (tuser.role.equals(UserRole.DEVELOPER)) {
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
		private final int nrOfWorkers;
		private int numberSuccess = 0;
		private int numberFailure = 0;
		private static Cancellable importer;
		
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
		}
		
		
		
		@Override
		public void postStop() throws Exception {
			
			super.postStop();
			
			importer.cancel();
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
		      .build();
		}
		
		public void startImport(StartImport message) throws Exception {
			try {
			
				AccessLog.log("Starting Autoimport...");
				PersistedSession.deleteExpired();
				try {
				   Administration.createStats();
				} catch (AppException e) {
					ErrorReporter.report("stats service", null, e);
				}
								
				User autorunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
				String handle = KeyManager.instance.login(1000l*60l*60l*23l, false);
				KeyManager.instance.unlock(autorunner._id, null);
				Set<Space> autoImports = Space.getAll(CMaps.map("autoImport", true), Sets.create("_id", "owner", "visualization"));
				
				for (Space space : autoImports) {										    
			       workerRouter.route(new ImportRequest(handle, autorunner._id, space), getSelf());
			    }
				
				AccessLog.log("Done scheduling Autoimport size="+autoImports.size());
			} catch (Exception e) {
				ErrorReporter.report("Autorun-Service", null, e);	
				throw e;
			} finally {
				ServerTools.endRequest();				
			}

		}
		
		public void processResult(ImportResult result) throws Exception {							
				if (result.exitCode == 0) numberSuccess++; else numberFailure++;
				AccessLog.log("Autoimport success="+numberSuccess+" fail="+numberFailure);
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
