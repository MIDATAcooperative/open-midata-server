

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;

import com.typesafe.config.Config;

import akka.actor.ActorSystem;
import akka.actor.Terminated;
import controllers.AutoRun;
import controllers.FHIR;
import controllers.Market;
import controllers.Plugins;
import controllers.PluginsAPI;
import controllers.research.AutoJoiner;
import models.PersistedSession;
import models.RecordGroup;
import play.inject.ApplicationLifecycle;
import play.libs.Json;
import play.libs.mailer.MailerClient;
import play.libs.ws.WSClient;
import setup.MinimalSetup;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;
import utils.json.CustomObjectMapper;
import utils.messaging.MailUtils;
import utils.messaging.Messager;
import utils.servlet.PlayHttpServletConfig;
import utils.stats.Stats;
import utils.sync.Instances;

/**
 * Actions that need to be done on application start and stop.
 *
 */
@Singleton
public class Global  {

@Inject
public Global(ActorSystem system, Config config, ApplicationLifecycle lifecycle, MailerClient mailerClient, WSClient ws) {
	    System.out.println("Starting UP");
		// Connect to production database
	    InstanceConfig.setInstance(new InstanceConfig(config));
	    
	    System.out.println("EMails");
	    MailUtils.setInstance(new MailUtils(mailerClient, config));
	    
	    System.out.println("Database connection");
		try {
		  DBLayer.connect(config);
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		  
		Plugins.init(ws, config);
		PluginsAPI.init(ws);
		
  		// Connect to search cluster
		//Search.connect();
		System.out.println("Object Mapper");
		// Set custom object mapper for Json
		Json.setObjectMapper(new CustomObjectMapper());		
		
		// Init FHIR
		System.out.println("FHIR Servlet");
		FHIR.servlet = new FHIRServlet();
		
		try {
			System.out.println("FHIR Servlet 2");
		  FHIR.servlet.setFhirContext(ResourceProvider.ctx);
		  System.out.println("FHIR Servlet 3");
		  FHIR.servlet.init(new PlayHttpServletConfig());
		
		  System.out.println("Minimal Setup");
		  MinimalSetup.dosetup();
		  
		  System.out.println("Market");
		  Market.correctOwners();
		  
		  System.out.println("Instances");
		  Instances.init();
		  
		  System.out.println("Record Group");
		  RecordGroup.load();		 
		  
		  System.out.println("Runtime Constants");
		  RuntimeConstants.instance = new RuntimeConstants();
		  
		  System.out.println("Expired Sessions");
		  PersistedSession.deleteExpired();
		  
		} catch (AppException e) {
		  AccessLog.logException("startup", e);
		  throw new NullPointerException();
		} catch (ServletException e) {
		  AccessLog.logException("startup", e);
		  throw new NullPointerException();
		}
		
		System.out.println("Statistiks");
		Stats.init(system);
		
		System.out.println("Messager");
		Messager.init(system);
		
		System.out.println("Auto-Join");
		AutoJoiner.init(system);
		
		System.out.println("Auto-Run");
		AutoRun.init();
		
				
		lifecycle.addStopHook(() -> {
			//AutoRun.shutdown();
		    System.out.println("Stopping MongoDB");
			DBLayer.close();
			System.out.println("Shutting down Cluster");
			CompletionStage<Terminated> result = Instances.shutdown();
			
			result.thenAccept(t -> { System.out.println("Terminated Cluster"); });
			return result;		
				
		}); 
		
		System.out.println("Finished STartup");
	}	

}
