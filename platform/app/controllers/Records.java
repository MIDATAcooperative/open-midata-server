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
import models.FormatInfo;
import models.Member;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.User;
import models.enums.AggregationType;
import models.enums.ConsentType;

import org.bson.BSONObject;
import models.MidataId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.access.APS;
import utils.access.Feature_FormatGroups;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.MemberSecured;
import utils.auth.RecordToken;
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
		   MidataId userId = new MidataId(request().username());
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
		if (tk==null) throw new BadRequestException("error.invalid.token", "Bad token");
		MidataId userId = new MidataId(request().username());
		
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
 	
		MidataId userId = new MidataId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "properties", "fields");
						
		MidataId aps = JsonValidation.getMidataId(json, "aps");
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
 	
		MidataId userId = new MidataId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "properties");
						
		MidataId aps = JsonValidation.getMidataId(json, "aps");
		if (aps == null) aps = userId;		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));			
		
		AggregationType aggrType = json.has("summarize") ? JsonValidation.getEnum(json, "summarize", AggregationType.class) : AggregationType.GROUP;
	    Collection<RecordsInfo> result = RecordManager.instance.info(userId, aps, properties, aggrType);	
						
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
		MidataId userId = new MidataId(request().username());
		MidataId apsId = new MidataId(aps);
				
		Map<String, Object> query = Circles.getQueries(userId, apsId);
		if (query == null) {			
			BSONObject b = RecordManager.instance.getMeta(userId, apsId, APS.QUERY);
			if (b!=null) {
				query = b.toMap();				
			}
		}
		if (query != null) Feature_FormatGroups.convertQueryToGroups("v1", query);
		Set<String> recordsIds = RecordManager.instance.listRecordIds(userId, apsId);
		
		Map<String, Object> props = new HashMap<String, Object>();
		Collection<RecordsInfo> infos = RecordManager.instance.info(userId, apsId, props, AggregationType.GROUP);
				
		ObjectNode result = Json.newObject();
		
		result.put("records", Json.toJson(recordsIds));
		result.put("query", Json.toJson(query));
		result.put("summary", Json.toJson(infos));
				
		return ok(result);
		
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
		MidataId userId = new MidataId(request().username());
		MidataId recordId = new MidataId(recordIdString);		
		Set<MidataId> spaceIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("spaces")));
		Set<MidataId> recordIds = new HashSet<MidataId>();
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
	public static Result share() throws JsonValidationException, AppException {
	
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "fromSpace", "toConsent");
		
		MidataId userId = new MidataId(request().username());
		MidataId fromSpace = JsonValidation.getMidataId(json, "fromSpace");
		MidataId toConsent = JsonValidation.getMidataId(json, "toConsent");
		
		Space space = Space.getByIdAndOwner(fromSpace, userId, Sets.create("autoShare"));
		if (space == null) throw new BadRequestException("error.unknown.space", "Bad space.");
		
		Consent consent = Consent.getByIdAndOwner(toConsent, userId, Sets.create("type"));
		if (consent == null) throw new BadRequestException("error.unknown.consent", "Bad consent.");
		
		if (space.autoShare == null) space.autoShare = new HashSet<MidataId>();
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
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result delete() throws JsonValidationException, AppException {
        JsonNode json = request().body().asJson();
        MidataId userId = new MidataId(request().username());
        		
		if (json.has("_id")) {
			String id = JsonValidation.getString(json, "_id");
			RecordToken tk = getRecordTokenFromString(id);				
			RecordManager.instance.wipe(userId, CMaps.map("_id", tk.recordId));
		} else if (json.has("group")) {
			String group = JsonValidation.getString(json, "group");
			
			RecordManager.instance.wipe(userId,  CMaps.map("group", group));			
		}
		
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
		MidataId userId = new MidataId(request().username());
						
		Set<MidataId> started = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("started")));
		Set<MidataId> stopped = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("stopped")));
		Set<String> recordIds = JsonExtraction.extractStringSet(json.get("records"));		
		Map<String, Object> query = json.has("query") ? JsonExtraction.extractMap(json.get("query")) : null;
		String groupSystem = null;
		if (query != null) {
			if (query.containsKey("group-system")) {
			  groupSystem = query.get("group-system").toString();
			} else {
			  groupSystem = "v1";
			}
		}
		
		if (query != null && query.isEmpty()) query = null;
		
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
		
        for (MidataId start : started) {
        	
        	boolean withMember = false;
        	Consent consent = Consent.getByIdAndOwner(start, userId, Sets.create("type"));
        	if (consent == null) {
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("_id"));
        		if (space == null) {
        		  throw new BadRequestException("error.unknown.consent", "Consent not found");
        		}
        	} else {        	
        	  ConsentType type = consent.type;
        	  
        	  if (type.equals(ConsentType.STUDYPARTICIPATION)) throw new BadRequestException("error.no_alter.consent", "Consents for studies may not be altered.");
        	  
        	  withMember = !type.equals(ConsentType.STUDYPARTICIPATION);
        	}        	         	
        	        	
        	for (String sourceAps :records.keySet()) {        	  
        	  RecordManager.instance.share(userId, new MidataId(sourceAps), start, ObjectIdConversion.toMidataIds(records.get(sourceAps)), withMember);
        	}    
        	
        	if (query != null) {
        		
        		AccessLog.log("QUERY1"+query.toString());
        		Feature_FormatGroups.convertQueryToContents(groupSystem, query);
        		AccessLog.log("QUERY2"+query.toString());
        		//query = Collections.unmodifiableMap(query);
        		
        		List<Record> recs = RecordManager.instance.list(userId, start, CMaps.map(query).map("flat", "true"), Sets.create("_id"));
        		Set<MidataId> remove = new HashSet<MidataId>();
        		for (Record r : recs) remove.add(r._id);
        		RecordManager.instance.unshare(userId, start, remove);
        		AccessLog.log("QUERY3"+query.toString());
        		if (consent == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
        		  RecordManager.instance.shareByQuery(userId, userId, start, query);
        		} else {
        		  Circles.setQuery(userId, start, query);        		          		  
	        	  RecordManager.instance.applyQuery(userId, query, userId, start, withMember);	        	  
        		}
        	}
        }
        
        for (MidataId start : stopped) {

        	boolean withMember = false;
        	Consent consent = Consent.getByIdAndOwner(start, userId, Sets.create("type"));
        	if (consent == null) {
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("_id"));
        		if (space == null) {
        		  throw new BadRequestException("error.unknown.consent", "Consent not found");
        		}
        	} else {        	
        	  ConsentType type = consent.type;
        	  
        	  if (type.equals(ConsentType.STUDYPARTICIPATION)) throw new BadRequestException("error.no_alter.consent", "Consents for studies may not be altered.");
        	  
        	  withMember = !type.equals(ConsentType.STUDYPARTICIPATION);
        	}        	         	
        	        	
        	for (String sourceAps :records.keySet()) {        	  
        	  RecordManager.instance.unshare(userId, start, ObjectIdConversion.toMidataIds(records.get(sourceAps)));
        	}    
        	
        	if (query != null) {
        		Feature_FormatGroups.convertQueryToContents(groupSystem, query);
        		
        		if (consent == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
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
		MidataId userId = new MidataId(request().username());
		RecordToken tk = Records.getRecordTokenFromString(recordIdString);
		
		Record record = RecordManager.instance.fetch(userId, tk, Sets.create("format","created"));
		if (record == null) throw new BadRequestException("error.unknown.record", "Record not found!");
		if (record.format == null) return ok();
		
		FormatInfo format = FormatInfo.getByName(record.format);
		if (format == null || format.visualization == null) return ok();
		
		Plugin visualization = Plugin.getById(format.visualization, Sets.create("filename", "url"));
					
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(new MidataId(tk.apsId), userId, new MidataId(tk.recordId));
		
		String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename + "/";
		
		ObjectNode obj = Json.newObject();
		obj.put("base", visualizationServer);
		obj.put("token", spaceToken.encrypt(request()));
		obj.put("preview", visualization.previewUrl);
		obj.put("main", visualization.url);
		obj.put("type", visualization.type);
		obj.put("name", record.name);
				
		return ok(obj);						
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
		MidataId userId = new MidataId(request().username());
						
		if (tk==null) throw new BadRequestException("error.invalid.token", "Bad token");
				
		FileData fileData = RecordManager.instance.fetchFile(userId, tk);
		setAttachmentContentDisposition(fileData.filename);		
		return ok(fileData.inputStream);
	}
	
	/**
	 * Check account tries to fix a broken account by removing indexes and precalculated results 
	 * @return
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result fixAccount() throws AppException {
		MidataId userId = new MidataId(request().username());
		
		RecordManager.instance.fixAccount(userId);
		
		return ok();
	}
}
