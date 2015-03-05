package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

public class App extends Plugin implements Comparable<App> {

	private static final String collection = "apps";

	public String type; // type can be one of: create, oauth1, oauth2, mobile
	public String url; // url to call the app
	
	// oauth 1.0/2.0 app
	public String authorizationUrl;
	public String accessTokenUrl;
	public String consumerKey;
	public String consumerSecret;
	// oauth 1.0 app
	public String requestTokenUrl;
	// oauth 2.0 app
	public String scopeParameters;
	// mobile app
	public String secret;

	@Override
	public int compareTo(App other) {
		if (this.name != null && other.name != null) {
			return this.name.compareTo(other.name);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(App.class, collection, properties);
	}

	public static App get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(App.class, collection, properties, fields);
	}
	
	public static App getByFilenameAndSecret(String name, String secret, Set<String> fields) throws ModelException {
		return Model.get(App.class, collection, CMaps.map("filename", name).map("secret", secret), fields);
	}

	public static Set<App> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(App.class, collection, properties, fields);
	}

	public static void set(ObjectId appId, String field, Object value) throws ModelException {
		Model.set(App.class, collection, appId, field, value);
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
		Model.delete(App.class, collection, new ChainedMap<String, ObjectId>().put("_id", appId).get());
	}

}
