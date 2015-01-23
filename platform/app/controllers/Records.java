package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.LargeRecord;
import models.ModelException;
import models.Record;
import models.Space;
import models.Member;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.records;
import views.html.details.record;
import views.html.dialogs.authorized;
import views.html.dialogs.createrecords;
import views.html.dialogs.importrecords;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Records extends Controller {

	public static Result index() {
		return ok(records.render());
	}

	public static Result filter(String filters) {
		return index();
	}

	public static Result details(String recordIdString) {
		return ok(record.render());
	}

	public static Result create(String appIdString) {
		return ok(createrecords.render());
	}

	public static Result importRecords(String appIdString) {
		return ok(importrecords.render());
	}

	public static Result onAuthorized(String appIdString) {
		return ok(authorized.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get records
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	/**
	 * Returns record data for visualizations. Also fetches the information for large records.
	 */
	static Result getRecordData(Map<String, Object> properties, Set<String> fields) {
		List<Record> records;
		try {
			records = new ArrayList<Record>(LargeRecord.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	public static Result getVisibleRecords() {
		// get own records
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("app").add("owner").add("creator").add("created").add("name").get();
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.getAll(properties, fields));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// get visible records
		properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> visible = new ChainedSet<String>().add("visible").get();
		Member user;
		try {
			user = Member.get(properties, visible);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		for (String userIdString : user.visible.keySet()) {
			visibleRecordIds.addAll(user.visible.get(userIdString));
		}
		Map<String, Set<ObjectId>> visibleRecords = new ChainedMap<String, Set<ObjectId>>().put("_id", visibleRecordIds).get();
		try {
			records.addAll(Record.getAll(visibleRecords, fields));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	public static Result search(String query) {
		// get the visible records
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("visible").get();
		Member user;
		try {
			user = Member.get(properties, fields);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// TODO use caching/incremental retrieval of results (scrolls)
		List<SearchResult> searchResults = Search.searchRecords(userId, user.visible, query);
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (SearchResult searchResult : searchResults) {
			recordIds.add(new ObjectId(searchResult.id));
		}

		// get records
		Map<String, Set<ObjectId>> recordProperties = new ChainedMap<String, Set<ObjectId>>().put("_id", recordIds).get();
		fields = new ChainedSet<String>().add("app").add("owner").add("creator").add("created").add("name").add("data").get();
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.getAll(recordProperties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	/**
	 * Updates the spaces the given record is in.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result updateSpaces(String recordIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "spaces");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// update spaces
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);
		Set<ObjectId> spaceIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("spaces")));
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("records").get();
		try {
			Set<Space> spaces = Space.getAll(properties, fields);
			for (Space space : spaces) {
				if (spaceIds.contains(space._id) && !space.records.contains(recordId)) {
					space.records.add(recordId);
					Space.set(space._id, "records", space.records);
				} else {
					space.records.remove(recordId);
					Space.set(space._id, "records", space.records);
				}
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Updates the circles the given record is shared with.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result updateSharing(String recordIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "started", "stopped");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request: record
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);
		try {
			if (!Record.exists(new ChainedMap<String, ObjectId>().put("_id", recordId).put("owner", userId).get())) {
				return badRequest("No record with this id exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// extract circle ids from posted data
		Set<ObjectId> startedCircleIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("started")));
		Set<ObjectId> stoppedCircleIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("stopped")));

		// get all circles
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("shared").add("members").get();
		try {
			Set<Circle> circles = Circle.getAll(properties, fields);

			// update 'shared' field of circles
			Set<ObjectId> startedMembers = new HashSet<ObjectId>();
			for (Circle circle : circles) {
				if (startedCircleIds.contains(circle._id)) {
					circle.shared.add(recordId);
					Circle.set(circle._id, "shared", circle.shared);
					startedMembers.addAll(circle.members);
				}
			}
			Set<ObjectId> stoppedMembers = new HashSet<ObjectId>();
			for (Circle circle : circles) {
				if (stoppedCircleIds.contains(circle._id)) {
					circle.shared.remove(recordId);
					Circle.set(circle._id, "shared", circle.shared);
					stoppedMembers.addAll(circle.members);
				}
			}

			// make record visible to started and invisible to stopped members
			Set<ObjectId> recordIds = new ChainedSet<ObjectId>().add(recordId).get();
			Users.makeVisible(userId, recordIds, startedMembers);
			Users.makeInvisible(userId, recordIds, stoppedMembers, circles);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Add a set of records to one or more spaces.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result showInSpaces() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "spaces", "records");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// update spaces
		ObjectId userId = new ObjectId(request().username());
		Set<ObjectId> spaceIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("spaces")));
		Set<ObjectId> recordIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("records")));
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("records").get();
		try {
			Set<Space> spaces = Space.getAll(properties, fields);
			for (Space space : spaces) {
				if (spaceIds.contains(space._id)) {
					int originalSize = space.records.size();
					space.records.addAll(recordIds);
					if (space.records.size() > originalSize) {
						Space.set(space._id, "records", space.records);
					}
				}
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Share a set of records with possibly multiple circles.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result shareWithCircles() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "circles", "records");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// update circles
		ObjectId userId = new ObjectId(request().username());
		Set<ObjectId> circleIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("circles")));
		Set<ObjectId> recordIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("records")));
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("shared").add("members").get();
		try {
			Set<Circle> circles = Circle.getAll(properties, fields);

			// update 'shared' field of circles and collect all members
			Set<ObjectId> members = new HashSet<ObjectId>();
			for (Circle circle : circles) {
				if (circleIds.contains(circle._id)) {
					int originalSize = circle.shared.size();
					circle.shared.addAll(recordIds);
					if (circle.shared.size() > originalSize) {
						Circle.set(circle._id, "shared", circle.shared);
						members.addAll(circle.members);
					}
				}
			}

			// make record visible to members
			Users.makeVisible(userId, recordIds, members);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Get the file associated with a record.
	 */
	public static Result getFile(String id) {
		FileData fileData = FileStorage.retrieve(new ObjectId(id));
		response().setHeader("Content-Disposition", "attachment; filename=" + fileData.filename);
		return ok(fileData.inputStream);
	}
}
