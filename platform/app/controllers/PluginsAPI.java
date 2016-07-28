package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Admin;
import models.Consent;
import models.ContentInfo;
import models.LargeRecord;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.User;
import models.enums.AggregationType;
import models.enums.UserRole;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
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
import utils.AccessLog;
import utils.DateTimeUtils;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.FileStorage.FileData;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.sandbox.FitbitImport;
import utils.sandbox.MidataServer;
import actions.VisualizationCall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

/**
 * API functions to be used by MIDATA plugins
 *
 */
public class PluginsAPI extends APIController {

	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@VisualizationCall
	public static Result checkPreflight() {		
		return ok();
	}
 
	/**
	 * return IDs of all records current plugin has access to. Deprecated. Use getRecords instead.
	 * @return list of object IDs
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getIds() throws AppException, JsonValidationException  {		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken 
		SpaceToken spaceToken = SpaceToken.decrypt(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
				
		Set<ObjectId> tokens = ObjectIdConversion.toObjectIds(RecordManager.instance.listRecordIds(spaceToken.executorId, spaceToken.spaceId));		
		return ok(Json.toJson(tokens));
	}

	/**
	 * retrieve configuration json for a space. 
	 * @return json previously stored with setConfig
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getConfig() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decrypt(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		if (spaceToken.recordId != null)  {
		   ObjectNode result = Json.newObject();
		   result.put("readonly", true);
		   return ok(result);
		}
		
		BSONObject meta = RecordManager.instance.getMeta(spaceToken.executorId, spaceToken.spaceId, "_config");
		
		if (meta != null) return ok(Json.toJson(meta.toMap()));
		
		return ok();
	}
	
	/**
	 * store configuration json for a space
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result setConfig() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken 
		SpaceToken spaceToken = SpaceToken.decrypt(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		
		if (json.has("config")) {
		   Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));		
		   RecordManager.instance.setMeta(spaceToken.executorId, spaceToken.spaceId, "_config", config);
		}
		if (json.has("autoimport")) {
			boolean auto = JsonValidation.getBoolean(json, "autoimport");
			Space space = Space.getByIdAndOwner(spaceToken.spaceId, spaceToken.userId, Sets.create("autoImport", "owner"));
			if (auto) {				
				User autoRunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
				RecordManager.instance.shareAPS(space._id, spaceToken.executorId, Collections.singleton(autoRunner._id));
				RecordManager.instance.materialize(spaceToken.executorId, space._id);
				Space.set(space._id, "autoImport", auto);
			} else {
				User autoRunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
				RecordManager.instance.unshareAPS(space._id, spaceToken.executorId, Collections.singleton(autoRunner._id));
				Space.set(space._id, "autoImport", auto);
			}
		}
						
		return ok();
	}
	
	/**
	 * clone current space with a different confuguration
	 * @return
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result cloneAs() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "name", "config");
		
		// decrypt authToken 
		SpaceToken spaceToken = SpaceToken.decrypt(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		String name = JsonValidation.getString(json, "name");
		Space current = Space.getByIdAndOwner(spaceToken.spaceId, spaceToken.userId, Sets.create("context", "visualization", "app"));
		if (current == null) throw new BadRequestException("error.unknown.space", "The current space does no longer exist.");
		
		Space space = Spaces.add(spaceToken.userId, name, current.visualization, current.type, current.context);		
		BSONObject bquery = RecordManager.instance.getMeta(spaceToken.executorId, spaceToken.spaceId, "_query");		
		Map<String, Object> query;
		if (bquery != null) {
			query = bquery.toMap();
						
			RecordManager.instance.shareByQuery(spaceToken.executorId, spaceToken.userId, space._id, query);
		}		
		RecordManager.instance.setMeta(spaceToken.executorId, space._id, "_config", config);
						
		return ok();
	}

	/**
	 * retrieve records current space has access to matching some criteria
	 * @return list of Records
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getRecords() throws JsonValidationException, AppException {		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
						
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
		
		Collection<Record> records = getRecords(inf, properties, fields);
				
		return ok(JsonOutput.toJson(records, "Record", fields));
	}
	
	/**
	 * helper function to retrieve records matching some criteria
	 * @param inf execution info manages access rights
	 * @param properties key-value map containing restrictions
	 * @param fields set of field names to return
	 * @return collection of records matching given criteria where executor has access to 
	 * @throws AppException
	 */
	public static Collection<Record> getRecords(ExecutionInfo inf, Map<String,Object> properties, Set<String> fields) throws AppException {
		// Do not check for fields that query for parts of the data.
		Set<String> chkFields = new HashSet<String>();
		for (String f : fields) {
			  if (!f.startsWith("data.")) chkFields.add(f);
		}
						
		Rights.chk("getRecords", UserRole.ANY, chkFields);
					
		if (inf.recordId != null) properties.put("_id", inf.recordId);

		// get record data
		Collection<Record> records = null;
				
		AccessLog.log("NEW QUERY");
		
		records = LargeRecord.getAll(inf.executorId, inf.targetAPS, properties, fields);		  
						
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		
		return records;
	}
	
