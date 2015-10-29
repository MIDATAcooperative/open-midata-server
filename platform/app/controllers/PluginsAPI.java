package controllers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.LargeRecord;
import models.Member;
import models.Record;
import models.Space;
import models.enums.UserRole;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.access.AccessLog;
import utils.access.RecordManager;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
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
import actions.VisualizationCall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

// not secured, accessible from visualizations server (only with valid authToken)
public class PluginsAPI extends Controller {

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
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
				
		Set<ObjectId> tokens = ObjectIdConversion.toObjectIds(RecordManager.instance.listRecordIds(spaceToken.userId, spaceToken.spaceId));		
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
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		if (spaceToken.recordId != null)  {
		   ObjectNode result = Json.newObject();
		   result.put("readonly", true);
		   return ok(result);
		}
		
		BSONObject meta = RecordManager.instance.getMeta(spaceToken.userId, spaceToken.spaceId, "_config");
		
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
		
		JsonValidation.validate(json, "authToken", "config");
		
		// decrypt authToken 
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		
		RecordManager.instance.setMeta(spaceToken.userId, spaceToken.spaceId, "_config", config);
						
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
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		String name = JsonValidation.getString(json, "name");
		Space current = Space.getByIdAndOwner(spaceToken.spaceId, spaceToken.userId, Sets.create("context", "visualization", "app"));
		if (current == null) throw new BadRequestException("error.space.missing", "The current space does no longer exist.");
		
		Space space = Spaces.add(spaceToken.userId, name, current.visualization, current.app, current.context);		
		BSONObject bquery = RecordManager.instance.getMeta(spaceToken.userId, spaceToken.spaceId, "_query");		
		Map<String, Object> query;
		if (bquery != null) {
			query = bquery.toMap();
						
			RecordManager.instance.shareByQuery(spaceToken.userId, spaceToken.userId, space._id, query);
		}		
		RecordManager.instance.setMeta(spaceToken.userId, space._id, "_config", config);
						
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
		
		Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
			
		if (authToken.recordId != null) properties.put("_id", authToken.recordId);

		// get record data
		Collection<Record> records = null;
		
		AccessLog.debug("NEW QUERY");
		
		records = LargeRecord.getAll(authToken.userId, authToken.spaceId, properties, fields);		  
				
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(JsonOutput.toJson(records, "Record", fields));
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
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		ObjectId recordId = JsonValidation.getObjectId(json, "_id");			
		FileData fileData = RecordManager.instance.fetchFile(authToken.userId, new RecordToken(recordId.toString(), authToken.spaceId.toString()));
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
	@VisualizationCall
	public static Result createRecord() throws AppException, JsonValidationException {
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "description", "format", "content");
		
		// decrypt authToken 
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		if (authToken.recordId != null) return badRequest("This view is readonly.");
		
		Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.userId, Sets.create("visualization", "app", "aps", "autoShare"));
		if (space == null) throw new BadRequestException("error.space.missing", "The current space does no longer exist.");
		
		ObjectId appId = space.visualization;
				
		Member targetUser;
		ObjectId targetAps = space._id;
				
		targetUser = Member.getById(authToken.userId, Sets.create("myaps", "tokens"));
		if (targetUser == null) return badRequest("Invalid authToken.");
		//owner = targetUser;
		
							
		// save new record with additional metadata
		if (!json.get("data").isTextual() || !json.get("name").isTextual() || !json.get("description").isTextual()) {
			return badRequest("At least one request parameter is of the wrong type.");
		}
		
		Map<String,String> tokens = targetUser.tokens.get(appId.toString());		
		
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
		record.owner = authToken.userId;
		record.creator = authToken.userId;
		record.created = DateTimeUtils.now();
		
		if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}
		
		record.format = format;
		record.content = content;
		
		String stream = tokens!=null ? tokens.get("stream") : null;
		if (stream!=null) { record.stream = new ObjectId(stream); record.direct = true; }
		
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			return badRequest("Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		RecordManager.instance.addRecord(targetUser._id, record);
				
		Set<ObjectId> records = new HashSet<ObjectId>();
		records.add(record._id);
		RecordManager.instance.share(targetUser._id, targetUser._id, targetAps, records, false);
		
		if (space.autoShare != null && !space.autoShare.isEmpty()) {
			for (ObjectId autoshareAps : space.autoShare) {
				Consent consent = Consent.getByIdAndOwner(autoshareAps, targetUser._id, Sets.create("type"));
				if (consent != null) { 
				  RecordManager.instance.share(targetUser._id, targetUser._id, autoshareAps, records, true);
				}
			}
		}
				
		return ok();
	}
}
