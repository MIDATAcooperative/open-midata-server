package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Member;
import models.MidataId;
import models.Plugin;
import models.Space;
import models.User;
import models.enums.PluginStatus;
import models.enums.UserRole;
import play.Play;
import play.libs.F;
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
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

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
    protected static Plugin getPluginAndCheckIfInstalled(MidataId pluginId, MidataId userId, Set<String> fields) throws AppException {
    	            
		Plugin app = Plugin.getById(pluginId, fields);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");

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
		if (!properties.containsKey("status")) {
		    properties.put("status", EnumSet.of(PluginStatus.DEVELOPMENT, PluginStatus.BETA, PluginStatus.ACTIVE, PluginStatus.DEPRECATED));
		}
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "recommendedPlugins");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("Plugins.get", getRole(), properties, fields);
		List<Plugin> visualizations = new ArrayList<Plugin>(Plugin.getAll(properties, fields));
		
		Collections.sort(visualizations);
		return ok(JsonOutput.toJson(visualizations, "Plugin", fields));
	}
	
	@BodyParser.Of(BodyParser.Json.class)	
	@APICall
	public static Result getInfo() throws JsonValidationException, InternalServerException, AuthException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "name");
		
		String name = JsonValidation.getString(json, "name");
		
		Set<String> fields = Sets.create("name", "description", "i18n", "defaultQuery", "resharesData", "allowsUserSearch", "linkedStudy", "mustParticipateInStudy", "termsOfUse", "requirements", "orgName");
		Plugin plugin = Plugin.get(CMaps.map("filename", name).map("type", "mobile"), fields);
				
		return ok(JsonOutput.toJson(plugin, "Plugin", fields));
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
		MidataId userId = new MidataId(request().username());
		MidataId visualizationId = null;
		Plugin visualization = null;
		if (MidataId.isValid(visualizationIdString)) {
			visualizationId = new MidataId(visualizationIdString);
			visualization = Plugin.getById(visualizationId, Sets.create("name", "defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator", "status"));
		}
		else {
            visualization = Plugin.getByFilename(visualizationIdString, Sets.create("name", "defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator", "status"));            
		}

		String spaceName = JsonValidation.getString(json, "spaceName");
		boolean applyRules = JsonValidation.getBoolean(json, "applyRules");		
		//boolean createSpace = JsonValidation.getBoolean(json, "createSpace");
		
		if (visualization == null) throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");
		if (visualization.status == PluginStatus.DELETED) throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");
		visualizationId = visualization._id;
		
		String context = json.has("context") ? JsonValidation.getString(json, "context") : visualization.defaultSpaceContext;
				
		User user = User.getById(userId, Sets.create("visualizations","apps", "role", "developer"));

		boolean testing = visualization.creator.equals(user._id) || (user.developer != null && visualization.creator.equals(user.developer));
		
		if (!user.role.equals(visualization.targetUserRole) && !visualization.targetUserRole.equals(UserRole.ANY) && !testing) {
			throw new BadRequestException("error.invalid.plugin", "Visualization is for a different role. Your role:"+user.role);
		}
		
		if (visualization.type.equals("visualization") ) {
			if (user.visualizations == null) user.visualizations = new HashSet<MidataId>();
			if (!user.visualizations.contains(visualizationId)) {
				user.visualizations.add(visualizationId);
				User.set(userId, "visualizations", user.visualizations);
			}
		} else {
			if (user.apps == null) user.apps = new HashSet<MidataId>();
			if (!user.apps.contains(visualizationId)) {
				user.apps.add(visualizationId);
				User.set(userId, "apps", user.apps);
			}
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
			
			if (json.has("config")) {
				Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));		
				RecordManager.instance.setMeta(userId, space._id, "_config", config);
			}
			return 	ok(JsonOutput.toJson(space, "Space", Space.ALL));
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
	public static Result uninstall(String visualizationIdString) throws InternalServerException {
		MidataId userId = new MidataId(request().username());		
		Set<String> fields = Sets.create("visualizations", "apps");
		
		User user = User.getById(userId, fields);
		user.visualizations.remove(new MidataId(visualizationIdString));
		user.apps.remove(new MidataId(visualizationIdString));
		User.set(userId, "visualizations", user.visualizations);
		User.set(userId, "apps", user.apps);
		
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
		MidataId userId = new MidataId(request().username());
		MidataId visualizationId = new MidataId(visualizationIdString);		
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
		MidataId userId = new MidataId(request().username());				
							
		BSONObject oauthmeta = RecordManager.instance.getMeta(userId, new MidataId(spaceIdString), "_oauth");
		if (oauthmeta == null) return F.Promise.pure((Result) ok(Json.toJson(false))); 
		 		
	    if (oauthmeta.containsField("refreshToken")) {
	      return requestAccessTokenOAuth2FromRefreshToken(spaceIdString, oauthmeta.toMap(), Json.toJson(true));
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
		MidataId visualizationId = new MidataId(visualizationIdString);
		Map<String, MidataId> properties = new ChainedMap<String, MidataId>().put("_id", visualizationId).get();
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
		MidataId appId = new MidataId(appIdString);
		MidataId consentId = new MidataId(consentIdString);
		MidataId userId = new MidataId(request().username());				
		Plugin app = Plugins.getPluginAndCheckIfInstalled(appId, userId, Sets.create("filename", "type", "url", "creator"));
		
		// create encrypted authToken
		SpaceToken appToken = new SpaceToken(PortalSessionToken.session().handle, consentId, userId, null, appId);		

        boolean testing = (app.creator.equals(PortalSessionToken.session().getDeveloper()) || app.creator.equals(userId)) && app.developmentServer != null && app.developmentServer.length()> 0;
		
		String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + app.filename;
		if (testing) visualizationServer = app.developmentServer;
		String url =  visualizationServer  + "/" + app.url;
		url = url.replace(":authToken", appToken.encrypt(request()));
		
		return ok(url);
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall	
	public static Result getRequestTokenOAuth1(String spaceIdString) throws AppException {
				
		// get app details			
		final MidataId spaceId = new MidataId(spaceIdString);
		final MidataId userId = new MidataId(request().username());
		String origin = Play.application().configuration().getString("portal.originUrl");
		if (origin.equals("https://demo.midata.coop:9002")) origin = "https://demo.midata.coop"; 
		String authPage = origin +"/authorized.html";
				
		Space space  = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null) throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth1")) throw new InternalServerException("error.internal", "Wrong type");

		
		MidataId appId = space.visualization;
		Map<String, Object> properties = new ChainedMap<String, Object>().put("_id", appId.toObjectId()).get();
		Set<String> fields = new ChainedSet<String>().add("consumerKey").add("consumerSecret").add("requestTokenUrl").add("accessTokenUrl")
				.add("authorizationUrl").get();
		Plugin app = Plugin.get(properties, fields);
		
		// get request token (pass callback url as argument)
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = client.retrieveRequestToken(authPage);		
		
		session("token", requestToken.token);
		session("secret", requestToken.secret);
		
		Map<String, Object> tokens = CMaps.map("token",requestToken.token).map("secret", requestToken.secret);        
        RecordManager.instance.setMeta(userId, space._id, "_oauth1", tokens);
        
		return ok(client.redirectUrl(requestToken.token));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result requestAccessTokenOAuth1(String spaceIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "code");
				
		Map<String, Object> additionalParams = Collections.EMPTY_MAP;
		if (json.has("params")) additionalParams = JsonExtraction.extractMap(json.get("params"));
		
		// get app details
		final MidataId spaceId = new MidataId(spaceIdString);
		final MidataId userId = new MidataId(request().username());
		
		Space space  = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null) throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth1")) throw new InternalServerException("error.internal", "Wrong type");
				
		Map<String, Object> properties = CMaps.map("_id", space.visualization);
		Set<String> fields = Sets.create("accessTokenUrl", "consumerKey", "consumerSecret");
		Plugin app = Plugin.get(properties, fields);
		Map<String, Object> reqTokens = RecordManager.instance.getMeta(userId, space._id, "_oauth1").toMap(); 
		
		// request access token
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = new RequestToken(reqTokens.get("token").toString(), reqTokens.get("secret").toString());
		RequestToken accessToken = client.retrieveAccessToken(requestToken, json.get("code").asText());
         
		// save token and secret to database
				
		Map<String, Object> tokens = CMaps.map("appId",space.visualization.toString())
				                          .map("oauthToken", accessToken.token)
			                              .map("oauthTokenSecret", accessToken.secret);
		for (String p : additionalParams.keySet()) {
			if (p.equals("oauth_token") || p.equals("oauth_verifier") || p.equals("appId") || p.equals("oauthToken") || p.equals("oauthTokenSecret")) {
				
			} else tokens.put(p, additionalParams.get(p));					
		}
		additionalParams.remove("oauth_token");
		additionalParams.remove("oauth_verifier");
		if (!additionalParams.isEmpty()) {
			RecordManager.instance.setMeta(userId, spaceId, "_oauthParams", additionalParams);
		}
		
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
		final MidataId spaceId = new MidataId(spaceIdString);
		final MidataId userId = new MidataId(request().username());
		final String sessionHandle = PortalSessionToken.session().handle;
				
		Space space  = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null) throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth2")) throw new InternalServerException("error.internal", "Wrong type");
					
		final MidataId appId = space.visualization;
		Map<String, Object> properties = CMaps.map("_id", space.visualization);
		Set<String> fields = Sets.create("accessTokenUrl", "consumerKey", "consumerSecret");
		Plugin app = Plugin.get(properties, fields);
		
		String origin = Play.application().configuration().getString("portal.originUrl");
		if (origin.equals("https://demo.midata.coop:9002")) origin = "https://demo.midata.coop"; 
		String authPage = origin +"/authorized.html";
		
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
				KeyManager.instance.continueSession(sessionHandle);
				AccessLog.log(response.getBody());
				JsonNode jsonNode = response.asJson();
				if (jsonNode.has("access_token") && jsonNode.get("access_token").isTextual()) {
					String accessToken = jsonNode.get("access_token").asText();
					String refreshToken = null;
					if (jsonNode.has("refresh_token") && jsonNode.get("refresh_token").isTextual()) {
						refreshToken = jsonNode.get("refresh_token").asText();
					}
					
					Map<String, Object> tokens = CMaps.map("appId", appId.toString())
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
		return oauthInfo(plugin, null);
	}
	
	public static Result oauthInfo(Plugin plugin, ObjectNode addTo) {
		ObjectNode result = addTo != null ? addTo : Json.newObject();
		result.put("appId", plugin._id.toString());
		result.put("name", plugin.name);
		result.put("type", plugin.type);
		result.put("authorizationUrl", plugin.authorizationUrl);
		result.put("consumerKey", plugin.consumerKey);
		result.put("scopeParameters", plugin.scopeParameters);					
		return ok(result);
	}
	
	// Needs active session
	public static Promise<Result> requestAccessTokenOAuth2FromRefreshToken(String spaceIdStr, Map<String, Object> tokens1, final JsonNode result) throws AppException {		
		final MidataId appId = new MidataId(tokens1.get("appId").toString());
		final MidataId userId = new MidataId(request().username());
		
		Map<String, Object> properties = new ChainedMap<String, Object>().put("_id", appId.toObjectId()).get();
		Set<String> fields = Sets.create("name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "type");
			
		final Plugin app = Plugin.get(properties, fields);
	
		return requestAccessTokenOAuth2FromRefreshToken(PortalSessionToken.session().handle, userId, app, spaceIdStr, tokens1).map(new Function<Boolean, Result>()  {
			public Result apply(Boolean success) throws AppException {
				if (success) return ok(result);
				ObjectNode resultBase = result instanceof ObjectNode ? (ObjectNode) result : null;
				return oauthInfo(app, resultBase);
			}
		});
	}
	

	public static Promise<Boolean> requestAccessTokenOAuth2FromRefreshToken(final String sessionHandle, final MidataId userId, final Plugin app, String spaceIdStr, Map<String, Object> tokens1) {

		final Map<String, Object> tokens = tokens1;
		final MidataId spaceId = new MidataId(spaceIdStr);
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
				KeyManager.instance.continueSession(sessionHandle);
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
