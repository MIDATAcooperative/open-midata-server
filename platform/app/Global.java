

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;

import com.typesafe.config.Config;

import akka.Done;
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
import utils.evolution.AccountPatches;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;
import utils.json.CustomObjectMapper;
import utils.messaging.MailUtils;
import utils.messaging.Messager;
import utils.messaging.SMSAPIProvider;
import utils.messaging.SMSUtils;
import utils.messaging.ServiceHandler;
import utils.messaging.SubscriptionManager;
import utils.servlet.PlayHttpServletConfig;
import utils.stats.Stats;
import utils.stats.UsageStatsRecorder;
import utils.sync.Instances;

/**
 * Actions that need to be done on application start and stop.
 *
 */
@Singleton
public class Global  {

@Inject
public Global(ActorSystem system, Config config, ApplicationLifecycle lifecycle, MailerClient mailerClient, WSClient ws) {
	    System.out.println("----------------------------------------------------------------");
	    System.out.println("Starting UP");
		// Connect to production database
	    InstanceConfig.setInstance(new InstanceConfig(config));
	    
	    System.out.println("EMails");
	    MailUtils.setInstance(new MailUtils(mailerClient, config));
	    
	    System.out.println("SMS");
	    
	    SMSUtils.setInstance(ws, config);
	    
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
		FHIR.servlet_r4 = new FHIRServlet();
		FHIR.servlet_stu3 = new utils.fhir_stu3.FHIRServlet();
		try {
			
		  FHIR.servlet_r4.setFhirContext(ResourceProvider.ctx);
		  FHIR.servlet_stu3.setFhirContext(utils.fhir_stu3.ResourceProvider.ctx);
		  
		  FHIR.servlet_r4.init(new PlayHttpServletConfig());
		  FHIR.servlet_stu3.init(new PlayHttpServletConfig());
			
		  System.out.println("Messager");
		  Messager.init(system);
		  
		  System.out.println("Instances");
		  Instances.init();
		  
		  System.out.println("Subscription Manager");
		  SubscriptionManager.init(Instances.system(), ws);
		  
		  System.out.println("Minimal Setup");
		  MinimalSetup.dosetup();
		  
		  System.out.println("Market");
		  Market.correctOwners();
		  
		  		  
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
		UsageStatsRecorder.init(Instances.system());
						
		System.out.println("Service Handler");
		ServiceHandler.startup();
		
		System.out.println("Auto-Join");
		AutoJoiner.init(system);
		
		System.out.println("Auto-Run");
		AutoRun.init();
		
		/* (Not needed anymore; Done on all instances)
		try {
		   AccountPatches.fixFhirConsents();
		} catch (AppException e) { e.printStackTrace(); }
		*/		
		lifecycle.addStopHook(() -> {
			//AutoRun.shutdown();
		    
			System.out.println("Shutting down Cluster");
			CompletionStage<Done> result = Instances.shutdown();
			
			return result.thenAccept(t -> {
				System.out.println("Stopping MongoDB");
				DBLayer.close();
			 });				
				
		}); 
		
		System.out.println("Finished Startup");
		System.out.println("----------------------------------------------------------------");
	}	

}