	/**
	 * retrieve aggregated information about records matching some criteria
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getInfo() throws AppException, JsonValidationException {
 	
		// check whether the request is complete
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "authToken", "properties", "summarize");
		
		// decrypt authToken 
		SpaceToken authToken = SpaceToken.decrypt(request(), json.get("authToken").asText());
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
					
		ObjectId targetAps = authToken.spaceId;
		
		Collection<RecordsInfo> result;

		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = json.has("fields") ? JsonExtraction.extractStringSet(json.get("fields")) : Sets.create();
		
		if (authToken.recordId != null) {
			Collection<Record> record = RecordManager.instance.list(authToken.executorId, authToken.spaceId, CMaps.map("_id", authToken.recordId), Sets.create("owner", "content", "format", "group"));
			result = new ArrayList<RecordsInfo>();
			for (Record r : record) result.add(new RecordsInfo(r));			
		} else {
							
			AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);		
		    result = RecordManager.instance.info(authToken.executorId, targetAps, properties, aggrType);	

		}
	    if (fields.contains("ownerName")) ReferenceTool.resolveOwnersForRecordsInfo(result, true);
		return ok(Json.toJson(result));
	}
	
	/**
	 * retrieve a file record current space has access to
	 * @return file
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall	
	public static Result getFile() throws AppException, JsonValidationException {
		
		// validate json
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "authToken", "_id");		
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken authToken = SpaceToken.decrypt(request(), json.get("authToken").asText());
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		
		ObjectId recordId = JsonValidation.getObjectId(json, "_id");			
		FileData fileData = RecordManager.instance.fetchFile(authToken.executorId, new RecordToken(recordId.toString(), authToken.spaceId.toString()));
		if (fileData == null) return badRequest();
		setAttachmentContentDisposition(fileData.filename);		
		return ok(fileData.inputStream);
	}
	
	/**
	 * create a new record. The current space will automatically have access to it.
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result createRecord() throws AppException, JsonValidationException {
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		
		ExecutionInfo authToken = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
				
		if (authToken.recordId != null) throw new BadRequestException("error.internal", "This view is readonly.");
																														
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		
		String content = JsonValidation.getStringOrNull(json, "content");
		Set<String> code = JsonExtraction.extractStringSet(json.get("code"));
		
		
		Record record = new Record();
		if (json.has("_id")) {
			record._id = JsonValidation.getObjectId(json, "_id");			
		} else {
		    record._id = new ObjectId();
		}
		record.app = authToken.pluginId;
		record.owner = authToken.ownerId;
		record.creator = authToken.executorId;
		record.created = DateTimeUtils.now();
		
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
		}
		record.name = name;
		record.description = description;
		
		createRecord(authToken, record);
									
		return ok();
	}
	
	/**
	 * Helper function to create a record
	 * @param inf execution context
	 * @param record record to add to the database
	 * @throws AppException
	 */
	public static void createRecord(ExecutionInfo inf, Record record) throws AppException  {
		if (record.format==null) record.format = "application/json";
		if (record.content==null) record.content = "other";
		
		if (inf.space != null) {				
		    RecordManager.instance.addRecord(inf.executorId, record, inf.space._id);
				
		    Set<ObjectId> records = new HashSet<ObjectId>();
			records.add(record._id);
			
		    if (inf.executorId.equals(inf.ownerId)) {				
				RecordManager.instance.share(inf.executorId, inf.ownerId, inf.targetAPS, records, false);
		    }
			
			if (inf.space != null && inf.space.autoShare != null && !inf.space.autoShare.isEmpty()) {
				for (ObjectId autoshareAps : inf.space.autoShare) {
					Consent consent = Consent.getByIdAndOwner(autoshareAps, inf.ownerId, Sets.create("type"));
					if (consent != null) { 
					  RecordManager.instance.share(inf.executorId, inf.space._id, autoshareAps, records, true);
					}
				}
			}
			
		} else {
			RecordManager.instance.addRecord(inf.executorId, record, inf.targetAPS);
		}
	}
	
