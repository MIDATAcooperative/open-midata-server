package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Plugin;
import models.Member;
import models.Record;
import models.User;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.oauth.OAuth;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.oauth.OAuth.ServiceInfo;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.auth.AppToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.exceptions.ModelException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;


public class Apps extends Controller {

	
	
/*
	static Result getApps(JsonNode json) {
		// get apps
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<App> apps;
		try {
			apps = new ArrayList<App>(App.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(apps);
		return ok(Json.toJson(apps));
	}
*/
	/*
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	
	public static Result install(String appIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());		
		App app = App.getById(new ObjectId(appIdString), Sets.create("targetUserRole"));
		if (app==null) return badRequest("Unknown App");
		
		if (app.targetUserRole.equals(UserRole.MEMBER)) { 
			Member user = Member.getById(userId, Sets.create("apps", "myaps", "tokens"));
			user.apps.add(new ObjectId(appIdString));
			User.set(userId, "apps", user.apps);
						
			
		} else { 
			User user = User.getById(userId, Sets.create("apps"));    
			user.apps.add(new ObjectId(appIdString));
			User.set(userId, "apps", user.apps);
		}
		
		return ok();
	}*/

	/*
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result uninstall(String appIdString) {
		ObjectId userId = new ObjectId(request().username());		
		Set<String> fields = new ChainedSet<String>().add("apps").get();
		try {
			User user = User.getById(userId, fields);
			user.apps.remove(new ObjectId(appIdString));
			User.set(userId, "apps", user.apps);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result isInstalled(String appIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);		
		boolean isInstalled = (User.getByIdAndApp(userId, appId, Sets.create()) != null);
		
		return ok(Json.toJson(isInstalled));
	}*/

	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getUrl(String appIdString) {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId userId = new ObjectId(request().username());
		String role = session().get("role");
		
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = Sets.create("filename", "type", "url", "developmentServer", "creator");
		Plugin app;
		try {
			app = Plugin.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create encrypted authToken
		AppToken appToken = new AppToken(appId, userId);
		String authToken = appToken.encrypt();

		// put together url to load in iframe
		String appServer = "https://" + Play.application().configuration().getString("apps.server") + "/" + app.filename;
		if (role.equals(UserRole.DEVELOPER.toString()) && userId.equals(app.creator) && app.developmentServer != null && app.developmentServer.length()> 0) appServer = app.developmentServer; 
		String url = app.url.replace(":authToken", authToken);
		return ok(appServer  + "/" + url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getPreviewUrl(String appIdString) {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId userId = new ObjectId(request().username());
		String role = session().get("role");
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = Sets.create("filename", "type", "previewUrl", "developmentServer", "creator");
		Plugin app;
		try {
			app = Plugin.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create encrypted authToken
		AppToken appToken = new AppToken(appId, userId);
		String authToken = appToken.encrypt();

		// put together url to load in iframe
		String appServer = "https://" + Play.application().configuration().getString("apps.server") + "/" + app.filename;
		if (role.equals(UserRole.DEVELOPER.toString()) && userId.equals(app.creator) && app.developmentServer != null && app.developmentServer.length()> 0) appServer = app.developmentServer;
		String url = app.previewUrl.replace(":authToken", authToken);
		return ok(appServer  + "/" + url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getUrlForConsent(String appIdString, String consentIdString) throws ModelException {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId consentId = new ObjectId(consentIdString);
		ObjectId ownerId = new ObjectId(request().username());				
		Plugin app = Plugin.getById(appId, Sets.create("filename", "type", "url"));
		
		// create encrypted authToken
		AppToken appToken = new AppToken(appId, ownerId, consentId);
		String authToken = appToken.encrypt();

		// put together url to load in iframe
		String appServer = Play.application().configuration().getString("apps.server") + "/" + app.filename;
		String url = app.url.replace(":authToken", authToken);
		return ok("https://" + appServer + "/" + url);
	}

	

	
}
