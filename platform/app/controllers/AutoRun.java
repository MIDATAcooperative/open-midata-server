package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import models.Admin;
import models.Plugin;
import models.Space;
import models.User;
import play.Play;
import play.libs.Akka;
import play.libs.F.Callback;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import utils.access.AccessLog;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class AutoRun extends APIController {

		
	private static Cancellable importer;
	private static ActorRef manager;
	
	public static void init() {
		
		manager = Akka.system().actorOf(Props.create(ImportManager.class), "manager");
		
		importer = Akka.system().scheduler().schedule(
                Duration.create(nextExecutionInSeconds(4, 0), TimeUnit.SECONDS),
                Duration.create(24, TimeUnit.HOURS),
                manager, new StartImport(),
                Akka.system().dispatcher(), null);		
	}
	
	public static void shutdown() {
		if (importer != null) importer.cancel();
	}
	
	public static Result run() throws AppException {
		
		//String host = request().getHeader("Origin");
  	     			
		manager.tell(new StartImport(), ActorRef.noSender());		
		return ok();
	}
	
	public static class ImportRequest {
		private final ObjectId autorunner;
		private final Space space;
		
		public ImportRequest(ObjectId autorunner, Space space) {
			this.autorunner = autorunner;
			this.space = space;
		}
		
		public ObjectId getAutorunner() {
			return autorunner;
		}
		public Space getSpace() {
			return space;
		}				
	}
	
	public static class ImportResult {
		private final int exitCode;

		public ImportResult(int exitCode) {
			this.exitCode = exitCode;
		}
		
		public int getExitCode() {
			return exitCode;
		}
		
		
	}
	
	public static class StartImport {
		
	}
	
	public static class Importer extends UntypedActor {

		@Override
		public void onReceive(Object message) throws Exception {
		    if (message instanceof ImportRequest) {
		    	ImportRequest request = (ImportRequest) message;
		    	ObjectId autorunner = request.autorunner;
		    	Space space = request.space;
		        
		    	final String nodepath = Play.application().configuration().getString("node.path");
				final String visPath = Play.application().configuration().getString("visualizations.path");
						    	
		    	final Plugin plugin = Plugin.getById(space.visualization, Sets.create("type", "filename", "name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret"));
				SpaceToken token = new SpaceToken(space._id, space.owner, null, null, autorunner);
				final String tokenstr = token.encrypt();
	
				if (plugin.type != null && plugin.type.equals("oauth2")) {
					BSONObject oauthmeta = RecordManager.instance.getMeta(autorunner, space._id, "_oauth");
					if (oauthmeta != null) {
						if (oauthmeta.containsField("refreshToken")) {							                        				
							Plugins.requestAccessTokenOAuth2FromRefreshToken(autorunner, plugin, space._id.toString(), oauthmeta.toMap()).onRedeem(new Callback<Boolean>() {
								public void invoke(Boolean success) throws AppException, IOException {
									AccessLog.logDB("Auth:"+success);
									if (success) {
										AccessLog.debug(nodepath+" "+visPath+"/"+plugin.filename+"/server.js"+" "+tokenstr);
										Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr).inheritIO().start();
										try {
										  p.waitFor();
										  getSender().tell(new ImportResult(p.exitValue()), getSelf());
										} catch (InterruptedException e) {
											getSender().tell(new ImportResult(-2), getSelf());
										}
									} else {
										getSender().tell(new ImportResult(-1), getSelf());
									}
									
								}
							});
	
						} else {
							getSender().tell(new ImportResult(-1), getSelf());
						}
					} else {}
				}
		    			    			    			    			       
		      } else {
		        unhandled(message);
		      }			
		}
		
	}
	
	public static class ImportManager extends UntypedActor {

		private final Router workerRouter;
		private final int nrOfWorkers;
		private int numberSuccess = 0;
		private int numberFailure = 0;
		
		public ImportManager() {
			this.nrOfWorkers = 4;
			
		    List<Routee> routees = new ArrayList<Routee>();
		    for (int i = 0; i < nrOfWorkers; i++) {
		      ActorRef r = getContext().actorOf(Props.create(Importer.class));
		      getContext().watch(r);
		      routees.add(new ActorRefRoutee(r));
		    }
		    workerRouter = new Router(new RoundRobinRoutingLogic(), routees);					    
		}
		
		@Override
		public void onReceive(Object message) throws Exception {
			if (message instanceof StartImport) {
				AccessLog.debug("Starting Autoimport...");
				User autorunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
				KeyManager.instance.unlock(autorunner._id, null);
				Set<Space> autoImports = Space.getAll(CMaps.map("autoImport", true), Sets.create("_id", "owner", "visualization"));
				
				for (Space space : autoImports) {										    
			       workerRouter.route(new ImportRequest(autorunner._id, space), getSelf());
			    }
				
				AccessLog.debug("Done scheduling Autoimport size="+autoImports.size());
			} else if (message instanceof ImportResult) {
				ImportResult result = (ImportResult) message;
				if (result.exitCode == 0) numberSuccess++; else numberFailure++;
				AccessLog.logDB("Autoimport success="+numberSuccess+" fail="+numberFailure);
			} else {
			    unhandled(message);
		    }			
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
