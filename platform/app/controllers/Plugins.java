package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.F;
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
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.PortalSessionToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * functions for managing MIDATA plugins
 *
 */
public class Plugins extends APIController {
	
	/**
	 * returns the requested plugin fields and checks if the user is allowed to use that plugin
	 * @param pluginId id of plugin to return
	 * @param userId id of user to check
	 * @param fields plugin fields to be returned
	 * @return requested Plugin object
	 * @throws InternalServerException
	 * @throws AuthException
	 */
    protected static Plugin getPluginAndCheckIfInstalled(ObjectId pluginId, ObjectId userId, Set<String> fields) throws AppException {
    	            
		Plugin app = Plugin.getById(pluginId, fields);
		if (app == null) throw new BadRequestException("error.unknownplugin", "Unknown Plugin");

		return app;				
    }
	
    /**
     * retrieve information about plugins
     * @return list of plugins
     * @throws JsonValidationException
     * @throws InternalServerException
     * @throws AuthException
     */
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result get() throws JsonValidationException, InternalServerException, AuthException {
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

	/**
	 * install a new plugin for the current user
	 * @param visualizationIdString id of plugin to add to user account
	 * @return status ok
	 * @throws AppException
	 */
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
		
		Plugin visualization = Plugin.getById(visualizationId, Sets.create("name", "defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator"));
		if (visualization == null) return badRequest("Unknown visualization");
		String context = json.has("context") ? JsonValidation.getString(json, "context") : visualization.defaultSpaceContext;
		
		
		User user = User.getById(userId, Sets.create("visualizations","apps", "role"));

		boolean testing = user.role.equals(UserRole.DEVELOPER) && visualization.creator.equals(user._id);
		
		if (!user.role.equals(visualization.targetUserRole) && !visualization.targetUserRole.equals(UserRole.ANY) && !testing) {
			return badRequest("Visualization is for a different role."+user.role);
		}
		
		if (visualization.type.equals("visualization") ) {
			user.visualizations.add(visualizationId);
			User.set(userId, "visualizations", user.visualizations);
		} else {
			user.apps.add(visualizationId);
			User.set(userId, "apps", user.apps);
		}
		
		
		if (!visualization.type.equals("mobile")) { 
		    if (spaceName == null || spaceName.equals("")) spaceName = visualization.name;
			
			
			Space space = null;
			space = Spaces.add(userId, spaceName, visualizationId, visualization.type, context);

			if (applyRules && space!=null) {
					
					if (json.has("query")) {
					  Map<String, Object> query = JsonExtraction.extractMap(json.get("query"));
					  RecordManager.instance.shareByQuery(userId, userId, space._id, query);
					} else {					
					  RecordManager.instance.shareByQuery(userId, userId, space._id, visualization.defaultQuery);
					  
					}
			}
				
		} 
						
		return ok();
	}

