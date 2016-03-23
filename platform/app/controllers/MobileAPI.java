package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.LargeRecord;
import models.Member;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.User;
import models.enums.AggregationType;
import models.enums.ConsentStatus;
import models.enums.UserRole;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.DateTimeUtils;
import utils.PasswordHash;
import utils.access.RecordManager;
import utils.auth.CodeGenerator;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.MobileAppToken;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;
import actions.MobileCall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

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
		
	
	private static boolean verifyAppInstance(MobileAppInstance appInstance, ObjectId ownerId, ObjectId applicationId) {
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
		Plugin app = Plugin.getByFilename(name, Sets.create("type", "name", "secret"));
		if (app == null) return badRequest("Unknown app");
		if (!app.secret.equals(secret)) return badRequest("Unknown app");
		if (!app.type.equals("mobile")) return internalServerError("Wrong app type");
	
		ObjectId appInstanceId = null;
		MobileAppInstance appInstance = null;
		String phrase;
		Map<String, Object> meta = null;
		
		if (json.has("refreshToken")) {
			MobileAppToken refreshToken = MobileAppToken.decrypt(JsonValidation.getString(json, "refreshToken"));
			appInstanceId = refreshToken.appInstanceId;
			
			appInstance = MobileAppInstance.getById(appInstanceId, Sets.create("owner", "applicationId", "status"));
			if (!verifyAppInstance(appInstance, refreshToken.ownerId, refreshToken.appId)) return badRequest("Bad refresh token.");            
            if (!refreshToken.appId.equals(app._id)) return badRequest("Bad refresh token.");
            
            phrase = refreshToken.phrase;
            KeyManager.instance.unlock(appInstance._id, phrase);	
            meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
                        
            if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
            	return status(UNAUTHORIZED);
            }
		} else {
			String username = JsonValidation.getEMail(json, "username");
			phrase = JsonValidation.getString(json, "password");
				
			Member member = Member.getByEmail(username, Sets.create("visualizations","tokens"));
			if (member == null) return badRequest("Unknown user or bad password");
			
			appInstance = MobileAppInstance.getByApplicationAndOwner(app._id, member._id, Sets.create("owner", "applicationId", "status", "passcode"));
			
			if (appInstance == null) {									
				appInstance = new MobileAppInstance();
				appInstance._id = new ObjectId();
				appInstance.name = "Mobile: "+ app.name;
				appInstance.applicationId = app._id;		
	            appInstance.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(appInstance._id, phrase);
	        	appInstance.owner = member._id;
	        	appInstance.passcode = Member.encrypt(phrase);
	   		    MobileAppInstance.add(appInstance);	
	   		    
	   		    KeyManager.instance.unlock(appInstance._id, phrase);	   		    
	   		    RecordManager.instance.createAnonymizedAPS(member._id, appInstance._id, appInstance._id);
	   		    
	   		    meta = new HashMap<String, Object>();
	   		    meta.put("phrase", phrase);
	   		    
			} else {
				if (appInstance.passcode != null && !Member.authenticationValid(phrase, appInstance.passcode)) return badRequest("Wrong password.");
				if (!verifyAppInstance(appInstance, member._id, app._id)) return badRequest("Access denied");
				KeyManager.instance.unlock(appInstance._id, phrase);
				meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			}
		}
				
		if (!phrase.equals(meta.get("phrase"))) return internalServerError("Internal error while validating consent");
						
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis()); 
        MobileAppToken refresh = new MobileAppToken(app._id, appInstance._id, appInstance.owner, phrase, System.currentTimeMillis());
		
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(appInstance._id, appInstance._id, "_app", meta);
        
		// create encrypted authToken		
		ObjectNode obj = Json.newObject();								
		obj.put("authToken", session.encrypt());
		obj.put("refreshToken", refresh.encrypt());
															
		return ok(obj);
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
			return badRequest("Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner"));
        if (appInstance == null) return badRequest("Invalid authToken.");
		
        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
		// get record data
		Collection<Record> records = null;
		
		AccessLog.log("NEW QUERY");
		
		records = LargeRecord.getAll(appInstance._id, appInstance._id, properties, fields);		  
				
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
		JsonValidation.validate(json, "authToken", "_id");		
		
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner"));
        if (appInstance == null) return badRequest("Invalid authToken.");
	
        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
		
		ObjectId recordId = JsonValidation.getObjectId(json, "_id");			
		FileData fileData = RecordManager.instance.fetchFile(appInstance.owner, new RecordToken(recordId.toString(), appInstance._id.toString()));
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
		JsonValidation.validate(json, "authToken", "data", "name", "format", "content");
		
		// decrypt authToken 
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare"));
        if (appInstance == null) return badRequest("Invalid authToken.");

        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
        
		ObjectId appId = appInstance.applicationId;
				
		Member targetUser;
		ObjectId targetAps = appInstance._id;
				
		targetUser = Member.getById(appInstance.owner, Sets.create("myaps", "tokens"));
		if (targetUser == null) return badRequest("Invalid authToken.");
		//owner = targetUser;											
				
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		if (format==null) format = "application/json";
		String content = JsonValidation.getString(json, "content");
		if (content==null) content = "other";
		Record record = new Record();
		record._id = new ObjectId();
		record.app = appId;
		record.owner = appInstance.owner;
		record.creator = appInstance.owner;
		record.created = DateTimeUtils.now();
		
		if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}
		
		record.format = format;
		record.content = content;				
		
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			return badRequest("Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		RecordManager.instance.addRecord(appInstance._id, record, targetAps);
				
		//Set<ObjectId> records = new HashSet<ObjectId>();
		//records.add(record._id);
		//RecordManager.instance.share(targetUser._id, targetUser._id, targetAps, records, false);

		/*
		if (appInstance.autoShare != null && !appInstance.autoShare.isEmpty()) {
			for (ObjectId autoshareAps : appInstance.autoShare) {
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
		
		record._id = JsonValidation.getObjectId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 				
		record.creator = inf.executorId;
		record.lastUpdated = DateTimeUtils.now();		
							
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			return badRequest("Record data is invalid JSON.");
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
			return badRequest("Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare"));
        if (appInstance == null) return badRequest("Invalid authToken.");

        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
        		
		ObjectId targetAps = appInstance._id;
				
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);
		
	    Collection<RecordsInfo> result = RecordManager.instance.info(appInstance._id, targetAps, properties, aggrType);	
						
		return ok(Json.toJson(result));
	}
}
