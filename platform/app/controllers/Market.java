package controllers;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;

import actions.APICall;
import models.Developer;
import models.MessageDefinition;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Plugin_i18n;
import models.Space;
import models.enums.MessageReason;
import models.enums.PluginStatus;
import models.enums.UserRole;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.Query;
import utils.access.RecordManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.DeveloperSecured;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for controlling the "market" of plugins
 *
 */
public class Market extends APIController {
			
	/**
	 * update a plugins meta data
	 * @param pluginIdStr ID of plugin to update
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)	
	public static Result updatePlugin(String pluginIdStr) throws JsonValidationException, AppException {
		if (!getRole().equals(UserRole.ADMIN) && !getRole().equals(UserRole.DEVELOPER)) return unauthorized();
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId pluginId = new MidataId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
		
		if (!app.creator.equals(userId) && !getRole().equals(UserRole.ADMIN)) throw new BadRequestException("error.not_authorized.not_plugin_owner", "Not your plugin!");
		
		app.version = JsonValidation.getLong(json, "version");		
		app.filename = JsonValidation.getString(json, "filename");
		app.name = JsonValidation.getString(json, "name");
		app.description = JsonValidation.getStringOrNull(json, "description");		
		app.type = JsonValidation.getString(json, "type");
		app.url = JsonValidation.getStringOrNull(json, "url");
		app.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
		app.addDataUrl = JsonValidation.getStringOrNull(json, "addDataUrl");
		app.developmentServer = "https://localhost:9004/"+app.filename;
		//app.developmentServer = JsonValidation.getStringOrNull(json, "developmentServer");
		app.tags = JsonExtraction.extractStringSet(json.get("tags"));
		app.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		app.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
		app.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
		app.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));
		app.resharesData = JsonValidation.getBoolean(json, "resharesData");
		app.allowsUserSearch = JsonValidation.getBoolean(json, "allowsUserSearch");
		app.i18n = new HashMap<String, Plugin_i18n>();
		
		
		Map<String, MessageDefinition> predefinedMessages = parseMessages(json);
		if (predefinedMessages != null) app.predefinedMessages = predefinedMessages;
		
		try {
		  Query.validate(app.defaultQuery, app.type.equals("mobile"));
		} catch (BadRequestException e) {
			throw new JsonValidationException(e.getLocaleKey(), "defaultQuery", "invalid", e.getMessage());
		}
		if (json.has("i18n")) {
			Map<String,Object> i18n = JsonExtraction.extractMap(json.get("i18n"));
			for (String lang : i18n.keySet()) {
				Map<String, Object> entry = (Map<String, Object>) i18n.get(lang);
				Plugin_i18n plugin_i18n = new Plugin_i18n();
				plugin_i18n.name = (String) entry.get("name");
				plugin_i18n.description = (String) entry.get("description");
				plugin_i18n.defaultSpaceName = (String) entry.get("defaultSpaceName");
				app.i18n.put(lang, plugin_i18n);
			}
		}
		

		// fill in specific fields
		if (app.type.equals("oauth1") || app.type.equals("oauth2")) {
			app.authorizationUrl = JsonValidation.getStringOrNull(json, "authorizationUrl");
			app.accessTokenUrl = JsonValidation.getStringOrNull(json, "accessTokenUrl");
			app.consumerKey = JsonValidation.getStringOrNull(json, "consumerKey");
			app.consumerSecret = JsonValidation.getStringOrNull(json, "consumerSecret");
			if (app.type.equals("oauth1")) {
				app.requestTokenUrl = JsonValidation.getStringOrNull(json, "requestTokenUrl");
			} else if (app.type.equals("oauth2")) {
				app.scopeParameters = JsonValidation.getStringOrNull(json, "scopeParameters");
			}
		}
		if (app.type.equals("mobile")) {
			app.secret = JsonValidation.getStringOrNull(json, "secret");
			app.redirectUri = JsonValidation.getStringOrNull(json, "redirectUri");
		}
		 
		try {
		   app.update();
		} catch (LostUpdateException e) {
			throw new BadRequestException("error.concurrent.update", "Concurrent updates. Reload page and try again.");
		}
		
		return ok();
	}
	
	/**
	 * update a plugins status
	 * @param pluginIdStr ID of plugin to update
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result updatePluginStatus(String pluginIdStr) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request		
		MidataId pluginId = new MidataId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Sets.create("creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","url","developmentServer", "status"));
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
						
		app.version = JsonValidation.getLong(json, "version");		
		app.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		app.status = JsonValidation.getEnum(json, "status", PluginStatus.class);
				 
		try {
		   app.update();
		} catch (LostUpdateException e) {
			throw new BadRequestException("error.concurrent.update", "Concurrent updates. Reload page and try again.");
		}
		
		return ok();
	}
	
	

	/**
	 * create a new plugin
	 * @return plugin
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(DeveloperSecured.class)
	public static Result registerPlugin() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "type");
		String type = JsonValidation.getString(json, "type");
		
		if (type.equals("create")) {
			JsonValidation.validate(json, "filename", "name", "description", "url");
		} else if (type.equals("oauth1")) {
			JsonValidation.validate(json, "filename", "name", "description", "url", "authorizationUrl", "accessTokenUrl",
					"consumerKey", "consumerSecret", "requestTokenUrl");
		} else if (type.equals("oauth2")) {
			JsonValidation.validate(json, "filename", "name", "description", "url", "authorizationUrl", "accessTokenUrl",
					"consumerKey", "consumerSecret", "scopeParameters");
		} else if (type.equals("mobile")) {
			JsonValidation.validate(json, "filename", "name", "description", "secret");
		} else if (type.equals("visualization")) {
			JsonValidation.validate(json, "filename", "name", "description", "url");
		} else {
			throw new BadRequestException("error.internal", "Unknown app type.");
		}

		// validate request
		MidataId userId = new MidataId(request().username());
		
		Developer dev = Developer.getById(userId, Sets.create("email"));
		
		String filename = JsonValidation.getString(json ,"filename");
		String name = JsonValidation.getString(json, "name");
		try {
			if (Plugin.exists(CMaps.map("filename", filename).map("status", EnumSet.of(PluginStatus.ACTIVE, PluginStatus.BETA, PluginStatus.DEPRECATED, PluginStatus.DEVELOPMENT)))) {
				throw new BadRequestException("error.exists.plugin", "A plugin with the same filename already exists.");
			} else if (Plugin.exists(CMaps.map("creator", userId.toDb()).map("name", name).map("status", EnumSet.of(PluginStatus.ACTIVE, PluginStatus.BETA, PluginStatus.DEPRECATED, PluginStatus.DEVELOPMENT)))) {
				throw new BadRequestException("error.exists.plugin", "A plugin with the same name already exists.");
			}
		} catch (InternalServerException e) {
			return internalServerError(e.getMessage());
		}

		// create new visualization
		Plugin plugin = new Plugin();
		plugin._id = new MidataId();
		
				
		plugin.creator = userId;
		plugin.creatorLogin = dev.email;
		
		plugin.version = JsonValidation.getLong(json, "version");		
		plugin.filename = JsonValidation.getStringOrNull(json, "filename");
		plugin.name = JsonValidation.getStringOrNull(json, "name");
		plugin.description = JsonValidation.getStringOrNull(json, "description");
		plugin.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		plugin.type = JsonValidation.getString(json, "type");
		plugin.url = JsonValidation.getStringOrNull(json, "url");
		plugin.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
		plugin.addDataUrl = JsonValidation.getStringOrNull(json, "addDataUrl");
		plugin.developmentServer = "https://localhost:9004/"+plugin.filename; //JsonValidation.getStringOrNull(json, "developmentServer");
		plugin.tags = JsonExtraction.extractStringSet(json.get("tags"));
		plugin.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		plugin.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
		plugin.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
		plugin.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));
		plugin.resharesData = JsonValidation.getBoolean(json, "resharesData");
		plugin.allowsUserSearch = JsonValidation.getBoolean(json, "allowsUserSearch");
		plugin.predefinedMessages = parseMessages(json);
		
		try {
		    Query.validate(plugin.defaultQuery, plugin.type.equals("mobile"));
		} catch (BadRequestException e) {
			throw new JsonValidationException(e.getLocaleKey(), "defaultQuery", "invalid", e.getMessage());
		}
		plugin.status = PluginStatus.DEVELOPMENT;
		plugin.i18n = new HashMap<String, Plugin_i18n>();
		Map<String,Object> i18n = JsonExtraction.extractMap(json.get("i18n"));
		for (String lang : i18n.keySet()) {
			Map<String, Object> entry = (Map<String, Object>) i18n.get(lang);
			Plugin_i18n plugin_i18n = new Plugin_i18n();
			plugin_i18n.name = (String) entry.get("name");
			plugin_i18n.description = (String) entry.get("description");
			plugin_i18n.defaultSpaceName = (String) entry.get("defaultSpaceName");
			plugin.i18n.put(lang, plugin_i18n);
		}

		// fill in specific fields
		if (plugin.type.equals("oauth1") || plugin.type.equals("oauth2")) {
			plugin.authorizationUrl = JsonValidation.getStringOrNull(json, "authorizationUrl");
			plugin.accessTokenUrl = JsonValidation.getStringOrNull(json, "accessTokenUrl");
			plugin.consumerKey = JsonValidation.getStringOrNull(json, "consumerKey");
			plugin.consumerSecret = JsonValidation.getStringOrNull(json, "consumerSecret");
			if (plugin.type.equals("oauth1")) {
				plugin.requestTokenUrl = JsonValidation.getStringOrNull(json, "requestTokenUrl");
			} else if (plugin.type.equals("oauth2")) {
				plugin.scopeParameters = JsonValidation.getStringOrNull(json, "scopeParameters");
			}
		}
		if (plugin.type.equals("mobile")) {
			plugin.secret = JsonValidation.getStringOrNull(json, "secret");
			plugin.redirectUri = JsonValidation.getStringOrNull(json, "redirectUri");
		}
		
			
		Plugin.add(plugin);
		
		return ok(JsonOutput.toJson(plugin, "Plugin", Plugin.ALL_DEVELOPER));
	}
	
	private static Map<String, MessageDefinition> parseMessages(JsonNode json) throws JsonValidationException {
		if (!json.has("predefinedMessages")) return null;
		Iterator<Entry<String,JsonNode>> messages = json.get("predefinedMessages").fields();
		if (!messages.hasNext()) return null;
		
		Map<String, MessageDefinition> result = new HashMap<String, MessageDefinition>();		
		while (messages.hasNext()) {		
			Entry<String,JsonNode> entry = messages.next();
			JsonNode def = entry.getValue();
			
			MessageDefinition messageDef  = new MessageDefinition();
			messageDef.reason = JsonValidation.getEnum(def, "reason", MessageReason.class); 
			messageDef.code = JsonValidation.getStringOrNull(def, "code");
			messageDef.text = JsonExtraction.extractStringMap(def.get("text"));
			messageDef.title = JsonExtraction.extractStringMap(def.get("title"));
			
			result.put(messageDef.reason.toString() + (messageDef.code != null ? "_"+messageDef.code : ""), messageDef);
		}
		return result;
	}
	
	/**
	 * delete a plugin
	 * @param pluginIdStr ID of plugin to delete
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result deletePlugin(String pluginIdStr) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request		
		MidataId pluginId = new MidataId(pluginIdStr);
		
		deletePlugin(pluginId);
		
		return ok();
	}
	
	private static void deletePlugin(MidataId pluginId) throws JsonValidationException, AppException {
		
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");

		if (app.type.equals("mobile")) {
			Set<MobileAppInstance> installations =  MobileAppInstance.getByApplication(pluginId, Sets.create("_id", "owner"));
			for (MobileAppInstance inst : installations) {				
				KeyManager.instance.deleteKey(inst._id);
				MobileAppInstance.delete(inst.owner, inst._id);
			}
		} else {
			Set<Space> installations = Space.getAll(CMaps.map("visualization", pluginId), Sets.create("_id","owner"));
			for (Space inst : installations) {
				Space.delete(inst.owner, inst._id);
			}
		}		 
		app.status = PluginStatus.DELETED;
		app.spotlighted = false;
		app.filename = null;
		
	    try {
	        app.update();
	    } catch (LostUpdateException e) {
	        throw new BadRequestException("error.concurrent.update", "Concurrent updates. Reload page and try again.");
	    }
	        
	}
	
	 /**
     * delete a plugins
     * @param pluginIdStr ID of plugin to update
     * @return status ok
     * @throws JsonValidationException
     * @throws InternalServerException
     */
    @APICall
    @Security.Authenticated(DeveloperSecured.class)
    public static Result deletePluginDeveloper(String pluginIdStr) throws JsonValidationException, AppException {
            
        // validate request     
        MidataId pluginId = new MidataId(pluginIdStr);
        MidataId userId = new MidataId(request().username());
        
        Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
        if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
        if (!app.creator.equals(userId)) throw new BadRequestException("error.auth", "You are not owner of this plugin.");
        if (app.status != PluginStatus.DEVELOPMENT && app.status != PluginStatus.BETA) throw new BadRequestException("error.auth", "Plugin may not be deleted. Ask an admin.");
              
        deletePlugin(pluginId);        
        
        return ok();
    }
	
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result correctOwners() throws AppException {
	   Set<Plugin> plugins = Plugin.getAll(CMaps.map(), Sets.create("_id", "creator", "creatorLogin"));
	   
	   for (Plugin plg : plugins) {
		   Developer u = Developer.getById(plg.creator, Sets.create("email"));
		   if (u == null && plg.creatorLogin != null) {
			   u = Developer.getByEmail(plg.creatorLogin, Sets.create("_id", "email"));
			   if (u != null) {
				   Plugin.set(plg._id, "creator", u._id);
			   }
		   }
	   }
	   
	   return ok();
	}
}
