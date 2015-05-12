package controllers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import models.HPUser;
import models.LargeRecord;
import models.Member;
import models.ModelException;
import models.Record;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.auth.AppToken;
import utils.auth.RecordToken;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

// not secured, accessible from visualizations server (only with valid authToken)
public class VisualizationsAPI extends Controller {

	public static Result checkPreflight() {
		// allow cross-origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result getIds() throws ModelException, JsonValidationException  {
		// allow cross origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);

		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken spaceToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (spaceToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		/*
		Map<String, ObjectId> spaceProperties = new ChainedMap<String, ObjectId>().put("_id", spaceToken.spaceId)
				.put("owner", spaceToken.userId).get();
		try {
			if (!Space.exists(spaceProperties)) {
				return badRequest("Invalid authToken.");
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		
		Space space;
		try {
			space = Space.get(spaceProperties, new ChainedSet<String>().add("records").get());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}*/
		
		Set<ObjectId> tokens = ObjectIdConversion.toObjectIds(RecordSharing.instance.listRecordIds(spaceToken.userId, spaceToken.spaceId));		
		return ok(Json.toJson(tokens));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result getRecords() throws JsonValidationException, ModelException {
		// allow cross origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);

		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));

		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		/*
		Map<String, ObjectId> spaceProperties = new ChainedMap<String, ObjectId>().put("_id", authToken.spaceId)
				.put("owner", authToken.userId).get();
		try {
			if (!Space.exists(spaceProperties)) {
				return badRequest("Invalid authToken.");
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// get ids of records in this space
		Space space;
		try {
			space = Space.get(spaceProperties, new ChainedSet<String>().add("records").get());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
        */
		// filter out records that are not assigned to that space
		
		/* TODO
		Object recordIdSet = properties.get("_id");
		if (recordIdSet instanceof Set<?>) {
			Set<?> recordIds = (Set<?>) recordIdSet;
			Iterator<?> iterator = recordIds.iterator();
			while (iterator.hasNext()) {
				Object recordId = iterator.next();
				if (!space.records.contains(recordId)) {
					iterator.remove();
				}
			}
		} else {
			return badRequest("No set of record ids found.");
		}
		
		*/
		//Object recordIdSet = properties.get("_id");
		//Set<String> recordIds = (Set<String>) recordIdSet;
		
		/*
		if (recordIdSet instanceof Set<?>) {					
			RecordSharing.instance.fetch(recordIds);				
		} else {
			return badRequest("No set of record ids found.");
		}*/
		
		if (properties.containsKey("owner")) {
			if (properties.get("owner").equals("self")) properties.put("owner", authToken.userId.toString());
		}

		// get record data
		Collection<Record> records = LargeRecord.getAll(authToken.userId, authToken.spaceId, properties, fields);
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(Json.toJson(records));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result createRecord() throws ModelException, JsonValidationException {
		// allow cross origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);
		//response().setHeader("Access-Control-Allow-Origin", "*");
		
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "authToken", "data", "name", "description");
		
		// decrypt authToken and check whether space with corresponding owner exists
		SpaceToken authToken = SpaceToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		
		Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.userId, Sets.create("visualization", "app", "aps"));
		
		ObjectId appId = space.visualization;
			
		User owner;
		Member targetUser;
		ObjectId targetAps = space.aps;
				
		targetUser = Member.getByIdAndApp(authToken.userId, appId, Sets.create("myaps", "tokens"));
		if (targetUser == null) return badRequest("Invalid authToken.");
		owner = targetUser;
		
							
		// save new record with additional metadata
		if (!json.get("data").isTextual() || !json.get("name").isTextual() || !json.get("description").isTextual()) {
			return badRequest("At least one request parameter is of the wrong type.");
		}
		
		Map<String,String> tokens = targetUser.tokens.get(appId.toString());		
		
		String data = json.get("data").asText();
		String name = json.get("name").asText();
		String description = json.get("description").asText();
		String format = JsonValidation.getString(json, "format");
		if (format==null) format = "json";
		Record record = new Record();
		record._id = new ObjectId();
		record.app = appId;
		record.owner = authToken.userId;
		record.creator = authToken.userId;
		record.created = DateTimeUtils.now();
		record.format = format;
		
		String stream = tokens!=null ? tokens.get("stream") : null;
		if (stream!=null) { record.stream = new ObjectId(stream); record.direct = true; }
		
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			return badRequest("Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		RecordSharing.instance.addRecord(targetUser, record);
				
		Set<ObjectId> records = new HashSet<ObjectId>();
		records.add(record._id);
		RecordSharing.instance.share(targetUser._id, targetUser.myaps, targetAps, records, false);
				
		return ok();
	}
}