	/**
	 * Helper method for OAuth 1.0 apps: Need to compute signature based on consumer secret, which should stay in the backend.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Promise<Result> oAuth1Call() throws AppException {
	
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
				
		Map<String, String> tokens = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_oauth").toMap();
				
		String oauthToken, oauthTokenSecret, appId;
		
		oauthToken = tokens.get("oauthToken");
		oauthTokenSecret = tokens.get("oauthTokenSecret");
		appId = tokens.get("appId");
		

		// also get the consumer key and secret
				
		Plugin app;
		try {			
				app = Plugin.getById(new ObjectId(appId), Sets.create("consumerKey", "consumerSecret"));
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
	
	/**
	 * update a record. 
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result updateRecord() throws AppException, JsonValidationException {
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "_id");
		
		ExecutionInfo authToken = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
				
		if (authToken.recordId != null) throw new BadRequestException("error.internal", "This view is readonly.");
																																	
		String data = JsonValidation.getJsonString(json, "data");
		
		//String name = JsonValidation.getString(json, "name");
		//String description = JsonValidation.getString(json, "description");
		//String format = JsonValidation.getString(json, "format");
		
		//String content = JsonValidation.getString(json, "content");
				
		Record record = new Record();
		
		record._id = JsonValidation.getObjectId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 		
		//record.app = authToken.pluginId;
		//record.owner = authToken.ownerId;
		record.creator = authToken.executorId;
		record.lastUpdated = DateTimeUtils.now();		
							
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
				
		updateRecord(authToken, record);
									
		return ok();
	}
	
	/**
	 * Helper function to update a record
	 * @param inf execution context
	 * @param record record to add to the database
	 * @throws AppException
	 * @return the new version string of the record
	 */
	public static String updateRecord(ExecutionInfo inf, Record record) throws AppException  {
		return RecordManager.instance.updateRecord(inf.executorId, inf.targetAPS, record);				
	}
	
	/**
	 * Helper method for OAuth 2.0 apps: API calls can sometimes only be done from the backend. Uses the
	 * "Authorization: Bearer [accessToken]" header.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Promise<Result> oAuth2Call() throws AppException {
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
		
		Promise<WSResponse> response = oAuth2Call(inf, json.get("url").asText());	
		
		Promise<Result> promise = response.map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				return ok(response.asJson());
			}
		});
		return promise;
	}
	
	/**
	 * Helper method for OAuth 2.0 apps
	 * @param inf execution context
	 * @param url URL to call
	 * @return result of oauth request
	 * @throws AppException
	 */
    public static Promise<WSResponse> oAuth2Call(ExecutionInfo inf, String url) throws AppException {
				
		Map<String, String> tokens = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_oauth").toMap();				
		String accessToken;
		accessToken = tokens.get("accessToken");
					
		// perform OAuth API call on behalf of the app
		WSRequestHolder holder = WS.url(url);
		holder.setHeader("Authorization", "Bearer " + accessToken);
		
		return holder.get();
				
	}
	
