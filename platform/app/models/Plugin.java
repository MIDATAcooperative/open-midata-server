package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.PluginStatus;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

/**
 * data model for a MIDATA plugin. This is the definition of a plugin.
 *
 */
@JsonFilter("Plugin")
public class Plugin extends Model implements Comparable<Plugin> {

	private static final String collection = "plugins";
	
	/**
	 * constant containing all fields visible to a developer
	 */
	public @NotMaterialized final static Set<String> ALL_DEVELOPER = 
			 Sets.create("_id", "version", "creator", "filename", "name", "description", "tags", 
	                     "targetUserRole", "spotlighted", "url", "previewUrl", "defaultSpaceName",
	                     "defaultSpaceContext", "defaultQuery", "type", "recommendedPlugins",
	                     "authorizationUrl", "accessTokenUrl", "consumerKey", "consumerSecret",
	                     "requestTokenUrl", "scopeParameters", "secret", "developmentServer", "status");
	
	/**
	 * constant containing all fields visible to anyone
	 */
	public @NotMaterialized final static Set<String> ALL_PUBLIC = 
			 Sets.create("_id", "version", "creator", "filename", "name", "description", "tags", 
	                     "targetUserRole", "spotlighted", "url", "previewUrl", "defaultSpaceName",
	                     "defaultSpaceContext", "defaultQuery", "type", "recommendedPlugins",
	                     "authorizationUrl", "consumerKey", "scopeParameters", "status");
	
	/**
	 * timestamp of last change. Used to prevent lost updates.
	 */
	public long version;
	
	/**
	 * status of plugin
	 */
	public PluginStatus status;
	/**
	 * id of creator of this plugin
	 */
	public ObjectId creator;
	
	/**
	 * internal name for this plugin
	 */
	public String filename;
	
	/**
	 * public name of this plugin
	 */
	public String name;
	
	/**
	 * description text of this plugin
	 */
	public String description;
	
	/**
	 * set of tags that determine for which categories this plugin should be displayed in the market
	 */
	public Set<String> tags;
	
	/**
	 * required role for users using this plugin. May be ANY. Developers may always test their own plugin.
	 */
	public UserRole targetUserRole;
	
	/**
	 * Is this plugin displayed in the market
	 */
	public boolean spotlighted;
	
	/**
	 * The URL from which this plugin is served. 
	 * null for mobile apps
	 */
	public String url;
	
	/**
	 * The URL from which a preview tile of this plugin is served.
	 * null for all plugins that do not have a preview tile implemented
	 */
	public String previewUrl;
	
	/**
	 * The default title for the tile where this plugin is displayed.
	 * null for mobile apps
	 */
	public String defaultSpaceName;
	
	/**
	 * The name of the default dashboard where this plugin should be added.
	 * null for mobile apps
	 */
	public String defaultSpaceContext;
	
	/**
	 * The default query to be executed to find records
	 */
	public Map<String, Object> defaultQuery;
	
	/**
	 * the type of the plugin
	 * 
	 * type can be one of: visualization, create, oauth1, oauth2, mobile
	 */
	public String type;
		
	/**
	 * list of ids of other plugins that a user may like if he likes this plugin
	 */
	public List<ObjectId> recommendedPlugins;	
	
	/**
	 * for OAUTH 1.0 and 2.0 : authorization URL
	 */
	public String authorizationUrl;
	
	/**
	 * for OAUTH 1.0 and 2.0 : access token URL
	 */
	public String accessTokenUrl;
	
	/**
	 * for OAUTH 1.0 and 2.0 : consumer key
	 */
	public String consumerKey;
	
	/**
	 * for OAUTH 1.0 and 2.0 : consumer secret
	 */
	public String consumerSecret;
	
	/**
	 * for OAUTH 1.0 only : request token URL
	 */
	public String requestTokenUrl;
	
	/**
	 * for OAUTH 2.0 only : scope parameters
	 */
	public String scopeParameters;
	
	/**
	 * for mobile apps : secret needed for "init" request
	 */
	public String secret;
	
	/**
	 * for development: localhost-URL-prefix to be used instead of plugin server domain for testing on local machine 
	 */
	public String developmentServer;

	@Override
	public int compareTo(Plugin other) {
		if (this.name != null && other.name != null) {
			return this.name.compareTo(other.name);
		} else {
			return super.compareTo(other);
		}
	}
	
	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(Plugin.class, collection, properties);
	}

	public static Plugin get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(Plugin.class, collection, properties, fields);
	}
	
	public static Plugin getById(ObjectId id, Set<String> fields) throws InternalServerException {
		return Model.get(Plugin.class, collection, CMaps.map("_id", id), fields);
	}

	public static Set<Plugin> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Plugin.class, collection, properties, fields);
	}

	public static void set(ObjectId pluginId, String field, Object value) throws InternalServerException {
		Model.set(Plugin.class, collection, pluginId, field, value);
	}
	
	public static Plugin getByFilename(String name, Set<String> fields) throws InternalServerException {
		return Model.get(Plugin.class, collection, CMaps.map("filename", name), fields);
	}
	
	public void update() throws InternalServerException, LostUpdateException {
		try {
			   DBLayer.secureUpdate(this, collection, "version", "creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","url","developmentServer", "status" );
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal.db", e);
		}
	}


	public static void add(Plugin plugin) throws InternalServerException {
		Model.insert(collection, plugin);

		// add to search index
		/*try {
			Search.add(Type.VISUALIZATION, plugin._id, plugin.name, plugin.description);
		} catch (SearchException e) {
			throw new InternalServerException("error.internal.db", e);
		}*/
	}

	public static void delete(ObjectId pluginId) throws InternalServerException {
		// remove from search index
		//Search.delete(Type.VISUALIZATION, pluginId);

		// TODO only hide or remove from all users (including deleting their spaces associated with it)?
		Model.delete(Plugin.class, collection, new ChainedMap<String, ObjectId>().put("_id", pluginId).get());
	}
}
