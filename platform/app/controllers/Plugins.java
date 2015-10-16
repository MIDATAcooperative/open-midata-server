package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Member;
import models.Plugin;
import models.User;
import models.Space;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
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
import utils.auth.Rights;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.ModelException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

public class Plugins extends APIController {
	

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result get() throws JsonValidationException, ModelException, AuthException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "fields");
		
		// get visualizations
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("Plugins.get", getRole(), properties, fields);
		List<Plugin> visualizations = new ArrayList<Plugin>(Plugin.getAll(properties, fields));
		
		Collections.sort(visualizations);
		return ok(JsonOutput.toJson(visualizations, "Plugin", fields));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result install(String visualizationIdString) throws AppException {
		JsonNode json = request().body().asJson();
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);

		String spaceName = JsonValidation.getString(json, "spaceName");
		boolean applyRules = JsonValidation.getBoolean(json, "applyRules");
		//boolean createSpace = JsonValidation.getBoolean(json, "createSpace");
		
		Plugin visualization = Plugin.getById(visualizationId, Sets.create("defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator"));
		if (visualization == null) return badRequest("Unknown visualization");
		
		User user = User.getById(userId, Sets.create("visualizations","apps", "role"));

		boolean testing = user.role.equals(UserRole.DEVELOPER) && visualization.creator.equals(user._id);
		
		if (!user.role.equals(visualization.targetUserRole) && !visualization.targetUserRole.equals(UserRole.ANY) && !testing) {
			return badRequest("Visualization is for a different role."+user.role);
		}
		
		if (visualization.type.equals("visualization")) {
			user.visualizations.add(visualizationId);
			User.set(userId, "visualizations", user.visualizations);
		} else {
			user.apps.add(visualizationId);
			User.set(userId, "apps", user.apps);
		}
		
		String context = json.has("context") ? JsonValidation.getString(json, "context") : visualization.defaultSpaceContext;
		
		if (visualization.type.equals("visualization")) { 
					
			if (spaceName!=null && !spaceName.equals("")) {
				Space space = null;
				space = Spaces.add(userId, spaceName, visualizationId, null, context);

				if (applyRules && space!=null) {
					
					if (json.has("query")) {
					  Map<String, Object> query = JsonExtraction.extractMap(json.get("query"));
					  RecordSharing.instance.shareByQuery(userId, userId, space._id, query);
					} else {					
					  RecordSharing.instance.shareByQuery(userId, userId, space._id, visualization.defaultQuery);
					  
					}
				}
			}
	
		} 
						
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result uninstall(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());		
		Set<String> fields = Sets.create("visualizations", "apps");
		try {
			User user = User.getById(userId, fields);
			user.visualizations.remove(new ObjectId(visualizationIdString));
			user.apps.remove(new ObjectId(visualizationIdString));
			User.set(userId, "visualizations", user.visualizations);
			User.set(userId, "apps", user.apps);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result isInstalled(String visualizationIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);		
		boolean isInstalled = Member.getByIdAndVisualization(userId, visualizationId, Sets.create()) != null
					        || Member.getByIdAndApp(userId, visualizationId, Sets.create()) != null;
		
		return ok(Json.toJson(isInstalled));
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result isAuthorized(String visualizationIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());				
			
		User me = User.getById(userId, Sets.create("tokens"));
		if (me == null) return badRequest("User not found");
		boolean isInstalled = me.tokens.containsKey(visualizationIdString);			
	
		return ok(Json.toJson(isInstalled));
	}

	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getUrl(String visualizationIdString) throws ModelException {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", visualizationId).get();
		Set<String> fields = new ChainedSet<String>().add("filename").add("url").get();
		Plugin visualization = Plugin.get(properties, fields);
		
		String visualizationServer = Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename;
		String url = "https://" + visualizationServer  + "/" + visualization.url;
		return ok(url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getRequestTokenOAuth1(String appIdString) throws ModelException {
		// get app details
		ObjectId appId = new ObjectId(appIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("consumerKey").add("consumerSecret").add("requestTokenUrl").add("accessTokenUrl")
				.add("authorizationUrl").get();
		Plugin app = Plugin.get(properties, fields);
		
		// get request token (pass callback url as argument)
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = client.retrieveRequestToken(routes.Records.onAuthorized(app._id.toString())
				.absoluteURL(request(), true));
		session("token", requestToken.token);
		session("secret", requestToken.secret);
		return ok(client.redirectUrl(requestToken.token));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result requestAccessTokenOAuth1(String appIdString) throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "code");
		
		// get app details
		final ObjectId appId = new ObjectId(appIdString);
		final ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("accessTokenUrl").add("consumerKey").add("consumerSecret").get();
		Plugin app = Plugin.get(properties, fields);
		
		// request access token
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = new RequestToken(session("token"), session("secret"));
		RequestToken accessToken = client.retrieveAccessToken(requestToken, json.get("code").asText());

		// save token and secret to database
		
		Map<String, String> tokens = new ChainedMap<String, String>().put("oauthToken", accessToken.token)
			.put("oauthTokenSecret", accessToken.secret).get();
		Users.setTokens(userId, appId, tokens);
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Promise<Result> requestAccessTokenOAuth2(String appIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "code");
		} catch (final JsonValidationException e) {
			return Promise.promise(new Function0<Result>() {
				public Result apply() {
					return badRequest(e.getMessage());
				}
			});
		}

		// get app details
		final ObjectId appId = new ObjectId(appIdString);
		final ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("accessTokenUrl").add("consumerKey").add("consumerSecret").get();
		Plugin app;
		try {
			app = Plugin.get(properties, fields);
		} catch (final ModelException e) {
			return Promise.promise(new Function0<Result>() {
				public Result apply() {
					return internalServerError(e.getMessage());
				}
			});
		} 

		// request access token
		Promise<WSResponse> promise = WS.url(app.accessTokenUrl).setQueryParameter("client_id", app.consumerKey)
				.setQueryParameter("client_secret", app.consumerSecret).setQueryParameter("grant_type", "authorization_code")
				.setQueryParameter("code", json.get("code").asText()).get();
		return promise.map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				JsonNode jsonNode = response.asJson();
				if (jsonNode.has("access_token") && jsonNode.get("access_token").isTextual()) {
					String accessToken = jsonNode.get("access_token").asText();
					String refreshToken = null;
					if (jsonNode.has("refresh_token") && jsonNode.get("refresh_token").isTextual()) {
						refreshToken = jsonNode.get("refresh_token").asText();
					}
					try {
						Map<String, String> tokens = new ChainedMap<String, String>().put("accessToken", accessToken)
								.put("refreshToken", refreshToken).get();
						Users.setTokens(userId, appId, tokens);
					} catch (ModelException e) {
						return badRequest(e.getMessage());
					}
					return ok();
				} else {
					return badRequest("Access token not found.");
				}
			}
		});
	}
}
