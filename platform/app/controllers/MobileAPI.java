package controllers;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSONParseException;

import actions.MobileCall;
import controllers.members.HealthProvider;
import models.Consent;
import models.ContentInfo;
import models.HPUser;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.ResearchUser;
import models.Space;
import models.StudyParticipation;
import models.User;
import models.enums.AggregationType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.MessageReason;
import models.enums.ParticipationStatus;
import models.enums.UserFeature;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.access.AccessContext;
import utils.access.AppAccessContext;
import utils.access.Feature_FormatGroups;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.MobileAppToken;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage.FileData;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.stats.Stats;

/**
 * functions for mobile APPs
 *
 */
public class MobileAPI extends Controller {

	
	public final static long DEFAULT_ACCESSTOKEN_EXPIRATION_TIME = 1000l * 60l * 60l * 6l;
	public final static long DEFAULT_REFRESHTOKEN_EXPIRATION_TIME = 1000l * 60l * 60l * 24l * 31l;
	
	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@MobileCall
	public static Result checkPreflight() {		
		return ok();
	}

	/**
	 * accessToken expired result
	 * @return
	 * @throws BadRequestException
	 */
	public static Result invalidToken() throws BadRequestException {
		throw new BadRequestException("error.invalid.token", "Invalid or expired authToken.", Http.Status.UNAUTHORIZED);
	}
		
		
	
	private static boolean verifyAppInstance(MobileAppInstance appInstance, MidataId ownerId, MidataId applicationId) throws AppException {
		if (appInstance == null) return false;
        if (!appInstance.owner.equals(ownerId)) return false;
        if (!appInstance.applicationId.equals(applicationId)) return false;
        
        if (appInstance.status.equals(ConsentStatus.REJECTED)) throw new BadRequestException("error.invalid.token", "Rejected");        
        if (appInstance.status.equals(ConsentStatus.EXPIRED)) return false;
        
        Plugin app = Plugin.getById(appInstance.applicationId);
        
        AccessLog.log("app-instance:"+appInstance.appVersion+" vs plugin:"+app.pluginVersion);
        
        if (appInstance.appVersion != app.pluginVersion) {      
        	MobileAPI.removeAppInstance(appInstance);
        	return false;
        }
        
        if (app.mustParticipateInStudy && app.linkedStudy != null) {
        	StudyParticipation sp = StudyParticipation.getByStudyAndMember(app.linkedStudy, appInstance.owner, Sets.create("status", "pstatus"));
        	if (sp == null) {
        		MobileAPI.removeAppInstance(appInstance);
            	return false;
        	}
        	if ( 
        		sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
        		sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED) || 
        		sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
        		throw new BadRequestException("error.blocked.consent", "Research consent expired or blocked.");
        	}
        }
        