	/**
	 * Accepts and stores files up to sizes of 2GB.
	 */
	public static Result uploadFile() throws AppException {
		// allow cross origin request from app server
		//String appServer = Play.application().configuration().getString("apps.server");
		//response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Origin", "*");

		// check meta data
		MultipartFormData formData = request().body().asMultipartFormData();
		Map<String, String[]> metaData = formData.asFormUrlEncoded();
		if (!metaData.containsKey("authToken") || !metaData.containsKey("name")) {
			throw new BadRequestException("error.internal", "At least one request parameter is missing.");
		}

		// decrypt authToken and check whether a user exists who has the app installed
		if (metaData.get("authToken").length != 1) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
	
		ExecutionInfo authToken = ExecutionInfo.checkSpaceToken(request(), metaData.get("authToken")[0]);
		
		if (authToken.recordId != null) throw new BadRequestException("error.internal", "This view is readonly.");
			try {
							
			// extract file from data
			FilePart fileData = formData.getFile("file");
			if (fileData == null) {
				throw new BadRequestException("error.internal", "No file found.");
			}
			File file = fileData.getFile();
			String filename = fileData.getFilename();
			String contentType = fileData.getContentType();
	
			// create record
			Record record = new Record();
			record._id = new ObjectId();
			record.app = authToken.pluginId;
			record.owner = authToken.ownerId;
			record.creator = authToken.executorId;
			record.created = DateTimeUtils.now();
			record.name = metaData.get("name")[0];
			record.description = metaData.containsKey("description") ? metaData.get("description")[0] : null;
			String[] formats = metaData.get("format");
			record.format = (formats != null && formats.length == 1) ? formats[0] : (contentType != null) ? contentType : "application/octet-stream";
			String[] contents = metaData.get("content");
			String[] subformats = metaData.get("subformat");
			record.subformat = (subformats != null && subformats.length == 1) ? subformats[0] : null;
			record.content =  (contents != null && contents.length == 1) ? contents[0] : "other";
						
			record.data = new BasicDBObject(CMaps
					.map("resourceType", "Binary")
					.map("type", "file")
					.map("title", filename)
					.map("contentType", contentType)
			        .map("size", file.length())
			);
			 			 						
			

		// save file with file storage utility
			
			if (authToken.space != null) {				
			    try {
					  RecordManager.instance.addRecord(authToken.executorId, record, new FileInputStream(file), filename, contentType);
			    } catch (FileNotFoundException e) {
				    	throw new InternalServerException("error.internal",e);
			    }
					
				Set<ObjectId> records = new HashSet<ObjectId>();
				records.add(record._id);
				RecordManager.instance.share(authToken.executorId, authToken.ownerId, authToken.targetAPS, records, false);
				
				if (authToken.space != null && authToken.space.autoShare != null && !authToken.space.autoShare.isEmpty()) {
					for (ObjectId autoshareAps : authToken.space.autoShare) {
						Consent consent = Consent.getByIdAndOwner(autoshareAps, authToken.ownerId, Sets.create("type"));
						if (consent != null) { 
						  RecordManager.instance.share(authToken.executorId, authToken.ownerId, autoshareAps, records, true);
						}
					}
				}
				
			} else {
				RecordManager.instance.addRecord(authToken.executorId, record, authToken.targetAPS);
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
	 * NOT FINISHED, EXPERIMENTAL CODE : Run a java plugin in a secure sandbox. 
	 * @return
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result run() throws JsonValidationException, AppException {		
		// validate json
		JsonNode json = request().body().asJson();		
		//JsonValidation.validate(json, "authToken", "properties", "fields");
		
		//Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		//Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
						
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
		
		MidataServer midataServer = new MidataServer(inf);
		FitbitImport test = new FitbitImport();
		test.run(midataServer);
						
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result generateId() throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();		
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());									
		return ok(new ObjectId().toString());
	}
	

	private static Promise<Result> badRequestPromise(final String errorMessage) {
		return Promise.promise(new Function0<Result>() {
			public Result apply() {
				return badRequest(errorMessage);
			}
		});
	}
}
