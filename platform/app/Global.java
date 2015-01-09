import play.Application;
import play.GlobalSettings;
import play.libs.Json;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.json.CustomObjectMapper;
import utils.search.Search;

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
	}

	@Override
	public void onStop(Application app) {
		// Close connection to database
		DBLayer.close();

		// Close connection to search cluster
		Search.close();
	}

}
