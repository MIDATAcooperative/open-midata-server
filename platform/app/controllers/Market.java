package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;

import actions.APICall;
import actions.MobileCall;
import models.Admin;
import models.Developer;
import models.HPUser;
import models.HealthcareProvider;
import models.MessageDefinition;
import models.MidataId;
import models.MobileAppInstance;
import models.Model;
import models.Plugin;
import models.PluginIcon;
import models.PluginReview;
import models.Plugin_i18n;
import models.Space;
import models.Study;
import models.StudyAppLink;
import models.StudyParticipation;
import models.SubscriptionData;
import models.TestPluginCall;
import models.User;
import models.UserGroupMember;
import models.enums.AppReviewChecklist;
import models.enums.IconUse;
import models.enums.JoinMethod;
import models.enums.LinkTargetType;
import models.enums.MessageReason;
import models.enums.ParticipantSearchStatus;
import models.enums.PluginStatus;
import models.enums.ReviewStatus;
import models.enums.StudyAppLinkType;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import models.stats.PluginDevStats;
import play.mvc.BodyParser;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.access.Query;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.DeveloperSecured;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.SubscriptionResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.SubscriptionManager;

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
	@Security.Authenticated(DeveloperSecured.class)	
	public Result updatePlugin(String pluginIdStr) throws JsonValidationException, AppException {
		//if (!getRole().equals(UserRole.ADMIN) && !getRole().equals(UserRole.DEVELOPER)) return unauthorized();
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId pluginId = new MidataId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
		
		if (!getRole().equals(UserRole.ADMIN) && !userId.equals(app.creator)) throw new BadRequestException("error.not_authorized.not_plugin_owner", "Not your plugin!");
		
		app.version = JsonValidation.getLong(json, "version");				
		
		boolean withLogout = JsonValidation.getBoolean(json, "withLogout");
		boolean msgOnly = JsonValidation.getBoolean(json, "msgOnly");
		
		Map<String, MessageDefinition> predefinedMessages = parseMessages(json);
		if (predefinedMessages != null) {
			app.predefinedMessages = predefinedMessages;
			markReviewObsolete(app._id, AppReviewChecklist.MAILS);
		}
		
		if (!msgOnly) { 
			if (withLogout) {
				String filename = JsonValidation.getString(json, "filename"); // .toLowerCase(); We have existing plugins with mixed case
				if (!app.filename.equals(filename)) {
					filename = filename.toLowerCase();
					
					if (Plugin.exists(CMaps.map("filename", filename).map("status", EnumSet.of(PluginStatus.ACTIVE, PluginStatus.BETA, PluginStatus.DEPRECATED, PluginStatus.DEVELOPMENT)))) {
						throw new BadRequestException("error.exists.plugin", "A plugin with the same filename already exists.");
					}
					
					app.filename = filename; 
				}
			}
				
			String name = JsonValidation.getString(json, "name");
			if (!app.name.equals(name)) {
				
			  if (Plugin.exists(CMaps.map("creator", userId.toDb()).map("name", name).map("status", EnumSet.of(PluginStatus.ACTIVE, PluginStatus.BETA, PluginStatus.DEPRECATED, PluginStatus.DEVELOPMENT)))) {
				throw new BadRequestException("error.exists.plugin", "A plugin with the same name already exists.");
			  }
				
			  app.name = JsonValidation.getString(json, "name");
			}
			
			
			if (withLogout) {
				Map<String, Object> oldDefaultQuery = app.defaultQuery;
				
				app.type = JsonValidation.getString(json, "type");
				app.requirements = JsonExtraction.extractEnumSet(json, "requirements", UserFeature.class);
				app.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
				app.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));
				app.resharesData = JsonValidation.getBoolean(json, "resharesData");
				app.allowsUserSearch = JsonValidation.getBoolean(json, "allowsUserSearch");
				app.writes = JsonValidation.getEnum(json, "writes", WritePermissionType.class);
				app.pluginVersion = System.currentTimeMillis();
				
				try {
				  Query.validate(app.defaultQuery, app.type.equals("mobile") || app.type.equals("service"));
				} catch (BadRequestException e) {
				  throw new JsonValidationException(e.getLocaleKey(), "defaultQuery", "invalid", e.getMessage());
				}
				
				if (app.defaultQuery != null && !app.defaultQuery.equals(oldDefaultQuery)) {
					markReviewObsolete(app._id, AppReviewChecklist.ACCESS_FILTER);
				}
			}
			
			app.noUpdateHistory = JsonValidation.getBoolean(json, "noUpdateHistory");
			app.orgName = JsonValidation.getStringOrNull(json, "orgName");
			app.publisher = JsonValidation.getStringOrNull(json, "publisher");
			app.description = JsonValidation.getStringOrNull(json, "description");				
			app.url = JsonValidation.getStringOrNull(json, "url");
			app.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
			app.addDataUrl = JsonValidation.getStringOrNull(json, "addDataUrl");
			app.developmentServer = "https://localhost:9004/"+app.filename;
			//app.developmentServer = JsonValidation.getStringOrNull(json, "developmentServer");
			app.tags = JsonExtraction.extractStringSet(json.get("tags"));
					
			app.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
			app.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
			
			app.unlockCode = JsonValidation.getStringOrNull(json, "unlockCode");
			app.sendReports = JsonValidation.getBoolean(json, "sendReports");
			
			app.i18n = new HashMap<String, Plugin_i18n>();
			
			
			if (getRole().equals(UserRole.ADMIN) && withLogout) {
				/*String linkedStudyCode = JsonValidation.getStringOrNull(json, "linkedStudyCode");
				if (linkedStudyCode != null) {
				  Study study = Study.getByCodeFromMember(linkedStudyCode, Sets.create("_id", "joinMethods", "executionStatus", "validationStatus", "participantSearchStatus"));
				  if (study == null) throw new JsonValidationException("error.invalid.study", "linkedStudy", "invalid", "Unknown Study");
				  if (study.executionStatus.equals(StudyExecutionStatus.ABORTED) || study.executionStatus.equals(StudyExecutionStatus.FINISHED)) throw new JsonValidationException("error.invalid.study", "linkedStudy", "invalid", "Study closed");
				  if (study.validationStatus.equals(StudyValidationStatus.REJECTED) || study.validationStatus.equals(StudyValidationStatus.DRAFT)) throw new JsonValidationException("error.invalid.study", "linkedStudy", "invalid", "Study rejected");
				  if (study.participantSearchStatus.equals(ParticipantSearchStatus.CLOSED)) throw new JsonValidationException("error.invalid.study", "linkedStudy", "invalid", "Study not searching");
				  if (study.joinMethods != null && !study.joinMethods.contains(JoinMethod.APP)) throw new JsonValidationException("error.invalid.study", "linkedStudy", "invalid", "Join by app not allowed");
				  
				  app.linkedStudy = study._id;
				} else {
				  app.linkedStudy = null;
				}
				app.mustParticipateInStudy = JsonValidation.getBoolean(json, "mustParticipateInStudy");
					*/
				
			   String termsOfUse = JsonValidation.getStringOrNull(json, "termsOfUse");
			   app.termsOfUse = termsOfUse;
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
				app.apiUrl = validApiUrl(JsonValidation.getStringOrNull(json, "apiUrl"));			
				
				app.authorizationUrl = JsonValidation.getStringOrNull(json, "authorizationUrl");
				app.accessTokenUrl = JsonValidation.getStringOrNull(json, "accessTokenUrl");
				app.consumerKey = JsonValidation.getStringOrNull(json, "consumerKey");
				app.consumerSecret = JsonValidation.getStringOrNull(json, "consumerSecret");
				if (app.type.equals("oauth1")) {
					app.requestTokenUrl = JsonValidation.getStringOrNull(json, "requestTokenUrl");
				} else if (app.type.equals("oauth2")) {
					app.scopeParameters = JsonValidation.getStringOrNull(json, "scopeParameters");
					app.tokenExchangeParams = JsonValidation.getStringOrNull(json, "tokenExchangeParams");
				}
			}
			if (app.type.equals("mobile") || app.type.equals("service")) {
				app.secret = JsonValidation.getStringOrNull(json, "secret");
				app.redirectUri = JsonValidation.getStringOrNull(json, "redirectUri");
			}
		
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
	public Result updatePluginStatus(String pluginIdStr) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request		
		MidataId pluginId = new MidataId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
						
		app.version = JsonValidation.getLong(json, "version");		
		app.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		app.status = JsonValidation.getEnum(json, "status", PluginStatus.class);
				 
		try {
		   app.update();
		   PluginIcon.updateStatus(app.filename, app.status);
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
	@Security.Authenticated(DeveloperSecured.class)	
	public Result updateDefaultSubscriptions(String pluginIdStr) throws JsonValidationException, AppException {
		//if (!getRole().equals(UserRole.ADMIN) && !getRole().equals(UserRole.DEVELOPER)) return unauthorized();
		// validate json
		JsonNode json = request().body().asJson();
			
		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId pluginId = new MidataId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
		
		if (!getRole().equals(UserRole.ADMIN) && !userId.equals(app.creator)) throw new BadRequestException("error.not_authorized.not_plugin_owner", "Not your plugin!");
		
		app.version = JsonValidation.getLong(json, "version");				
		
		parseSubscriptions(app, json);
						
		app.updateDefaultSubscriptions(app.defaultSubscriptions);		   
		
		return ok();
	}
		
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result exportPlugin(String pluginIdStr) throws JsonValidationException, AppException {
			
		// validate request		
		MidataId pluginId = new MidataId(pluginIdStr);
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
				
		Set<PluginIcon> icons = PluginIcon.getByPlugin(app.filename);
		
		List<Model> mixed = new ArrayList<Model>();
		mixed.add(app);
		mixed.addAll(icons);
		Map<String, Set<String>> mapping = new HashMap<String, Set<String>>();
		mapping.put("Plugin", Plugin.ALL_DEVELOPER);
		mapping.put("PluginIcon", PluginIcon.FIELDS);
		mapping.put("SubscriptionData", SubscriptionData.ALL);
		
		String json = JsonOutput.toJson(mixed, mapping);
		//String base64 = Base64.getEncoder().encodeToString(json.getBytes());
		return ok(json);
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result importPlugin() throws JsonValidationException, AppException {
        JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "base64");
		String base64 = JsonValidation.getString(json, "base64");
		
		//byte[] decoded = Base64.getDecoder().decode(base64);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode pluginJson = null;
		JsonNode allJson = null;
		try {
			allJson = mapper.readTree(base64);
			pluginJson = allJson.get(0);
		} catch (JsonProcessingException e) {
			AccessLog.logException("parse json", e);
		  throw new BadRequestException("error.invalid.json", "Invalid JSON provided");
		} catch (IOException e) {
		  throw new BadRequestException("error.invalid.json", "Invalid JSON provided");
		}
		    							     
		MidataId pluginId = new MidataId(pluginJson.get("_id").asText());
		String pluginName = JsonValidation.getString(pluginJson, "filename");
		
		Plugin app = Plugin.getByFilename(pluginName, Plugin.ALL_DEVELOPER);
		boolean isNew = false;
		if (app == null) {
			app = new Plugin();
			app._id = pluginId;
			isNew = true;
			app.version = JsonValidation.getLong(pluginJson, "version");		
			app.filename = JsonValidation.getStringOrNull(pluginJson, "filename");						
			app.creatorLogin = JsonValidation.getStringOrNull(pluginJson, "creatorLogin");
			app.status = JsonValidation.getEnum(pluginJson, "status", PluginStatus.class);
			app.icons = EnumSet.noneOf(IconUse.class);
			User u = Developer.getByEmail(app.creatorLogin, Sets.create("_id", "email"));
			if (u != null) {
			   app.creator = u._id;
			}
		}
		app.name = JsonValidation.getStringOrNull(pluginJson, "name");
		parsePlugin(app, pluginJson);
		
		try {
		List<PluginIcon> icons = new ArrayList<PluginIcon>();
		for (int i=1;i<allJson.size();i++) {
			JsonNode iconData = allJson.get(i);			
			PluginIcon icon = new PluginIcon();
			icon._id = JsonValidation.getMidataId(iconData, "_id");
			icon.contentType = JsonValidation.getString(iconData, "contentType");
			icon.plugin = app.filename;
			icon.use = JsonValidation.getEnum(iconData, "use", IconUse.class);
			icon.status = app.status;		
			icon.data = iconData.get("data").binaryValue();
			if (app.icons == null) app.icons = EnumSet.noneOf(IconUse.class);
			app.icons.add(icon.use);
			icons.add(icon);
		}
		
		if (isNew) {
			Plugin.add(app);
			for (PluginIcon icon : icons) PluginIcon.add(icon);
		} else {
			try {
			  app.update();
			  app.updateIcons(app.icons);
			  PluginIcon.delete(app.filename);
			  for (PluginIcon icon : icons) PluginIcon.add(icon);
			} catch (LostUpdateException e) {
			  throw new BadRequestException("error.concurrent.update", "Concurrent updates. Reload page and try again.");
			}
		}								
		return ok(JsonOutput.toJson(app, "Plugin", Plugin.ALL_DEVELOPER)).as("application/json");
		
		} catch (IOException e) {
			throw new BadRequestException("error.internal", "Cannot parse JSON");
		}
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
	public Result registerPlugin() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "type", "writes");
		String type = JsonValidation.getString(json, "type");
		
		if (type.equals("create")) {
			JsonValidation.validate(json, "filename", "name", "description", "url");
		} else if (type.equals("oauth1")) {
			JsonValidation.validate(json, "filename", "name", "description", "url", "authorizationUrl", "accessTokenUrl",
					"consumerKey", "consumerSecret", "requestTokenUrl", "apiUrl");
		} else if (type.equals("oauth2")) {
			JsonValidation.validate(json, "filename", "name", "description", "url", "authorizationUrl", "accessTokenUrl",
					"consumerKey", "consumerSecret", "scopeParameters", "apiUrl");
		} else if (type.equals("mobile") || type.equals("service")) {
			JsonValidation.validate(json, "filename", "name", "description", "secret");
		} else if (type.equals("visualization")) {
			JsonValidation.validate(json, "filename", "name", "description", "url");
		} else {
			throw new BadRequestException("error.internal", "Unknown app type.");
		}

		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		Developer dev = Developer.getById(userId, Sets.create("email"));
		
		String filename = JsonValidation.getString(json ,"filename").toLowerCase();
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
		plugin.filename = filename;
		plugin.name = JsonValidation.getStringOrNull(json, "name");
		plugin.orgName = JsonValidation.getStringOrNull(json, "orgName");
		plugin.publisher = JsonValidation.getStringOrNull(json, "publisher");
		plugin.description = JsonValidation.getStringOrNull(json, "description");
		plugin.spotlighted = JsonValidation.getBoolean(json, "spotlighted");
		plugin.type = JsonValidation.getString(json, "type");
		plugin.url = JsonValidation.getStringOrNull(json, "url");
		plugin.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
		plugin.addDataUrl = JsonValidation.getStringOrNull(json, "addDataUrl");
		plugin.developmentServer = "https://localhost:9004/"+plugin.filename; //JsonValidation.getStringOrNull(json, "developmentServer");
		plugin.tags = JsonExtraction.extractStringSet(json.get("tags"));
		plugin.requirements = JsonExtraction.extractEnumSet(json, "requirements", UserFeature.class);
		plugin.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		plugin.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
		plugin.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
		plugin.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));
		plugin.resharesData = JsonValidation.getBoolean(json, "resharesData");
		plugin.allowsUserSearch = JsonValidation.getBoolean(json, "allowsUserSearch");
		plugin.unlockCode = JsonValidation.getStringOrNull(json, "unlockCode");
		plugin.writes = JsonValidation.getEnum(json, "writes", WritePermissionType.class);
		plugin.predefinedMessages = parseMessages(json);
		plugin.pluginVersion = System.currentTimeMillis();
		plugin.noUpdateHistory = JsonValidation.getBoolean(json, "noUpdateHistory");
		
		try {
		    Query.validate(plugin.defaultQuery, plugin.type.equals("mobile") || plugin.type.equals("service"));
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
			plugin.apiUrl = validApiUrl(JsonValidation.getStringOrNull(json, "apiUrl"));			
			plugin.authorizationUrl = JsonValidation.getStringOrNull(json, "authorizationUrl");
			plugin.accessTokenUrl = JsonValidation.getStringOrNull(json, "accessTokenUrl");
			plugin.consumerKey = JsonValidation.getStringOrNull(json, "consumerKey");
			plugin.consumerSecret = JsonValidation.getStringOrNull(json, "consumerSecret");
			if (plugin.type.equals("oauth1")) {
				plugin.requestTokenUrl = JsonValidation.getStringOrNull(json, "requestTokenUrl");
			} else if (plugin.type.equals("oauth2")) {
				plugin.scopeParameters = JsonValidation.getStringOrNull(json, "scopeParameters");
				plugin.tokenExchangeParams = JsonValidation.getStringOrNull(json, "tokenExchangeParams");
			}
		}
		if (plugin.type.equals("mobile") || plugin.type.equals("service")) {
			plugin.secret = JsonValidation.getStringOrNull(json, "secret");
			plugin.redirectUri = JsonValidation.getStringOrNull(json, "redirectUri");
		}
		
			
		Plugin.add(plugin);
		
		return ok(JsonOutput.toJson(plugin, "Plugin", Plugin.ALL_DEVELOPER)).as("application/json");
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
	
	public static String validApiUrl(String url) throws AppException {
		if (url == null) return null;
		if (!url.startsWith("http://") && !url.startsWith("https://")) throw new JsonValidationException("error.invalid.url", "apiUrl", "invalid", "Invalid API Url");
        if (!url.endsWith("/")) url += "/";
        String domain = url.substring(url.indexOf("//")+2).trim();
        if (domain.indexOf("localhost") >= 0 || domain.indexOf("midata.coop") >=0 ) throw new JsonValidationException("error.invalid.url", "apiUrl", "invalid", "Invalid API Url");
        if (domain.startsWith("172.") || domain.startsWith("192.168.") || domain.startsWith("10.")) throw new JsonValidationException("error.invalid.url", "apiUrl", "invalid", "Invalid API Url");
        return url;
	}
	
	private static void parsePlugin(Plugin app, JsonNode json) throws JsonValidationException, AppException {
		app.orgName = JsonValidation.getStringOrNull(json, "orgName");
		app.publisher = JsonValidation.getStringOrNull(json, "publisher");
		app.description = JsonValidation.getStringOrNull(json, "description");		
		app.type = JsonValidation.getString(json, "type");
		app.url = JsonValidation.getStringOrNull(json, "url");
		app.previewUrl = JsonValidation.getStringOrNull(json, "previewUrl");
		app.addDataUrl = JsonValidation.getStringOrNull(json, "addDataUrl");
		app.developmentServer = "https://localhost:9004/"+app.filename;
		//app.developmentServer = JsonValidation.getStringOrNull(json, "developmentServer");
		app.tags = JsonExtraction.extractStringSet(json.get("tags"));
		app.requirements = JsonExtraction.extractEnumSet(json, "requirements", UserFeature.class);
		app.targetUserRole = JsonValidation.getEnum(json, "targetUserRole", UserRole.class);
		app.defaultSpaceName = JsonValidation.getStringOrNull(json, "defaultSpaceName");
		app.defaultSpaceContext = JsonValidation.getStringOrNull(json, "defaultSpaceContext");
		app.defaultQuery = JsonExtraction.extractMap(json.get("defaultQuery"));
		app.resharesData = JsonValidation.getBoolean(json, "resharesData");
		app.allowsUserSearch = JsonValidation.getBoolean(json, "allowsUserSearch");
		app.unlockCode = JsonValidation.getStringOrNull(json, "unlockCode");
		app.noUpdateHistory = JsonValidation.getBoolean(json, "noUpdateHistory");
		app.writes = JsonValidation.getEnum(json, "writes", WritePermissionType.class);
		app.i18n = new HashMap<String, Plugin_i18n>();
		app.pluginVersion = System.currentTimeMillis();	
		app.sendReports = false;
		
		Map<String, MessageDefinition> predefinedMessages = parseMessages(json);
		if (predefinedMessages != null) app.predefinedMessages = predefinedMessages;
		
		try {
		  Query.validate(app.defaultQuery, app.type.equals("mobile") || app.type.equals("service"));
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
			app.apiUrl = JsonValidation.getStringOrNull(json, "apiUrl");
			if (app.apiUrl != null && (!app.apiUrl.startsWith("http://") && !app.apiUrl.startsWith("https://"))) throw new JsonValidationException("error.invalid.url", "apiUrl", "invalid", "Invalid API Url");
			app.authorizationUrl = JsonValidation.getStringOrNull(json, "authorizationUrl");
			app.accessTokenUrl = JsonValidation.getStringOrNull(json, "accessTokenUrl");
			app.consumerKey = JsonValidation.getStringOrNull(json, "consumerKey");
			app.consumerSecret = JsonValidation.getStringOrNull(json, "consumerSecret");
			if (app.type.equals("oauth1")) {
				app.requestTokenUrl = JsonValidation.getStringOrNull(json, "requestTokenUrl");
			} else if (app.type.equals("oauth2")) {
				app.scopeParameters = JsonValidation.getStringOrNull(json, "scopeParameters");
				app.tokenExchangeParams = JsonValidation.getStringOrNull(json, "tokenExchangeParams");
			}
		}
		if (app.type.equals("mobile") || app.type.equals("service")) {
			app.secret = JsonValidation.getStringOrNull(json, "secret");
			app.redirectUri = JsonValidation.getStringOrNull(json, "redirectUri");
		}
		
		parseSubscriptions(app, json);
	}
	
	private static void parseSubscriptions(Plugin app, JsonNode json) throws JsonValidationException, InternalServerException {
		if (json.has("defaultSubscriptions")) {
			app.defaultSubscriptions = new ArrayList<SubscriptionData>();
			for (JsonNode elem : json.get("defaultSubscriptions")) {
				SubscriptionData data = new SubscriptionData();	
				data._id = new MidataId();
				data.active = JsonValidation.getBoolean(elem, "active");
				data.app = null;
				data.content = JsonValidation.getStringOrNull(elem, "content");
				data.endDate = JsonValidation.getDate(elem, "endDate");
				data.fhirSubscription = BasicDBObject.parse(JsonValidation.getJsonString(elem, "fhirSubscription"));
				data.format = JsonValidation.getStringOrNull(elem, "format");
				data.lastUpdated = System.currentTimeMillis();
				data.owner = null;
				SubscriptionResourceProvider.populateSubscriptionCriteria(data, data.fhirSubscription.get("criteria").toString());
				app.defaultSubscriptions.add(data);
			}
		} else app.defaultSubscriptions = null;
	}
	
	/**
	 * delete a plugin
	 * @param pluginIdStr ID of plugin to delete
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */	
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result deletePlugin(String pluginIdStr) throws JsonValidationException, AppException {		
		// validate request		
		MidataId pluginId = new MidataId(pluginIdStr);
		
		deletePlugin(pluginId);
		
		return ok();
	}
	
	private static void deletePlugin(MidataId pluginId) throws JsonValidationException, AppException {
		
		
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");

		if (app.type.equals("mobile") || app.type.equals("service")) {
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
		String filename = app.filename;
		app.filename = null;
		
	    try {
	        app.update();
	        PluginIcon.delete(filename);
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
    public Result deletePluginDeveloper(String pluginIdStr) throws JsonValidationException, AppException {
            
        // validate request     
        MidataId pluginId = new MidataId(pluginIdStr);
        MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        
        Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
        if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
        if (app.creator==null || !app.creator.equals(userId)) throw new BadRequestException("error.auth", "You are not owner of this plugin.");
        if (app.status != PluginStatus.DEVELOPMENT && app.status != PluginStatus.BETA) throw new BadRequestException("error.auth", "Plugin may not be deleted. Ask an admin.");
              
        deletePlugin(pluginId);        
        
        return ok();
    }
	
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result correctOwners() throws AppException {
	   Set<Plugin> plugins = Plugin.getAll(CMaps.map(), Sets.create("_id", "creator", "creatorLogin"));
	   
	   for (Plugin plg : plugins) {
		   Developer u = plg.creator==null ? null : Developer.getById(plg.creator, Sets.create("email"));
		   if (u == null && plg.creatorLogin != null) {
			   u = Developer.getByEmail(plg.creatorLogin, Sets.create("_id", "email"));
			   if (u != null) {
				   Plugin.set(plg._id, "creator", u._id);
			   }
		   }
	   }
	   
	   return ok();
	}
		
	@APICall
	@Security.Authenticated(DeveloperSecured.class)	
	public Result getPluginStats(String pluginIdStr) throws JsonValidationException, AppException {
		//if (!getRole().equals(UserRole.ADMIN) && !getRole().equals(UserRole.DEVELOPER)) return unauthorized();
		
		MidataId pluginId = new MidataId(pluginIdStr);
        MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        
        Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
        if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
        if (!getRole().equals(UserRole.ADMIN) && (app.creator==null || !app.creator.equals(userId))) throw new BadRequestException("error.auth", "You are not owner of this plugin.");
   
		List<PluginDevStats> stats = new ArrayList(PluginDevStats.getByPlugin(pluginId, PluginDevStats.ALL));
		
		return ok(JsonOutput.toJson(stats, "PluginDevStats", PluginDevStats.ALL)).as("application/json");
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)	
	public Result deletePluginStats(String pluginIdStr) throws JsonValidationException, AppException {
		if (!getRole().equals(UserRole.ADMIN) && !getRole().equals(UserRole.DEVELOPER)) return unauthorized();
		
		MidataId pluginId = new MidataId(pluginIdStr);
        MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        
        Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
        if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
        if (!getRole().equals(UserRole.ADMIN) && (app.creator==null || !app.creator.equals(userId))) throw new BadRequestException("error.auth", "You are not owner of this plugin.");
   
		PluginDevStats.deleteByPlugin(pluginId);
		
		return ok();
	}
	
	
	/**
	 * Retrieve icon for an App. 
	 * Public function - no session required	
	 * @param id - name of plugin
	 * @return the icon which has been set for the app
	 * @throws AppException
	 */
	@APICall
	public Result getIcon(String use, String id) throws AppException {
		IconUse iconUse = IconUse.valueOf(use);
		if (iconUse == null) return notFound();
		
		if (ObjectId.isValid(id)) {
			Plugin p = Plugin.getById(MidataId.from(id));
			if (p != null) {
			  id = p.filename;			
			  if (p.icons == null || !p.icons.contains(iconUse)) id = "portal";
			}
		}		
        PluginIcon icon = PluginIcon.getByPluginAndUse( id, iconUse);	
        if (icon == null || icon.status.equals(PluginStatus.DELETED)) return notFound();
                        		
		return ok(icon.data).as(icon.contentType);
	}
	
	/**
	 * Add icon to app
	 */	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)	
	public Result uploadIcon(String pluginIdStr) throws AppException {
		MidataId pluginId = MidataId.from(pluginIdStr);
		try {
		
			//response().setHeader("Access-Control-Allow-Origin", "*");
	
			// check meta data
			MultipartFormData formData = request().body().asMultipartFormData();
			Map<String, String[]> metaData = formData.asFormUrlEncoded();
			if (!metaData.containsKey("use")) {
				throw new BadRequestException("error.internal", "At least one request parameter is missing.");
			}
							
			MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));			
			IconUse use = IconUse.valueOf(metaData.get("use")[0]);
			if (use == null) throw new BadRequestException("error.internal", "Unknown icon use");
					
			Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
			if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");			
			if ((app.creator==null || !app.creator.equals(userId)) && !getRole().equals(UserRole.ADMIN)) throw new BadRequestException("error.not_authorized.not_plugin_owner", "Not your plugin!");
		
			
			// extract file from data
			FilePart fileData = formData.getFile("file");
			if (fileData == null) {
				throw new BadRequestException("error.internal", "No file found.");
			}
			File file = (File) fileData.getFile();
			if (file.length() > 1024 * 1024) throw new BadRequestException("error.too_large.file", "Maximum file size is 100kb");
			String filename = fileData.getFilename().toUpperCase();
			String contentType = fileData.getContentType();
			if (!filename.endsWith(".JPG") && !filename.endsWith(".JPEG") && !filename.endsWith(".PNG") && !filename.endsWith(".GIF")) throw new BadRequestException("error.invalid.file", "File extension not known.");
			if (!contentType.equals("image/png") && !contentType.equals("image/jpeg") && !contentType.equals("image/gif"))  throw new BadRequestException("error.invalid.file", "Mime type not known.");
			
			boolean isvalid = false;
			try (InputStream input = new FileInputStream(file)) {
			    try {			    	
			        ImageIO.read(input).toString();
			        isvalid = true;
			    } catch (Exception e) {
			        isvalid = false;
			    }
			}
			if (!isvalid) 
						
			PluginIcon.delete(app.filename, use);
		
			PluginIcon icon = new PluginIcon();
			icon._id = new MidataId();
			icon.plugin = app.filename;
			icon.use = use;
			icon.status = app.status;
			icon.contentType = contentType;
			icon.data = IOUtils.toByteArray(new FileInputStream(file));
						
			PluginIcon.add(icon);
			
			if (app.icons == null) app.icons = EnumSet.noneOf(IconUse.class);
			
			app.icons.add(use);
			app.updateIcons(app.icons);
			
			markReviewObsolete(app._id, AppReviewChecklist.ICONS);
			
			return ok();
		
		} catch (IOException e) {
			throw new InternalServerException("error.internal", e);
		}
			
		
	}
	
	/**
	 * Remove icon from app
	 */	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)	
	public Result deleteIcon(String pluginIdStr, String useStr) throws AppException {
		MidataId pluginId = MidataId.from(pluginIdStr);
		
		IconUse use = IconUse.valueOf(useStr);
		if (use == null) throw new BadRequestException("error.internal", "Unknown icon use");
									
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));			
					
		Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");			
		if ((app.creator==null || !app.creator.equals(userId)) && !getRole().equals(UserRole.ADMIN)) throw new BadRequestException("error.not_authorized.not_plugin_owner", "Not your plugin!");
				
		PluginIcon.delete(app.filename, use);
				
		app.icons.remove(use);
		app.updateIcons(app.icons);
			
		return ok();				
		
	}
	
	@APICall
	public Result getStudyAppLinks(String type, String idStr) throws AppException {
		Set<StudyAppLink> result = Collections.emptySet();
		if (type.equals("study") || type.equals("study-use")) {
			result = StudyAppLink.getByStudy(MidataId.from(idStr));
			for (StudyAppLink sal : result) {
				sal.app = Plugin.getById(sal.appId);
			}
			
			if (type.equals("study-use")) {				
				Iterator<StudyAppLink> sal_it = result.iterator();
				while (sal_it.hasNext()) {
					StudyAppLink sal = sal_it.next();					
					if (!sal.isConfirmed() || !sal.active) sal_it.remove();
					
				}
			}
			
		} else if (type.equals("app") || type.equals("app-use")) {
			result = StudyAppLink.getByApp(MidataId.from(idStr));
			for (StudyAppLink sal : result) {
				if (sal.linkTargetType == null || sal.linkTargetType == LinkTargetType.STUDY) {
				  Study study = Study.getById(sal.studyId, Sets.create("_id", "code","name", "type", "description", "termsOfUse", "executionStatus","validationStatus","participantSearchStatus", "joinMethods"));
				  sal.study = study;
				  sal.termsOfUse = study.termsOfUse;
				} else {					
				  HealthcareProvider prov = HealthcareProvider.getById(sal.providerId, HealthcareProvider.ALL);
				  sal.provider = prov;
				  HPUser user = HPUser.getById(sal.userId, Sets.create("email"));
				  sal.userLogin = user.email;
				}
								
			}
			
			if (type.equals("app-use")) {
				Iterator<StudyAppLink> sal_it = result.iterator();
				while (sal_it.hasNext()) {
					StudyAppLink sal = sal_it.next();	
					if (sal.linkTargetType == LinkTargetType.ORGANIZATION) {
						if (!sal.isConfirmed()) sal_it.remove();
					} else {
						if (!sal.isConfirmed() || !sal.usePeriod.contains(sal.study.executionStatus)) sal_it.remove();
						else if (!sal.study.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING)) {
							sal.type.remove(StudyAppLinkType.OFFER_P);
							if (sal.type.isEmpty()) sal_it.remove();
						}
					}
				}
			}
		}
		Map<String, Set<String>> mapping = new HashMap<String, Set<String>>();
		mapping.put("Plugin", Plugin.ALL_PUBLIC);
		mapping.put("HealthcareProvider", HealthcareProvider.ALL);
		mapping.put("Study", Sets.create("_id", "code","name","type", "description", "termsOfUse", "executionStatus", "validationStatus", "participantSearchStatus", "joinMethods"));
		mapping.put("StudyAppLink", StudyAppLink.ALL);
		
		return ok(JsonOutput.toJson(result, mapping)).as("application/json");
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result insertStudyAppLink() throws AppException {
        JsonNode json = request().body().asJson();	
        
        LinkTargetType lt = JsonValidation.getEnum(json, "linkTargetType", LinkTargetType.class);
        if (lt != null && lt.equals(LinkTargetType.ORGANIZATION)) return insertAppLink();
        
		JsonValidation.validate(json, "studyId", "appId", "type", "usePeriod");

		
		StudyAppLink link = new StudyAppLink();
									
		link._id = new MidataId();
		link.linkTargetType = LinkTargetType.STUDY;
		link.appId = JsonValidation.getMidataId(json, "appId");
		link.restrictRead = JsonValidation.getBoolean(json, "restrictRead");
		link.shareToStudy = JsonValidation.getBoolean(json, "shareToStudy");
		link.studyGroup = JsonValidation.getStringOrNull(json, "studyGroup");
		link.studyId = JsonValidation.getMidataId(json, "studyId");
		link.type = JsonValidation.getEnumSet(json, "type", StudyAppLinkType.class);
		link.usePeriod = JsonValidation.getEnumSet(json, "usePeriod", StudyExecutionStatus.class);
		
		link.validationResearch = StudyValidationStatus.VALIDATION;
		link.validationDeveloper = StudyValidationStatus.VALIDATION;
		
		checkValidation(link);
										
		link.add();
		
		markReviewObsolete(link.appId, AppReviewChecklist.PROJECTS);
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result insertAppLink() throws AppException {
        JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "userLogin", "appId", "type");

		
		StudyAppLink link = new StudyAppLink();
									
		link._id = new MidataId();
		link.linkTargetType = LinkTargetType.ORGANIZATION;
		link.appId = JsonValidation.getMidataId(json, "appId");				
		link.type = JsonValidation.getEnumSet(json, "type", StudyAppLinkType.class);
		link.identifier = JsonValidation.getString(json, "identifier");
		link.termsOfUse = JsonValidation.getStringOrNull(json, "termsOfUse");
		
		HPUser user = HPUser.getByEmail(JsonValidation.getString(json, "userLogin"), Sets.create("status","provider"));
		if (user == null || user.status != UserStatus.ACTIVE) throw new JsonValidationException("error.invalid.user", "User not found or not active");
		HealthcareProvider prov = HealthcareProvider.getById(user.provider, HealthcareProvider.ALL);
		if (prov == null) throw new JsonValidationException("error.invalid.user", "User not found or not active");
		link.providerId = prov._id;
		link.userId = user._id;
				
		link.validationResearch = StudyValidationStatus.VALIDATION;
		link.validationDeveloper = StudyValidationStatus.VALIDATION;
		
		checkValidation(link);
										
		link.add();
		
		markReviewObsolete(link.appId, AppReviewChecklist.PROJECTS);
		
		return ok();
	}

	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result deleteStudyAppLink(String id) throws AppException {		
		StudyAppLink link = StudyAppLink.getById(MidataId.from(id));		
		checkValidation(link);		
		link.delete();		
		return ok();
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result validateStudyAppLink(String id) throws AppException {
		StudyAppLink link = StudyAppLink.getById(MidataId.from(id));		
		checkValidation(link);
		link.update();
		return ok();
	}
	
	public static void updateActiveStatus(Study study) throws AppException {
		Set<StudyAppLink> links = StudyAppLink.getByStudy(study._id);
		for (StudyAppLink link : links) updateActiveStatus(study, link);
	}
	
	public static void updateActiveStatus(Study study, StudyAppLink link) throws AppException {
		boolean oldactive = link.active;
		link.active = link.isConfirmed() && link.usePeriod.contains(study.executionStatus);
		if (link.active != oldactive) {
			link.update();
		}
	}
	
	private static void checkValidation(StudyAppLink link) throws AppException {
		if (link == null) throw new BadRequestException("error.internal", "Unknown link");
		
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		UserRole role = getRole();
		
		Plugin plugin = Plugin.getById(link.appId);   
		if (plugin == null) throw new BadRequestException("error.internal", "Unknown plugin");
				
		
        if (role.equals(UserRole.DEVELOPER)) {		 
		   if (plugin.creator==null || !plugin.creator.equals(userId)) throw new AuthException("error.notauthorized.not_plugin_owner", "You are not authorized to change this plugin.");
		   link.validationDeveloper = StudyValidationStatus.VALIDATED;
		} else if (role.equals(UserRole.RESEARCH) && link.studyId != null) {
			UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(link.studyId, userId);
			if (self == null)
				throw new AuthException("error.notauthorized.study", "User not member of study group");
			if (!self.role.maySetup())
				throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");
	        link.validationResearch = StudyValidationStatus.VALIDATED;
		} else if (role.equals(UserRole.ADMIN)) {
			link.validationResearch = StudyValidationStatus.VALIDATED;
			link.validationDeveloper = StudyValidationStatus.VALIDATED;
		} else throw new AuthException("error.notauthorized.action", "You are not authorized to do this action.");
        
        boolean autoValidDeveloper = true;
        boolean autoValidResearcher = true;
        
        if (link.type.contains(StudyAppLinkType.OFFER_P)) {
        	autoValidDeveloper = false;
        	autoValidResearcher = false;
        }
        if (link.type.contains(StudyAppLinkType.REQUIRE_P)) {
        	autoValidDeveloper = false;
        	autoValidResearcher = false;
        }
        if (link.type.contains(StudyAppLinkType.RECOMMEND_A)) {
        	autoValidResearcher = false;
        }
        if (link.type.contains(StudyAppLinkType.AUTOADD_A)) {
        	autoValidResearcher = false;
        }
        if (link.type.contains(StudyAppLinkType.DATALINK)) {
        	autoValidResearcher = false;
        }
        if (link.linkTargetType == LinkTargetType.ORGANIZATION) {
        	autoValidResearcher = true;
        }
        if (autoValidDeveloper) link.validationDeveloper = StudyValidationStatus.VALIDATED;
        if (autoValidResearcher) link.validationResearch = StudyValidationStatus.VALIDATED;
        
        if (link.linkTargetType == null || link.linkTargetType == LinkTargetType.STUDY) {
  		  Study study = Study.getById(link.studyId, Study.ALL);
  		  if (study == null) throw new BadRequestException("error.internal", "Unknown study");
  		  link.active = link.isConfirmed() && link.usePeriod.contains(study.executionStatus);
  		} else {
          link.active = link.isConfirmed();
  		}
	}
	
	@MobileCall
	public Result getOpenDebugCalls(String handle) throws AppException {
		AccessLog.log("Read open debug calls for handle: "+handle);
		List<TestPluginCall> calls = TestPluginCall.getForHandle(handle);
		
		//TestPluginCall.delete(handle);
		return ok(JsonOutput.toJson(calls, "TestPluginCall", TestPluginCall.ALL)).as("application/json");
	}
	
	@MobileCall
	@BodyParser.Of(BodyParser.Json.class)
	public Result answerDebugCall(String handle) throws AppException {
		AccessLog.log("Answer debug call handle="+handle);
		JsonNode json = request().body().asJson();
		String sender = JsonValidation.getJsonString(json, "returnPath");
		String content = JsonValidation.getJsonString(json, "content");
		int status = JsonValidation.getInteger(json, "status", -100, 10000);
		
		TestPluginCall call = TestPluginCall.getForHandleAndId(handle, MidataId.from(sender));		
		call.setAnswer(status, content);
				
		return ok();  
				
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public Result setSubscriptionDebug() throws AppException {
		JsonNode json = request().body().asJson();	
		JsonValidation.validate(json, "plugin", "action");
		
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId pluginId = JsonValidation.getMidataId(json, "plugin");
		String action = JsonValidation.getString(json, "action");
		
		Plugin plugin = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
	    if (plugin == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
		
		if (!getRole().equals(UserRole.ADMIN) && !userId.equals(plugin.creator)) throw new BadRequestException("error.not_authorized.not_plugin_owner", "Not your plugin!");
					
		if (plugin.debugHandle != null) {
			TestPluginCall.delete(plugin.debugHandle);
		}
		if (action.equals("start")) {
			if (!InstanceConfig.getInstance().getInstanceType().getDebugFunctionsAvailable()) {
				throw new BadRequestException("error.notauthorized.no_debug", "No debugging functions available on this instance.");
			}
			if (plugin.status != PluginStatus.DEVELOPMENT && plugin.status != PluginStatus.BETA) {
				throw new BadRequestException("error.notauthorized.no_debug", "Plugin does not have 'development/beta' status.");
			}
			
			plugin.debugHandle = CodeGenerator.nextUniqueCode();
		} else {
			plugin.debugHandle = null;
		}
		
		Plugin.set(plugin._id, "debugHandle", plugin.debugHandle);
		
		return ok(JsonOutput.toJson(plugin, "Plugin", Sets.create("debugHandle")));
	}
	
	public void markReviewObsolete(MidataId pluginId, AppReviewChecklist check) throws AppException {
		List<PluginReview> reviews = PluginReview.getReviews(pluginId, check);
		for (PluginReview review : reviews) review.markObsolete();
	}
	
	@APICall
	@Security.Authenticated(DeveloperSecured.class)	
	public Result getReviews(String pluginIdStr) throws AppException {
		MidataId pluginId = new MidataId(pluginIdStr);
        MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        
        Plugin app = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
        if (app == null) throw new BadRequestException("error.unknown.plugin", "Unknown plugin");
        if (!getRole().equals(UserRole.ADMIN) && (app.creator==null || !app.creator.equals(userId))) throw new BadRequestException("error.auth", "You are not owner of this plugin.");
   
		List<PluginReview> reviews = PluginReview.getReviews(pluginId);
		
		return ok(JsonOutput.toJson(reviews, "PluginReview", PluginReview.ALL)).as("application/json");
	}
	
	@APICall
	@Security.Authenticated(AdminSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result addReview() throws AppException {
		JsonNode json = request().body().asJson();	
		JsonValidation.validate(json, "pluginId", "check", "status");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		Admin me = Admin.getById(userId, Sets.create("email"));
		
		PluginReview review = new PluginReview();
		review._id = new MidataId();
		review.pluginId = JsonValidation.getMidataId(json, "pluginId");
		review.check = JsonValidation.getEnum(json, "check", AppReviewChecklist.class);
		review.timestamp = new Date(System.currentTimeMillis());
		review.userId = userId; 
		review.userLogin = me.email;
		review.comment = JsonValidation.getStringOrNull(json, "comment");
		review.status = JsonValidation.getEnum(json, "status", ReviewStatus.class);
		
		review.add();
		return ok();
	}
	
}
