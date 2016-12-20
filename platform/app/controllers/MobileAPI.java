package controllers;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

import actions.MobileCall;
import models.Consent;
import models.ContentInfo;
import models.HPUser;
import models.LargeRecord;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.User;
import models.enums.AggregationType;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.access.Feature_FormatGroups;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.MobileAppToken;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for mobile APPs
 *
 */
public class MobileAPI extends Controller {

	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@MobileCall
	public static Result checkPreflight() {		
		return ok();
	}
 	
	
	
	/**
	 * login function for MIDATA app
	 * @return status ok
	 * @throws AppException
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public static Result midataLogin() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();	
		User user;
		String passphrase = null;
		
		if (json.has("email")) {		
		  JsonValidation.validate(json, "email", "password");	
		  String email = JsonValidation.getEMail(json, "email");
		  String password = JsonValidation.getString(json, "password");
		  passphrase = JsonValidation.getStringOrNull(json, "passphrase");
		
		  // check status
		  user = Member.getByEmail(email , Sets.create("password", "status", "contractStatus", "emailStatus", "confirmationCode", "accountVersion", "role"));
		  if (user == null) return badRequest("Invalid user or password.");
		  if (!Member.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		  }
		} else {
		  JsonValidation.validate(json, "token");
		  MobileAppToken test = MobileAppToken.decrypt(JsonValidation.getString(json, "token"));
		  user = User.getById(test.appId , Sets.create("password", "status", "contractStatus", "emailStatus", "confirmationCode", "accountVersion", "role"));
		  passphrase = test.phrase;
		}
			 
		KeyManager.instance.unlock(user._id, passphrase);
	    Set<Space> spaces = Space.getAll(CMaps.map("owner", user._id).map("context", "mobile"), Space.ALL);
	    
	    for (Space space : spaces) {
	       SpaceToken spaceToken = new SpaceToken(space._id, user._id);
	       Plugin visualization = Plugin.getById(space.visualization, Sets.create("type", "name", "filename", "url", "creator", "developmentServer", "accessTokenUrl", "authorizationUrl", "consumerKey", "scopeParameters"));
	       String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename;
	 	   String url =  visualizationServer  + "/" + visualization.url;
	 	   url = url.replace(":authToken", spaceToken.encrypt());
	 	   space.context = url;
	    }
	    
	    ObjectNode obj = Json.newObject();
	    obj.put("spaces", JsonOutput.toJsonNode(spaces, "Space", Sets.create("name", "context")));
	    obj.put("token", new MobileAppToken(user._id, user._id, passphrase).encrypt());
		return ok(obj);
	
	}
	 */
		
	
	private static boolean verifyAppInstance(MobileAppInstance appInstance, MidataId ownerId, MidataId applicationId) {
		if (appInstance == null) return false;
        if (!appInstance.owner.equals(ownerId)) return false;
        if (!appInstance.applicationId.equals(applicationId)) return false;
        
        if (appInstance.status.equals(ConsentStatus.EXPIRED) || appInstance.status.equals(ConsentStatus.REJECTED)) return false;
        
        return true;
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
		if (!json.has("refreshToken")) JsonValidation.validate(json, "username", "password");
					
		String name = JsonValidation.getString(json, "appname");
		String secret = JsonValidation.getString(json,"secret");
				
	    // Validate Mobile App	
		Plugin app = Plugin.getByFilename(name, Sets.create("type", "name", "secret", "status"));
		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");
		
		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
		if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
		
	
		MidataId appInstanceId = null;
		MobileAppInstance appInstance = null;
		String phrase;
		Map<String, Object> meta = null;
		
		if (json.has("refreshToken")) {
			MobileAppToken refreshToken = MobileAppToken.decrypt(JsonValidation.getString(json, "refreshToken"));
			appInstanceId = refreshToken.appInstanceId;
			
			appInstance = MobileAppInstance.getById(appInstanceId, Sets.create("owner", "applicationId", "status"));
			if (!verifyAppInstance(appInstance, refreshToken.ownerId, refreshToken.appId)) throw new BadRequestException("error.invalid.token", "Bad refresh token.");            
            if (!refreshToken.appId.equals(app._id)) throw new BadRequestException("error.invalid.token", "Bad refresh token.");  
            
            phrase = refreshToken.phrase;
            KeyManager.instance.unlock(appInstance._id, phrase);	
            meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
                        
            if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
            	return status(UNAUTHORIZED);
            }
		} else {
			String username = JsonValidation.getEMail(json, "username");
			phrase = JsonValidation.getString(json, "password");
			UserRole role = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;
			
				
			User user = null;
			switch (role) {
			case MEMBER : user = Member.getByEmail(username, Sets.create("visualizations","tokens"));break;
			case PROVIDER : user = HPUser.getByEmail(username, Sets.create("visualizations","tokens"));break;
			}
			if (user == null) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
			
			appInstance = MobileAppInstance.getByApplicationAndOwner(app._id, user._id, Sets.create("owner", "applicationId", "status", "passcode"));
			
			if (appInstance == null) {									
				appInstance = installApp(null, app._id, user, phrase);				
	   		    meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			} else {
				if (appInstance.passcode != null && !User.authenticationValid(phrase, appInstance.passcode)) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
				if (!verifyAppInstance(appInstance, user._id, app._id)) throw new BadRequestException("error.expired.token", "Access denied");
				KeyManager.instance.unlock(appInstance._id, phrase);
				meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			}
		}
				
