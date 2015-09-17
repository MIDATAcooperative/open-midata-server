package controllers;

import models.ModelException;
import models.Plugin;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

import controllers.developer.DeveloperSecured;

@Security.Authenticated(DeveloperSecured.class)
public class Market extends Controller {
		
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result updatePlugin(String pluginIdStr) throws JsonValidationException, ModelException {
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
		app.description = JsonValidation.getString(json, "description");
		app.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		app.type = JsonValidation.getString(json, "type");
		app.url = JsonValidation.getString(json, "url");
		app.previewUrl = JsonValidation.getString(json, "previewUrl");
		app.developmentServer = JsonValidation.getString(json, "developmentServer");
		app.tags = JsonExtraction.extractStringSet(json.get("tags"));
		app.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		app.defaultSpaceName = JsonValidation.getString(json, "defaultSpaceName");
		app.defaultSpaceContext = JsonValidation.getString(json, "defaultSpaceContext");
		app.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));

		// fill in specific fields
		if (app.type.equals("oauth1") || app.type.equals("oauth2")) {
			app.authorizationUrl = json.get("authorizationUrl").asText();
			app.accessTokenUrl = json.get("accessTokenUrl").asText();
			app.consumerKey = json.get("consumerKey").asText();
			app.consumerSecret = json.get("consumerSecret").asText();
			if (app.type.equals("oauth1")) {
				app.requestTokenUrl = json.get("requestTokenUrl").asText();
			} else if (app.type.equals("oauth2")) {
				app.scopeParameters = json.get("scopeParameters").asText();
			}
		}
		if (app.type.equals("mobile")) {
			app.secret = JsonValidation.getString(json, "secret");
		}
		 
		try {
		   app.update();
		} catch (LostUpdateException e) {
			return badRequest("Concurrent updates. Reload page and try again.");
		}
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result registerPlugin() throws JsonValidationException, ModelException {
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
		} else {
			return badRequest("Unknown app type.");
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String filename = json.get("filename").asText();
		String name = json.get("name").asText();
		try {
			if (Plugin.exists(new ChainedMap<String, String>().put("filename", filename).get())) {
				return badRequest("A visualization with the same filename already exists.");
			} else if (Plugin.exists(new ChainedMap<String, Object>().put("creator", userId).put("name", name).get())) {
				return badRequest("A visualization with the same name already exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create new visualization
		Plugin plugin = new Plugin();
		plugin._id = new ObjectId();
		
		plugin.version = JsonValidation.getLong(json, "version");		
		plugin.filename = JsonValidation.getString(json, "filename");
		plugin.name = JsonValidation.getString(json, "name");
		plugin.description = JsonValidation.getString(json, "description");
		plugin.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		plugin.type = JsonValidation.getString(json, "type");
		plugin.url = JsonValidation.getString(json, "url");
		plugin.previewUrl = JsonValidation.getString(json, "previewUrl");
		plugin.developmentServer = JsonValidation.getString(json, "developmentServer");
		plugin.tags = JsonExtraction.extractStringSet(json.get("tags"));
		plugin.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		plugin.defaultSpaceName = JsonValidation.getString(json, "defaultSpaceName");
		plugin.defaultSpaceContext = JsonValidation.getString(json, "defaultSpaceContext");
		plugin.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));

		// fill in specific fields
		if (plugin.type.equals("oauth1") || plugin.type.equals("oauth2")) {
			plugin.authorizationUrl = json.get("authorizationUrl").asText();
			plugin.accessTokenUrl = json.get("accessTokenUrl").asText();
			plugin.consumerKey = json.get("consumerKey").asText();
			plugin.consumerSecret = json.get("consumerSecret").asText();
			if (plugin.type.equals("oauth1")) {
				plugin.requestTokenUrl = json.get("requestTokenUrl").asText();
			} else if (plugin.type.equals("oauth2")) {
				plugin.scopeParameters = json.get("scopeParameters").asText();
			}
		}
		if (plugin.type.equals("mobile")) {
			plugin.secret = JsonValidation.getString(json, "secret");
		}
		
			
		Plugin.add(plugin);
		
		return ok(Json.toJson(plugin));
	}

}
