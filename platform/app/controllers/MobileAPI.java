package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSONParseException;

import actions.MobileCall;
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
import models.Study;
import models.StudyAppLink;
import models.StudyParticipation;
import models.StudyRelated;
import models.User;
import models.enums.AggregationType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.LinkTargetType;
import models.enums.ParticipationStatus;
import models.enums.StudyAppLinkType;
import models.enums.UsageAction;
import models.enums.UserFeature;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.InstanceConfig;
import utils.LinkTools;
import utils.QueryTagTools;
import utils.access.AccessContext;
import utils.access.AppAccessContext;
import utils.access.EncryptionUtils;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.OAuthRefreshToken;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.auth.TokenCrypto;
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
import utils.stats.Stats;
import utils.stats.UsageStatsRecorder;

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
	public Result checkPreflight() {		
		return ok();
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
        	ApplicationTools.removeAppInstance(appInstance.owner, appInstance);
        	return false;
        }
        
        Set<StudyAppLink> links = StudyAppLink.getByApp(app._id);
        for (StudyAppLink sal : links) {
        	if (sal.isConfirmed() && sal.type.contains(StudyAppLinkType.REQUIRE_P) && sal.active) {
        		
        		   if (sal.linkTargetType == LinkTargetType.ORGANIZATION) {
        			  Consent c = LinkTools.findConsentForAppLink(appInstance.owner, sal);
        			  if (c == null) {
        				  ApplicationTools.removeAppInstance(appInstance.owner, appInstance);
		                  return false;
        			  }
        		   } else {
	        		   StudyParticipation sp = StudyParticipation.getByStudyAndMember(sal.studyId, appInstance.owner, Sets.create("status", "pstatus"));
	        		   
	        		   if (sp == null) {
		               		ApplicationTools.removeAppInstance(appInstance.owner, appInstance);
		                   	return false;
		               	}
		               	if ( 
		               		sp.pstatus.equals(ParticipationStatus.MEMBER_RETREATED) || 
		               		sp.pstatus.equals(ParticipationStatus.MEMBER_REJECTED)) {
		               		    throw new BadRequestException("error.blocked.projectconsent", "Research consent expired or blocked.");
						    }
						if (sp.pstatus.equals(ParticipationStatus.RESEARCH_REJECTED)) {
		               		    throw new BadRequestException("error.blocked.participation", "Research consent expired or blocked.");
		               	}
        		   }        		
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
	public Result authenticate() throws AppException {
				
        JsonNode json = request().body().asJson();		
		//JsonValidation.validate(json, "appname", "secret");
		if (!json.has("refreshToken")) JsonValidation.validate(json, "appname", "secret", "username", "password" /*, "device" */);
		
		//if (!app.type.equals("mobile") && !app.type.equals("service")) throw new InternalServerException("error.internal", "Wrong app type");
		// if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
	
		MidataId appInstanceId = null;
		MidataId executor = null;
		MobileAppInstance appInstance = null;
		String phrase = null;
		Map<String, Object> meta = null;
		UserRole role = null;
		
		Plugin app = null;
		boolean deprecated = false;
		KeyManager.instance.login(60000l, false);
		if (json.has("refreshToken")) {
			String refresh_token = JsonValidation.getString(json, "refreshToken");

			Pair<User, MobileAppInstance> pair = OAuth2.useRefreshToken(refresh_token);
			User user = pair.getLeft();
			appInstance = pair.getRight();
			role = user.role;

			/*
			OAuthRefreshToken refreshToken = OAuthRefreshToken.decrypt();
			
			if (refreshToken.created + MobileAPI.DEFAULT_REFRESHTOKEN_EXPIRATION_TIME < System.currentTimeMillis()) return OAuth2.invalidToken();
			appInstanceId = refreshToken.appInstanceId;

			appInstance = MobileAppInstance.getById(appInstanceId, Sets.create("owner", "applicationId", "status", "appVersion", "writes", "sharingQuery"));
			if (!verifyAppInstance(appInstance, refreshToken.ownerId, refreshToken.appId)) throw new BadRequestException("error.invalid.token", "Bad refresh token.");
			
			app = Plugin.getById(appInstance.applicationId, Sets.create("type", "name", "secret", "status", "targetUserRole"));
			if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");
			
			if (!app.type.equals("mobile") && !app.type.equals("analyzer") && !app.type.equals("external")) throw new InternalServerException("error.internal", "Wrong app type");
			
            if (!refreshToken.appId.equals(app._id)) throw new BadRequestException("error.invalid.token", "Bad refresh token.");
            User user = User.getById(appInstance.owner, User.ALL_USER_INTERNAL);
            if (user == null) return status(UNAUTHORIZED); 
            Set<UserFeature> req = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(user.role);
            if (app.requirements != null) req.addAll(app.requirements);
            if (Application.loginHelperPreconditionsFailed(user, req) != null) return status(UNAUTHORIZED); 
            role = user.role;
            phrase = refreshToken.phrase;
            if (KeyManager.instance.unlock(appInstance._id, phrase) == KeyManager.KEYPROTECTION_FAIL) return status(UNAUTHORIZED);
            executor = appInstance._id;
            meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
                        
            if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
            	return status(UNAUTHORIZED);
            }*/
            
            phrase = KeyManager.instance.newAESKey(appInstance._id);
            
            //UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.REFRESH);
		} else {
			deprecated = true;
			String name = JsonValidation.getString(json, "appname");
			String secret = JsonValidation.getString(json,"secret");
				
			app = Plugin.getByFilename(name, Sets.create("type", "name", "secret", "status", "targetUserRole"));
			if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");
			
			if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
			if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
			
			String username = JsonValidation.getEMail(json, "username");
			String password = JsonValidation.getString(json, "password");
			String device = JsonValidation.getStringOrNull(json, "device");
			if (device != null && device.length()<4) throw new BadRequestException("error.illegal.device", "Value for device is too short.");
			
			role = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;
			
			if (device != null) phrase = device; else phrase = "???"+password;
				
			if (app.type.equals("service")) phrase = "-----";
			
			User user = null;
			switch (role) {
			case MEMBER : user = Member.getByEmail(username, Sets.create(User.FOR_LOGIN,"apps","password","firstname","lastname","email","language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp", "failedLogins", "lastFailed","publicExtKey"));break;
			case PROVIDER : user = HPUser.getByEmail(username, Sets.create(User.FOR_LOGIN,"apps","password","firstname","lastname","email","language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp", "failedLogins", "lastFailed","publicExtKey"));break;
			case RESEARCH: user = ResearchUser.getByEmail(username, Sets.create(User.FOR_LOGIN,"apps","password","firstname","lastname","email","language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp", "failedLogins", "lastFailed","publicExtKey"));break;
			}
			if (user == null) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
			
			
			
			AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user, app._id);
			
						
			if (!user.authenticationValid(password) && !user.authenticationValid(TokenCrypto.sha512(password))) {
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
				appInstance = ApplicationTools.installApp(executor, app._id, user, phrase, autoConfirm, Collections.emptySet());
				KeyManager.instance.changePassphrase(appInstance._id, phrase);
				if (executor != null) RecordManager.instance.clearCache();
				executor = appInstance._id;
	   		    meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			} else {
				
				if (KeyManager.instance.unlock(appInstance._id, phrase) == KeyManager.KEYPROTECTION_FAIL) return status(UNAUTHORIZED);		
				
				executor = appInstance._id;
				meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();			
            }
			UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.LOGIN);							
			role = user.role;
			
			if (app.targetUserRole.equals(UserRole.RESEARCH)) {						
				BSONObject q = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_query");
				if (!q.containsField("link-study")) throw new BadRequestException("error.invalid.study", "The research app is not properly linked to a study! Please log in as researcher and link the app properly.");
			}
			//phrase = KeyManager.instance.newAESKey(appInstance._id);	
			
			//throw new InternalServerException("error.notimplemented", "This feature is not implemented.");
		}
				
		//if (!phrase.equals(meta.get("phrase"))) return internalServerError("Internal error while validating consent");
				
		
		
		AuditManager.instance.success();
		
		return authResult(executor, role, appInstance, phrase, deprecated);
	}
	
	/**
	 * Returns result for authentication request. 
	 * @param appInstance instance of app to login
	 * @param meta _app meta object from app instance
	 * @param phrase app password
	 * @return
	 * @throws AppException
	 */
	public static Result authResult(MidataId executor, UserRole role, MobileAppInstance appInstance, String phrase, boolean deprecated) throws AppException {
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis() + MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME, role); 
        OAuthRefreshToken refresh = OAuth2.createRefreshToken(executor, appInstance, phrase);
		        
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
		if (deprecated) obj.put("warning", "You are using a deprecated API! Please use OAuth login instead!");
															
		return ok(obj);
	}

	protected static MidataId prepareMobileExecutor(MobileAppInstance appInstance, MobileAppSessionToken tk) throws AppException {
		KeyManager.instance.login(1000l*60l, false);
		if (KeyManager.instance.unlock(tk.appInstanceId, tk.aeskey) == KeyManager.KEYPROTECTION_FAIL) { OAuth2.invalidToken(); }
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
	
	/**
	 * retrieve records current mobile app has access to matching some criteria
	 * @return list of Records
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result getRecords() throws JsonValidationException, AppException {		
		// validate json
		Stats.startRequest(request());
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), true);		
		if (inf == null) OAuth2.invalidToken(); 
							
        Stats.setPlugin(inf.pluginId);
        UsageStatsRecorder.protokoll(inf.pluginId, UsageAction.GET);
        
        if (!((AppAccessContext) inf.context).getAppInstance().status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Record", fields)).as("application/json");
        }
                
		// get record data
		Collection<Record> records = null;
		
		AccessLog.log("NEW QUERY");		
		records = RecordManager.instance.list(inf.executorId, inf.role, inf.context, properties, fields);		  
				
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
	@MobileCall	
	public Result getFile() throws AppException, JsonValidationException {
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
		
		return getFile(info, recordId, false);
	}
	
	public static Result getFile(ExecutionInfo info, MidataId recordId, boolean asAttachment) throws AppException {					
		FileData fileData = RecordManager.instance.fetchFile(info.executorId, new RecordToken(recordId.toString(), info.targetAPS.toString()));
		if (fileData == null) return badRequest();
		String contentType = "application/binary";
		if (fileData.contentType != null) contentType = fileData.contentType;
		if (contentType.startsWith("data:")) contentType = "application/binary";
		if (asAttachment) PluginsAPI.setAttachmentContentDisposition(fileData.filename);
		
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
	public Result createRecord() throws AppException, JsonValidationException {
		Stats.startRequest(request());
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		UsageStatsRecorder.protokoll(inf.pluginId, UsageAction.POST);	
		
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
		
			
		ContentInfo.setRecordCodeAndContent(inf.pluginId, record, code, content);
				
		try {
			record.data = BasicDBObject.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		} catch (ClassCastException e2) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
								
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
	public Result updateRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "_id", "version");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		UsageStatsRecorder.protokoll(inf.pluginId, UsageAction.PUT);
		
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
	}*/
	
	@MobileCall	
	@BodyParser.Of(BodyParser.Json.class)
    public Result unshareRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "target-study", "target-study-group");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
													
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");	
		
		unshareRecord(inf, studyId, group, properties);
									
		return ok();
	}
    
    public static int unshareRecord(ExecutionInfo inf, MidataId studyId, String group, Map<String, Object> properties) throws AppException, JsonValidationException {
				
		if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");		
		Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));		
		List<Record> recs = RecordManager.instance.list(inf.executorId, inf.role, inf.context, properties, Sets.create("_id"));
		
		if (!srs.isEmpty()) {
			for (StudyRelated sr : srs ) {
			  RecordManager.instance.unshare(inf.executorId, sr._id, recs);
			}
		}																		
		
		return recs.size();
	}

    @MobileCall	
    @BodyParser.Of(BodyParser.Json.class)
    public Result shareRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request());
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "target-study", "target-study-group");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText(), false);
		Stats.setPlugin(inf.pluginId);	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
												
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");		
	
		shareRecord(inf, studyId, group, properties);
																	
		return ok();
	}
	
    public static int shareRecord(ExecutionInfo inf, MidataId studyId, String group, Map<String, Object> properties) throws AppException, JsonValidationException {
										
		if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");
							
		Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));
		
		List<Record> recs = RecordManager.instance.list(inf.executorId, inf.role, inf.context, properties, Sets.create("_id"));
		
		if (!recs.isEmpty()) {
			
			if (srs.isEmpty()) {				
				Study study = Study.getById(studyId, Study.ALL);
				Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyId, group, Sets.create());
				controllers.research.Studies.joinSharing(inf.executorId, inf.executorId, study, group, false, new ArrayList<StudyParticipation>(parts));
				srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));
			}
			
			for (StudyRelated sr : srs ) {
			  RecordManager.instance.share(inf.executorId, inf.executorId, sr._id, sr.owner, properties, false);
			}
		}								
								
		return recs.size();
	}
			
	
	
	/**
	 * retrieve aggregated information about records matching some criteria
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result getInfo() throws AppException, JsonValidationException {
 	
		// check whether the request is complete
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "authToken", "properties", "summarize");
		
		// decrypt authToken 
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) OAuth2.invalidToken(); 
	
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare", "status"));
        if (appInstance == null) OAuth2.invalidToken(); 

        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(Json.toJson(Collections.EMPTY_LIST));
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
        		
		MidataId targetAps = appInstance._id;
				
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);
		
	    Collection<RecordsInfo> result = RecordManager.instance.info(executor, authToken.role, targetAps, RecordManager.instance.createContextFromApp(executor, appInstance), properties, aggrType);
						
		return ok(Json.toJson(result));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result getConsents() throws JsonValidationException, AppException {		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		//Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		//ObjectIdConversion.convertMidataIds(properties, "_id");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		//Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) OAuth2.invalidToken(); 
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "status"));
        if (appInstance == null) OAuth2.invalidToken(); 
		
        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Consent", fields)).as("application/json");
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
		// get record data
		Collection<Consent> consents = Consent.getAllActiveByAuthorized(executor);
		
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
		
		
		return ok(JsonOutput.toJson(consents, "Consent", fields)).as("application/json");
	}
		
}
