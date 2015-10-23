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
import models.Consent;
import models.FilterRule;
import models.ContentInfo;
import models.FormatInfo;
import models.LargeRecord;
import models.MemberKey;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.Member;
import models.StudyParticipation;
import models.User;
import models.enums.ConsentType;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import scala.NotImplementedError;
import utils.access.RecordManager;
import utils.access.APS;
import utils.auth.AnyRoleSecured;
import utils.auth.RecordToken;
import utils.auth.MemberSecured;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.dialogs.authorized;


import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * functions for handling the records
 *
 */
public class Records extends APIController {

	@Security.Authenticated(AnyRoleSecured.class)
	public static Result onAuthorized(String appIdString) {
		return ok(authorized.render());
	}
	
	/**
	 * parse a string into a record token. Three different formats are supported
	 * @param id record ID to be parsed
	 * @return
	 */
	protected static RecordToken getRecordTokenFromString(String id) {
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
	
	/**
	 * retrieve a specific record
	 * @return record
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "_id");
		
		// get parameters
		String id = JsonValidation.getString(json, "_id");
		RecordToken tk = getRecordTokenFromString(id);								   		
		if (tk==null) return badRequest("Bad token");
		ObjectId userId = new ObjectId(request().username());
		
		// execute
		Record target = RecordManager.instance.fetch(userId, tk);
						
		return ok(JsonOutput.toJson(target, "Record", Record.ALL_PUBLIC));
	}		
	
	/**
	 * retrieve a list of records matching some criteria. Can retrieve only records the user has access to.
	 * @return list of records
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getRecords() throws AppException, JsonValidationException {
 	
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "properties", "fields");
						
		ObjectId aps = JsonValidation.getObjectId(json, "aps");
		if (aps == null) aps = userId;
		List<Record> records = new ArrayList<Record>();
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
					
	    records.addAll(RecordManager.instance.list(userId, aps, properties, fields));	
				
		Collections.sort(records);
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(JsonOutput.toJson(records, "Record", fields));
	}
	
	/**
	 * retrieve aggregated information about records matching some criteria
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getInfo() throws AppException, JsonValidationException {
 	
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "properties");
						
		ObjectId aps = JsonValidation.getObjectId(json, "aps");
		if (aps == null) aps = userId;		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));			
		
	    Collection<RecordsInfo> result = RecordManager.instance.info(userId, aps, properties);	
						
		return ok(Json.toJson(result));
	}
	
	/**
	 * retrieve record IDs and query of an access permission set (consent or space)	
	 * @param aps ID of consent or space
	 * @return
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getSharingDetails(String aps) throws AppException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId apsId = new ObjectId(aps);
				
		Map<String, Object> query = Circles.getQueries(userId, apsId);
		if (query == null) {			
			BSONObject b = RecordManager.instance.getMeta(userId, apsId, APS.QUERY);
			if (b!=null) query = b.toMap();
		}
		Set<String> recordsIds = RecordManager.instance.listRecordIds(userId, apsId);
		
		ObjectNode result = Json.newObject();
		
		result.put("records", Json.toJson(recordsIds));
		result.put("query", Json.toJson(query));
				
		return ok(result);
		
	}

	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result search(String query) throws AppException {
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
		
		Set<String> fields = Sets.create("app","owner","creator","created","name","data");
		List<Record> records = new ArrayList<Record>(Record.getAllByIds(recordIds, fields));
		
		Collections.sort(records);
		return ok(JsonOutput.toJson(records, "Record", fields));
	}
	

	/**
	 * Updates the spaces the given record is in.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result updateSpaces(String recordIdString) throws JsonValidationException, AppException {
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
			  RecordManager.instance.share(userId, owner.myaps, space._id, recordIds, false);			
		  } else {
			  RecordManager.instance.unshare(userId, space._id, recordIds);				
		  }
		}
		
		return ok();
	}
	
	/**
	 * automatically share all records created in a space into a consent. Used for tasks
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result share() throws JsonValidationException, InternalServerException {
	
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "fromSpace", "toConsent");
		
		ObjectId userId = new ObjectId(request().username());
		ObjectId fromSpace = JsonValidation.getObjectId(json, "fromSpace");
		ObjectId toConsent = JsonValidation.getObjectId(json, "toConsent");
		
		Space space = Space.getByIdAndOwner(fromSpace, userId, Sets.create("autoShare"));
		if (space == null) return badRequest("Bad space.");
		
		Consent consent = Consent.getByIdAndOwner(toConsent, userId, Sets.create("type"));
		if (consent == null) return badRequest("Bad consent.");
		
		if (space.autoShare == null) space.autoShare = new HashSet<ObjectId>();
		space.autoShare.add(toConsent);
		Space.set(space._id, "autoShare", space.autoShare);
								
		return ok();
	}
	
	/**
	 * delete a record of the current user.
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result delete() throws JsonValidationException, AppException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "_id");
		
		String id = JsonValidation.getString(json, "_id");
		RecordToken tk = getRecordTokenFromString(id);
		ObjectId userId = new ObjectId(request().username());
		
		RecordManager.instance.deleteRecord(userId, tk);
		return ok();
	}
	
	/**
	 * changes sharing of records to consents and/or spaces
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result updateSharing() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "records", "started", "stopped");
		
		// validate request: record
		ObjectId userId = new ObjectId(request().username());
						
		Set<ObjectId> started = ObjectIdConversion.toObjectIds(JsonExtraction.extractStringSet(json.get("started")));
		Set<ObjectId> stopped = ObjectIdConversion.toObjectIds(JsonExtraction.extractStringSet(json.get("stopped")));
		Set<String> recordIds = JsonExtraction.extractStringSet(json.get("records"));		
		Map<String, Object> query = json.has("query") ? JsonExtraction.extractMap(json.get("query")) : null;
		
		// get owner
		User owner = User.getById(userId, Sets.create("myaps"));
		
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
        	
        	boolean withMember = false;
        	Consent consent = Consent.getByIdAndOwner(start, userId, Sets.create("type"));
        	if (consent == null) {
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("_id"));
        		if (space == null) {
        		  throw new BadRequestException("error.unknown.consent", "Consent not found");
        		}
        	} else {        	
        	  ConsentType type = consent.type;
        	  withMember = !type.equals(ConsentType.STUDYPARTICIPATION);
        	}        	         	
        	        	
        	for (String sourceAps :records.keySet()) {        	  
        	  RecordManager.instance.share(userId, new ObjectId(sourceAps), start, ObjectIdConversion.toObjectIds(records.get(sourceAps)), withMember);
        	}    
        	
        	if (query != null) {
        		if (consent == null) {
        		  RecordManager.instance.shareByQuery(userId, userId, start, query);
        		} else {
        		  Circles.setQuery(userId, start, query);        		          		  
	        	  RecordManager.instance.applyQuery(userId, query, userId, start, withMember);	        	  
        		}
        	}
        }
        
        for (ObjectId start : stopped) {

        	boolean withMember = false;
        	Consent consent = Consent.getByIdAndOwner(start, userId, Sets.create("type"));
        	if (consent == null) {
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("_id"));
        		if (space == null) {
        		  throw new BadRequestException("error.unknown.consent", "Consent not found");
        		}
        	} else {        	
        	  ConsentType type = consent.type;
        	  withMember = !type.equals(ConsentType.STUDYPARTICIPATION);
        	}        	         	
        	        	
        	for (String sourceAps :records.keySet()) {        	  
        	  RecordManager.instance.unshare(userId, start, ObjectIdConversion.toObjectIds(records.get(sourceAps)));
        	}    
        	
        	if (query != null) {
        		if (consent == null) {
        		  RecordManager.instance.shareByQuery(userId, userId, start, query);
        		} else {
        		  Circles.setQuery(userId, start, query);        		          		  
	        	  RecordManager.instance.applyQuery(userId, query, userId, start, withMember);	        	  
        		}
        	}
        	        	
        }
		
				 
		return ok();
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getRecordUrl(String recordIdString) throws AppException {
		ObjectId userId = new ObjectId(request().username());
		RecordToken tk = Records.getRecordTokenFromString(recordIdString);
		
		Record record = RecordManager.instance.fetch(userId, tk, Sets.create("format","created"));
		if (record == null) return badRequest("Record not found!");
		if (record.format == null) return ok();
		
		FormatInfo format = FormatInfo.getByName(record.format);
		if (format == null || format.visualization == null) return ok();
		
		Plugin visualization = Plugin.getById(format.visualization, Sets.create("filename", "url"));
					
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(new ObjectId(tk.apsId), userId, new ObjectId(tk.recordId));
		
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualization.filename + "/" + visualization.url;
		url = url.replace(":authToken", spaceToken.encrypt());
		return ok(url);						
	}

	/**
	 * download a file associated with a record
	 * @param id record ID token
	 * @return file
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getFile(String id) throws AppException {
		
		RecordToken tk = getRecordTokenFromString(id);
		ObjectId userId = new ObjectId(request().username());
						
		if (tk==null) return badRequest("Bad token");
				
		FileData fileData = RecordManager.instance.fetchFile(userId, tk);
		response().setHeader("Content-Disposition", "attachment; filename=" + fileData.filename);
		return ok(fileData.inputStream);
	}
}
