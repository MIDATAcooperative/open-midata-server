package controllers;

import models.App;
import models.ModelException;
import models.Visualization;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.market;
import views.html.dialogs.registerapp;
import views.html.dialogs.registervisualization;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Market extends Controller {

	public static Result index() {
		return ok(market.render());
	}

	public static Result registerAppForm() {
		return ok(registerapp.render());
	}

	public static Result registerVisualizationForm() {
		return ok(registervisualization.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result registerApp(String type) throws JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();
		
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
			if (App.exists(new ChainedMap<String, Object>().put("filename", filename).get())) {
				return badRequest("An app with the same filename already exists.");
			} else if (App.exists(new ChainedMap<String, Object>().put("creator", userId).put("name", name).get())) {
				return badRequest("An app with the same name already exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create new app
		App app = new App();
		app._id = new ObjectId();
		app.creator = userId;
		app.filename = filename;
		app.name = name;
		app.description = json.get("description").asText();
		app.spotlighted = true;
		app.type = type;
		app.url = JsonValidation.getString(json, "url");

		// fill in specific fields
		if (type.equals("oauth1") || type.equals("oauth2")) {
			app.authorizationUrl = json.get("authorizationUrl").asText();
			app.accessTokenUrl = json.get("accessTokenUrl").asText();
			app.consumerKey = json.get("consumerKey").asText();
			app.consumerSecret = json.get("consumerSecret").asText();
			if (type.equals("oauth1")) {
				app.requestTokenUrl = json.get("requestTokenUrl").asText();
			} else if (type.equals("oauth2")) {
				app.scopeParameters = json.get("scopeParameters").asText();
			}
		}
		if (type.equals("mobile")) {
			app.secret = JsonValidation.getString(json, "secret");
		}

		// add app to the platform
		try {
			App.add(app);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result registerVisualization() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "filename", "name", "description", "url");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String filename = json.get("filename").asText();
		String name = json.get("name").asText();
		try {
			if (Visualization.exists(new ChainedMap<String, String>().put("filename", filename).get())) {
				return badRequest("A visualization with the same filename already exists.");
			} else if (Visualization.exists(new ChainedMap<String, Object>().put("creator", userId).put("name", name).get())) {
				return badRequest("A visualization with the same name already exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create new visualization
		Visualization visualization = new Visualization();
		visualization._id = new ObjectId();
		visualization.creator = userId;
		visualization.filename = filename;
		visualization.name = name;
		visualization.description = json.get("description").asText();
		visualization.spotlighted = true;
		visualization.url = json.get("url").asText();
		try {
			Visualization.add(visualization);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

}
