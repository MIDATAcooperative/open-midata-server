package controllers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.HPUser;
import models.LargeRecord;
import models.Member;
import models.Record;
import models.Space;
import models.User;
import models.enums.UserRole;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
import utils.access.AccessLog;
import utils.access.RecordSharing;
import utils.auth.AppToken;
import utils.auth.RecordToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage;
import utils.db.ObjectIdConversion;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.ModelException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;
import actions.VisualizationCall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

// not secured, accessible from visualizations server (only with valid authToken)
public class PluginsAPI extends Controller {

	@VisualizationCall
	public static Result checkPreflight() {
		// allow cross-origin request from visualizations server
		/*String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");*/
		return ok();
	}
 
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getIds() throws AppException, JsonValidationException  {
		
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		
		Set<ObjectId> tokens = ObjectIdConversion.toObjectIds(RecordSharing.instance.listRecordIds(spaceToken.userId, spaceToken.spaceId));		
		return ok(Json.toJson(tokens));
	}

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
		
		BSONObject meta = RecordSharing.instance.getMeta(spaceToken.userId, spaceToken.spaceId, "_config");
		
		if (meta != null) return ok(Json.toJson(meta.toMap()));
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result setConfig() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "config");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		
		RecordSharing.instance.setMeta(spaceToken.userId, spaceToken.spaceId, "_config", config);
						
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result cloneAs() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "name", "config");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		String name = JsonValidation.getString(json, "name");
		Space current = Space.getByIdAndOwner(spaceToken.spaceId, spaceToken.userId, Sets.create("context", "visualization", "app"));
		
		Space space = Spaces.add(spaceToken.userId, name, current.visualization, current.app, current.context);
		
		BSONObject bquery = RecordSharing.instance.getMeta(spaceToken.userId, spaceToken.spaceId, "_query");		
		Map<String, Object> query;
		if (bquery != null) {
			query = bquery.toMap();
			
			/*if (json.has("query")) {
				
			}*/
			RecordSharing.instance.shareByQuery(spaceToken.userId, spaceToken.userId, space._id, query);
		}		
		RecordSharing.instance.setMeta(spaceToken.userId, space._id, "_config", config);
						
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result getRecords() throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Rights.chk("getRecords", UserRole.ANY, fields);

		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
	
		
		/*if (properties.containsKey("owner")) {
			if (properties.get("owner").equals("self")) properties.put("owner", authToken.userId.toString());
		}*/
		
		if (authToken.recordId != null) properties.put("_id", authToken.recordId);

		// get record data
		Collection<Record> records = null;
		
		AccessLog.debug("NEW QUERY");
		/*if (properties.containsKey("convert")) {
		   // Load direct result
		   records = LargeRecord.getAll(authToken.userId, authToken.spaceId, properties, fields);
		   
		   // Search for convertable
		   Map<String, Object> extended = new HashMap<String,Object>(properties);
		   extended.remove("format");
		   Set<String> candidates = RecordSharing.instance.listRecordIds(authToken.userId, authToken.spaceId, extended);
		   if (properties.containsKey("format")) {
			   // extended.put("format", properties.get("format"));
			   extended.put("part", properties.get("format"));
		   }
		   extended.put("document", ObjectIdConversion.toObjectIds(candidates));		   		 
		   records.addAll(RecordSharing.instance.list(authToken.userId, authToken.spaceId, extended, fields));
		   		   
		} else {*/
		   records = LargeRecord.getAll(authToken.userId, authToken.spaceId, properties, fields);		  
		//}
		
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(JsonOutput.toJson(records, "Record", fields));
	}
	
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
			
		FileData fileData = RecordSharing.instance.fetchFile(authToken.userId, new RecordToken(recordId.toString(), authToken.spaceId.toString()));
		if (fileData == null) return badRequest();
		//response().setHeader("Content-Disposition", "attachment; filename=" + fileData.filename);
		return ok(fileData.inputStream);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result createRecord() throws AppException, JsonValidationException {
		//response().setHeader("Access-Control-Allow-Origin", "*");
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "data", "name", "description", "format", "content");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		if (authToken.recordId != null) return badRequest("This view is readonly.");
		
		Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.userId, Sets.create("visualization", "app", "aps", "autoShare"));
		
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
		
		String data = json.get("data").asText();
		String name = json.get("name").asText();
		String description = json.get("description").asText();
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
		
		RecordSharing.instance.addRecord(targetUser._id, record);
				
		Set<ObjectId> records = new HashSet<ObjectId>();
		records.add(record._id);
		RecordSharing.instance.share(targetUser._id, targetUser._id, targetAps, records, false);
		
		if (space.autoShare != null && !space.autoShare.isEmpty()) {
			for (ObjectId autoshareAps : space.autoShare) {
				Consent consent = Consent.getByIdAndOwner(autoshareAps, targetUser._id, Sets.create("type"));
				if (consent != null) { 
				  RecordSharing.instance.share(targetUser._id, targetUser._id, autoshareAps, records, true);
				}
			}
		}
				
		return ok();
	}
}