        return true;
	}
	
	public static MobileAppInstance getAppInstance(String phrase, MidataId applicationId, MidataId owner, Set<String> fields) throws AppException {
		Set<MobileAppInstance> candidates = MobileAppInstance.getByApplicationAndOwner(applicationId, owner, fields);
		AccessLog.log("CS:"+candidates.size());
		if (candidates.isEmpty()) return null;
		if (candidates.size() >= 10) throw new BadRequestException("error.blocked.app", "Maximum number of consents reached for this app. Please cleanup using the MIDATA portal.");
		for (MobileAppInstance instance : candidates) {
		  if (User.phraseValid(phrase, instance.passcode)) return instance;
		}
		AccessLog.log("CS:fail");
		return null;
	}
	/**
	 * Authentication function for mobile apps
	 * @return json with authToken
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result authenticate() throws AppException {
				
        JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "appname", "secret");
		if (!json.has("refreshToken")) JsonValidation.validate(json, "username", "password" /*, "device" */);
					
		String name = JsonValidation.getString(json, "appname");
		String secret = JsonValidation.getString(json,"secret");
				
	    // Validate Mobile App	
		Plugin app = Plugin.getByFilename(name, Sets.create("type", "name", "secret", "status", "targetUserRole"));
		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");
		
		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
		if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
		
	
		MidataId appInstanceId = null;
		MidataId executor = null;
		MobileAppInstance appInstance = null;
		String phrase;
		Map<String, Object> meta = null;
		
		KeyManager.instance.login(60000l, false);
		if (json.has("refreshToken")) {
			MobileAppToken refreshToken = MobileAppToken.decrypt(JsonValidation.getString(json, "refreshToken"));
			if (refreshToken.created + MobileAPI.DEFAULT_REFRESHTOKEN_EXPIRATION_TIME < System.currentTimeMillis()) return MobileAPI.invalidToken();
			appInstanceId = refreshToken.appInstanceId;
			
			appInstance = MobileAppInstance.getById(appInstanceId, Sets.create("owner", "applicationId", "status", "appVersion", "writes", "sharingQuery"));
			if (!verifyAppInstance(appInstance, refreshToken.ownerId, refreshToken.appId)) throw new BadRequestException("error.invalid.token", "Bad refresh token.");            
            if (!refreshToken.appId.equals(app._id)) throw new BadRequestException("error.invalid.token", "Bad refresh token.");
            User user = User.getById(appInstance.owner, User.ALL_USER_INTERNAL);
            Set<UserFeature> req = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(user.role);
            if (app.requirements != null) req.addAll(app.requirements);
            if (Application.loginHelperPreconditionsFailed(user, req) != null) return status(UNAUTHORIZED); 
            
            phrase = refreshToken.phrase;
            KeyManager.instance.unlock(appInstance._id, phrase);
            executor = appInstance._id;
            meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
                        
            if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
            	return status(UNAUTHORIZED);
            }
		} else {
			String username = JsonValidation.getEMail(json, "username");
			String password = JsonValidation.getString(json, "password");
			String device = JsonValidation.getStringOrNull(json, "device");
			UserRole role = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;
			
			if (device != null) phrase = device; else phrase = "???"+password;
				
			User user = null;
			switch (role) {
			case MEMBER : user = Member.getByEmail(username, Sets.create("apps","password","firstname","lastname","email","language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp", "failedLogins", "lastFailed"));break;
			case PROVIDER : user = HPUser.getByEmail(username, Sets.create("apps","password","firstname","lastname","email","language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp", "failedLogins", "lastFailed"));break;
			case RESEARCH: user = ResearchUser.getByEmail(username, Sets.create("apps","password","firstname","lastname","email","language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp", "failedLogins", "lastFailed"));break;
			}
			if (user == null) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
			
			if (!user.authenticationValid(password)) {
				throw new BadRequestException("error.invalid.credentials",  "Unknown user or bad password");
			}
			Set<UserFeature> req = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(user.role);
			if (app.requirements != null) req.addAll(app.requirements);
			if (Application.loginHelperPreconditionsFailed(user, req)!=null) throw new BadRequestException("error.invalid.credentials",  "Login preconditions failed.");
			
			appInstance= getAppInstance(phrase, app._id, user._id, Sets.create("owner", "applicationId", "status", "passcode", "appVersion"));			
			if (appInstance != null && !verifyAppInstance(appInstance, user._id, app._id)) {
				AccessLog.log("CSCLEAR");
				appInstance = null;
				RecordManager.instance.clearCache();
			}
						
			if (appInstance == null) {									
				boolean autoConfirm = InstanceConfig.getInstance().getInstanceType().autoconfirmConsentsMidataApi() && KeyManager.instance.unlock(user._id, null) == KeyManager.KEYPROTECTION_NONE;
				executor = autoConfirm ? user._id : null;
				AccessLog.log("REINSTALL");
				if (!autoConfirm && app.targetUserRole.equals(UserRole.RESEARCH)) throw new BadRequestException("error.invalid.study", "The research app is not properly linked to a study! Please log in as researcher and link the app properly.");
				appInstance = installApp(executor, app._id, user, phrase, autoConfirm, false);
				if (executor != null) RecordManager.instance.clearCache();
				executor = appInstance._id;
	   		    meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			} else {
				
				
				KeyManager.instance.unlock(appInstance._id, phrase);
				executor = appInstance._id;
				meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
				
			}
		}
				
		if (!phrase.equals(meta.get("phrase"))) return internalServerError("Internal error while validating consent");
				
		if (app.targetUserRole.equals(UserRole.RESEARCH)) {
		  BSONObject q = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_query");
          if (!q.containsField("link-study")) throw new BadRequestException("error.invalid.study", "The research app is not properly linked to a study! Please log in as researcher and link the app properly.");
		}
		
		
		return authResult(executor, appInstance, meta, phrase);
	}
	
	public static void removeAppInstance(MobileAppInstance appInstance) throws AppException {
		AccessLog.logBegin("start remove app instance");
		// Device or password changed, regenerates consent				
		Circles.consentStatusChange(appInstance.owner, appInstance, ConsentStatus.EXPIRED);
		RecordManager.instance.deleteAPS(appInstance._id, appInstance.owner);									
		Circles.removeQueries(appInstance.owner, appInstance._id);										
		MobileAppInstance.delete(appInstance.owner, appInstance._id);
		AccessLog.logEnd("end remove app instance");
	}
	
	/**
	 * Returns result for authentication request. 
	 * @param appInstance instance of app to login
	 * @param meta _app meta object from app instance
	 * @param phrase app password
	 * @return
	 * @throws AppException
	 */
	public static Result authResult(MidataId executor, MobileAppInstance appInstance, Map<String, Object> meta, String phrase) throws AppException {
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis() + MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME); 
        MobileAppToken refresh = new MobileAppToken(appInstance.applicationId, appInstance._id, appInstance.owner, phrase, System.currentTimeMillis());
		
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(executor, appInstance._id, "_app", meta);
        
        BSONObject q = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_query");
        if (q.containsField("link-study")) {
        	MidataId studyId = MidataId.from(q.get("link-study"));
        	MobileAPI.prepareMobileExecutor(appInstance, session);
        	controllers.research.Studies.autoApproveCheck(appInstance.applicationId, studyId, appInstance.owner);
        }
        
		// create encrypted authToken		
		ObjectNode obj = Json.newObject();								
		obj.put("authToken", session.encrypt());
		obj.put("refreshToken", refresh.encrypt());
		obj.put("status", appInstance.status.toString());
		obj.put("owner", appInstance.owner.toString());
															
		return ok(obj);
	}
	
	public static MobileAppInstance installApp(MidataId executor, MidataId appId, User member, String phrase, boolean autoConfirm, boolean studyConfirm) throws AppException {
		Plugin app = Plugin.getById(appId, Sets.create("name", "pluginVersion", "defaultQuery", "predefinedMessages", "linkedStudy", "mustParticipateInStudy", "termsOfUse", "writes"));

		if (app.linkedStudy != null && app.mustParticipateInStudy && !studyConfirm) {
			StudyParticipation sp = StudyParticipation.getByStudyAndMember(app.linkedStudy, member._id, Sets.create("status", "pstatus"));
        	if (sp == null || 
        		sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
        		sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED) || 
        		sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
        		throw new BadRequestException("error.missing.study_accept", "Study belonging to app must be accepted.");        		
        	}
			
		}
		
		if (app.linkedStudy != null && studyConfirm) {			
			controllers.members.Studies.precheckRequestParticipation(member._id, app.linkedStudy);
		}
		
		
		MobileAppInstance appInstance = new MobileAppInstance();
		appInstance._id = new MidataId();
		appInstance.owner = member._id;
		appInstance.name = "App: "+ app.name+" (Device: "+phrase.substring(0, 3)+")";
		appInstance.applicationId = app._id;	
		appInstance.appVersion = app.pluginVersion;
		
		AuditManager.instance.addAuditEvent(AuditEventType.APP_FIRST_USE, app._id, member, null, appInstance, null, null);
		
        appInstance.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(appInstance._id, phrase);    	
    	appInstance.passcode = Member.encrypt(phrase); 
    	appInstance.dateOfCreation = new Date();
    	appInstance.writes = app.writes;
		
    	if (app.defaultQuery != null && !app.defaultQuery.isEmpty()) {
			
		    Feature_FormatGroups.convertQueryToContents(app.defaultQuery);
		    
		    appInstance.sharingQuery = app.defaultQuery;						   
		}
    	
    	
    	MobileAppInstance.add(appInstance);	
		KeyManager.instance.unlock(appInstance._id, phrase);	   		    
		RecordManager.instance.createAnonymizedAPS(member._id, appInstance._id, appInstance._id, true);
		
		
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("phrase", phrase);
		if (executor == null) executor = appInstance._id;
		RecordManager.instance.setMeta(executor, appInstance._id, "_app", meta);
		
		if (app.defaultQuery != null && !app.defaultQuery.isEmpty()) {			
		    RecordManager.instance.shareByQuery(executor, member._id, appInstance._id, app.defaultQuery);
		}
				
		if (app.linkedStudy != null && studyConfirm) {								
			controllers.members.Studies.requestParticipation(new ExecutionInfo(executor), member._id, app.linkedStudy, app._id);
		}
		
		if (autoConfirm) {
		   HealthProvider.confirmConsent(appInstance.owner, appInstance._id);
		   appInstance.status = ConsentStatus.ACTIVE;
		}
		
		if (!member.apps.contains(app._id)) {
			member.apps.add(app._id);
			User.set(member._id, "apps", member.apps);
		}
		
		if (app.termsOfUse != null) member.agreedToTerms(app.termsOfUse, app._id);
				
		if (app.predefinedMessages!=null) {
			if (!app._id.equals(member.initialApp)) {
				Messager.sendMessage(app._id, MessageReason.FIRSTUSE_EXISTINGUSER, null, Collections.singleton(member._id), member.language, new HashMap<String, String>());	
			} 
			Messager.sendMessage(app._id, MessageReason.FIRSTUSE_ANYUSER, null, Collections.singleton(member._id), member.language, new HashMap<String, String>());								
		}
			
		AuditManager.instance.success();
		return appInstance;
	}
	
	protected static MidataId prepareMobileExecutor(MobileAppInstance appInstance, MobileAppSessionToken tk) throws AppException {
		KeyManager.instance.login(1000l*60l, false);
		KeyManager.instance.unlock(tk.appInstanceId, tk.passphrase);
		Map<String, Object> appobj = RecordManager.instance.getMeta(tk.appInstanceId, tk.appInstanceId, "_app").toMap();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			MidataId alias = new MidataId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("aliaskey");
			KeyManager.instance.unlock(appInstance.owner, alias, key);		
			RecordManager.instance.clearCache();
			return appInstance.owner;
		} else {
			RecordManager.instance.setAccountOwner(appInstance._id, appInstance.owner);
		}
		return tk.appInstanceId;
	}
	
	public static void confirmMobileConsent(MidataId userId, MidataId consentId) throws AppException {
		BSONObject meta = RecordManager.instance.getMeta(userId, consentId, "_app");
		if (meta == null) throw new InternalServerException("error.internal", "_app object not found,");
		MidataId alias = new MidataId();
		byte[] key = KeyManager.instance.generateAlias(userId, alias);
		meta.put("alias", alias.toString());
		meta.put("aliaskey", key);
		RecordManager.instance.setMeta(userId, consentId, "_app", meta.toMap());
	}

	/**
	 * retrieve records current mobile app has access to matching some criteria
	 * @return list of Records
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result getRecords() throws JsonValidationException, AppException {		
		// validate json
		Stats.startRequest(request());
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), true);		
		if (inf == null) return invalidToken(); 
							
        Stats.setPlugin(inf.pluginId);
        
        if (!((AppAccessContext) inf.context).getAppInstance().status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Record", fields)).as("application/json");
        }
                
		// get record data
		Collection<Record> records = null;
		
		AccessLog.log("NEW QUERY");
					
		records = RecordManager.instance.list(inf.executorId, inf.context, properties, fields);		  
				
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		
		Stats.finishRequest(request(), "200", properties.keySet());
		return ok(JsonOutput.toJson(records, "Record", fields)).as("application/json");
	}
	
	/**
	 * retrieve a file record current mobile app has access to
	 * @return file
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall	
	public static Result getFile() throws AppException, JsonValidationException {
		Stats.startRequest(request());
		// validate json
		JsonNode json = request().body().asJson();				
						
		ExecutionInfo info = null;
		
		Optional<String> param = request().header("Authorization");
		String param2 = request().getQueryString("access_token");
		
		if (param.isPresent() && param.get().startsWith("Bearer ")) {
          info = ExecutionInfo.checkToken(request(), param.get().substring("Bearer ".length()), false);                  
		} else if (json != null && json.has("authToken")) {
		  info = ExecutionInfo.checkToken(request(), JsonValidation.getString(json, "authToken"), false);
		} else if (param2 != null) {
		  info = ExecutionInfo.checkToken(request(), param2, false);
		} else throw new BadRequestException("error.auth", "Please provide authorization token as 'Authorization' header or 'authToken' request parameter.");
					
		MidataId recordId = json != null ? JsonValidation.getMidataId(json, "_id") : new MidataId(request().getQueryString("_id"));			
		FileData fileData = RecordManager.instance.fetchFile(info.executorId, new RecordToken(recordId.toString(), info.targetAPS.toString()));
		if (fileData == null) return badRequest();
		String contentType = "application/binary";
		if (fileData.contentType != null) contentType = fileData.contentType;
		//response().setHeader("Content-Disposition", "attachment; filename=" + fileData.filename);
		
		Stats.finishRequest(request(), "200", Collections.EMPTY_SET);
		return ok(fileData.inputStream).as(contentType);
	}
	
	/**
	 * create a new record. The current space will automatically have access to it.
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result createRecord() throws AppException, JsonValidationException {
		Stats.startRequest(request());
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
					
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		if (format==null) format = "application/json";
		String content = JsonValidation.getStringOrNull(json, "content");
		Set<String> code = JsonExtraction.extractStringSet(json.get("code"));
		
		MidataId owner = inf.ownerId;
		MidataId ownerOverride = JsonValidation.getMidataId(json, "owner");
		if (ownerOverride != null && !ownerOverride.equals(owner)) {			
			owner = ownerOverride;
		}
				
		Record record = new Record();
		record._id = new MidataId();
		record.app = inf.pluginId;
		record.owner = owner;
		record.creator = inf.ownerId;
		record.created = record._id.getCreationDate();
		
		/*if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}*/
		
		record.format = format;
		
			
		ContentInfo.setRecordCodeAndContent(record, code, content);
				
		try {
			record.data = BasicDBObject.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		} catch (ClassCastException e2) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
						
		autoLearnAccessQuery(inf, record.format, record.content);
		PluginsAPI.createRecord(inf, record, null, null,null, inf.context);
		
		Stats.finishRequest(request(), "200", Collections.EMPTY_SET);
		ObjectNode obj = Json.newObject();		
		obj.put("_id", record._id.toString());		
		return ok(JsonOutput.toJson(record, "Record", Sets.create("_id", "created", "version"))).as("application/json");
	}
	
	/**
	 * update a record.
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result updateRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "_id", "version");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		
        String data = JsonValidation.getJsonString(json, "data");
						
		Record record = new Record();
		
		record._id = JsonValidation.getMidataId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 				
		record.creator = inf.executorId;
		record.lastUpdated = new Date();		
							
		try {
			record.data = BasicDBObject.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
				
		String version = PluginsAPI.updateRecord(inf, record);
		
		Stats.finishRequest(request(), "200", Collections.EMPTY_SET);
		
		ObjectNode obj = Json.newObject();		
		obj.put("version", version);		
		return ok(obj);
	}
	
	/*
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result deleteRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
				
		String message = "";
		for (Map.Entry<String, Object> prop : properties.entrySet()) {				
			message += (message.length()>0?"&":"")+prop.getKey()+"="+prop.getValue();			
		}
		
		if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");
		
		AuditManager.instance.addAuditEvent(AuditEventType.DATA_DELETION, null, inf.executorId, null, message);
		RecordManager.instance.delete(inf.executorId,  properties);		
				
		Stats.finishRequest(request(), "200", Collections.EMPTY_SET);						
		return ok();
	}
	
    public static Result unshareRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "target-study", "target-study-group");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
								
		if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");
		
		
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");		
		Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));
		
		List<Record> recs = RecordManager.instance.list(inf.executorId, inf.targetAPS, properties, Sets.create("_id"));
		
		if (!srs.isEmpty()) {
			for (StudyRelated sr : srs ) {
			  RecordManager.instance.unshare(inf.executorId, sr._id, recs);
			}
		}								
								
		return ok();
	}
	
    
    public static Result shareRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "target-study", "target-study-group");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
								
		if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");
		
		
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");		
		Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));
		
		List<Record> recs = RecordManager.instance.list(inf.executorId, inf.targetAPS, properties, Sets.create("_id"));
		
		if (!srs.isEmpty()) {
			for (StudyRelated sr : srs ) {
			  RecordManager.instance.share(inf.executorId, inf.executorId, sr._id, sr.owner, properties, false);
			}
		}								
								
		return ok();
	}
	*/
			
	
	
	/**
	 * retrieve aggregated information about records matching some criteria
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result getInfo() throws AppException, JsonValidationException {
 	
		// check whether the request is complete
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "authToken", "properties", "summarize");
		
		// decrypt authToken 
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) return invalidToken(); 
	
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare", "status"));
        if (appInstance == null) return invalidToken(); 

        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(Json.toJson(Collections.EMPTY_LIST));
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
        		
		MidataId targetAps = appInstance._id;
				
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);
		
	    Collection<RecordsInfo> result = RecordManager.instance.info(executor, targetAps, RecordManager.instance.createContextFromApp(executor, appInstance), properties, aggrType);
						
		return ok(Json.toJson(result));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result getConsents() throws JsonValidationException, AppException {		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		//Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		//ObjectIdConversion.convertMidataIds(properties, "_id");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		//Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) return invalidToken(); 
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "status"));
        if (appInstance == null) return invalidToken(); 
		
        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Consent", fields)).as("application/json");
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
		// get record data
		Collection<Consent> consents = Consent.getAllActiveByAuthorized(executor);
		
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
		
		
		return ok(JsonOutput.toJson(consents, "Consent", fields)).as("application/json");
	}
	
	public static void autoLearnAccessQuery(ExecutionInfo info, String format, String content) throws AppException {
		if (! InstanceConfig.getInstance().getInstanceType().allowQueryLearning()) return;
		
		AccessContext context = info.context;
		if (context instanceof AppAccessContext) {
			AppAccessContext appcontext = (AppAccessContext) context;
			if (!appcontext.getAppInstance().sharingQuery.containsKey("learn")) return;
			
			if (!context.mayAccess(null, format)) {
				addToSharingQuery(info, appcontext.getAppInstance(), format, null);
			}
    	
			if (!context.mayAccess(content, null)) {
				addToSharingQuery(info, appcontext.getAppInstance(), null, content);
			}
			            
		}		
	}
	
	private static void addToSharingQuery(ExecutionInfo info, MobileAppInstance instance, String format, String content) throws AppException {
		Plugin plugin = Plugin.getById(instance.applicationId, Plugin.ALL_DEVELOPER);
		if (!plugin.defaultQuery.containsKey("learn")) {
			return;
		}
		if (format != null && plugin.defaultQuery.containsKey("format")) {
			
			((Collection) plugin.defaultQuery.get("format")).add(format);					
			
			if (!instance.sharingQuery.containsKey("format")) {
				instance.sharingQuery.put("format", Collections.singleton(format));
			} else {
				((Collection) instance.sharingQuery.get("format")).add(format);		
			}
		}
		if (content != null && plugin.defaultQuery.containsKey("content")) {
			
			((Collection) plugin.defaultQuery.get("content")).add(content);					
			
			if (!instance.sharingQuery.containsKey("content")) {
				instance.sharingQuery.put("content", Collections.singleton(content));
			} else {
				((Collection) instance.sharingQuery.get("content")).add(content);		
			}
		}
		try {
		  plugin.pluginVersion = System.currentTimeMillis();
		  plugin.update();
		} catch (LostUpdateException e) {}
		RecordManager.instance.shareByQuery(info.executorId, info.ownerId, info.targetAPS, instance.sharingQuery);
		instance.appVersion = plugin.pluginVersion;
		MobileAppInstance.set(instance._id, "appVersion", instance.appVersion);
	}
	
	private static void addToSharingQuery(ExecutionInfo info, Space instance, String format, String content) throws AppException {
		Plugin plugin = Plugin.getById(instance.visualization, Plugin.ALL_DEVELOPER);
		if (!plugin.defaultQuery.containsKey("learn")) {
			return;
		}
		if (format != null && plugin.defaultQuery.containsKey("format")) {			
			((Collection) plugin.defaultQuery.get("format")).add(format);											
		}
		if (content != null && plugin.defaultQuery.containsKey("content")) {			
			((Collection) plugin.defaultQuery.get("content")).add(content);								
		}
		try {
		  plugin.pluginVersion = System.currentTimeMillis();
		  plugin.update();
		} catch (LostUpdateException e) {}
		RecordManager.instance.shareByQuery(info.executorId, info.ownerId, info.targetAPS, plugin.defaultQuery);		
	}
}
