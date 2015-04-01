package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.LargeRecord;
import models.MemberKey;
import models.ModelException;
import models.Record;
import models.Space;
import models.Member;
import models.StudyParticipation;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.RecordToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
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

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	public static RecordToken getRecordTokenFromString(String id) {
        int pos = id.indexOf('.');
		
		if (pos > 0) {
		   return new RecordToken(id.substring(0,pos), id.substring(pos+1)); 
		} else if (id.length()>25) {
		   return RecordToken.decrypt(id);
		} else {
		   ObjectId userId = new ObjectId(request().username());
		   return new RecordToken(id, userId.toString());
		}
	}
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "_id");
		
		String id = JsonValidation.getString(json, "_id");
		RecordToken tk = getRecordTokenFromString(id);
		ObjectId userId = new ObjectId(request().username());
						   	
		   /*Member user = Member.getById(userId, Sets.create("myaps"));
		
		   Set<String> ids = RecordSharing.instance.listRecordIds(user._id, user.myaps);
		   if (ids.contains(id)) tk = new RecordToken(id, user.myaps.toString());
		   else {		
			  Set<Circle> circles = Circle.getAllByMember(userId);
			
		   	  for (Circle circle : circles) {
				ids = RecordSharing.instance.listRecordIds(userId, circle.aps);
				if (ids.contains(id)) tk = new RecordToken(id, circle.aps.toString());
			  }
		   }*/
		
		
		if (tk==null) return badRequest("Bad token");
		Record target = RecordSharing.instance.fetch(userId, tk);
						
		return ok(Json.toJson(target));
	}

	/**
	 * Returns record data for visualizations. Also fetches the information for large records.
	 */	
	static Result getRecordData(Map<String, Object> properties, Set<String> fields) throws ModelException {
		List<Record> records = new ArrayList<Record>(LargeRecord.getAll(properties, fields));		
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	@APICall
	public static Result getVisibleRecords() throws ModelException {
		// get own records
		ObjectId userId = new ObjectId(request().username());
		
		Member self = Member.getById(userId, Sets.create("myaps"));
		
		List<Record> records = new ArrayList<Record>();
		
		Set<String> fields = Sets.create("app","owner","creator","created","name","id");
		records.addAll(RecordSharing.instance.list(userId, self.myaps, RecordSharing.FULLAPS, fields));
		
		//Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		
		//List<Record> records = new ArrayList<Record>(Record.getAll(properties, fields));
		
		// get visible records
		Set<Circle> circles = Circle.getAllByMember(userId);
		
		for (Circle circle : circles) {
			records.addAll(RecordSharing.instance.list(userId, circle.aps, RecordSharing.FULLAPS, fields));
		}
		
		/*
		properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> visible = new ChainedSet<String>().add("visible").get();
		Member user = Member.get(properties, visible);
		
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		for (String userIdString : user.visible.keySet()) {
			visibleRecordIds.addAll(user.visible.get(userIdString));
		}
		Map<String, Set<ObjectId>> visibleRecords = new ChainedMap<String, Set<ObjectId>>().put("_id", visibleRecordIds).get();
		
		records.addAll(Record.getAll(visibleRecords, fields));
		*/		
		 
		Collections.sort(records);
		ReferenceTool.resolveOwners(records);
		return ok(Json.toJson(records));
	}
	
	@APICall
	public static Result getSharingInfo() throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		
		Map<String, Set<String>> circleResult = new HashMap<String, Set<String>>();
		Map<String, Set<String>> spaceResult = new HashMap<String, Set<String>>();
		Map<String, Set<String>> participationResult = new HashMap<String, Set<String>>();
		Map<String, Set<String>> memberkeyResult = new HashMap<String, Set<String>>();
		
		ObjectNode result = Json.newObject();
		ObjectNode shared = Json.newObject();
		result.put("shared", shared);
		
        Set<Circle> circles = Circle.getAllByOwner(userId);       
		for (Circle circle : circles) {
			circleResult.put(circle._id.toString(), RecordSharing.instance.listRecordIds(userId, circle.aps));
		}
		
		result.put("circles", Json.toJson(circles));
		shared.put("circles", Json.toJson(circleResult));
		
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("name", "aps"));
		for (Space space : spaces) {
			spaceResult.put(space._id.toString(), RecordSharing.instance.listRecordIds(userId, space.aps));
		}
		
		result.put("spaces", Json.toJson(spaces));
		shared.put("spaces", Json.toJson(spaceResult));
		
		Set<StudyParticipation> participations = StudyParticipation.getAllByMember(userId, Sets.create("studyName", "aps"));
		for (StudyParticipation participation : participations) {
			participationResult.put(participation._id.toString(), RecordSharing.instance.listRecordIds(userId, participation.aps));
		}
		
		result.put("participations", Json.toJson(participations));
		shared.put("participations", Json.toJson(participationResult));
		
		Set<MemberKey> memberkeys = MemberKey.getByMember(userId);
		for (MemberKey memberkey : memberkeys) {
		    memberkeyResult.put(memberkey._id.toString(), RecordSharing.instance.listRecordIds(userId, memberkey.aps));
		}
		result.put("memberkeys", Json.toJson(memberkeys));
		shared.put("memberkeys", Json.toJson(memberkeyResult));
		
		
		return ok(result);
	}

	@APICall
	public static Result search(String query) throws ModelException {
		// get the visible records
		ObjectId userId = new ObjectId(request().username());
		
		
		Member user = Member.getById(userId, Sets.create("myaps"));	
		
		Set<Circle> circles = Circle.getAllByMember(userId);
		
		// TODO use caching/incremental retrieval of results (scrolls)
		List<SearchResult> searchResults = Search.searchRecords(userId, circles, query);
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (SearchResult searchResult : searchResults) {
			recordIds.add(new ObjectId(searchResult.id));
		}
		
		List<Record> records = new ArrayList<Record>(Record.getAllByIds(recordIds, Sets.create("app","owner","creator","created","name","data")));
		
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	/**
	 * Updates the spaces the given record is in.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result updateSpaces(String recordIdString) throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "spaces");
		
		// update spaces
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);		
		Set<ObjectId> spaceIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("spaces")));
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		recordIds.add(recordId);
		
		Member owner = Member.getById(userId, Sets.create("myaps"));
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("aps"));
				
		for (Space space : spaces) {
		  if (spaceIds.contains(space._id)) {
			  RecordSharing.instance.share(userId, owner.myaps, space.aps, recordIds, false);			
		  } else {
			  RecordSharing.instance.unshare(userId, space.aps, recordIds);				
		  }
		}
		
		return ok();
	}

	/**
	 * Updates the circles the given record is shared with.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result updateSharing() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "type", "records", "started", "stopped");
		
		// validate request: record
		ObjectId userId = new ObjectId(request().username());
						
		Set<ObjectId> started = ObjectIdConversion.toObjectIds(JsonExtraction.extractStringSet(json.get("started")));
		Set<ObjectId> stopped = ObjectIdConversion.toObjectIds(JsonExtraction.extractStringSet(json.get("stopped")));
		Set<String> recordIds = JsonExtraction.extractStringSet(json.get("records"));
		String type = JsonValidation.getString(json, "type");
		
		// get owner
		Member owner = Member.getById(userId, Sets.create("myaps"));
		
		Map<String,Set<String>> records = new HashMap<String,Set<String>>();
		for (String recordId :recordIds) {
      	   RecordToken rt = getRecordTokenFromString(recordId);
      	   Set<String> recs = records.get(rt.apsId);
      	   if (recs == null) {
      		   recs = new HashSet<String>();
      		   records.put(rt.apsId, recs);
      	   }
      	   recs.add(rt.recordId);      	  
      	}
		
        for (ObjectId start : started) {
        	ObjectId aps = null;
        	boolean withMember = false;
        	switch (type) {
        	case "circles" :
        		Circle circle = Circle.getByIdAndOwner(start, userId, Sets.create("aps"));
        		aps = circle.aps;
        		withMember = true;
        		break;
        	case "spaces" :
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("aps"));
        		aps = space.aps;
        		break;
        	case "participations" :
        		StudyParticipation part = StudyParticipation.getById(start, Sets.create("aps"));
        		aps = part.aps;
        		break;
        	case "memberkeys" :
        		MemberKey memberkey = MemberKey.getById(start);
        		aps = memberkey.aps;
        		withMember = true;
        		break;
        	}
        	        	
        	for (String sourceAps :records.keySet()) {        	  
        	  RecordSharing.instance.share(userId, new ObjectId(sourceAps), aps, ObjectIdConversion.toObjectIds(records.get(sourceAps)), withMember);
        	}        	
        }
        
        for (ObjectId start : stopped) {
        	ObjectId aps = null;        	
        	switch (type) {
        	case "circles" :
        		Circle circle = Circle.getByIdAndOwner(start, userId, Sets.create("aps"));
        		aps = circle.aps;        		
        		break;
        	case "spaces" :
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("aps"));
        		aps = space.aps;
        		break;
        	case "participations" :
        		StudyParticipation part = StudyParticipation.getById(start, Sets.create("aps"));
        		aps = part.aps;
        		break;
        	case "memberkeys" :
        		MemberKey memberkey = MemberKey.getById(start);
        		aps = memberkey.aps;
        		break;
        	}        
        	        
        	for (String sourceAps :records.keySet()) {        	  
          	  RecordSharing.instance.unshare(userId, aps, ObjectIdConversion.toObjectIds(records.get(sourceAps)));
          	}            	
        }
		
				
		return ok();
	}

	/**
	 * Get the file associated with a record.
	 */
	@APICall
	public static Result getFile(String id) throws ModelException {
		
		RecordToken tk = getRecordTokenFromString(id);
		ObjectId userId = new ObjectId(request().username());
						
		if (tk==null) return badRequest("Bad token");
		Record target = RecordSharing.instance.fetch(userId, tk);
		if (target==null) return badRequest("Unknown Record");
		
		FileData fileData = FileStorage.retrieve(new ObjectId(tk.recordId));
		response().setHeader("Content-Disposition", "attachment; filename=" + fileData.filename);
		return ok(fileData.inputStream);
	}
}
