package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.HPUser;
import models.MemberKey;
import models.Plugin;
import models.Record;
import models.Member;
import models.User;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.OAuthCalculator;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.access.RecordManager;
import utils.auth.AppToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.FileStorage;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;
import actions.AppsCall;
import actions.VisualizationCall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

/**
 * API for input forms. May be removed when all plugins use the Plugins API
 *
 */
public class AppsAPI extends Controller {

	/**
	 * handling of OPTIONS requests
	 * @return status ok
	 */
	public static Result checkPreflight() {
		// allow cross-origin request from app server
		//String appServer = Play.application().configuration().getString("apps.server");
		//response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Origin", "*");
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}
	
	
	
	@BodyParser.Of(BodyParser.Json.class)
	@AppsCall
	public static Result getRecords() throws JsonValidationException, AppException {	
		response().setHeader("Access-Control-Allow-Origin", "*");
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "aps", "properties", "fields");
		
		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		Member owner = Member.getByIdAndApp(appToken.userId, appToken.appId, Sets.create("myaps", "tokens"));
		if (owner == null) return badRequest("Invalid authToken.");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));

		Rights.chk("getRecords", UserRole.ANY, properties, fields);
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("aps").asText());
		if (spaceToken == null) {
			return badRequest("Invalid spaceToken.");
		}
		if (!spaceToken.userId.equals(appToken.userId)) {
			return badRequest("Invalid spaceToken.");
		}
				
		// get record data	
		Collection<Record> records = RecordManager.instance.list(spaceToken.userId, spaceToken.spaceId, properties, fields);
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(JsonOutput.toJson(records, "Record", fields));
		
	}
	

	@BodyParser.Of(BodyParser.Json.class)
	@AppsCall
	public static Result createRecord() throws AppException, JsonValidationException {		
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "description", "format", "content");
		

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		User executor;
		Member targetUser;
		ObjectId targetAps = null;
		
		if (appToken.consentId == null) {
			targetUser = Member.getByIdAndApp(appToken.userId, appToken.appId, Sets.create("myaps", "tokens"));
			if (targetUser == null) return badRequest("Invalid authToken.");
			executor = targetUser;
		} else {						
			MemberKey mk = MemberKey.getById(appToken.consentId);
			if (mk == null) return badRequest("Invalid consent");
			if (!mk.authorized.contains(appToken.userId)) return badRequest("Invalid consent");
			
			HPUser hpuser = HPUser.getByIdAndApp(appToken.userId, appToken.appId, Sets.create("tokens","role","provider","firstname","lastname"));
			if (hpuser == null) return badRequest("Invalid authToken.");			
			targetUser = Member.getById(mk.owner, Sets.create("myaps", "tokens"));
			if (targetUser == null) return badRequest("Invalid authToken.");
			
			targetAps = appToken.consentId;
			executor = hpuser;
		}
				
				
		// save new record with additional metadata
		if (!json.get("data").isTextual() || !json.get("name").isTextual() || !json.get("description").isTextual()) {
			return badRequest("At least one request parameter is of the wrong type.");
		}
		
		Map<String,String> tokens = targetUser.tokens.get(appToken.appId.toString());		
		
		String data = JsonValidation.getString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		String content = JsonValidation.getString(json, "content");
		ObjectId document = JsonValidation.getObjectId(json, "document");
		String part = JsonValidation.getString(json, "part");
		
		if (format==null) format = "json";
		Record record = new Record();
		record._id = new ObjectId();
		record.app = appToken.appId;
		record.owner = targetUser._id;
		record.creator = executor._id;
		record.created = DateTimeUtils.now();
		if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}		
		record.format = format;
		record.content = content;
		record.document = document;
		record.part = part;
		
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			return badRequest("Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		RecordManager.instance.addRecord(executor._id, record, targetAps);
		
		/*if (targetAps != null) {
			Set<ObjectId> records = new HashSet<ObjectId>();
			records.add(record._id);
			RecordManager.instance.share(targetUser._id, targetUser.myaps, targetAps, records, true);
		}*/
		
		ObjectNode obj = Json.newObject();
		obj.put("_id", record._id.toString());
		return ok(obj);
	}

	/**
	 * Accepts and stores files up to sizes of 2GB.
	 */
	public static Result uploadFile() {
		// allow cross origin request from app server
		//String appServer = Play.application().configuration().getString("apps.server");
		//response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Origin", "*");

		// check meta data
		MultipartFormData formData = request().body().asMultipartFormData();
		Map<String, String[]> metaData = formData.asFormUrlEncoded();
		if (!metaData.containsKey("authToken") || !metaData.containsKey("name") || !metaData.containsKey("description")) {
			return badRequest("At least one request parameter is missing.");
		}

		// decrypt authToken and check whether a user exists who has the app installed
		if (metaData.get("authToken").length != 1) {
			return badRequest("Invalid authToken.");
		}
		AppToken appToken = AppToken.decrypt(metaData.get("authToken")[0]);
		if (appToken == null) {
			return badRequest("Invalid authToken.");
		}
				
		try {
			Member owner = Member.getByIdAndApp(appToken.userId, appToken.appId, Sets.create("myaps"));
			if (owner == null) return badRequest("Invalid authToken.");
			
					
			// extract file from data
			FilePart fileData = formData.getFile("file");
			if (fileData == null) {
				return badRequest("No file found.");
			}
			File file = fileData.getFile();
			String filename = fileData.getFilename();
			String contentType = fileData.getContentType();
	
			// create record
			Record record = new Record();
			record._id = new ObjectId();
			record.app = appToken.appId;
			record.owner = appToken.userId;
			record.creator = appToken.userId;
			record.created = DateTimeUtils.now();
			record.name = metaData.get("name")[0];
			record.description = metaData.get("description")[0];
			String[] formats = metaData.get("format");
			record.format = (formats != null && formats.length == 1) ? formats[0] : (contentType != null) ? contentType : "application/octet-stream";
			String[] contents = metaData.get("contents");
			record.content =  (contents != null && contents.length == 1) ? contents[0] : "other";
						
			record.data = new BasicDBObject(new ChainedMap<String, String>().put("type", "file").put("name", filename)
					.put("contentType", contentType).get());

		// save file with file storage utility
		    try {
			  RecordManager.instance.addRecord(owner._id, record, new FileInputStream(file), filename, contentType);
		    } catch (FileNotFoundException e) {
		    	throw new InternalServerException("error.internal",e);
		    }
						
			ObjectNode obj = Json.newObject();
			obj.put("_id", record._id.toString());
			return ok(obj);
		} catch (AppException e) {
			return badRequest(e.getMessage());
		} catch (DatabaseException e) {
			return badRequest(e.getMessage());
		}
		
	}

	/**
	 * Helper method for OAuth 2.0 apps: API calls can sometimes only be done from the backend. Uses the
	 * "Authorization: Bearer [accessToken]" header.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Promise<Result> oAuth2Call() {
		// allow cross origin request from app server
		//String appServer = Play.application().configuration().getString("apps.server");
		//response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Origin", "*");

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequestPromise("Invalid authToken.");
		}
		
		String accessToken;
		try {
			
			Member user = Member.getByIdAndApp(appToken.userId, appToken.appId, Sets.create("tokens." + appToken.appId.toString()));
			if (user == null) return badRequestPromise("Invalid authToken.");
			accessToken = user.tokens.get(appToken.appId.toString()).get("accessToken");
			
		} catch (InternalServerException e) {
			return badRequestPromise(e.getMessage());
		}

		// perform OAuth API call on behalf of the app
		WSRequestHolder holder = WS.url(json.get("url").asText());
		holder.setHeader("Authorization", "Bearer " + accessToken);
		Promise<Result> promise = holder.get().map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				return ok(response.asJson());
			}
		});
		return promise;
	}

	/**
	 * Helper method for OAuth 1.0 apps: Need to compute signature based on consumer secret, which should stay in the backend.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Promise<Result> oAuth1Call() {
		// allow cross origin request from app server
		//String appServer = Play.application().configuration().getString("apps.server");
		//response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Origin", "*");

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequestPromise("Invalid authToken.");
		}
		
		String oauthToken, oauthTokenSecret;
		try {
			
			Member user = Member.getByIdAndApp(appToken.userId, appToken.appId, Sets.create("tokens." + appToken.appId.toString()));
			if (user == null) return badRequestPromise("Invalid authToken.");
			oauthToken = user.tokens.get(appToken.appId.toString()).get("oauthToken");
			oauthTokenSecret = user.tokens.get(appToken.appId.toString()).get("oauthTokenSecret");

		} catch (InternalServerException e) {
			return badRequestPromise(e.getMessage());
		}

		// also get the consumer key and secret
				
		Plugin app;
		try {			
				app = Plugin.getById(appToken.appId, Sets.create("consumerKey", "consumerSecret"));
				if (app == null) return badRequestPromise("Invalid authToken");			
		} catch (InternalServerException e) {
			return badRequestPromise(e.getMessage());
		}

		// perform the api call
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		RequestToken token = new RequestToken(oauthToken, oauthTokenSecret);
		return WS.url(json.get("url").asText()).sign(new OAuthCalculator(key, token)).get().map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				return ok(response.asJson());
			}
		});
	}

	private static Promise<Result> badRequestPromise(final String errorMessage) {
		return Promise.promise(new Function0<Result>() {
			public Result apply() {
				return badRequest(errorMessage);
			}
		});
	}

}