		if (!phrase.equals(meta.get("phrase"))) return internalServerError("Internal error while validating consent");
						
		return authResult(appInstance, meta, phrase);
	}
	
	/**
	 * Returns result for authentication request. 
	 * @param appInstance instance of app to login
	 * @param meta _app meta object from app instance
	 * @param phrase app password
	 * @return
	 * @throws AppException
	 */
	public static Result authResult(MobileAppInstance appInstance, Map<String, Object> meta, String phrase) throws AppException {
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis()); 
        MobileAppToken refresh = new MobileAppToken(appInstance.applicationId, appInstance._id, appInstance.owner, phrase, System.currentTimeMillis());
		
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(appInstance._id, appInstance._id, "_app", meta);
        
		// create encrypted authToken		
		ObjectNode obj = Json.newObject();								
		obj.put("authToken", session.encrypt());
		obj.put("refreshToken", refresh.encrypt());
		obj.put("status", appInstance.status.toString());
		obj.put("owner", appInstance.owner.toString());
															
		return ok(obj);
	}
	
	public static MobileAppInstance installApp(MidataId executor, MidataId appId, User member, String phrase) throws AppException {
		Plugin app = Plugin.getById(appId, Sets.create("name", "defaultQuery"));
		MobileAppInstance appInstance = new MobileAppInstance();
		appInstance._id = new MidataId();
		appInstance.name = "Mobile: "+ app.name;
		appInstance.applicationId = app._id;		
        appInstance.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(appInstance._id, phrase);
    	appInstance.owner = member._id;
    	appInstance.passcode = Member.encrypt(phrase);
		MobileAppInstance.add(appInstance);	
		    
		KeyManager.instance.unlock(appInstance._id, phrase);	   		    
		RecordManager.instance.createAnonymizedAPS(member._id, appInstance._id, appInstance._id, true);
		    
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("phrase", phrase);
		if (executor == null) executor = appInstance._id;
		RecordManager.instance.setMeta(executor, appInstance._id, "_app", meta);
		
		if (app.defaultQuery != null && !app.defaultQuery.isEmpty()) {
			String groupSystem = null;
			if (app.defaultQuery != null) {
				if (app.defaultQuery.containsKey("group-system")) {
				  groupSystem = app.defaultQuery.get("group-system").toString();
				} else {
				  groupSystem = "v1";
				}
			}
		    Feature_FormatGroups.convertQueryToContents(groupSystem, app.defaultQuery);
				
		    RecordManager.instance.shareByQuery(executor, member._id, appInstance._id, app.defaultQuery);
		}
				
		return appInstance;
	}
	
	private static MidataId prepareMobileExecutor(MobileAppInstance appInstance, MobileAppSessionToken tk) throws AppException {
		KeyManager.instance.unlock(tk.appInstanceId, tk.passphrase);
		Map<String, Object> appobj = RecordManager.instance.getMeta(tk.appInstanceId, tk.appInstanceId, "_app").toMap();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			MidataId alias = new MidataId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("aliaskey");
			KeyManager.instance.unlock(appInstance.owner, alias, key);
			RecordManager.instance.clear();
			return appInstance.owner;
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
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "status"));
        if (appInstance == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		
        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Record", fields));
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
		// get record data
		Collection<Record> records = null;
		
		AccessLog.log("NEW QUERY");
		
		/*if (properties.containsKey("content")) {
			Set<String> contents = Query.getRestriction(properties.get("content"), "content");
			Set<String> add = new HashSet<String>();
			for (String c : contents) {
				add.add(ContentInfo.getNormalizedName(c));				
			}			
			properties.put("content", add);
		}*/
		
		records = LargeRecord.getAll(executor, appInstance._id, properties, fields);		  
				
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(JsonOutput.toJson(records, "Record", fields));
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
		
		// validate json
		JsonNode json = request().body().asJson();				
						
		ExecutionInfo info = null;
		
		String param = request().getHeader("Authorization");		
		if (param != null && param.startsWith("Bearer ")) {
          info = ExecutionInfo.checkToken(request(), param.substring("Bearer ".length()));                  
		} else if (json != null && json.has("authToken")) {
		  info = ExecutionInfo.checkToken(request(), JsonValidation.getString(json, "authToken"));
		} else throw new BadRequestException("error.auth", "Please provide authorization token as 'Authorization' header or 'authToken' request parameter.");
					
		MidataId recordId = json != null ? JsonValidation.getMidataId(json, "_id") : new MidataId(request().getQueryString("_id"));			
		FileData fileData = RecordManager.instance.fetchFile(info.executorId, new RecordToken(recordId.toString(), info.targetAPS.toString()));
		if (fileData == null) return badRequest();
		//response().setHeader("Content-Disposition", "attachment; filename=" + fileData.filename);
		return ok(fileData.inputStream);
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
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		// decrypt authToken 
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token","Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare","status"));
        if (appInstance == null) throw new BadRequestException("error.invalid.token","Invalid authToken.");
        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");

        MidataId executor = prepareMobileExecutor(appInstance, authToken);
        
		MidataId appId = appInstance.applicationId;
				
		Member targetUser;
		MidataId targetAps = appInstance._id;
				
		targetUser = Member.getById(appInstance.owner, Sets.create("myaps", "tokens"));
		if (targetUser == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		//owner = targetUser;											
				
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		if (format==null) format = "application/json";
		String content = JsonValidation.getStringOrNull(json, "content");
		Set<String> code = JsonExtraction.extractStringSet(json.get("code"));
		
		MidataId owner = appInstance.owner;
		MidataId ownerOverride = JsonValidation.getMidataId(json, "owner");
		if (ownerOverride != null && !ownerOverride.equals(owner)) {
			Set<Consent> consent = Consent.getHealthcareActiveByAuthorizedAndOwner(executor, ownerOverride);
			if (consent == null || consent.isEmpty()) throw new BadRequestException("error.noconsent", "No active consent that allows to add data for target person.");
			owner = ownerOverride;
		}
				
		Record record = new Record();
		record._id = new MidataId();
		record.app = appId;
		record.owner = owner;
		record.creator = appInstance.owner;
		record.created = new Date();
		
		if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}
		
		record.format = format;
		record.subformat = JsonValidation.getStringOrNull(json, "subformat");
			
		ContentInfo.setRecordCodeAndContent(record, code, content);
				
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		} catch (ClassCastException e2) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		RecordManager.instance.addRecord(executor, record, targetAps);
						
		Set<MidataId> records = new HashSet<MidataId>();
		records.add(record._id);
		RecordManager.instance.share(executor, appInstance.owner, targetAps, records, false);

		/*
		if (appInstance.autoShare != null && !appInstance.autoShare.isEmpty()) {
			for (MidataId autoshareAps : appInstance.autoShare) {
				Consent consent = Consent.getByIdAndOwner(autoshareAps, targetUser._id, Sets.create("type"));
				if (consent != null) { 
				  RecordManager.instance.share(targetUser._id, targetUser._id, autoshareAps, records, true);
				}
			}
		}
		*/
				
		ObjectNode obj = Json.newObject();		
		obj.put("_id", record._id.toString());		
		return ok(JsonOutput.toJson(record, "Record", Sets.create("_id", "created", "version")));
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
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "_id", "version");
		
		ExecutionInfo inf = ExecutionInfo.checkMobileToken(json.get("authToken").asText());
			
        String data = JsonValidation.getJsonString(json, "data");
						
		Record record = new Record();
		
		record._id = JsonValidation.getMidataId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 				
		record.creator = inf.executorId;
		record.lastUpdated = new Date();		
							
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
				
		String version = PluginsAPI.updateRecord(inf, record);
		ObjectNode obj = Json.newObject();		
		obj.put("version", version);		
		return ok(obj);
	}
	
	
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
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare", "status"));
        if (appInstance == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");

        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(Json.toJson(Collections.EMPTY_LIST));
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
        		
		MidataId targetAps = appInstance._id;
				
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);
		
	    Collection<RecordsInfo> result = RecordManager.instance.info(executor, targetAps, properties, aggrType);
						
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
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "status"));
        if (appInstance == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		
        if (!appInstance.status.equals(ConsentStatus.ACTIVE)) {
        	return ok(JsonOutput.toJson(Collections.EMPTY_LIST, "Consent", fields));
        }
        
        MidataId executor = prepareMobileExecutor(appInstance, authToken);
		// get record data
		Collection<Consent> consents = Consent.getAllActiveByAuthorized(executor);
		
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(consents, true);
		
		
		return ok(JsonOutput.toJson(consents, "Consent", fields));
	}
}