	/**
	 * uninstall a plugin for the current user
	 * @param visualizationIdString id of plugin to uninstall
	 * @return
	 */
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
		} catch (InternalServerException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * check if a plugin is installed
	 * @param visualizationIdString id of plugin to check
	 * @return boolean result
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result isInstalled(String visualizationIdString) throws InternalServerException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);		
		boolean isInstalled = Member.getByIdAndVisualization(userId, visualizationId, Sets.create()) != null
					        || Member.getByIdAndApp(userId, visualizationId, Sets.create()) != null;
		
		return ok(Json.toJson(isInstalled));
	}
	
	/**
	 * check if an import plugin is authorized to query its endpoint
	 * @param spaceIdString id of space which uses plugin
	 * @return boolean result
	 * @throws AppException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Promise<Result> isAuthorized(String spaceIdString) throws AppException {
		ObjectId userId = new ObjectId(request().username());				
							
		BSONObject oauthmeta = RecordManager.instance.getMeta(userId, new ObjectId(spaceIdString), "_oauth");
		if (oauthmeta == null) return F.Promise.pure((Result) ok(Json.toJson(false))); 
		 		
	    if (oauthmeta.containsField("refreshToken")) {
	      return requestAccessTokenOAuth2FromRefreshToken(spaceIdString, oauthmeta.toMap(), (Result) ok(Json.toJson(true)));
	    } else {		
		  return F.Promise.pure((Result) ok(Json.toJson(true)));
	    }
	}
	
    /**
     * retrieve URL for plugin
     * @param visualizationIdString id of plugin
     * @return URL
     * @throws InternalServerException
     */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getUrl(String visualizationIdString) throws InternalServerException {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", visualizationId).get();
		Set<String> fields = new ChainedSet<String>().add("filename").add("url").get();
		Plugin visualization = Plugin.get(properties, fields);
		
		String visualizationServer = Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename;
		String url = "https://" + visualizationServer  + "/" + visualization.url;
		return ok(url);
	}
	
	/**
     * retrieve URL to be used for an input form that may add data to the data set of a specific consent. Includes access token for this input form and user	
     * @param appIdString - ID of input form
     * @param consentIdString - ID of consent
     * @throws AppException
     * @return URL
     */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getUrlForConsent(String appIdString, String consentIdString) throws AppException {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		ObjectId consentId = new ObjectId(consentIdString);
		ObjectId userId = new ObjectId(request().username());				
		Plugin app = Plugins.getPluginAndCheckIfInstalled(appId, userId, Sets.create("filename", "type", "url", "creator"));
		
		// create encrypted authToken
		SpaceToken appToken = new SpaceToken(consentId, userId, null, appId);		

        boolean testing = PortalSessionToken.session().getRole().equals(UserRole.DEVELOPER) && app.creator.equals(userId) && app.developmentServer != null && app.developmentServer.length()> 0;
		
		String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + app.filename;
		if (testing) visualizationServer = app.developmentServer;
		String url =  visualizationServer  + "/" + app.url;
		url = url.replace(":authToken", appToken.encrypt(request()));
		
		return ok(url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getRequestTokenOAuth1(String appIdString) throws InternalServerException {
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
	public static Result requestAccessTokenOAuth1(String spaceIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "code");
						
		// get app details
		final ObjectId spaceId = new ObjectId(spaceIdString);
		final ObjectId userId = new ObjectId(request().username());
		
		Space space  = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null) throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth1")) throw new InternalServerException("error.internal", "Wrong type");
				
		Map<String, Object> properties = CMaps.map("_id", space.visualization);
		Set<String> fields = Sets.create("accessTokenUrl", "consumerKey", "consumerSecret");
		Plugin app = Plugin.get(properties, fields);
		
		// request access token
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = new RequestToken(session("token"), session("secret"));
		RequestToken accessToken = client.retrieveAccessToken(requestToken, json.get("code").asText());

		// save token and secret to database
				
		Map<String, Object> tokens = CMaps.map("appId",space.visualization)
				                          .map("oauthToken", accessToken.token)
			                              .map("oauthTokenSecret", accessToken.secret);
		RecordManager.instance.setMeta(userId, spaceId, "_oauth", tokens);
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Promise<Result> requestAccessTokenOAuth2(String spaceIdString) throws AppException {
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
		final ObjectId spaceId = new ObjectId(spaceIdString);
		final ObjectId userId = new ObjectId(request().username());
				
		Space space  = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null) throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth2")) throw new InternalServerException("error.internal", "Wrong type");
					
		final ObjectId appId = space.visualization;
		Map<String, Object> properties = CMaps.map("_id", space.visualization);
		Set<String> fields = Sets.create("accessTokenUrl", "consumerKey", "consumerSecret");
		Plugin app = Plugin.get(properties, fields);
		
		String authPage = Play.application().configuration().getString("portal.originUrl")+"/authorized.html";
		
        try {
		// request access token	
		Promise<WSResponse> promise = WS
		   .url(app.accessTokenUrl)
		   .setAuth(app.consumerKey, app.consumerSecret)
		   .setContentType("application/x-www-form-urlencoded; charset=utf-8")
		   .post("client_id="+app.consumerKey+"&grant_type=authorization_code&code="+json.get("code").asText()+"&redirect_uri="+URLEncoder.encode(authPage, "UTF-8"));
		return promise.map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) throws AppException {
				try {
				AccessLog.log(response.getBody());
				JsonNode jsonNode = response.asJson();
				if (jsonNode.has("access_token") && jsonNode.get("access_token").isTextual()) {
					String accessToken = jsonNode.get("access_token").asText();
					String refreshToken = null;
					if (jsonNode.has("refresh_token") && jsonNode.get("refresh_token").isTextual()) {
						refreshToken = jsonNode.get("refresh_token").asText();
					}
					
					Map<String, Object> tokens = CMaps.map("appId", appId)
							                          .map("accessToken", accessToken)
							                          .map("refreshToken", refreshToken);
					RecordManager.instance.setMeta(userId, spaceId, "_oauth", tokens);
					
					return ok();
				} else {
					return badRequest("Access token not found.");
				}
				} finally {
					RecordManager.instance.clear();
					AccessLog.newRequest();	
				}
			}
		});
        } catch (UnsupportedEncodingException e) { return null; }
	}
	
	public static Result oauthInfo(Plugin plugin) {
		ObjectNode result = Json.newObject();
		result.put("appId", plugin._id.toString());
		result.put("name", plugin.name);
		result.put("type", plugin.type);
		result.put("authorizationUrl", plugin.authorizationUrl);
		result.put("consumerKey", plugin.consumerKey);
		result.put("scopeParameters", plugin.scopeParameters);					
		return ok(result);
	}
	
	
	public static Promise<Result> requestAccessTokenOAuth2FromRefreshToken(String spaceIdStr, Map<String, Object> tokens1, final Result result) throws AppException {		
		final ObjectId appId = new ObjectId(tokens1.get("appId").toString());
		final ObjectId userId = new ObjectId(request().username());
		
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = Sets.create("name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "type");
			
		final Plugin app = Plugin.get(properties, fields);
	
		return requestAccessTokenOAuth2FromRefreshToken(userId, app, spaceIdStr, tokens1).map(new Function<Boolean, Result>()  {
			public Result apply(Boolean success) throws AppException {
				if (success) return result;
				return oauthInfo(app);
			}
		});
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Promise<Boolean> requestAccessTokenOAuth2FromRefreshToken(final ObjectId userId, final Plugin app, String spaceIdStr, Map<String, Object> tokens1) {

		final Map<String, Object> tokens = tokens1;
		final ObjectId spaceId = new ObjectId(spaceIdStr);
		// get app details
					
		
		String refreshToken = tokens.get("refreshToken").toString();
       
		// request access token	
		Promise<WSResponse> promise = WS
		   .url(app.accessTokenUrl)
		   .setAuth(app.consumerKey, app.consumerSecret)
		   .setContentType("application/x-www-form-urlencoded; charset=utf-8")
		   .post("client_id="+app.consumerKey+"&grant_type=refresh_token&refresh_token="+refreshToken);
		return promise.map(new Function<WSResponse, Boolean>()  {
			public Boolean apply(WSResponse response) throws AppException {
				AccessLog.log(response.getBody());
				JsonNode jsonNode = response.asJson();
				if (jsonNode.has("access_token") && jsonNode.get("access_token").isTextual()) {
					String accessToken = jsonNode.get("access_token").asText();
					if (jsonNode.has("refresh_token") && jsonNode.get("refresh_token").isTextual()) {
						tokens.put("refreshToken", jsonNode.get("refresh_token").asText());
					}
					try {
						tokens.put("accessToken", accessToken);
						RecordManager.instance.setMeta(userId, spaceId, "_oauth", tokens);						
					} catch (InternalServerException e) {
						return false;
					} finally {
						RecordManager.instance.clear();
						AccessLog.newRequest();	
					}
					return true;
				} else {
					return false;
				}
			}
		});
     /*
		} catch (final InternalServerException e) {
			return Promise.promise(new Function0<Result>() {
				public Result apply() {
					return internalServerError(e.getMessage());
				}
			});
		}*/
	}
}
