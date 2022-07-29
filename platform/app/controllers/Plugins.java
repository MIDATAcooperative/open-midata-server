/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;

import actions.APICall;
import models.Member;
import models.MidataId;
import models.Plugin;
import models.Space;
import models.StudyAppLink;
import models.StudyParticipation;
import models.User;
import models.enums.LoginTemplate;
import models.enums.PluginStatus;
import models.enums.StudyAppLinkType;
import models.enums.UserFeature;
import models.enums.UserRole;
import play.libs.Json;
import play.libs.oauth.OAuth;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.oauth.OAuth.ServiceInfo;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.access.Feature_QueryRedirect;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.KeyManager;
import utils.auth.LicenceChecker;
import utils.auth.PortalSessionToken;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.SubscriptionManager;
import utils.stats.Stats;

/**
 * functions for managing MIDATA plugins
 *
 */
public class Plugins extends APIController {

	
	static WSClient ws;		
	static Config config;
	
	public static void init(WSClient ws1, Config config1) {
		ws = ws1;
		config = config1;
	}

	/**
	 * returns the requested plugin fields and checks if the user is allowed to
	 * use that plugin
	 * 
	 * @param pluginId
	 *            id of plugin to return
	 * @param userId
	 *            id of user to check
	 * @param fields
	 *            plugin fields to be returned
	 * @return requested Plugin object
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	protected static Plugin getPluginAndCheckIfInstalled(MidataId pluginId, MidataId userId, Set<String> fields) throws AppException {

		Plugin app = Plugin.getById(pluginId, fields);
		if (app == null)
			throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");

		return app;
	}

	/**
	 * retrieve information about plugins
	 * 
	 * @return list of plugins
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result get(Request request) throws JsonValidationException, InternalServerException, AuthException {
		// validate json
		JsonNode json = request.body().asJson();
		JsonValidation.validate(json, "properties", "fields");

		// get visualizations
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		if (!properties.containsKey("status")) {
			properties.put("status", EnumSet.of(PluginStatus.DEVELOPMENT, PluginStatus.BETA, PluginStatus.ACTIVE, PluginStatus.DEPRECATED));
		}
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "recommendedPlugins", "developerTeam");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));

		Rights.chk("Plugins.get", getRole(), properties, fields);
		Set<Plugin> vis = Plugin.getAll(properties, fields);
		if (properties.containsKey("developerTeam")) {
			properties.put("creator", properties.get("developerTeam"));
			properties.remove("developerTeam");
			vis.addAll(Plugin.getAll(properties, fields));
		}
		List<Plugin> visualizations = new ArrayList<Plugin>(vis);

		Collections.sort(visualizations);
		
		if (fields.contains("developerTeamLogins")) {
			for (Plugin plugin : visualizations) {
				if (plugin.developerTeam != null) {
					plugin.developerTeamLogins = new ArrayList<String>(plugin.developerTeam.size());
					for (MidataId devId : plugin.developerTeam) {
						User developer = User.getById(devId, Sets.create("email"));
						if (developer!=null) plugin.developerTeamLogins.add(developer.email);
						else plugin.developerTeamLogins.add("unknown");
					}
				}
			}
		}
		return ok(JsonOutput.toJson(visualizations, "Plugin", fields)).as("application/json");
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result getInfo(Request request) throws JsonValidationException, InternalServerException, AuthException {
		// validate json
		JsonNode json = request.body().asJson();
		JsonValidation.validate(json, "name");

		String name = JsonValidation.getString(json, "name");
		String type = JsonValidation.getStringOrNull(json, "type");
		if (type == null || !type.equals("visualization")) type = "mobile";

		Set<String> fields = Sets.create("name", "description", "i18n", "defaultQuery", "resharesData", "allowsUserSearch", "termsOfUse", "requirements",
				"orgName", "publisher", "unlockCode", "targetUserRole", "icons", "filename", "loginTemplate", "loginButtonsTemplate", "loginTemplateApprovedDate");
		Plugin plugin = Plugin.get(CMaps.map("filename", name).map("type", type), fields);
		if (plugin != null && plugin.unlockCode != null)
			plugin.unlockCode = "true";
        if (plugin != null && plugin.loginTemplateApprovedDate == null && !InstanceConfig.getInstance().getInstanceType().getNoLoginScreenValidation()) {
        	if (plugin.loginTemplate != LoginTemplate.TERMS_OF_USE_AND_GENERATED) plugin.loginTemplate = LoginTemplate.GENERATED;        	
        }
        
        //plugin.requirements.addAll(InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(UserRole.MEMBER));
		return ok(JsonOutput.toJson(plugin, "Plugin", fields)).as("application/json");
	}

	/**
	 * install a new plugin for the current user
	 * 
	 * @param visualizationIdString
	 *            id of plugin to add to user account
	 * @return status ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result install(Request request, String visualizationIdString) throws AppException {
		JsonNode json = request.body().asJson();
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context1 = portalContext(request);
		MidataId visualizationId = null;
		Plugin visualization = null;
		if (MidataId.isValid(visualizationIdString)) {
			visualizationId = new MidataId(visualizationIdString);
			visualization = Plugin.getById(visualizationId, Sets.create("name", "defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator", "status", "defaultSubscriptions", "licenceDef"));
		} else {
			visualization = Plugin.getByFilename(visualizationIdString, Sets.create("name", "defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator", "status", "defaultSubscriptions", "licenceDef"));
		}

		if (visualization == null)
			throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");
		
		if (visualization.type.equals("mobile")) throw new BadRequestException("error.invalid.plugin", "Wrong app type.");

		if (visualization.type.equals("service")) {
			
				User user = User.getById(userId, User.ALL_USER_INTERNAL);
				ApplicationTools.refreshOrInstallService(context1, visualization._id, user, Collections.emptySet());
			
		} else if (visualization.type.equals("external")) {
			
				User user = User.getById(userId, User.ALL_USER_INTERNAL);
				ApplicationTools.refreshOrInstallService(context1, visualization._id, user, Collections.emptySet());
			
		} else {
			String spaceName = JsonValidation.getString(json, "spaceName");
			
			String context = json.has("context") ? JsonValidation.getString(json, "context") : visualization.defaultSpaceContext;
			MidataId study = json.has("study") ? JsonValidation.getMidataId(json, "study") : null;
			
	        Space space = install(context1, userId, visualization, context, spaceName, study);
	        
	        if (space != null) {
			   return ok(JsonOutput.toJson(space, "Space", Space.ALL)).as("application/json");
	        }
		}
		
		return ok();
	}
	
	public static Space install(AccessContext context1, MidataId userId, Plugin visualization, String context, String spaceName, MidataId study) throws AppException {
		AccessLog.log("install userId="+userId.toString()+" context="+context+" study="+study);					
		if (visualization == null)
			throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");
		if (visualization.status == PluginStatus.DELETED)
			throw new BadRequestException("error.unknown.plugin", "Unknown Plugin");
		MidataId visualizationId = visualization._id;
		
		if (context == null)
			context = "me";
		if (context.equals("me") && visualization.defaultSpaceContext != null && visualization.defaultSpaceContext.length() > 0)
			context = visualization.defaultSpaceContext;

		User user = User.getById(userId, Sets.create("visualizations", "apps", "role", "developer"));
		if (user == null) throw new BadRequestException("error.unknown.user", "Unknown user");

		boolean testing = visualization.isDeveloper(user._id) || (user.developer != null && visualization.isDeveloper(user.developer));

		if (!user.role.equals(visualization.targetUserRole) && !visualization.targetUserRole.equals(UserRole.ANY) && !testing) {
			throw new BadRequestException("error.invalid.plugin", "Visualization is for a different role. Your role:" + user.role);
		}
		
		MidataId licence = null;
		if (!testing && LicenceChecker.licenceRequired(visualization)) {
			licence = LicenceChecker.hasValidLicence(user._id, visualization, null);
			if (licence == null) throw new AuthException("error.missing.licence", "No licence found.", UserFeature.VALID_LICENCE);
		}

		if (visualization.type.equals("visualization")) {
			if (user.visualizations == null)
				user.visualizations = new HashSet<MidataId>();
			if (!user.visualizations.contains(visualizationId)) {
				user.visualizations.add(visualizationId);
				User.set(userId, "visualizations", user.visualizations);
			}
		} else {
			if (user.apps == null)
				user.apps = new HashSet<MidataId>();
			if (!user.apps.contains(visualizationId)) {
				user.apps.add(visualizationId);
				User.set(userId, "apps", user.apps);
			}
		}

		if (!visualization.type.equals("mobile") && !visualization.type.equals("service")) {
			if (spaceName == null || spaceName.equals(""))
				spaceName = visualization.name;

			Space space = null;
			space = Spaces.add(userId, spaceName, visualizationId, visualization.type, context, licence);

			if (space != null) {
				Map<String, Object> query = new HashMap<String, Object>(Feature_QueryRedirect.simplifyAccessFilter(visualization._id, visualization.defaultQuery));
				if (study != null) {
					query.put("link-study", study.toString());
					query.put("study", study.toString());
					AccessLog.log("set link");					
				} 
				RecordManager.instance.shareByQuery(context1.forAccountReshare(), space._id, query);
				
			}

			/*if (json.has("config")) {
				Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
				RecordManager.instance.setMeta(userId, space._id, "_config", config);
			}*/
			SubscriptionManager.activateSubscriptions(userId, visualization, space._id, false);
			return space;
		} 		

		return null;
	}

	/**
	 * uninstall a plugin for the current user
	 * 
	 * @param visualizationIdString
	 *            id of plugin to uninstall
	 * @return
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result uninstall(Request request, String visualizationIdString) throws InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		Set<String> fields = Sets.create("visualizations", "apps");

		User user = User.getByIdAlsoDeleted(userId, fields);
		user.visualizations.remove(new MidataId(visualizationIdString));
		user.apps.remove(new MidataId(visualizationIdString));
		User.set(userId, "visualizations", user.visualizations);
		User.set(userId, "apps", user.apps);

		return ok();
	}

	/**
	 * check if a plugin is installed
	 * 
	 * @param visualizationIdString
	 *            id of plugin to check
	 * @return boolean result
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result isInstalled(Request request, String visualizationIdString) throws InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId visualizationId = new MidataId(visualizationIdString);
		boolean isInstalled = Member.getByIdAndVisualization(userId, visualizationId, Sets.create()) != null || Member.getByIdAndApp(userId, visualizationId, Sets.create()) != null;

		return ok(Json.toJson(isInstalled));
	}

	/**
	 * check if an import plugin is authorized to query its endpoint
	 * 
	 * @param spaceIdString
	 *            id of space which uses plugin
	 * @return boolean result
	 * @throws AppException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public CompletionStage<Result> isAuthorized(Request request, String spaceIdString) throws AppException {
		
		AccessContext context = portalContext(request);

		BSONObject oauthmeta = RecordManager.instance.getMeta(context, new MidataId(spaceIdString), "_oauth");
		if (oauthmeta == null)
			return CompletableFuture.completedFuture((Result) ok(Json.toJson(false)));

		if (oauthmeta.containsField("refreshToken") && oauthmeta.get("refreshToken") != null) {
			return requestAccessTokenOAuth2FromRefreshToken(request, spaceIdString, oauthmeta.toMap(), Json.toJson(true));
		} else {
			return CompletableFuture.completedFuture((Result) ok(Json.toJson(true)));
		}
	}

	/**
	 * retrieve URL for plugin
	 * 
	 * @param visualizationIdString
	 *            id of plugin
	 * @return URL
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result getUrl(String visualizationIdString) throws InternalServerException {
		MidataId visualizationId = new MidataId(visualizationIdString);
		Map<String, MidataId> properties = new ChainedMap<String, MidataId>().put("_id", visualizationId).get();
		Set<String> fields = new ChainedSet<String>().add("filename").add("url").get();
		Plugin visualization = Plugin.get(properties, fields);

		String visualizationServer = config.getString("visualizations.server") + "/" + visualization.filename;
		String url = "https://" + visualizationServer + "/" + visualization.url;
		return ok(url);
	}

	/**
	 * retrieve URL to be used for an input form that may add data to the data
	 * set of a specific consent. Includes access token for this input form and
	 * user
	 * 
	 * @param appIdString
	 *            - ID of input form
	 * @param consentIdString
	 *            - ID of consent
	 * @throws AppException
	 * @return URL
	 */
	/*@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result getUrlForConsent(String appIdString, String consentIdString) throws AppException {
		// get app
		MidataId appId = new MidataId(appIdString);
		MidataId consentId = new MidataId(consentIdString);
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		Plugin app = Plugins.getPluginAndCheckIfInstalled(appId, userId, Sets.create("filename", "type", "url", "creator"));

		// create encrypted authToken
		SpaceToken appToken = new SpaceToken(PortalSessionToken.session().handle, consentId, userId, getRole(), appId);

		boolean testing = (app.creator.equals(PortalSessionToken.session().getDeveloperId()) || app.creator.equals(userId)) && app.developmentServer != null && app.developmentServer.length() > 0;

		String visualizationServer = "https://" + config.getString("visualizations.server") + "/" + app.filename;
		if (testing)
			visualizationServer = app.developmentServer;
		String url = visualizationServer + "/" + app.url;
		url = url.replace(":authToken", appToken.encrypt(request));

		return ok(url);
	}*/

	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result getRequestTokenOAuth1(Request request, String spaceIdString) throws AppException {

		// get app details
		final MidataId spaceId = new MidataId(spaceIdString);
		final MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		String origin = config.getString("portal.originUrl");
		if (origin.equals("https://demo.midata.coop:9002"))
			origin = "https://demo.midata.coop";
		String authPage = origin + "/authorized.html";

		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null)
			throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth1"))
			throw new InternalServerException("error.internal", "Wrong type");

		MidataId appId = space.visualization;
		Map<String, Object> properties = new ChainedMap<String, Object>().put("_id", appId.toObjectId()).get();
		Set<String> fields = new ChainedSet<String>().add("consumerKey").add("consumerSecret").add("requestTokenUrl").add("accessTokenUrl").add("authorizationUrl").get();
		Plugin app = Plugin.get(properties, fields);

		// get request token (pass callback url as argument)
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = client.retrieveRequestToken(authPage);

		//Not compatible with play 2.8 - What does this even do? 
		//session("token", requestToken.token);
		//session("secret", requestToken.secret);
		//End non compatible

		Map<String, Object> tokens = CMaps.map("token", requestToken.token).map("secret", requestToken.secret);
		RecordManager.instance.setMeta(context, space._id, "_oauth1", tokens);

		return ok(client.redirectUrl(requestToken.token));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result requestAccessTokenOAuth1(Request request, String spaceIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();
		JsonValidation.validate(json, "code");

		Map<String, Object> additionalParams = Collections.EMPTY_MAP;
		if (json.has("params"))
			additionalParams = JsonExtraction.extractMap(json.get("params"));

		// get app details
		final MidataId spaceId = new MidataId(spaceIdString);
		final MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);

		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null)
			throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth1"))
			throw new InternalServerException("error.internal", "Wrong type");

		Map<String, Object> properties = CMaps.map("_id", space.visualization);
		Set<String> fields = Sets.create("accessTokenUrl", "consumerKey", "consumerSecret");
		Plugin app = Plugin.get(properties, fields);

		BSONObject oauth1Params = RecordManager.instance.getMeta(context, space._id, "_oauth1");
		Map<String, Object> reqTokens = oauth1Params.toMap();

		// request access token
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		ServiceInfo info = new ServiceInfo(app.requestTokenUrl, app.accessTokenUrl, app.authorizationUrl, key);
		OAuth client = new OAuth(info);
		RequestToken requestToken = new RequestToken(reqTokens.get("token").toString(), reqTokens.get("secret").toString());
		RequestToken accessToken = client.retrieveAccessToken(requestToken, json.get("code").asText());

		// save token and secret to database

		Map<String, Object> tokens = CMaps.map("appId", space.visualization.toString()).map("oauthToken", accessToken.token).map("oauthTokenSecret", accessToken.secret);
		for (String p : additionalParams.keySet()) {
			if (p.equals("oauth_token") || p.equals("oauth_verifier") || p.equals("appId") || p.equals("oauthToken") || p.equals("oauthTokenSecret")) {

			} else
				tokens.put(p, additionalParams.get(p));
		}
		additionalParams.remove("oauth_token");
		additionalParams.remove("oauth_verifier");
		if (!additionalParams.isEmpty()) {
			RecordManager.instance.setMeta(context, spaceId, "_oauthParams", additionalParams);
		}

		RecordManager.instance.setMeta(context, spaceId, "_oauth", tokens);

		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public CompletionStage<Result> requestAccessTokenOAuth2(Request request, String spaceIdString) throws AppException {
		// validate json
		JsonNode json = request.body().asJson();
		try {
			JsonValidation.validate(json, "code");
		} catch (final JsonValidationException e) {
			return CompletableFuture.completedFuture(badRequest(e.getMessage()));
		}

		// get app details
		final MidataId spaceId = new MidataId(spaceIdString);
		final MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		final String sessionHandle = PortalSessionToken.session().handle;

		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("visualization", "type"));
		if (space == null)
			throw new InternalServerException("error.internal", "Unknown Space");
		if (!space.type.equals("oauth2"))
			throw new InternalServerException("error.internal", "Wrong type");

		final MidataId appId = space.visualization;
		Map<String, Object> properties = CMaps.map("_id", space.visualization);
		Set<String> fields = Sets.create("accessTokenUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "refreshTkExchangeParams");
		Plugin app = Plugin.get(properties, fields);

		String origin = config.getString("portal.originUrl");
		if (origin.equals("https://demo.midata.coop:9002"))
			origin = "https://demo.midata.coop";
		String authPage = origin + "/authorized.html";
		final Http.Request req = request;
		try {

			String postBuilder = app.tokenExchangeParams;
			if (postBuilder == null)
				postBuilder = "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>";
			postBuilder = postBuilder.replace("<code>", json.get("code").asText());
			postBuilder = postBuilder.replace("<redirect_uri>", URLEncoder.encode(authPage, "UTF-8"));
			postBuilder = postBuilder.replace("<client_id>", app.consumerKey);
			postBuilder = postBuilder.replace("<client_secret>", app.consumerSecret);
			postBuilder = postBuilder.replace("<grant_type>", "authorization_code");
			final String post = postBuilder;

			WSRequest holder = ws.url(app.accessTokenUrl);

			if (postBuilder.indexOf("client_secret") < 0)
				holder = holder.setAuth(app.consumerKey, app.consumerSecret);
			// request access token
			CompletionStage<WSResponse> promise = holder.setContentType("application/x-www-form-urlencoded; charset=utf-8").post(post);
			return promise.thenApply(response -> {
				try {
					KeyManager.instance.continueSession(sessionHandle);
					final String body = response.getBody();
					AccessLog.log("OAuth2 Request: "+post);
					AccessLog.log("OAuth2 Response: "+body);
					JsonNode jsonNode = response.asJson();
					
					// Try to deal with non-standard response formats.
					if (!jsonNode.has("access_token")) {
						for (JsonNode child : jsonNode) {
							if (child.has("access_token")) {
								jsonNode = child;
								break;
							}
						}						
					}
					
					if (jsonNode.has("access_token") && jsonNode.get("access_token").isTextual()) {
						String accessToken = jsonNode.get("access_token").asText();
						String refreshToken = null;
						if (jsonNode.has("refresh_token") && jsonNode.get("refresh_token").isTextual()) {
							refreshToken = jsonNode.get("refresh_token").asText();
						}
						
						if (refreshToken == null || refreshToken.equals("null")) {
							Stats.startRequest();
							Stats.setPlugin(appId);
							Stats.addComment("send:" + post);
							Stats.addComment("extern server: " + response.getStatus() + " " + body);
							Stats.finishRequest(req, "400");
						}

						Map<String, Object> tokens = CMaps.map("appId", appId.toString()).map("accessToken", accessToken).mapNotEmpty("refreshToken", refreshToken);
						RecordManager.instance.setMeta(context, spaceId, "_oauth", tokens);

						return ok();
					} else {
						Stats.startRequest();
						Stats.setPlugin(appId);
						Stats.addComment("send:" + post);
						Stats.addComment("extern server: " + response.getStatus() + " " + body);
						Stats.finishRequest(req, "400");

						return badRequest("Access token not found.");
					}
				} catch (AppException e) {
					ErrorReporter.report("oauth2", null, e);
					return internalServerError("Error requesting access token");
				} finally {
					ServerTools.endRequest();
				}
			});
		} catch (UnsupportedEncodingException e) {
			return null;
		}
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
	public static CompletionStage<Result> requestAccessTokenOAuth2FromRefreshToken(Request request, String spaceIdStr, Map<String, Object> tokens1, final JsonNode result) throws AppException {
		final MidataId appId = new MidataId(tokens1.get("appId").toString());
		final MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));

		Map<String, Object> properties = new ChainedMap<String, Object>().put("_id", appId.toObjectId()).get();
		Set<String> fields = Sets.create("name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "refreshTkExchangeParams", "type");

		final Plugin app = Plugin.get(properties, fields);

		return requestAccessTokenOAuth2FromRefreshToken(PortalSessionToken.session().handle, userId, app, spaceIdStr, tokens1).thenApplyAsync(success -> {
			
				if ((boolean) success)
					return ok(result);
				ObjectNode resultBase = result instanceof ObjectNode ? (ObjectNode) result : null;
				return oauthInfo(app, resultBase);
			});
	}

	public static CompletionStage<Boolean> requestAccessTokenOAuth2FromRefreshToken(final String sessionHandle, final MidataId userId, final Plugin app, String spaceIdStr,
			Map<String, Object> tokens1) {

		final Map<String, Object> tokens = tokens1;
		final MidataId spaceId = new MidataId(spaceIdStr);
		// get app details
		Object rt = tokens.get("refreshToken");
		if (rt == null) {
			AccessLog.log("tokens=" + tokens.toString());
			return CompletableFuture.completedFuture(false);
		}
		String refreshToken = rt.toString();

		String post0 = app.refreshTkExchangeParams;
		if (post0 == null || post0.length()==0) {
			
		  String postBuilder = app.tokenExchangeParams;
		  if (postBuilder == null)
			postBuilder = "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>";
		    post0 = "grant_type=refresh_token&refresh_token=" + refreshToken;
		    if (postBuilder.indexOf("client_secret") >= 0)
			  post0 = "client_secret=" + app.consumerSecret + "&" + post0;
		    if (postBuilder.indexOf("client_id") >= 0)
			  post0 = "client_id=" + app.consumerKey + "&" + post0;
		}

		post0 = post0.replace("<refresh_token>", refreshToken);
		
		post0 = post0.replace("<client_id>", app.consumerKey);
		post0 = post0.replace("<client_secret>", app.consumerSecret);
		post0 = post0.replace("<grant_type>", "refresh_token");
		
		
		final String post = post0;
		// request access token
		WSRequest holder = ws.url(app.accessTokenUrl);
		if (post0.indexOf("client_secret") < 0)
			holder = holder.setAuth(app.consumerKey, app.consumerSecret);
		CompletionStage<WSResponse> promise = holder.setContentType("application/x-www-form-urlencoded; charset=utf-8").post(post);
		return promise.thenApply(response -> {

			try {
				KeyManager.instance.continueSession(sessionHandle, userId);
				AccessLog.log("OAUTH POST: "+post);
				AccessLog.log("OAUTH RESPONSE: "+response.getBody());
				JsonNode jsonNode = response.asJson();
				
				// Try to deal with non-standard response formats.
				if (!jsonNode.has("access_token")) {
					for (JsonNode child : jsonNode) {
						if (child.has("access_token")) {
							jsonNode = child;
							break;
						}
					}						
				}
				
				if (jsonNode.has("access_token") && jsonNode.get("access_token").isTextual()) {
					String accessToken = jsonNode.get("access_token").asText();
					if (jsonNode.has("refresh_token") && jsonNode.get("refresh_token").isTextual()) {
						tokens.put("refreshToken", jsonNode.get("refresh_token").asText());
					}
					try {
						tokens.put("accessToken", accessToken);
						RecordManager.instance.setMeta(ContextManager.instance.createSessionForDownloadStream(userId, UserRole.MEMBER), spaceId, "_oauth", tokens);
					} catch (InternalServerException e) {
						return false;
					} finally {
						ServerTools.endRequest();
					}
					return true;
				} else {
					Stats.startRequest();
					Stats.setPlugin(app._id);
					Stats.addComment("send:" + post);
					Stats.addComment("extern server: " + response.getStatus() + " " + response.getBody());
					Stats.finishRequest("intern", "/oauth2", null, "400", Collections.<String> emptySet());
	
					return false;
				}
			} catch (AppException e) {
				return false;
			} finally {
				ServerTools.endRequest();
			}
		});
		/*
		 * } catch (final InternalServerException e) { return
		 * Promise.promise(new Function0<Result>() { public Result apply() {
		 * return internalServerError(e.getMessage()); } }); }
		 */
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result addMissingPlugins(Request request) throws AppException {
		
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));				
		addMissingPlugins(portalContext(request), userId, getRole());				
		return ok();
		
	}
		
	public static void addMissingPlugins(AccessContext context, MidataId userId, UserRole role) throws AppException {
							
		if (role.equals(UserRole.MEMBER)) {
			AccessLog.log("Looking for plugins to add...");
			Set<StudyParticipation> parts = StudyParticipation.getAllActiveByMember(userId, Sets.create("study"));
			
			if (!parts.isEmpty()) {
				Set<MidataId> apps = new HashSet<MidataId>();
				for (StudyParticipation part : parts) {
					Set<StudyAppLink> links = StudyAppLink.getByStudy(part.study);
					for (StudyAppLink link : links) {
						if (link.isConfirmed() && link.active && link.type.contains(StudyAppLinkType.AUTOADD_A)) {
							apps.add(link.appId);
						}
					}
				}
				
				if (!apps.isEmpty()) {
					//User user = User.getById(userId, Sets.create("apps", "visualizations"));
										
					for (MidataId appId : apps) {
					  AccessLog.log("Possible plugins to add: "+appId.toString());
					  //if (user.apps != null && user.apps.contains(appId)) continue;
					  //if (user.visualizations != null && user.visualizations.contains(appId)) continue;
					  
					  Set<Space> spaces = Space.getByOwnerVisualization(userId, appId, Sets.create("context"));
					  if (spaces.isEmpty()) {
						  Plugin visualization = Plugin.getById(appId, Sets.create("name", "defaultQuery", "type", "targetUserRole", "defaultSpaceName", "defaultSpaceContext", "creator", "status", "defaultSubscriptions","licenceDef"));
						  AccessLog.log("add plugins: "+appId.toString());
						  install(context, userId, visualization, null, null, null);
					  }
					} 
					 
				}
				
			}
		}				
		
	}
}
