package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.UserRole;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.exceptions.ModelException;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

public class Plugin extends Model implements Comparable<Plugin> {

	private static final String collection = "plugins";
	
	public long version;
	public ObjectId creator;
	public String filename;
	public String name;
	public String description;
	public Set<String> tags;
	public UserRole targetUserRole;
	public boolean spotlighted;
	
	public String url;
	public String previewUrl;
	public String defaultSpaceName;
	public String defaultSpaceContext;
	public Map<String, Object> defaultQuery;
	
	public String type; // type can be one of: visualization, create, oauth1, oauth2, mobile
		
	public List<ObjectId> recommendedPlugins;	
	
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
	public String developmentServer;

	@Override
	public int compareTo(Plugin other) {
		if (this.name != null && other.name != null) {
			return this.name.compareTo(other.name);
		} else {
			return super.compareTo(other);
		}
	}
	
	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Plugin.class, collection, properties);
	}

	public static Plugin get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Plugin.class, collection, properties, fields);
	}
	
	public static Plugin getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(Plugin.class, collection, CMaps.map("_id", id), fields);
	}

	public static Set<Plugin> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Plugin.class, collection, properties, fields);
	}

	public static void set(ObjectId pluginId, String field, Object value) throws ModelException {
		Model.set(Plugin.class, collection, pluginId, field, value);
	}
	
	public static Plugin getByFilenameAndSecret(String name, String secret, Set<String> fields) throws ModelException {
		return Model.get(Plugin.class, collection, CMaps.map("filename", name).map("secret", secret), fields);
	}
	
	public void update() throws ModelException, LostUpdateException {
		try {
			   DBLayer.secureUpdate(this, collection, "version", "creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","url","developmentServer" );
		} catch (DatabaseException e) {
			throw new ModelException("error.internal.db", e);
		}
	}


	public static void add(Plugin plugin) throws ModelException {
		Model.insert(collection, plugin);

		// add to search index
		try {
			Search.add(Type.VISUALIZATION, plugin._id, plugin.name, plugin.description);
		} catch (SearchException e) {
			throw new ModelException("error.internal.db", e);
		}
	}

	public static void delete(ObjectId pluginId) throws ModelException {
		// remove from search index
		Search.delete(Type.VISUALIZATION, pluginId);

		// TODO only hide or remove from all users (including deleting their spaces associated with it)?
		Model.delete(Plugin.class, collection, new ChainedMap<String, ObjectId>().put("_id", pluginId).get());
	}
}
