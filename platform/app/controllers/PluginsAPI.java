package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

import actions.VisualizationCall;
import models.Admin;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.StudyRelated;
import models.User;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.UserRole;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
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
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.access.AccessContext;
import utils.access.AccountCreationAccessContext;
import utils.access.ConsentAccessContext;
import utils.access.DBRecord;
import utils.access.RecordConversion;
import utils.access.RecordManager;
import utils.access.UserGroupAccessContext;
import utils.auth.ExecutionInfo;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage.FileData;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.stats.Stats;

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
		SpaceToken spaceToken = SpaceToken.decryptAndSession(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
				
		Set<MidataId> tokens = ObjectIdConversion.toMidataIds(RecordManager.instance.listRecordIds(spaceToken.executorId, spaceToken.spaceId));		
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
		SpaceToken spaceToken = SpaceToken.decryptAndSession(request(), json.get("authToken").asText());
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
	 * retrieve oauth extra json for a space. 
	 * @return json previously stored with setConfig
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getOAuthParams() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decryptAndSession(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		
		BSONObject meta = RecordManager.instance.getMeta(spaceToken.executorId, spaceToken.spaceId, "_oauthParams");
		
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
		Stats.startRequest(request());
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken 
		SpaceToken spaceToken = SpaceToken.decryptAndSession(request(), json.get("authToken").asText());
		if (spaceToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		Stats.setPlugin(spaceToken.pluginId);
		
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
				
		Stats.finishRequest(request(), "200");
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
		SpaceToken spaceToken = SpaceToken.decryptAndSession(request(), json.get("authToken").asText());
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
		Stats.startRequest(request());
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
						
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
		Stats.setPlugin(inf.pluginId);
		
		Collection<Record> records = getRecords(inf, properties, fields);
				
		Stats.finishRequest(request(), "200", properties.keySet());
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
		
		records = RecordManager.instance.list(inf.executorId, inf.context, properties, fields);		  
						
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
		Stats.startRequest(request());
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "authToken", "properties", "summarize");
		
		// decrypt authToken 
		ExecutionInfo authToken = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
		//SpaceToken authToken = SpaceToken.decryptAndSession(request(), json.get("authToken").asText());
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		Stats.setPlugin(authToken.pluginId);		
		MidataId targetAps = authToken.targetAPS;
		
		Collection<RecordsInfo> result;

		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = json.has("fields") ? JsonExtraction.extractStringSet(json.get("fields")) : Sets.create();
		
		if (authToken.recordId != null) {
			Collection<Record> record = RecordManager.instance.list(authToken.executorId, authToken.context, CMaps.map("_id", authToken.recordId), Sets.create("owner", "content", "format", "group"));
			result = new ArrayList<RecordsInfo>();
			for (Record r : record) result.add(new RecordsInfo(r));			
		} else {
							
			AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);		
		    result = RecordManager.instance.info(authToken.executorId, targetAps, authToken.context, properties, aggrType);

		    

		}
	    if (fields.contains("ownerName")) ReferenceTool.resolveOwnersForRecordsInfo(result, true);
	    
	    Stats.finishRequest(request(), "200", properties.keySet());
		return ok(JsonOutput.toJson(result, "Record", Record.ALL_PUBLIC));
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
		Stats.startRequest(request());
	
		String authTokenStr = request().getQueryString("authToken");
		String id = request().getQueryString("id");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken authToken = SpaceToken.decryptAndSession(request(), authTokenStr);
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		Stats.setPlugin(authToken.pluginId);
		
		MidataId recordId = new MidataId(id);			
		FileData fileData = RecordManager.instance.fetchFile(authToken.executorId, new RecordToken(recordId.toString(), authToken.spaceId.toString()));
		if (fileData == null) return badRequest();
		setAttachmentContentDisposition(fileData.filename);
		
		Stats.finishRequest(request(), "200");
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
		Stats.startRequest(request());		
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		
		ExecutionInfo authToken = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
		Stats.setPlugin(authToken.pluginId);
		if (authToken.recordId != null) throw new BadRequestException("error.internal", "This view is readonly.");
																														
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		
		String content = JsonValidation.getStringOrNull(json, "content");
		Set<String> code = JsonExtraction.extractStringSet(json.get("code"));
		
		
		Record record = new Record();
		if (json.has("_id")) {
			record._id = JsonValidation.getMidataId(json, "_id");			
		} else {
		    record._id = new MidataId();
		}
		record.app = authToken.pluginId;
		record.owner = authToken.ownerId;
		record.creator = authToken.executorId;
		record.created = record._id.getCreationDate();
		
		/*if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}*/
		
		record.format = format;
		
		
		ContentInfo.setRecordCodeAndContent(record, code, content);		
					
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		createRecord(authToken, record);
		
		Stats.finishRequest(request(), "200", Collections.EMPTY_SET);
		return ok();
	}
	
	/**
	 * Helper function to create a record
	 * @param inf execution context
	 * @param record record to add to the database
	 * @throws AppException
	 */
	public static void createRecord(ExecutionInfo inf, Record record) throws AppException  {
       createRecord(inf, record, null, null, null, inf.context);		
	}
	
	public static void createRecord(ExecutionInfo inf, Record record, AccessContext context) throws AppException  {
	    createRecord(inf, record, null, null, null, context);		
	}
	
	public static void createRecord(ExecutionInfo inf, Record record, InputStream fileData, String fileName, String contentType, AccessContext context) throws AppException  {
		if (record.format==null) record.format = "application/json";
		if (record.content==null) record.content = "other";
		if (record.owner==null) record.owner = inf.ownerId;
		
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
        	
		if (!record.owner.equals(inf.executorId) && !inf.executorId.equals(RuntimeConstants.instance.autorunService) && !(context instanceof ConsentAccessContext) && !(context instanceof AccountCreationAccessContext)) {
			BSONObject query = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_query");
			Set<Consent> consent = null;
			if (query != null && query.containsField("link-study")) {
				MidataId groupId = MidataId.from(query.get("link-study"));
                UserGroupMember ugm = UserGroupMember.getByGroupAndMember(groupId, inf.executorId);
                if (ugm != null) context = RecordManager.instance.createContextForUserGroup(ugm, context);
				consent = Consent.getHealthcareOrResearchActiveByAuthorizedAndOwner(groupId, record.owner);
			} else {
			    consent = Consent.getHealthcareOrResearchActiveByAuthorizedAndOwner(inf.executorId, record.owner);
			}
									
			if (consent == null || consent.isEmpty()) throw new BadRequestException("error.noconsent", "No active consent that allows to add data for target person.");
			AccessContext contextWithConsent = null;
			for (Consent c : consent) {
				ConsentAccessContext cac = new ConsentAccessContext(c, context);
				if (cac.mayCreateRecord(dbrecord)) {
					contextWithConsent = cac;
					break;
				}
			}
			if (contextWithConsent == null) throw new InternalServerException("error.internal", "Record may not be created!");
			context = contextWithConsent;
		} else if (!context.mayCreateRecord(dbrecord)) {
			throw new InternalServerException("error.internal", "Record may not be created!");			
		}
		
		//MidataId targetAPS = targetConsent != null ? targetConsent : inf.targetAPS;
		
		if (fileData != null) {
			  RecordManager.instance.addRecord(context, record, context.getTargetAps(), fileData, fileName, contentType);
		} else {
			  RecordManager.instance.addRecord(context, record, context.getTargetAps());
		}
		
		Set<MidataId> records = Collections.singleton(record._id);
								    				
		AccessContext myContext = context;		
		
		if (inf.executorId.equals(inf.ownerId)) {
			while (myContext != null) {
				if (!myContext.isIncluded(dbrecord)) {
					RecordManager.instance.share(inf.executorId, inf.ownerId, myContext.getTargetAps(), records, false);
				}
				myContext = myContext.getParent();
			}									
		} else {
			myContext = myContext.getParent();
			while (myContext != null) {
				if (!myContext.isIncluded(dbrecord)) {
					RecordManager.instance.share(inf.executorId, context.getTargetAps(), myContext.getTargetAps(), records, false);
				}
				myContext = myContext.getParent();
			}					
		}
		
		/*
		if (inf.space != null && inf.space.autoShare != null && !inf.space.autoShare.isEmpty()) {
			for (MidataId autoshareAps : inf.space.autoShare) {
				Consent consent = Consent.getByIdAndOwner(autoshareAps, inf.ownerId, Sets.create("type"));
				if (consent != null) { 
				  RecordManager.instance.share(inf.executorId, inf.space._id, autoshareAps, records, true);
				}
			}
		}
		*/
		
		/* Publication of study results */ 
		if (record.owner.equals(inf.executorId)) {
			BSONObject query = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_query");
			if (query != null && query.containsField("target-study")) {
				Map<String, Object> q = query.toMap(); 
				MidataId studyId = MidataId.from(q.get("target-study"));
				Object groupObj = q.get("target-study-group");
				String group = groupObj != null ? groupObj.toString() : null;
				Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));
				if (!srs.isEmpty()) {
					for (StudyRelated sr : srs ) {
					  RecordManager.instance.share(inf.executorId, inf.ownerId, sr._id, records, false);
					}
				}
			}
		}
	}
	
	/**
	 * Helper method for OAuth 1.0 apps: Need to compute signature based on consumer secret, which should stay in the backend.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result oAuth1Call() throws AppException {
	
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());
				
		BSONObject oauthMeta = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_oauth");
    	if (oauthMeta == null) throw new BadRequestException("error.notauthorized.action", "No valid oauth credentials.");
		Map<String, Object> tokens = oauthMeta.toMap();	
						
		String oauthToken, oauthTokenSecret;
		MidataId appId;
		
		oauthToken = (String) tokens.get("oauthToken");
		oauthTokenSecret = (String) tokens.get("oauthTokenSecret");
		appId = new MidataId(tokens.get("appId").toString());
		

		// also get the consumer key and secret
				
		Plugin app;
		try {			
				app = Plugin.getById(appId, Sets.create("consumerKey", "consumerSecret"));
				if (app == null) return badRequest("Invalid authToken");			
		} catch (InternalServerException e) {
			return badRequest(e.getMessage());
		}

		// perform the api call
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		RequestToken token = new RequestToken(oauthToken, oauthTokenSecret);
		AccessLog.log(app.consumerKey);
		AccessLog.log(app.consumerSecret);
		AccessLog.log(oauthToken);
		AccessLog.log(oauthTokenSecret);
		OAuthCalculator calc = new OAuthCalculator(key, token);
		try {
		String signed = calc.sign(json.get("url").asText());
		AccessLog.log(signed);
		URL target = new URL(signed);
		
		HttpURLConnection con = (HttpURLConnection) target.openConnection();
		con.connect();
		InputStream str = con.getInputStream();
		response().setContentType(con.getContentType());
		//for (String hn : con.getHeaderFields().keySet()) response().setHeader(hn, con.getHeaderField(hn));		
		return status(con.getResponseCode(), str);
		
		/*return wsh.get().map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				return ok(response.asJson());
			}
		});*/
		
		} catch (OAuthCommunicationException e) {
			throw new InternalServerException("error.internal", e);
		} catch (OAuthExpectationFailedException e2) {
			throw new InternalServerException("error.internal", e2);
		} catch (OAuthMessageSignerException e3) {
			throw new InternalServerException("error.internal", e3);		
		} catch (MalformedURLException e4) {
			throw new InternalServerException("error.internal", e4);
		} catch (IOException e5) {
			throw new InternalServerException("error.internal", e5);
		}
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
        Stats.startRequest(request());				
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
		
		record._id = JsonValidation.getMidataId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 		
		//record.app = authToken.pluginId;
		//record.owner = authToken.ownerId;
		record.creator = authToken.executorId;
		record.lastUpdated = new Date();		
							
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
				
		updateRecord(authToken, record);
			
		Stats.finishRequest(request(), "200", Collections.EMPTY_SET);
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
		return RecordManager.instance.updateRecord(inf.executorId, inf.context, record);				
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
				String ct = response.getHeader("Content-Type");
				if (ct!=null) response().setContentType(ct);
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
				
    	BSONObject oauthMeta = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_oauth");
    	if (oauthMeta == null) throw new BadRequestException("error.notauthorized.action", "No valid oauth credentials.");
		Map<String, String> tokens = oauthMeta.toMap();				
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
		Stats.startRequest(request());
		// allow cross origin request from app server
		//String appServer = Play.application().configuration().getString("apps.server");
		//response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		try {
		
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
		Stats.setPlugin(authToken.pluginId);
		
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
			record._id = new MidataId();
			record.app = authToken.pluginId;
			record.owner = authToken.ownerId;
			record.creator = authToken.executorId;
			record.created = record._id.getCreationDate();
			record.name = metaData.get("name")[0];
			record.description = metaData.containsKey("description") ? metaData.get("description")[0] : null;
			String[] formats = metaData.get("format");
			record.format = (formats != null && formats.length == 1) ? formats[0] : (contentType != null) ? contentType : "application/octet-stream";
			String[] contents = metaData.get("content");
			String[] codes = metaData.get("code");
			
			ContentInfo.setRecordCodeAndContent(record, codes != null ? new HashSet<String>(Arrays.asList(codes)) : null, contents != null ? contents[0] : null);					
						
			if (metaData.containsKey("data")) {
				String data = metaData.get("data")[0];
				try {
					BasicDBObject att = new BasicDBObject(CMaps							
							.map("title", filename)
							.map("contentType", contentType)
					        .map("size", file.length())
					);
					record.data = (DBObject) JSON.parse(data);
					
					Object rt = record.data.get("resourceType");
					
					if (rt != null && rt.equals("Media")) {
					    record.data.put("content", att);	
					} else {
						BasicDBObject attachment = new BasicDBObject();
						attachment.put("attachment", att);
						BasicDBList contentArray = new BasicDBList();
						contentArray.add(attachment);
						record.data.put("content", contentArray);
					}
				} catch (JSONParseException e) {
					throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
				}
			} else {
			
				record.data = new BasicDBObject(CMaps
						.map("resourceType", "Binary")
						.map("type", "file")
						.map("title", filename)
						.map("contentType", contentType)
				        .map("size", file.length())
				);
			 		
			}
					
			createRecord(authToken, record, new FileInputStream(file), filename, contentType, authToken.context);
					
			Stats.finishRequest(request(), "200");
			ObjectNode obj = Json.newObject();
			obj.put("_id", record._id.toString());
			return ok(obj);
		} catch (AppException e) {
			return badRequest(e.getMessage());
		} 
			
		} catch (Exception e2) {
			ErrorReporter.report("Plugin API", ctx(), e2);
			return internalServerError(e2.getMessage());			
		} finally {
			ServerTools.endRequest();			
		}
		
	}
		
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result generateId() throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();		
		ExecutionInfo inf = ExecutionInfo.checkSpaceToken(request(), json.get("authToken").asText());									
		return ok(new MidataId().toString());
	}
	

	private static Promise<Result> badRequestPromise(final String errorMessage) {
		return Promise.promise(new Function0<Result>() {
			public Result apply() {
				return badRequest(errorMessage);
			}
		});
	}
}
