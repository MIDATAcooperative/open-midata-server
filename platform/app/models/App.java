package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

public class App extends Plugin implements Comparable<App> {

	private static final String collection = "apps";

	public String detailsUrl; // url for detailed view of a record
	public String type; // type can be one of: create, oauth1, oauth2

	// create app
	public String createUrl; // url for creating a new record
	// oauth 1.0/2.0 app
	public String authorizationUrl;
	public String accessTokenUrl;
	public String consumerKey;
	// oauth 2.0 app
	public String consumerSecret;
	public String scopeParameters;

	@Override
	public int compareTo(App other) {
		return this.name.compareTo(other.name);
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(collection, properties);
	}

	public static App get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(App.class, collection, properties, fields);
	}

	public static Set<App> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(App.class, collection, properties, fields);
	}

	public static void set(ObjectId appId, String field, Object value) throws ModelException {
		Model.set(collection, appId, field, value);
	}

	public static void add(App app) throws ModelException {
		Model.insert(collection, app);

		// add to search index
		try {
			Search.add(Type.APP, app._id, app.name, app.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId appId) throws ModelException {
		// remove from search index
		Search.delete(Type.APP, appId);

		// TODO also remove from installed list of users
		Model.delete(collection, new ChainedMap<String, ObjectId>().put("_id", appId).get());
	}

}
