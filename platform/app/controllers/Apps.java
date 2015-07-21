package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.App;
import models.Member;
import models.ModelException;
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
import utils.auth.AppToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.details.app;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;


public class Apps extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result details(String appIdString) {
		return ok(app.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}
		return getApps(json);
	}

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
			
			/*if (app.recommendedStream != null) {
				Record stream = RecordSharing.instance.createStream(user._id, user._id, user.myaps, app.recommendedStream, true);
				Map<String, String> apptokens = new HashMap<String, String>();
				apptokens.put("stream", stream._id.toString());
			    user.tokens.put(appIdString, apptokens);
			    User.set(userId, "tokens", user.tokens);
			}*/
			
		} else { 
			User user = User.getById(userId, Sets.create("apps"));    
			user.apps.add(new ObjectId(appIdString));
			User.set(userId, "apps", user.apps);
		}
		
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)
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
	}

	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getUrl(String appIdString) {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("filename").add("type").add("url").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create encrypted authToken
		AppToken appToken = new AppToken(appId, userId);
		String authToken = appToken.encrypt();

		// put together url to load in iframe
		String appServer = Play.application().configuration().getString("apps.server");
		String url = app.url.replace(":authToken", authToken);
		return ok("https://" + appServer + "/" + app.filename + "/" + url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getPreviewUrl(String appIdString) {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("filename").add("type").add("previewUrl").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create encrypted authToken
		AppToken appToken = new AppToken(appId, userId);
		String authToken = appToken.encrypt();

		// put together url to load in iframe
		String appServer = Play.application().configuration().getString("apps.server");
		String url = app.previewUrl.replace(":authToken", authToken);
		return ok("https://" + appServer + "/" + app.filename + "/" + url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getUrlForMember(String appIdString, String userIdString) throws ModelException {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId userId = new ObjectId(userIdString);
		ObjectId ownerId = new ObjectId(request().username());				
		App app = App.getById(appId, Sets.create("filename", "type", "url"));
		
		// create encrypted authToken
		AppToken appToken = new AppToken(appId, ownerId, userId);
		String authToken = appToken.encrypt();

		// put together url to load in iframe
		String appServer = Play.application().configuration().getString("apps.server");
		String url = app.url.replace(":authToken", authToken);
		return ok("https://" + appServer + "/" + app.filename + "/" + url);
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
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
		App app;
		try {
			app = App.get(properties, fields);
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

	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getRequestTokenOAuth1(String appIdString) {
		// get app details
		ObjectId appId = new ObjectId(appIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("consumerKey").add("consumerSecret").add("requestTokenUrl").add("accessTokenUrl")
				.add("authorizationUrl").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

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
	public static Result requestAccessTokenOAuth1(String appIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "code");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get app details
		final ObjectId appId = new ObjectId(appIdString);
		final ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("accessTokenUrl").add("consumerKey").add("consumerSecret").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// request access token
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = new RequestToken(session("token"), session("secret"));
		RequestToken accessToken = client.retrieveAccessToken(requestToken, json.get("code").asText());

		// save token and secret to database
		try {
			Map<String, String> tokens = new ChainedMap<String, String>().put("oauthToken", accessToken.token)
					.put("oauthTokenSecret", accessToken.secret).get();
			Users.setTokens(userId, appId, tokens);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}
}
