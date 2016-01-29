package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import models.enums.UserRole;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.access.AccessLog;
import utils.access.RecordManager;
import utils.auth.CodeGenerator;
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
	 * assigns a new mobile app instance a security token
	 * @return mobile app token
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result init() throws JsonValidationException, AppException {        		
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "appname", "secret");
		
		String name = JsonValidation.getString(json, "appname");
		String secret = JsonValidation.getString(json,"secret");
				
		Plugin app = Plugin.getByFilenameAndSecret(name, secret, Sets.create("type"));
		if (app == null) return badRequest("Unknown app");
		if (!app.type.equals("mobile")) return internalServerError("Wrong app type");
		
		ObjectId appInstance = new ObjectId();
						
		String passphrase = CodeGenerator.generatePassphrase();
		
		MobileAppToken appToken = new MobileAppToken(app._id, appInstance, passphrase);
			
		ObjectNode obj = Json.newObject();								
		obj.put("instanceToken", appToken.encrypt()); 
		return ok(obj);
	}
	
	/**
	 * login function for MIDATA app
	 * @return status ok
	 * @throws AppException
	 */
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
	
	/**
	 * Authentication function for mobile apps
	 * @return json with authToken
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result authenticate() throws AppException {
				
        JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "instanceToken", "username", "password");
		
		MobileAppToken appToken = MobileAppToken.decrypt(JsonValidation.getString(json, "instanceToken"));		
		String username = JsonValidation.getEMail(json, "username");
		String password = JsonValidation.getString(json, "password");
		
		Member member = Member.getByEmail(username, Sets.create("password","visualizations","tokens"));
		if (member == null) return badRequest("Unknown user or bad password");
		
		// check password
		if (!Member.authenticationValid(password, member.password)) return badRequest("Unknown user or bad password");
						
		MobileAppInstance appInstance = MobileAppInstance.getByInstanceAndOwner(appToken.instanceId, member._id, Sets.create("owner"));
		if (appInstance == null) {		
			
			Plugin app = Plugin.getById(appToken.appId, Sets.create("name"));
			if (app == null) throw new InternalServerException("error.internal", "Plugin not found.");
			
			appInstance = new MobileAppInstance();
			appInstance._id = new ObjectId();
			appInstance.name = "Mobile: "+ app.name;
			appInstance.applicationId = appToken.appId;		
            appInstance.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(appInstance._id, appToken.phrase);
        	appInstance.owner = member._id;
   		    MobileAppInstance.add(appInstance);	
   		    
   		    KeyManager.instance.unlock(appInstance._id, appToken.phrase);
   		    
   		    RecordManager.instance.createAnonymizedAPS(member._id, appInstance._id, appInstance._id);   		       		    
		} 
								
		// create encrypted authToken		
		ObjectNode obj = Json.newObject();								
		obj.put("authToken", new MobileAppSessionToken(appInstance._id, appToken.phrase, System.currentTimeMillis()).encrypt());
												
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
		
		AccessLog.debug("NEW QUERY");
		
		records = LargeRecord.getAll(appInstance.owner, appInstance._id, properties, fields);		  
				
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
		
							
		// save new record with additional metadata
		if (!json.get("data").isTextual() || !json.get("name").isTextual() || !json.get("description").isTextual()) {
			return badRequest("At least one request parameter is of the wrong type.");
		}
				
		String data = JsonValidation.getString(json, "data");
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
				
		Set<ObjectId> records = new HashSet<ObjectId>();
		records.add(record._id);
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
				
		return ok();
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
