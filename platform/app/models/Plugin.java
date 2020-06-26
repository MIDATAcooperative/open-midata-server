package models;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.IconUse;
import models.enums.PluginStatus;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.WritePermissionType;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.sync.Instances;

/**
 * data model for a MIDATA plugin. This is the definition of a plugin.
 *
 */
@JsonFilter("Plugin")
public class Plugin extends Model implements Comparable<Plugin> {

	private static final String collection = "plugins";
	private static Map<MidataId, Plugin> cache = new ConcurrentHashMap<MidataId, Plugin>();
	public @NotMaterialized static final Set<PluginStatus> NOT_DELETED = EnumSet.of(PluginStatus.ACTIVE, PluginStatus.BETA, PluginStatus.DEPRECATED, PluginStatus.DEVELOPMENT);
	
	/**
	 * constant containing all fields visible to a developer
	 */
	public @NotMaterialized final static Set<String> ALL_DEVELOPER = 
			 Sets.create("_id", "version", "creator", "creatorLogin", "developerTeam", "developerTeamLogins", "filename", "name", "description", "tags", 
	                     "targetUserRole", "spotlighted", "url", "addDataUrl", "previewUrl", "defaultSpaceName",
	                     "defaultSpaceContext", "defaultQuery", "type", "recommendedPlugins",
	                     "authorizationUrl", "accessTokenUrl", "consumerKey", "consumerSecret","tokenExchangeParams",
	                     "requestTokenUrl", "scopeParameters", "secret", "redirectUri", "developmentServer", "status", "i18n",
	                     "predefinedMessages", "resharesData", "allowsUserSearch", "pluginVersion", "termsOfUse", "requirements", "orgName", "publisher", "unlockCode", "writes", "icons", "apiUrl", "noUpdateHistory", "defaultSubscriptions", "debugHandle", "sendReports", "licenceDef", "pseudonymize", "consentObserving");
	
	/**
	 * constant containing all fields visible to anyone
	 */
	public @NotMaterialized final static Set<String> ALL_PUBLIC = 
			 Sets.create("_id", "version", "creator", "creatorLogin", "developerTeam", "filename", "name", "description", "tags", 
	                     "targetUserRole", "spotlighted", "url", "addDataUrl", "previewUrl", "defaultSpaceName",
	                     "defaultSpaceContext", "defaultQuery", "type", "recommendedPlugins",
	                     "authorizationUrl", "consumerKey", "scopeParameters", "status", "i18n", "lang", "predefinedMessages", "resharesData", "pluginVersion",
	                     "termsOfUse", "requirements", "orgName", "publisher", "unlockCode", "writes", "icons", "apiUrl", "noUpdateHistory", "defaultSubscriptions", "licenceDef", "pseudonymize", "consentObserving");
	
	/**
	 * timestamp of last change. Used to prevent lost updates.
	 */
	public long version;
	
	/**
	 * Also a timestamp. Changing this timestamp needs all users to confirm to the app consent again. Does not need to be changed if only messages or translation is edited 
	 */
	public long pluginVersion;
	
	/**
	 * status of plugin
	 */
	public PluginStatus status;
	
	/**
	 * id of creator of this plugin
	 */
	public MidataId creator;
	
	/**
	 * the login of the creator of the plugin
	 */
	public String creatorLogin;
	
	/**
	 * other developers of the plugin
	 */
	public List<MidataId> developerTeam;
	
	/**
	 * login names of developers of the plugin
	 */
	@NotMaterialized
	public List<String> developerTeamLogins;
	
	/**
	 * Name of organization developing plugin
	 */
	public String orgName;
	
	/**
	 * Name of organization publishing plugin
	 */
	public String publisher;
	
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
	 * The default title for the tile where this plugin is displayed.
	 * null for mobile apps
	 */
	public String defaultSpaceName;
	
	/**
	 * Internationalization support
	 */
	public Map<String, Plugin_i18n> i18n;
	
	/**
	 * Predefined messages
	 */
	public Map<String, MessageDefinition> predefinedMessages;
	
	/**
	 * App shares data it has access to to 3rd party
	 */
	public boolean resharesData;
	
	/**
	 * Users of app are allowed to search each other
	 */
	public boolean allowsUserSearch;	
	
	/**
	 * for project analyzers
	 */
	public boolean pseudonymize;
	
	/**
	 * this external service observes consent changes and may be added to apps and projects for monitoring
	 */
	public boolean consentObserving;
	
			
	/**
	 * set of tags that determine for which categories this plugin should be displayed in the market
	 */
	public Set<String> tags;
	
	/**
	 * required role for users using this plugin. May be ANY. Developers may always test their own plugin.
	 */
	public UserRole targetUserRole;
	
	/**
	 * Requirements about user account that must be fulfilled for use of app
	 */
	public Set<UserFeature> requirements;
	
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
	 * The URL from which an add data dialog for this plugin is served
	 */
	public String addDataUrl;
	
	/**
	 * The URL from which a preview tile of this plugin is served.
	 * null for all plugins that do not have a preview tile implemented
	 */
	public String previewUrl;
	
