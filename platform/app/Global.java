import javax.servlet.ServletException;

import controllers.AutoRun;
import controllers.FHIR;
import play.Application;
import play.GlobalSettings;
import play.libs.Json;
import setup.MinimalSetup;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;
import utils.json.CustomObjectMapper;
import utils.search.Search;
import utils.servlet.PlayHttpServletConfig;
import utils.servlet.PlayHttpServletContext;

/**
 * Actions that need to be done on application start and stop.
 *
 */
public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		// Connect to production database
		try {
		  DBLayer.connect();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		  
  		// Connect to search cluster
		Search.connect();

		// Set custom object mapper for Json
		Json.setObjectMapper(new CustomObjectMapper());
		
		// Init FHIR
		FHIR.servlet = new FHIRServlet();
		
		try {
		  FHIR.servlet.setFhirContext(ResourceProvider.ctx);
		  FHIR.servlet.init(new PlayHttpServletConfig());
		} catch (ServletException e) {
		   throw new NullPointerException();
		}
		
		try {
		MinimalSetup.dosetup();
		} catch (AppException e) {
			throw new NullPointerException();
		}
		
		AutoRun.init();
	}

	@Override
	public void onStop(Application app) {
		AutoRun.shutdown();
		
		// Close connection to database
		DBLayer.close();

		// Close connection to search cluster
		Search.close();
	}

}
