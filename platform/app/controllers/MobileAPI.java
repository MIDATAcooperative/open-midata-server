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
import models.Study;
import models.StudyParticipation;
import models.StudyRelated;
import models.User;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.UsageAction;
import models.enums.UserFeature;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.InstanceConfig;
import utils.PluginLoginCache;
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
import utils.context.AccessContext;
import utils.context.AppAccessContext;
import utils.context.ContextManager;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
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
	public final static long DEFAULT_IMAGE_EXPIRATION_TIME = 1000l * 30l;
	
	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@MobileCall
	public Result checkPreflight() {		
		return ok();
	}

	public static MobileAppInstance getAppInstance(AccessContext tempContext, String phrase, MidataId applicationId, MidataId owner, Set<String> fields) throws AppException {
		Set<MobileAppInstance> candidates = MobileAppInstance.getByApplicationAndOwner(applicationId, owner, fields);
		AccessLog.log("getAppInstance size="+candidates.size());
		if (candidates.isEmpty()) return null;
		if (candidates.size() >= 10) {
			if (InstanceConfig.getInstance().getInstanceType().getDebugFunctionsAvailable()) {
				for (MobileAppInstance mai : candidates) {
					ApplicationTools.removeAppInstance(tempContext, owner, mai);
				}
				throw new BadRequestException("error.blocked.app_test", "Maximum number of consents reached for this app. Please cleanup using the MIDATA portal.");
			} else 
			throw new BadRequestException("error.blocked.app", "Maximum number of consents reached for this app. Please cleanup using the MIDATA portal.");
		}
		String deviceId = phrase.substring(0,3);
		
		for (MobileAppInstance instance : candidates) {
			if (deviceId.equals(instance.deviceId)) {
				try {
					Map<String, Object> meta = RecordManager.instance.getMeta(tempContext, instance._id, "_app").toMap();				
					if (phrase.equals(meta.get("phrase"))) return instance; 
				} catch (AppException e) {}						
			}
		}
						
		boolean cleanJunk = candidates.size() > 2;
		
		// Upgrade old entries
		for (MobileAppInstance instance : candidates) {
			if (instance.deviceId==null) {
				if (User.phraseValid(phrase, instance.passcode)) {		    	  		    	  
					instance.deviceId = deviceId;
					MobileAppInstance.set(instance._id,"deviceId",instance.deviceId);
					AccessLog.log("getAppInstance: Set missing device id");
					return instance;
				} else if (cleanJunk) {
					ApplicationTools.removeAppInstance(tempContext, owner, instance);
				}
			}
		}
		
		AccessLog.log("getAppInstance: fail");
		return null;
	}
	/**
	 * Authentication function for mobile apps
	 * @return json with authToken
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result authenticate(Request request) throws AppException {
				
        JsonNode json = request.body().asJson();		
		//JsonValidation.validate(json, "appname", "secret");
		if (!json.has("refreshToken")) JsonValidation.validate(json, "appname", "secret", "username", "password" /*, "device" */);
		
		//if (!app.type.equals("mobile") && !app.type.equals("service")) throw new InternalServerException("error.internal", "Wrong app type");
		// if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
	
		MidataId appInstanceId = null;
		//MidataId executor = null;
		MobileAppInstance appInstance = null;
		AccessContext tempContext = null;
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

			            
            phrase = KeyManager.instance.newAESKey(appInstance._id);
            tempContext = ContextManager.instance.createLoginOnlyContext(appInstance._id, user.role, appInstance);
            
		} else {
			deprecated = true;
			String name = JsonValidation.getString(json, "appname");
			String secret = JsonValidation.getString(json,"secret");
				
			app = PluginLoginCache.getByFilename(name);
			if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");
			
			if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
			if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
			
			Stats.startRequest(request);
			Stats.setPlugin(app._id);
			Stats.addComment("Old authentication API no longer supported!");
			throw new PluginException(app._id, "error.unsupported", "Old authentication API no longer supported!");
			
			/*
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
			tempContext = ContextManager.instance.createLoginOnlyContext(user._id, role);
			
			appInstance= getAppInstance(tempContext, phrase, app._id, user._id, MobileAppInstance.APPINSTANCE_ALL);
			
			
			if (appInstance != null && !OAuth2.verifyAppInstance(tempContext, appInstance, user._id, app._id, null)) {
				AccessLog.log("CSCLEAR");
				appInstance = null;
				ContextManager.instance.clearCache();
			}
						
			if (appInstance == null) {									
				boolean autoConfirm = InstanceConfig.getInstance().getInstanceType().autoconfirmConsentsMidataApi() && KeyManager.instance.unlock(user._id, null) == KeyManager.KEYPROTECTION_NONE;
				tempContext = autoConfirm ? ContextManager.instance.createLoginOnlyContext(user._id, user.role) : null;
				AccessLog.log("REINSTALL");
				if (!autoConfirm && app.targetUserRole.equals(UserRole.RESEARCH)) throw new BadRequestException("error.invalid.study", "The research app is not properly linked to a study! Please log in as researcher and link the app properly.");
				appInstance = ApplicationTools.installApp(tempContext, app._id, user, phrase, autoConfirm, Collections.emptySet(), null);
				KeyManager.instance.changePassphrase(appInstance._id, phrase);
				if (tempContext != null) ContextManager.instance.clearCache();
				
				tempContext = ContextManager.instance.createLoginOnlyContext(appInstance._id, user.role);
	   		    meta = RecordManager.instance.getMeta(tempContext, appInstance._id, "_app").toMap();
			} else {
				
				if (KeyManager.instance.unlock(appInstance._id, phrase) == KeyManager.KEYPROTECTION_FAIL) return status(UNAUTHORIZED);		
								
				tempContext = ContextManager.instance.createLoginOnlyContext(appInstance._id, user.role);
				meta = RecordManager.instance.getMeta(tempContext, appInstance._id, "_app").toMap();			
            }
			UsageStatsRecorder.protokoll(app._id, app.filename, UsageAction.LOGIN);							
			role = user.role;
			
			if (app.targetUserRole.equals(UserRole.RESEARCH)) {						
				BSONObject q = RecordManager.instance.getMeta(tempContext, appInstance._id, "_query");
				if (!q.containsField("link-study")) throw new BadRequestException("error.invalid.study", "The research app is not properly linked to a study! Please log in as researcher and link the app properly.");
			}
			*/
		}
											
		AuditManager.instance.success();
		
		return authResult(tempContext, role, appInstance, phrase, deprecated);
	}
	
	/**
	 * Returns result for authentication request. 
	 * @param appInstance instance of app to login
	 * @param meta _app meta object from app instance
	 * @param phrase app password
	 * @return
	 * @throws AppException
	 */
	public static Result authResult(AccessContext context1, UserRole role, MobileAppInstance appInstance, String phrase, boolean deprecated) throws AppException {
		AccessContext context = context1;
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis() + MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME, role, null); 
        OAuthRefreshToken refresh = OAuth2.createRefreshToken(context, appInstance, phrase);
		        
        BSONObject q = RecordManager.instance.getMeta(context, appInstance._id, "_query");
        if (q.containsField("link-study")) {
        	MidataId studyId = MidataId.from(q.get("link-study"));
        	MobileAPI.prepareMobileExecutor(appInstance, session);
        	controllers.research.Studies.autoApproveCheck(appInstance.applicationId, studyId, context1);
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

	protected static AccessContext prepareMobileExecutor(MobileAppInstance appInstance, MobileAppSessionToken tk) throws AppException {
		KeyManager.instance.login(1000l*60l, false);
		if (KeyManager.instance.unlock(tk.appInstanceId, tk.aeskey) == KeyManager.KEYPROTECTION_FAIL) { OAuth2.invalidToken(); }
		AccessContext tempContext = ContextManager.instance.createLoginOnlyContext(tk.appInstanceId, tk.role, appInstance);
		
		return ContextManager.instance.upgradeSessionForApp(tempContext, appInstance);				
	}
	
	/**
	 * retrieve records current mobile app has access to matching some criteria
	 * @return list of Records
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result getRecords(Request request) throws JsonValidationException, AppException {		
		// validate json
		Stats.startRequest(request);
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		AccessContext inf = ExecutionInfo.checkMobileToken(request, json.get("authToken").asText(), false, true);		
		if (inf == null) OAuth2.invalidToken(); 
							
        Stats.setPlugin(inf.getUsedPlugin());
        UsageStatsRecorder.protokoll(inf, UsageAction.GET);
        
        if (!((AppAccessContext) inf).getAppInstance().status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Record", fields)).as("application/json");
        }
                
		// get record data
		Collection<Record> records = null;
		
		AccessLog.log("NEW QUERY");		
		records = RecordManager.instance.list(inf.getAccessorRole(), inf, properties, fields);		  
				
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		
		Stats.finishRequest(request, "200", properties.keySet());
		return ok(JsonOutput.toJson(records, "Record", fields)).as("application/json");
	}
	
	/**
	 * retrieve a file record current mobile app has access to
	 * @return file
	 * @throws AppException
	 * @throws JsonValidationException
	 */	
	@MobileCall	
	public Result getFile(Request request) throws AppException, JsonValidationException {
		Stats.startRequest(request);
		// validate json
		JsonNode json = request.body().asJson();				
						
		AccessContext info = null;
		
		Optional<String> param = request.header("Authorization");
		String param2 = request.queryString("access_token").orElse(null);
		
		if (param.isPresent() && param.get().startsWith("Bearer ")) {
          info = ExecutionInfo.checkToken(request, param.get().substring("Bearer ".length()), false, true);                  
		} else if (json != null && json.has("authToken")) {
		  info = ExecutionInfo.checkToken(request, JsonValidation.getString(json, "authToken"), false, true);
		} else if (param2 != null) {
		  info = ExecutionInfo.checkToken(request, param2, false, true);
		} else throw new BadRequestException("error.auth", "Please provide authorization token as 'Authorization' header or 'authToken' request parameter.");
					
		String id =json != null ? JsonValidation.getString(json, "_id") : request.queryString("_id").orElseThrow();
		Pair<String, Integer> rec = RecordManager.instance.parseFileId(id);
				
		return getFile(request, info, MidataId.from(rec.getLeft()), rec.getRight(), false);
	}
	
	public static Result getFile(Request request, AccessContext info, MidataId recordId, int idx, boolean asAttachment) throws AppException {					
		FileData fileData = RecordManager.instance.fetchFile(info, new RecordToken(recordId.toString(), info.getTargetAps().toString()), idx);
		if (fileData == null) return badRequest();
		String contentType = "application/binary";
		if (fileData.contentType != null) contentType = fileData.contentType;
		if (contentType.startsWith("data:")) contentType = "application/binary";
				
		Stats.finishRequest(request, "200", Collections.EMPTY_SET);
		Result result = ok(fileData.inputStream).as(contentType);
		if (asAttachment) return PluginsAPI.setAttachmentContentDisposition(result, fileData.filename);
		return result;
	}
	
	/**
	 * create a new record. The current space will automatically have access to it.
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result createRecord(Request request) throws AppException, JsonValidationException {
		Stats.startRequest(request);
		
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) throw new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		AccessContext inf = ExecutionInfo.checkMobileToken(request, json.get("authToken").asText(), false, false);
		Stats.setPlugin(inf.getUsedPlugin());	
		UsageStatsRecorder.protokoll(inf, UsageAction.POST);	
		
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		if (format==null) format = "application/json";
		String content = JsonValidation.getStringOrNull(json, "content");
		Set<String> code = JsonExtraction.extractStringSet(json.get("code"));
		
		MidataId owner = inf.getLegacyOwner();
		MidataId ownerOverride = JsonValidation.getMidataId(json, "owner");
		if (ownerOverride != null && !ownerOverride.equals(owner)) {			
			owner = ownerOverride;
		}
				
		Record record = new Record();
		record._id = new MidataId();
		record.app = inf.getUsedPlugin();
		record.owner = owner;
		record.creator = inf.getActor();
		record.modifiedBy = inf.getActor();
		record.created = record._id.getCreationDate();
		
		/*if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}*/
		
		record.format = format;
		
			
		ContentInfo.setRecordCodeAndContent(inf.getUsedPlugin(), record, code, content);
				
		try {
			record.data = BasicDBObject.parse(data);	
		} catch (ClassCastException e2) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
								
		PluginsAPI.createRecord(inf, record);
		
		Stats.finishRequest(request, "200", Collections.EMPTY_SET);
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
	public Result updateRecord(Request request) throws AppException, JsonValidationException {
		
		Stats.startRequest(request);
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "_id", "version");
		
		AccessContext inf = ExecutionInfo.checkMobileToken(request, json.get("authToken").asText(), false, false);
		Stats.setPlugin(inf.getUsedPlugin());	
		UsageStatsRecorder.protokoll(inf, UsageAction.PUT);
		
        String data = JsonValidation.getJsonString(json, "data");
						
		Record record = new Record();
		
		record._id = JsonValidation.getMidataId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 				
		record.modifiedBy = inf.getActor();
		record.lastUpdated = new Date();		
							
		try {
			record.data = BasicDBObject.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
				
		String version = PluginsAPI.updateRecord(inf, record);
		
		Stats.finishRequest(request, "200", Collections.EMPTY_SET);
		
		ObjectNode obj = Json.newObject();		
		obj.put("version", version);		
		return ok(obj);
	}
	
	/*
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result deleteRecord() throws AppException, JsonValidationException {
		
		Stats.startRequest(request);
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
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
				
		Stats.finishRequest(request, "200", Collections.EMPTY_SET);						
		return ok();
	}*/
	
	@MobileCall	
	@BodyParser.Of(BodyParser.Json.class)
    public Result unshareRecord(Request request) throws AppException, JsonValidationException {
		
		Stats.startRequest(request);
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "target-study", "target-study-group");
		
		AccessContext inf = ExecutionInfo.checkMobileToken(request, json.get("authToken").asText(), false, false);
		Stats.setPlugin(inf.getUsedPlugin());	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
													
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");	
		
		unshareRecord(inf, studyId, group, properties);
									
		return ok();
	}
    
    public static int unshareRecord(AccessContext inf, MidataId studyId, String group, Map<String, Object> properties) throws AppException, JsonValidationException {
		
    	UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(studyId, inf.getAccessor());
    	if (ugm == null) throw new BadRequestException("error.invalid.study", "You are now allowed to do that");
    	
		if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");		
		Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudyPublic(inf.getAccessor(), group, studyId, Sets.create("_id"));		
		srs.addAll(StudyRelated.getActiveByOwnerGroupAndStudyPublic(studyId, group, studyId, Sets.create("_id")));
		properties.put("force-local", true);
		List<Record> recs = RecordManager.instance.list(inf.getAccessorRole(), inf, properties, Sets.create("_id"));
		AccessLog.log("unshareRecord: srs="+srs.size()+" recs="+recs.size());
		if (!srs.isEmpty()) {
			AccessContext infGroup = inf.forUserGroup(ugm);
			for (StudyRelated sr : srs ) {
			  RecordManager.instance.unshare(infGroup.forConsent(sr), recs);
			}
		}																		
		
		return recs.size();
	}

    @MobileCall	
    @BodyParser.Of(BodyParser.Json.class)
    public Result shareRecord(Request request) throws AppException, JsonValidationException {
		
		Stats.startRequest(request);
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "target-study", "target-study-group");
		
		AccessContext inf = ExecutionInfo.checkMobileToken(request, json.get("authToken").asText(), false, false);
		Stats.setPlugin(inf.getUsedPlugin());	
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
												
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");		
	
		shareRecord(inf, studyId, group, properties);
																	
		return ok();
	}
	
    public static int shareRecord(AccessContext inf, MidataId studyId, String group, Map<String, Object> properties) throws AppException, JsonValidationException {
			
    	AccessLog.logBegin("start share records by query context="+inf.toString());
    	try {
			if (properties.get("format") == null) throw new BadRequestException("error.internal", "No format");
			MidataId sharer = properties.get("usergroup") != null ? studyId : inf.getAccessor(); 	
			//properties.remove("usergroup");
			
			Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudyPublic(sharer, group, studyId, Sets.create("_id"));
			//Set<StudyRelated> nsrs = StudyRelated.getActiveByOwnerGroupAndStudyPrivate(sharer, group, studyId, Sets.create("_id"));
			
			int count = 0;
			
				
			if (srs.isEmpty()) {				
				Study study = Study.getById(studyId, Study.ALL);
				Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyId, group, Sets.create());
				controllers.research.Studies.joinSharing(inf, sharer, study, group, false, new ArrayList<StudyParticipation>(parts));
				srs = StudyRelated.getActiveByOwnerGroupAndStudyPublic(sharer, group, studyId, Sets.create("_id"));
			}
			
			AccessContext context = ContextManager.instance.createSharingContext(inf, sharer);
			for (StudyRelated sr : srs ) {
			  count = RecordManager.instance.share(context, sr._id, sr.owner, properties, false);
			}
				
										
									
			return count;
    	} finally {
    		AccessLog.logEnd("end share records by query");
    	}
	}
			
	
	
	/**
	 * retrieve aggregated information about records matching some criteria
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result getInfo(Request request) throws AppException, JsonValidationException {
 	
		// check whether the request is complete
		JsonNode json = request.body().asJson();				
		JsonValidation.validate(json, "authToken", "properties", "summarize");
		
		// decrypt authToken 
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) OAuth2.invalidToken(); 
	
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, MobileAppInstance.APPINSTANCE_ALL);
        if (appInstance == null) OAuth2.invalidToken(); 

        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(Json.toJson(Collections.EMPTY_LIST));
        }
        
        AccessContext context = prepareMobileExecutor(appInstance, authToken);
        		
		MidataId targetAps = appInstance._id;
				
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);
		
	    Collection<RecordsInfo> result = RecordManager.instance.info(authToken.role, targetAps, context, properties, aggrType);
						
		return ok(Json.toJson(result));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public Result getConsents(Request request) throws JsonValidationException, AppException {		
		// validate json
		JsonNode json = request.body().asJson();		
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
        
        AccessContext context = prepareMobileExecutor(appInstance, authToken);
		// get record data
		Collection<Consent> consents = Consent.getAllActiveByAuthorized(context.getLegacyOwner());
		
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
				
		return ok(JsonOutput.toJson(consents, "Consent", fields)).as("application/json");
	}
	

	@MobileCall
	public Result getImageToken(Request request) throws AppException {
        String param = request.header("Authorization").get();
		
		if (param != null && param.startsWith("Bearer ")) {
			  MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(param.substring("Bearer ".length()));
			  if (authToken == null) OAuth2.invalidToken(); 
	          AccessContext info = ExecutionInfo.checkMobileToken(request, authToken, false, false);
	          if (info == null) OAuth2.invalidToken(); 
	          Stats.setPlugin(info.getUsedPlugin());
	          String id = request.queryString("_id").orElseThrow();
	         
	          MobileAppSessionToken tk = new MobileAppSessionToken(authToken.appInstanceId, authToken.aeskey, System.currentTimeMillis() + MobileAPI.DEFAULT_IMAGE_EXPIRATION_TIME, authToken.role, MidataId.from(id), authToken.getExtra());
	          try {
				return ok("https://"+InstanceConfig.getInstance().getPlatformServer()+request.uri()+"&access_token="+URLEncoder.encode(tk.encrypt(), "UTF-8")).as("text/plain");
			  } catch (UnsupportedEncodingException e) {
				 throw new InternalServerException("error.internal", e);
			  } 
	    } 
		throw new BadRequestException("error.invalid.token", "Invalid or expired authToken.", Http.Status.UNAUTHORIZED);	
	}
		
}
