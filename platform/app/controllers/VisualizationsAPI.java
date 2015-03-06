package controllers;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Record;
import models.Space;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.auth.RecordToken;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

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
		
		Set<String> tokens = RecordSharing.instance.listRecordIds(spaceToken.userId, spaceToken.spaceId);		
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
		Object recordIdSet = properties.get("_id");
		Set<String> recordIds = (Set<String>) recordIdSet;
		
		/*
		if (recordIdSet instanceof Set<?>) {					
			RecordSharing.instance.fetch(recordIds);				
		} else {
			return badRequest("No set of record ids found.");
		}*/

		// get record data
		Set<Record> records = RecordSharing.instance.fetchMultiple(authToken.userId, authToken.spaceId, recordIds, fields);
		if (fields.contains("ownerName")) ReferenceTool.resolveOwners(records);
		return ok(Json.toJson(records));
	}
}