	/**
	 * internal name of terms of use to be shown to user
	 */
	public String termsOfUse;
		
			
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
	 * Default policy for writes with this plugin
	 */
	public WritePermissionType writes;
	
	/**
	 * the type of the plugin
	 * 
	 * type can be one of: visualization, service, oauth1, oauth2, mobile, external, analyzer
	 */
	public String type;
	
	/**
	 * code word that must be entered by user to register using this app
	 */
	public String unlockCode;
		
	/**
	 * list of ids of other plugins that a user may like if he likes this plugin
	 */
	public List<MidataId> recommendedPlugins;
	
	/**
	 * for OAUTH 1.0 and 2.0 : URL of target API
	 */
	public String apiUrl;
	
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
	 * for OAUTH 2.0 : how to obtain token
	 */
	public String tokenExchangeParams;
	
	/**
	 * for mobile apps : secret needed for "init" request
	 */
	public String secret;
	
	/**
	 * for SMART ON FHIR apps : oauth redirect uri
	 */
	public String redirectUri;
	
	/**
	 * for development: localhost-URL-prefix to be used instead of plugin server domain for testing on local machine 
	 */
	public String developmentServer;
	
	/**
	 * Types of icons used by this plugin
	 */
	public Set<IconUse> icons;
	
	/**
	 * Updates of resources performed with this plugin should not be recorded
	 */
	public boolean noUpdateHistory;
	
	/**
	 * Subscriptions that are required to use this plugin
	 */
	public List<SubscriptionData> defaultSubscriptions;
	
	/**
	 * Handle to connect subscription debugger to
	 */
	public String debugHandle;
	
	/**
	 * Send error reports to developer
	 */
	public boolean sendReports;
	
	/**
	 * What type of licence is required to use this application
	 */
	public LicenceDefinition licenceDef;

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
	
	public static Plugin getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(Plugin.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static Plugin getById(MidataId id) throws InternalServerException {
		if (id == null) return null;
		
		Plugin result = cache.get(id);
		if (result != null) return result;
		
		result = getById(id, ALL_PUBLIC);
		if (result == null) return null;
		cache.put(id, result);
		return result;		
	}

	public static Set<Plugin> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Plugin.class, collection, properties, fields);
	}

	public static void set(MidataId pluginId, String field, Object value) throws InternalServerException {
		Model.set(Plugin.class, collection, pluginId, field, value);
	}
	
	public static Plugin getByFilename(String name, Set<String> fields) throws InternalServerException {
		return Model.get(Plugin.class, collection, CMaps.map("filename", name).map("status", Plugin.NOT_DELETED), fields);
	}
	
	public void update() throws InternalServerException, LostUpdateException {		
		try {
		   DBLayer.secureUpdate(this, collection, "version", "creator", "developerTeam", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer", "status", "i18n", "predefinedMessages", "resharesData", "allowsUserSearch", "pluginVersion", "termsOfUse", "requirements", "orgName", "publisher", "unlockCode", "writes", "apiUrl", "noUpdateHistory", "sendReports", "pseudonymize", "consentObserving" );
		   Instances.cacheClear("plugin",  _id);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateLicenceDef() throws InternalServerException, LostUpdateException {		
		try {
		   DBLayer.secureUpdate(this, collection, "version", "licenceDef");
		   Instances.cacheClear("plugin",  _id);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateIcons(Set<IconUse> icons) throws InternalServerException {
		this.icons = icons;
		Model.set(Plugin.class, collection, _id, "icons", icons);
		Instances.cacheClear("plugin",  _id);
	}
	
	public void updateDefaultSubscriptions(List<SubscriptionData> subscriptions) throws InternalServerException {
        this.defaultSubscriptions = subscriptions;
        Model.set(Plugin.class, collection, _id, "defaultSubscriptions", subscriptions);
        Instances.cacheClear("plugin",  _id);
	}


	public static void add(Plugin plugin) throws InternalServerException {
		Model.insert(collection, plugin);	
	}

	public static void delete(MidataId pluginId) throws InternalServerException {				
		Model.delete(Plugin.class, collection, new ChainedMap<String, MidataId>().put("_id", pluginId).get());
		Instances.cacheClear("plugin",  pluginId);
	}
	
	public static void cacheRemove(MidataId pluginId) {
		AccessLog.log("cache remove");
		cache.remove(pluginId);
	}
	
	public void setLanguage(String lang) {
		Plugin_i18n in = i18n.get(lang);
		if (in != null) {
			this.name = in.name;
			this.description = in.description;
			this.defaultSpaceName = in.defaultSpaceName;
		}
	}
	
	public static long count() throws AppException {
		return Model.count(Plugin.class, collection, CMaps.map("status", Plugin.NOT_DELETED));
	}
	
	public boolean isDeveloper(MidataId user) {
		if (user==null) return false;
		if (this.creator != null && this.creator.equals(user)) return true;
		if (this.developerTeam != null && this.developerTeam.contains(user)) return true;
		return false;
	}
}
