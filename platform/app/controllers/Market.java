package controllers;

import models.Plugin;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.DeveloperSecured;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

/**
 * functions for controlling the "market" of plugins
 *
 */
@Security.Authenticated(DeveloperSecured.class)
public class Market extends Controller {
		
	/**
	 * update a plugins meta data
	 * @param pluginIdStr ID of plugin to update
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result updatePlugin(String pluginIdStr) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId pluginId = new ObjectId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Sets.create("creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","url","developmentServer"));
		if (app == null) return badRequest("Unknown plugin");
		
		if (!app.creator.equals(userId)) return badRequest("Not your plugin!");
		
		app.version = JsonValidation.getLong(json, "version");		
		app.filename = JsonValidation.getString(json, "filename");
		app.name = JsonValidation.getString(json, "name");
		app.description = JsonValidation.getStringOrNull(json, "description");
		app.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		app.type = JsonValidation.getString(json, "type");
		app.url = JsonValidation.getStringOrNull(json, "url");
		app.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
		app.developmentServer = JsonValidation.getStringOrNull(json, "developmentServer");
		app.tags = JsonExtraction.extractStringSet(json.get("tags"));
		app.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		app.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
		app.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
		app.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));

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
		}
		 
		try {
		   app.update();
		} catch (LostUpdateException e) {
			return badRequest("Concurrent updates. Reload page and try again.");
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
	public static Result registerPlugin() throws JsonValidationException, InternalServerException {
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
			return badRequest("Unknown app type.");
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String filename = JsonValidation.getString(json ,"filename");
		String name = JsonValidation.getString(json, "name");
		try {
			if (Plugin.exists(new ChainedMap<String, String>().put("filename", filename).get())) {
				return badRequest("A visualization with the same filename already exists.");
			} else if (Plugin.exists(new ChainedMap<String, Object>().put("creator", userId).put("name", name).get())) {
				return badRequest("A visualization with the same name already exists.");
			}
		} catch (InternalServerException e) {
			return internalServerError(e.getMessage());
		}

		// create new visualization
		Plugin plugin = new Plugin();
		plugin._id = new ObjectId();
		
		plugin.creator = userId;
		plugin.version = JsonValidation.getLong(json, "version");		
		plugin.filename = JsonValidation.getStringOrNull(json, "filename");
		plugin.name = JsonValidation.getStringOrNull(json, "name");
		plugin.description = JsonValidation.getStringOrNull(json, "description");
		plugin.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		plugin.type = JsonValidation.getString(json, "type");
		plugin.url = JsonValidation.getStringOrNull(json, "url");
		plugin.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
		plugin.developmentServer = JsonValidation.getStringOrNull(json, "developmentServer");
		plugin.tags = JsonExtraction.extractStringSet(json.get("tags"));
		plugin.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		plugin.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
		plugin.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
		plugin.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));

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
		}
		
			
		Plugin.add(plugin);
		
		return ok(JsonOutput.toJson(plugin, "Plugin", Plugin.ALL_DEVELOPER));
	}

}
