package controllers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64InputStream;
import org.bson.BSONObject;
import org.hl7.fhir.r4.model.DomainResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import models.Consent;
import models.FormatInfo;
import models.Member;
import models.MidataId;
import models.Model;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.enums.AggregationType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.access.APS;
import utils.access.AccessContext;
import utils.access.DBIterator;
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
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.RequestTooLargeException;
import utils.fhir.FHIRServlet;
import utils.fhir.FHIRTools;
import utils.fhir.ResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.stats.Stats;

/**
 * functions for handling the records
 *
 */
public class Records extends APIController {

	/**
	 * parse a string into a record token. Three different formats are supported
	 * 
	 * @param id
	 *            record ID to be parsed
	 * @return
	 */
	protected static RecordToken getRecordTokenFromString(String id) {
		int pos = id.indexOf('.');

		if (pos > 0) {
			return new RecordToken(id.substring(0, pos), id.substring(pos + 1));
		} else if (id.length() > 25) {
			return RecordToken.decrypt(id);
		} else {
			MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
			return new RecordToken(id, userId.toString());
		}
	}

	/**
	 * retrieve a specific record
	 * 
	 * @return record
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result get() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "_id");

		// get parameters
		String id = JsonValidation.getString(json, "_id");
		RecordToken tk = getRecordTokenFromString(id);
		if (tk == null)
			throw new BadRequestException("error.invalid.token", "Bad token");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		// execute
		Record target = RecordManager.instance.fetch(userId, getRole(), tk);
		if (target != null) ReferenceTool.resolveOwners(Collections.singleton(target), true, true);
		return ok(JsonOutput.toJson(target, "Record", Record.ALL_PUBLIC_WITHNAMES)).as("application/json");
	}

	/**
	 * retrieve a list of records matching some criteria. Can retrieve only
	 * records the user has access to.
	 * 
	 * @return list of records
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getRecords() throws AppException, JsonValidationException {

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "properties", "fields");

		MidataId aps = JsonValidation.getMidataId(json, "aps");
		if (aps == null)
			aps = userId;
		List<Record> records = new ArrayList<Record>();

		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));

		if (!properties.containsKey("limit")) {
			properties.put("limit", 10000);
			properties.put("consent-limit", 1000);
		}

		try {
			records.addAll(RecordManager.instance.list(userId, getRole(), RecordManager.instance.createContext(userId, aps), properties, fields));
		} catch (RequestTooLargeException e) {
			return ok();
		}

		Collections.sort(records);
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		return ok(JsonOutput.toJson(records, "Record", fields)).as("application/json");
	}

	/**
	 * retrieve aggregated information about records matching some criteria
	 * 
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getInfo() throws AppException, JsonValidationException {

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "properties");

		MidataId aps = JsonValidation.getMidataId(json, "aps");
		if (aps == null)
			aps = userId;
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));

		AggregationType aggrType = json.has("summarize") ? JsonValidation.getEnum(json, "summarize", AggregationType.class) : AggregationType.GROUP;

		try {
			Collection<RecordsInfo> result = RecordManager.instance.info(userId, getRole(), aps, RecordManager.instance.createContext(userId, aps), properties, aggrType);
			return ok(Json.toJson(result));
		} catch (RequestTooLargeException e) {
			return status(202);
		}
	}

	/**
	 * retrieve record IDs and query of an access permission set (consent or
	 * space)
	 * 
	 * @param aps
	 *            ID of consent or space
	 * @return
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getSharingDetails(String aps) throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId apsId = new MidataId(aps);

		Map<String, Object> query = null;
		boolean readRecords = true;
		AccessContext context = null;

		Consent consent = Circles.getConsentById(userId, apsId, Sets.create(Consent.SMALL, "authorized"));
		if (consent != null) {

			Circles.fillConsentFields(userId, Collections.singleton(consent), Sets.create("sharingQuery"));
			query = consent.sharingQuery;
			if (!consent.status.equals(ConsentStatus.ACTIVE) && !userId.equals(consent.owner))
				readRecords = false;
			context = RecordManager.instance.createContextFromConsent(userId, consent);
		} else {
			BSONObject b = RecordManager.instance.getMeta(userId, apsId, APS.QUERY);
			if (b != null) {
				query = b.toMap();
			}
			context = RecordManager.instance.createContext(userId, apsId);
		}
		if (query != null)
			Feature_FormatGroups.convertQueryToGroups("v1", query);

		ObjectNode result = Json.newObject();
		result.set("query", Json.toJson(query));

		if (readRecords) {
			Set<String> recordsIds = RecordManager.instance.listRecordIds(userId, getRole(), context);
			result.set("records", Json.toJson(recordsIds));

			try {
				Map<String, Object> props = new HashMap<String, Object>();
				Collection<RecordsInfo> infos = RecordManager.instance.info(userId, getRole(), apsId, context, props, AggregationType.CONTENT);
				result.set("summary", Json.toJson(infos));
			} catch (RequestTooLargeException e) {
				result.putArray("summary");
			}
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
	public Result updateSpaces(String recordIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "spaces");

		// update spaces
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId recordId = new MidataId(recordIdString);
		Set<MidataId> spaceIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("spaces")));
		Set<MidataId> recordIds = new HashSet<MidataId>();
		recordIds.add(recordId);

		Member owner = Member.getById(userId, Sets.create("myaps"));
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("aps"));

		for (Space space : spaces) {
			if (spaceIds.contains(space._id)) {
				RecordManager.instance.share(userId, owner.myaps, space._id, recordIds, true);
			} else {
				RecordManager.instance.unshare(userId, space._id, recordIds);
			}
		}

		return ok();
	}
	

	/**
	 * delete a record of the current user.
	 * 
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result delete() throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		if (json.has("_id")) {
			String id = JsonValidation.getString(json, "_id");
			RecordToken tk = getRecordTokenFromString(id);
			AuditManager.instance.addAuditEvent(AuditEventType.DATA_DELETION, null, userId, null, "id=" + tk.recordId);
			RecordManager.instance.delete(userId, CMaps.map("_id", tk.recordId));
			if (getRole().equals(UserRole.ADMIN)) RecordManager.instance.deleteFromPublic(userId, CMaps.map("_id", tk.recordId));
		} else if (json.has("group") || json.has("content") || json.has("app")) {

			Map<String, Object> properties = new HashMap<String, Object>();
			if (json.has("group"))
				properties.put("group", JsonValidation.getString(json, "group"));
			if (json.has("content"))
				properties.put("content", JsonValidation.getString(json, "content"));
			if (json.has("app"))
				properties.put("app", JsonValidation.getString(json, "app"));
			String message = "";
			for (Map.Entry<String, Object> prop : properties.entrySet()) {
				message += (message.length() > 0 ? "&" : "") + prop.getKey() + "=" + prop.getValue();
			}

			AuditManager.instance.addAuditEvent(AuditEventType.DATA_DELETION, null, userId, null, message);
			RecordManager.instance.delete(userId, properties);
			if (getRole().equals(UserRole.ADMIN)) RecordManager.instance.deleteFromPublic(userId, properties);
		}
		AuditManager.instance.success();

		return ok();
	}

	/**
	 * changes sharing of records to consents and/or spaces
	 * 
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result updateSharing() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "records", "started", "stopped");

		// validate request: record
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		Set<MidataId> started = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("started")));
		Set<MidataId> stopped = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("stopped")));
		Set<String> recordIds = JsonExtraction.extractStringSet(json.get("records"));
		Map<String, Object> query = json.has("query") ? JsonExtraction.extractMap(json.get("query")) : null;

		if (query != null && query.isEmpty())
			query = null;

		Map<String, Set<String>> records = new HashMap<String, Set<String>>();
		for (String recordId : recordIds) {
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
				withMember = true;
			} else {
				ConsentType type = consent.type;

				if (type.equals(ConsentType.STUDYPARTICIPATION))
					throw new BadRequestException("error.no_alter.consent", "Consents for studies may not be altered.");
				if (!userId.equals(consent.owner) && !consent.status.equals(ConsentStatus.UNCONFIRMED))
					throw new BadRequestException("error.no_alter.consent", "Consent may not be altered.");
				withMember = false;
				hasAccess = userId.equals(consent.owner);
				apsOwner = consent.owner;
			}

			if (hasAccess) {
				for (String sourceAps : records.keySet()) {
					RecordManager.instance.share(userId, new MidataId(sourceAps), start, ObjectIdConversion.toMidataIds(records.get(sourceAps)), withMember);
				}
			}

			if (query != null) {

				Feature_FormatGroups.convertQueryToContents(query);

				if (consent == null || consent.type.equals(ConsentType.EXTERNALSERVICE)) {
					if (hasAccess) {
						List<Record> recs = RecordManager.instance.list(userId, UserRole.ANY, RecordManager.instance.createContext(userId, start), CMaps.map(query).map("flat", "true"), Sets.create("_id"));
						Set<MidataId> remove = new HashSet<MidataId>();
						for (Record r : recs)
							remove.add(r._id);
						RecordManager.instance.unshare(userId, start, remove);
					}

					RecordManager.instance.shareByQuery(userId, userId, start, query);
				} else {
					consent.set(consent._id, "sharingQuery", query);
					if (consent.status == ConsentStatus.ACTIVE) {
						Circles.setQuery(userId, apsOwner, start, query);
						if (hasAccess)
							RecordManager.instance.applyQuery(RecordManager.instance.createContextFromAccount(userId), query, userId, start, withMember);
					}
				}
			}
			
			Consent.set(consent._id, "lastUpdated", new Date());
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

				if (!consent.owner.equals(userId) && !consent.status.equals(ConsentStatus.UNCONFIRMED))
					throw new BadRequestException("error.no_alter.consent", "Consent may not be altered.");
				withMember = !type.equals(ConsentType.STUDYPARTICIPATION);
				hasAccess = consent.owner.equals(userId);
				apsOwner = consent.owner;

			}

			if (hasAccess) {
				for (String sourceAps : records.keySet()) {
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
						if (hasAccess)
							RecordManager.instance.applyQuery(RecordManager.instance.createContextFromAccount(userId), query, userId, start, withMember);
					}
				}
			}
			
			Consent.set(consent._id, "lastUpdated", new Date());

		}

		return ok();
	}

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getRecordUrl(String recordIdString) throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		RecordToken tk = Records.getRecordTokenFromString(recordIdString);

		Record record = RecordManager.instance.fetch(userId, getRole(), tk, Sets.create("format", "created"));
		if (record == null)
			throw new BadRequestException("error.unknown.record", "Record not found!");
		if (record.format == null)
			return ok();

		FormatInfo format = FormatInfo.getByName(record.format);
		if (format == null || format.visualization == null)
			return ok();

		Plugin visualization = Plugin.getByFilename(format.visualization, Plugin.ALL_PUBLIC);
		if (visualization == null)
			return ok();

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, new MidataId(tk.apsId), userId, getRole() ,new MidataId(tk.recordId));

		String visualizationServer = "https://" + InstanceConfig.getInstance().getConfig().getString("visualizations.server") + "/" + visualization.filename + "/";

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
	 * 
	 * @param id
	 *            record ID token
	 * @return file
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getFile(String id) throws AppException {

		RecordToken tk = getRecordTokenFromString(id);
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		if (tk == null)
			throw new BadRequestException("error.invalid.token", "Bad token");		
		
		FileData fileData = RecordManager.instance.fetchFile(userId, tk);
		
		String contentType = "application/binary";
		if (fileData.contentType != null) contentType = fileData.contentType;
		if (contentType.startsWith("data:")) contentType = "application/binary";
		setAttachmentContentDisposition(fileData.filename);
		return ok(fileData.inputStream).as(contentType);
	}

	/**
	 * Check account tries to fix a broken account by removing indexes and
	 * precalculated results
	 * 
	 * @return
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result fixAccount() throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		RecordManager.instance.fixAccount(userId);

		return ok();
	}

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result downloadAccountData() throws AppException, IOException {

		final MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        final UserRole role = getRole();
		AuditManager.instance.addAuditEvent(AuditEventType.DATA_EXPORT, executorId);

		setAttachmentContentDisposition("yourdata.json");

		final String handle = PortalSessionToken.session().handle;

		KeyManager.instance.continueSession(handle);
		ResourceProvider.setExecutionInfo(new ExecutionInfo(executorId, role));
		String headerStr = "{ \"resourceType\" : \"Bundle\", \"type\" : \"searchset\", \"entry\" : [ ";

		boolean first = true;

		final akka.japi.function.Creator<Iterator<ByteString>> creator = new akka.japi.function.Creator<Iterator<ByteString>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<ByteString> create() throws Exception {
				try {
					KeyManager.instance.continueSession(handle);
					ResourceProvider.setExecutionInfo(new ExecutionInfo(executorId, role));
	
					DBIterator<Record> allRecords = RecordManager.instance.listIterator(executorId, role, RecordManager.instance.createContextFromAccount(executorId), CMaps.map("owner", "self"), RecordManager.COMPLETE_DATA);
					return new RecIterator(allRecords);
				} finally {
				  ServerTools.endRequest();
				}
			}

			class RecIterator implements Iterator<ByteString> {

				private DBIterator<Record> it;
				private boolean first = true;

				RecIterator(DBIterator it) {
					this.it = it;
				}

				@Override
				public boolean hasNext() {
					try {
						return it.hasNext();
					} catch (AppException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public ByteString next() {
					try {
						StringBuffer out = new StringBuffer();
						KeyManager.instance.continueSession(handle);
						ResourceProvider.setExecutionInfo(new ExecutionInfo(executorId, role));
						Record rec = it.next();
						String format = rec.format.startsWith("fhir/") ? rec.format.substring("fhir/".length()) : "Basic";

						ResourceProvider<DomainResource, Model> prov = FHIRServlet.myProviders.get(format);
						DomainResource r = prov.parse(rec, prov.getResourceType());
						String location = FHIRServlet.getBaseUrl() + "/" + prov.getResourceType().getSimpleName() + "/" + rec._id.toString() + "/_history/" + rec.version;
						if (r != null) {
							String ser = prov.serialize(r);
							int attpos = ser.indexOf(FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING);
							if (attpos > 0) {
								out.append((first?"":",") + "{ \"fullUrl\" : \"" + location + "\", \"resource\" : " + ser.substring(0, attpos));
								FileData fileData = RecordManager.instance.fetchFile(executorId, new RecordToken(rec._id.toString(), rec.stream.toString()));

								int BUFFER_SIZE = 3 * 1024;

								try (InputStreamReader in = new InputStreamReader(new Base64InputStream(fileData.inputStream, true, -1, null));) {

									char[] chunk = new char[BUFFER_SIZE];
									int len = 0;
									while ((len = in.read(chunk)) != -1) {
										out.append(String.valueOf(chunk, 0, len));
									}

								}

								out.append(ser.substring(attpos + FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING.length()) + " } ");
							} else
								out.append((first ? "" : ",") + "{ \"fullUrl\" : \"" + location + "\", \"resource\" : " + ser + " } ");
						} else {
							out.append((first ? "" : ",") + "{ \"fullUrl\" : \"" + location + "\" } ");
						}
					    first = false;
						return ByteString.fromString(out.toString());
					} catch (AppException e) {
						throw new RuntimeException(e);
					} catch (IOException e2) {
						throw new RuntimeException(e2);
					} finally {
						ServerTools.endRequest();
					}
				}

			}

		};

		AuditManager.instance.success();

		Source<ByteString, NotUsed> header = Source.single(ByteString.fromString(headerStr));
		Source<ByteString, NotUsed> footer = Source.single(ByteString.fromString("] }"));
		Source<ByteString, NotUsed> main = Source.fromIterator(creator);

		Source<ByteString, NotUsed> outstream = header.concat(main).concat(footer);

		// Serves this stream with 200 OK
		return ok().chunked(outstream).as("application/json+fhir");
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
    public Result unshareRecord() throws AppException, JsonValidationException {
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "target-study", "target-study-group");
		
		final MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        final UserRole role = getRole();
        
		ExecutionInfo inf = new ExecutionInfo(executorId, role);		
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
													
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");	
		
		MobileAPI.unshareRecord(inf, studyId, group, properties);
									
		return ok();
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
    public Result shareRecord() throws AppException, JsonValidationException {
				
		// check whether the request is complete
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "target-study", "target-study-group");
		
		final MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        final UserRole role = getRole();
        
		ExecutionInfo inf = new ExecutionInfo(executorId, role);		
		        		
    	Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
													
		MidataId studyId = JsonValidation.getMidataId(json, "target-study");
		String group = JsonValidation.getString(json, "target-study-group");	
		
		MobileAPI.shareRecord(inf, studyId, group, properties);
									
		return ok();
	}
}
