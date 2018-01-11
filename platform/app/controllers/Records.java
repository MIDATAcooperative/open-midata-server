package controllers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64InputStream;
import org.bson.BSONObject;
import org.hl7.fhir.dstu3.model.DomainResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Consent;
import models.FormatInfo;
import models.Member;
import models.MidataId;
import models.Model;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.Study;
import models.StudyParticipation;
import models.StudyRelated;
import models.User;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.APS;
import utils.access.Feature_FormatGroups;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MemberSecured;
import utils.auth.PortalSessionToken;
import utils.auth.RecordToken;
import utils.auth.ResearchSecured;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage.FileData;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.FHIRServlet;
import utils.fhir.FHIRTools;
import utils.fhir.PractitionerResourceProvider;
import utils.fhir.ResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.dialogs.authorized;

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
		ReferenceTool.resolveOwners(Collections.singleton(target), true, true);		
		return ok(JsonOutput.toJson(target, "Record", Record.ALL_PUBLIC_WITHNAMES));
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
	    Collection<RecordsInfo> result = RecordManager.instance.info(userId, aps, null, properties, aggrType);	
						
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
		
		Map<String, Object> query = null;
		boolean readRecords = true;
		
		Consent consent = Circles.getConsentById(userId, apsId, Sets.create("owner", "authorized", "status", "type", "sharingQuery"));
		if (consent != null) {
			
			Circles.fillConsentFields(userId, Collections.singleton(consent), Sets.create("sharingQuery"));
			query = consent.sharingQuery;
			if (!consent.status.equals(ConsentStatus.ACTIVE) && !userId.equals(consent.owner)) readRecords = false;
		} else {										
			BSONObject b = RecordManager.instance.getMeta(userId, apsId, APS.QUERY);
			if (b!=null) {
				query = b.toMap();				
			}
		}
		if (query != null) Feature_FormatGroups.convertQueryToGroups("v1", query);
		
		ObjectNode result = Json.newObject();
		result.set("query", Json.toJson(query));
		
		if (readRecords) {
			Set<String> recordsIds = RecordManager.instance.listRecordIds(userId, apsId);		
			Map<String, Object> props = new HashMap<String, Object>();
			Collection<RecordsInfo> infos = RecordManager.instance.info(userId, apsId, null, props, AggregationType.CONTENT);
								
			result.set("records", Json.toJson(recordsIds));		
			result.set("summary", Json.toJson(infos));
		} else {
			result.putArray("records");
			result.putArray("summary");
		}
				
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
			AuditManager.instance.addAuditEvent(AuditEventType.DATA_DELETION, null, userId, null, "id="+tk.recordId);
			RecordManager.instance.delete(userId, CMaps.map("_id", tk.recordId));
		} else if (json.has("group") || json.has("content") || json.has("app")) {
			
			Map<String, Object> properties = new HashMap<String, Object>();
			if (json.has("group")) properties.put("group", JsonValidation.getString(json, "group"));
			if (json.has("content")) properties.put("content", JsonValidation.getString(json, "content"));
			if (json.has("app")) properties.put("app", JsonValidation.getString(json, "app"));
			String message = "";
			for (Map.Entry<String, Object> prop : properties.entrySet()) {				
				message += (message.length()>0?"&":"")+prop.getKey()+"="+prop.getValue();			
			}
			
			AuditManager.instance.addAuditEvent(AuditEventType.DATA_DELETION, null, userId, null, message);
			RecordManager.instance.delete(userId,  properties);			
		}
		AuditManager.instance.success();
		
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
		
		
		if (query != null && query.isEmpty()) query = null;
			
		
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
        	boolean hasAccess = true;
        	MidataId apsOwner = userId;
        	Consent consent = Circles.getConsentById(userId, start, Sets.create("type", "status", "owner", "authorized", "sharingQuery"));
        	if (consent == null) {
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("_id"));
        		if (space == null) {
        		  throw new BadRequestException("error.unknown.consent", "Consent not found");
        		}
        	} else {        	
        	  ConsentType type = consent.type;
        	  
        	  if (type.equals(ConsentType.STUDYPARTICIPATION)) throw new BadRequestException("error.no_alter.consent", "Consents for studies may not be altered.");
        	  if (!userId.equals(consent.owner) && !consent.status.equals(ConsentStatus.UNCONFIRMED)) throw new BadRequestException("error.no_alter.consent", "Consent may not be altered.");
        	  withMember = !type.equals(ConsentType.STUDYPARTICIPATION);        	  
        	  hasAccess = userId.equals(consent.owner);
        	  apsOwner = consent.owner;
        	}        	         	
        	      
        	if (hasAccess) {
	        	for (String sourceAps :records.keySet()) {        	  
	        	  RecordManager.instance.share(userId, new MidataId(sourceAps), start, ObjectIdConversion.toMidataIds(records.get(sourceAps)), withMember);
	        	}    
        	}
        	
        	if (query != null) {
        		        		
        		Feature_FormatGroups.convertQueryToContents(query);        		
        		        		
        		
        		if (consent == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
        		  if (hasAccess) {
    	        	List<Record> recs = RecordManager.instance.list(userId, start, CMaps.map(query).map("flat", "true"), Sets.create("_id"));
    	        	Set<MidataId> remove = new HashSet<MidataId>();
    	        	for (Record r : recs) remove.add(r._id);
    	        	RecordManager.instance.unshare(userId, start, remove);
            	  }
        			
        		  RecordManager.instance.shareByQuery(userId, userId, start, query);
        		} else {
        		  consent.set(consent._id, "sharingQuery", query);
        		  if (consent.status == ConsentStatus.ACTIVE) {
        		    Circles.setQuery(userId, apsOwner, start, query);        		          		  
	        	    if (hasAccess) RecordManager.instance.applyQuery(RecordManager.instance.createContextFromAccount(userId), query, userId, start, withMember);
        		  }
        		}
        	}
        }
        
        for (MidataId start : stopped) {

        	boolean withMember = false;
        	boolean hasAccess = true;
        	MidataId apsOwner = userId;
        	Consent consent = Circles.getConsentById(userId, start, Sets.create("type", "status", "owner", "authorized"));        	
        	if (consent == null) {
        		Space space = Space.getByIdAndOwner(start, userId, Sets.create("_id"));
        		if (space == null) {
        		  throw new BadRequestException("error.unknown.consent", "Consent not found");
        		}
        	} else {        	
        	  ConsentType type = consent.type;
        	  
        	  if (!consent.owner.equals(userId) && !consent.status.equals(ConsentStatus.UNCONFIRMED)) throw new BadRequestException("error.no_alter.consent", "Consent may not be altered.");
        	  withMember = !type.equals(ConsentType.STUDYPARTICIPATION);        	  
        	  hasAccess = consent.owner.equals(userId);
        	  apsOwner = consent.owner;
        	  
        	  
        	}        	         	
        	       
        	if (hasAccess) {
        	for (String sourceAps :records.keySet()) {        	  
        	  RecordManager.instance.unshare(userId, start, ObjectIdConversion.toMidataIds(records.get(sourceAps)));
        	}    
        	}
        	
        	if (query != null) {
        		Feature_FormatGroups.convertQueryToContents(query);
        		
        		if (consent == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
        		  RecordManager.instance.shareByQuery(userId, userId, start, query);
        		} else {
        		  consent.set(consent._id, "sharingQuery", query);
          		  if (consent.status == ConsentStatus.ACTIVE) {
          		    Circles.setQuery(userId, apsOwner, start, query);        		          		  
  	        	    if (hasAccess) RecordManager.instance.applyQuery(RecordManager.instance.createContextFromAccount(userId), query, userId, start, withMember);
          		  }        		        	  
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
		
		Plugin visualization = Plugin.getById(format.visualization);
		if (visualization == null) return ok();
					
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, new MidataId(tk.apsId), userId, new MidataId(tk.recordId));
		
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
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result downloadAccountData() throws AppException, IOException {
		 
		 final MidataId executorId = new MidataId(request().username());
		   		 
		 AuditManager.instance.addAuditEvent(AuditEventType.DATA_EXPORT, executorId);
		 		 		 
		 setAttachmentContentDisposition("yourdata.json");
				 		 				 				
		 final String handle = PortalSessionToken.session().handle;
		 
		 
		 Chunks<String> chunks = new StringChunks() {
                
		        // Called when the stream is ready
		        public void onReady(Chunks.Out<String> out) {
		        	try {
		        		KeyManager.instance.continueSession(handle);
		        		ResourceProvider.setExecutionInfo(new ExecutionInfo(executorId));
		        		out.write("{ \"resourceType\" : \"Bundle\", \"type\" : \"searchset\", \"entry\" : [ ");

		        		boolean first = true;
		        		
		        				        				        				        		
		        
		        		List<Record> allRecords = RecordManager.instance.list(executorId, executorId, CMaps.map("owner", "self").map("deleted", true), Sets.create("_id"));
		        		Iterator<Record> recordIterator = allRecords.iterator();

		        		while (recordIterator.hasNext()) {
				            int i = 0;
				            Set<MidataId> ids = new HashSet<MidataId>();		           
				            while (i < 100 && recordIterator.hasNext()) {
				            	ids.add(recordIterator.next()._id);i++;
				            }
				            List<Record> someRecords = RecordManager.instance.list(executorId, executorId, CMaps.map("owner", "self").map("_id", ids), RecordManager.COMPLETE_DATA);
				            for (Record rec : someRecords) {
				            	
				            	String format = rec.format.startsWith("fhir/") ? rec.format.substring("fhir/".length()) : "Basic";
				            	
				            	ResourceProvider<DomainResource, Model> prov = FHIRServlet.myProviders.get(format); 
				            	DomainResource r = prov.parse(rec, prov.getResourceType());
				            	String location = FHIRServlet.getBaseUrl()+"/"+prov.getResourceType().getSimpleName()+"/"+rec._id.toString()+"/_history/"+rec.version;
				            	if (r!=null) {
				            		String ser = prov.serialize(r);
				            		int attpos = ser.indexOf(FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING);
				            		if (attpos > 0) {
				            			out.write((first?"":",")+"{ \"fullUrl\" : \""+location+"\", \"resource\" : "+ser.substring(0, attpos));
				            			FileData fileData = RecordManager.instance.fetchFile(executorId, new RecordToken(rec._id.toString(), rec.stream.toString()));
				            			
				            			
				            			int BUFFER_SIZE = 3 * 1024;

				            			try ( InputStreamReader in = new InputStreamReader(new Base64InputStream(fileData.inputStream, true, -1, null)); ) {				            			    
				            			    
				            			    char[] chunk = new char[BUFFER_SIZE];
				            			    int len = 0;
				            			    while ( (len = in.read(chunk)) != -1 ) {				            			    	
				            			         out.write(String.valueOf(chunk, 0, len));
				            			    }
				            			    
				            			}
				            							            							            			
				            			out.write(ser.substring(attpos+FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING.length())+" } ");
				            		} else out.write((first?"":",")+"{ \"fullUrl\" : \""+location+"\", \"resource\" : "+ser+" } ");
				            	} else {
				            		out.write((first?"":",")+"{ \"fullUrl\" : \""+location+"\" } ");
				            	}
			            		first = false;
				            }
		        		}
		        				        		
		        		out.write("] }");
			        	out.close();
		        	} catch (Exception e) {
		        		AccessLog.logException("download", e);
		        		ErrorReporter.report("Account export", null, e);		        		
		        	} finally {
		        		ServerTools.endRequest();		        		
		        	}
		        }

		 };

		 AuditManager.instance.success();
		    // Serves this stream with 200 OK
		  return ok(chunks);			    				 
	}
}
