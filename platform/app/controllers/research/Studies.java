package controllers.research;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64InputStream;
import org.hl7.fhir.dstu3.model.DomainResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import actions.APICall;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import controllers.APIController;
import controllers.Circles;
import controllers.Market;
import controllers.MobileAPI;
import controllers.Spaces;
import controllers.members.HealthProvider;
import models.AccessPermissionSet;
import models.Admin;
import models.Consent;
import models.FilterRule;
import models.Info;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Model;
import models.ParticipationCode;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.ResearchUser;
import models.Space;
import models.Study;
import models.StudyGroup;
import models.StudyParticipation;
import models.StudyRelated;
import models.Task;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.AssistanceType;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.Frequency;
import models.enums.InfoType;
import models.enums.InformationType;
import models.enums.JoinMethod;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.PluginStatus;
import models.enums.ResearcherRole;
import models.enums.StudyExecutionStatus;
import models.enums.StudyType;
import models.enums.StudyValidationStatus;
import models.enums.UserFeature;
import models.enums.UserGroupType;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.access.DBIterator;
import utils.access.Feature_FormatGroups;
import utils.access.Query;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.RecordToken;
import utils.auth.ResearchSecured;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.FHIRServlet;
import utils.fhir.FHIRTools;
import utils.fhir.GroupResourceProvider;
import utils.fhir.PractitionerResourceProvider;
import utils.fhir.ResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import views.txt.mails.studynotify;

/**
 * functions about studies to be used by researchers
 *
 */
public class Studies extends APIController {

	public final static int STUDY_CONSENT_SIZE = 10000;

	/**
	 * create a new study
	 * 
	 * @return Study
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result create() throws AppException {
		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "name", "description", "type");

		String name = JsonValidation.getString(json, "name");
		if (Study.existsByName(name))
			return inputerror("name", "exists", "A study with this name already exists.");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId research = PortalSessionToken.session().getOrg();

		if (research == null)
			throw new InternalServerException("error.internal", "No organization associated with session.");
		Study study = new Study();

		study._id = new MidataId();
		study.name = name;
		do {
			study.code = CodeGenerator.nextUniqueCode();
		} while (Study.existsByCode(study.code));
		study.type = JsonValidation.getEnum(json, "type", StudyType.class);
		study.description = JsonValidation.getString(json, "description");

		study.createdAt = new Date();
		study.createdBy = userId;
		study.owner = research;

		study.validationStatus = StudyValidationStatus.DRAFT;
		study.participantSearchStatus = ParticipantSearchStatus.PRE;
		study.executionStatus = StudyExecutionStatus.PRE;
		
		study.participantRules = new HashSet<FilterRule>();
		study.recordQuery = new HashMap<String, Object>();
		study.requiredInformation = InformationType.RESTRICTED;
		study.anonymous = false;
		study.assistance = AssistanceType.NONE;
		study.groups = new ArrayList<StudyGroup>();

		study.studyKeywords = new HashSet<MidataId>();

		UserGroup userGroup = new UserGroup();

		userGroup.name = study.name;
		userGroup.type = UserGroupType.RESEARCHTEAM;
		userGroup.status = UserStatus.ACTIVE;
		userGroup.creator = userId;
		userGroup._id = study._id;
		userGroup.nameLC = userGroup.name.toLowerCase();
		userGroup.keywordsLC = new HashSet<String>();
		userGroup.registeredAt = study.createdAt;
		userGroup.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(userGroup._id, null);
		GroupResourceProvider.updateMidataUserGroup(userGroup);
		userGroup.add();

		UserGroupMember member = new UserGroupMember();
		member._id = new MidataId();
		member.member = userId;
		member.userGroup = userGroup._id;
		member.status = ConsentStatus.ACTIVE;
		member.startDate = new Date();
		member.role = ResearcherRole.SPONSOR();
		Map<String, Object> accessData = new HashMap<String, Object>();
		accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroup._id, member._id));
		RecordManager.instance.createPrivateAPS(userId, member._id);
		RecordManager.instance.setMeta(userId, member._id, "_usergroup", accessData);
		member.add();

		RecordManager.instance.createPrivateAPS(userGroup._id, userGroup._id);

		Study.add(study);

		AuditManager.instance.addAuditEvent(AuditEventType.ADDED_AS_TEAM_MEMBER, null, userId, userId, null, study._id);
		AuditManager.instance.success();

		return ok(JsonOutput.toJson(study, "Study", Study.ALL));
	}

	/**
	 * download data about a study of the current research organization
	 * 
	 * @param id
	 *            ID of study
	 * @return not yet: (ZIP file)
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result download(String id) throws AppException, IOException {
		MidataId studyid = new MidataId(id);
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		Study study = Study.getById(studyid, Sets.create("name", "type", "executionStatus", "participantSearchStatus", "validationStatus", "owner", "groups", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");

		AuditManager.instance.addAuditEvent(AuditEventType.DATA_EXPORT, executorId, null, study);
		setAttachmentContentDisposition("study.zip");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, executorId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.mayExportData())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to export");

		ByteArrayOutputStream servletOutputStream = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(servletOutputStream); // create
																		// a
																		// ZipOutputStream
																		// from
																		// servletOutputStream

		ZipEntry entry = new ZipEntry("participants.json");
		zos.putNextEntry(entry);

		Writer output = new OutputStreamWriter(zos);
		for (StudyGroup group : study.groups) {
			Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(study._id, group.name, Sets.create("ownerName", "yearOfBirth", "country", "gender", "group"));

			// for (StudyParticipation part : parts) {
			output.append(JsonOutput.toJson(parts, "Consent", Sets.create("ownerName", "yearOfBirth", "country", "gender", "group")));
			// }
		}
		output.flush();
		zos.closeEntry();

		for (StudyGroup group : study.groups) {
			entry = new ZipEntry("records-" + group.name + ".json");
			zos.putNextEntry(entry);
			output = new OutputStreamWriter(zos);

			Set<String> fields = Sets.create("owner", "ownerName", "app", "creator", "created", "name", "format", "content", "description", "data");
			List<Record> allRecords = RecordManager.instance.list(executorId, UserRole.RESEARCH, executorId, CMaps.map("study", study._id).map("study-group", group.name), fields);

			output.append(JsonOutput.toJson(allRecords, "Record", fields));

			output.flush();
			zos.closeEntry();
		}

		zos.close(); // finally closing the ZipOutputStream to mark completion
						// of ZIP file

		AuditManager.instance.success();
		return ok(servletOutputStream.toByteArray());

	}

	/**
	 * download FHIR data about a study of the current research organization
	 * 
	 * @param id
	 *            ID of study
	 * @return not yet: (ZIP file)
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result downloadFHIR(String id, final String studyGroup, final String mode) throws AppException, IOException {

		final MidataId studyid = new MidataId(id);
		MidataId owner = PortalSessionToken.session().getOrg();
		final MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
        final UserRole role = getRole();
		final Study study = Study.getById(studyid, Sets.create("name", "type", "executionStatus", "participantSearchStatus", "validationStatus", "owner", "groups", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");

		Map<String, String[]> params = request().queryString();
						
		final Date startDate = params.containsKey("startDate")? new Date(Long.parseLong(params.get("startDate")[0])) : null;
		final Date endDate   = params.containsKey("endDate") ? new Date(Long.parseLong(params.get("endDate")[0])) : null;
		
		AuditManager.instance.addAuditEvent(AuditEventType.DATA_EXPORT, executorId, null, study);

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, executorId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.mayExportData())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to export data.");

		setAttachmentContentDisposition("study.json");

		final String handle = PortalSessionToken.session().handle;

		StringBuffer out = new StringBuffer();

		KeyManager.instance.continueSession(handle);
		ResourceProvider.setExecutionInfo(new ExecutionInfo(executorId, role));
		out.append("{ \"resourceType\" : \"Bundle\", \"type\" : \"searchset\", \"entry\" : [ ");

		boolean first = true;

		Set<UserGroupMember> ugms = UserGroupMember.getAllByGroup(study._id);
		Map<MidataId, UserGroupMember> idmap = new HashMap<MidataId, UserGroupMember>();
		for (UserGroupMember member : ugms)
			idmap.put(member.member, member);
		Set<User> users = User.getAllUser(CMaps.map("_id", idmap.keySet()), User.ALL_USER);

		ResourceProvider<DomainResource, Model> pprov = FHIRServlet.myProviders.get("Practitioner");
		for (User user : users) {
			String location = FHIRServlet.getBaseUrl() + "/" + pprov.getResourceType().getSimpleName() + "/" + user._id.toString();
			String ser = pprov.serialize(PractitionerResourceProvider.practitionerFromMidataUser(user));
			out.append((first ? "" : ",") + "{ \"fullUrl\" : \"" + location + "\", \"resource\" : " + ser + " } ");
			first = false;
		}

		final akka.japi.function.Creator<Iterator<ByteString>> creator = new akka.japi.function.Creator<Iterator<ByteString>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<ByteString> create() throws Exception {
				KeyManager.instance.continueSession(handle);
				ResourceProvider.setExecutionInfo(new ExecutionInfo(executorId, role));
				DBIterator<Record> allRecords = RecordManager.instance.listIterator(executorId, executorId, CMaps.map("export", mode).map("study", study._id).map("study-group", studyGroup).mapNotEmpty("shared-after",  startDate).mapNotEmpty("updated-before", endDate),
						RecordManager.COMPLETE_DATA);
				return new RecIterator(allRecords);
			}

			class RecIterator implements Iterator<ByteString> {

				private DBIterator<Record> it;

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
								out.append("," + "{ \"fullUrl\" : \"" + location + "\", \"resource\" : " + ser.substring(0, attpos));
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
								out.append((",") + "{ \"fullUrl\" : \"" + location + "\", \"resource\" : " + ser + " } ");
						} else {
							out.append((",") + "{ \"fullUrl\" : \"" + location + "\" } ");
						}
						// first = false;
						return ByteString.fromString(out.toString());
					} catch (AppException e) {
						throw new RuntimeException(e);
					} catch (IOException e2) {
						throw new RuntimeException(e2);
					}
				}

			}

		};

		// for (Consent part : parts) {

		/*
		 * } catch (Exception e) { AccessLog.logException("download", e);
		 * ErrorReporter.report("Study Download", null, e); } finally {
		 * ServerTools.endRequest(); }
		 */

		AuditManager.instance.success();

		Source<ByteString, NotUsed> header = Source.single(ByteString.fromString(out.toString()));
		Source<ByteString, NotUsed> footer = Source.single(ByteString.fromString("] }"));
		Source<ByteString, NotUsed> main = Source.fromIterator(creator);

		Source<ByteString, NotUsed> outstream = header.concat(main).concat(footer);

		// Serves this stream with 200 OK
		return ok().chunked(outstream).as("application/json+fhir");
	}

	/**
	 * list all studies of current research user
	 * 
	 * @return list of studies
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result list() throws JsonValidationException, InternalServerException {
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();

		Set<UserGroupMember> ugms = UserGroupMember.getAllByMember(executorId);
		Set<MidataId> ids = new HashSet<MidataId>();
		for (UserGroupMember ugm : ugms)
			ids.add(ugm.userGroup);

		Set<String> fields = Sets.create("createdAt", "createdBy", "description", "executionStatus", "name", "participantSearchStatus", "validationStatus");
		Set<Study> studies = Study.getAll(null, CMaps.map("_id", ids), fields);

		return ok(JsonOutput.toJson(studies, "Study", fields));
	}

	/**
	 * list all studies waiting for validation
	 * 
	 * @return list of studies
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result listAdmin() throws JsonValidationException, InternalServerException {

		JsonNode json = request().body().asJson();
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = Sets.create("type", "createdAt", "createdBy", "description", "name", "startDate", "endDate", "dataCreatedBefore");
		Set<Study> studies = Study.getAll(null, properties, fields);

		return ok(JsonOutput.toJson(studies, "Study", fields));
	}

	/**
	 * retrieve one study of current research organization
	 * 
	 * @param id
	 *            ID of study
	 * @return Study
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result get(String id) throws JsonValidationException, InternalServerException {

		MidataId studyid = new MidataId(id);
		MidataId userid = MidataId.from(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();

		Set<String> fields = Sets.create("createdAt", "createdBy", "description", "executionStatus", "name", "participantSearchStatus", "validationStatus", "infos", "infosPart", "infosInternal", "owner", "participantRules",
				"recordQuery", "studyKeywords", "code", "groups", "requiredInformation", "anonymous", "assistance", "termsOfUse", "requirements", "startDate", "endDate", "dataCreatedBefore", "myRole",
				"processFlags", "autoJoinGroup", "type", "joinMethods");
		Study study = Study.getById(studyid, fields);

		UserGroupMember ugm = UserGroupMember.getByGroupAndMember(studyid, userid);
		if (ugm != null)
			study.myRole = ugm.role;
		else
			throw new InternalServerException("error.notauthorized.action", "User is not allowed to see study.");

		return ok(JsonOutput.toJson(study, "Study", fields));
	}

	/**
	 * retrieve one study (for validation by admin)
	 * 
	 * @param id
	 *            ID of study
	 * @return Study
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result getAdmin(String id) throws JsonValidationException, InternalServerException {

		MidataId studyid = new MidataId(id);

		Set<String> fields = Sets.create("createdAt", "createdBy", "description", "executionStatus", "name", "participantSearchStatus", "validationStatus", "infos", "infosPart", "infosInternal", "owner", "participantRules",
				"recordQuery", "studyKeywords", "code", "groups", "requiredInformation", "anonymous", "assistance", "termsOfUse", "requirements", "startDate", "endDate", "dataCreatedBefore", "type", "joinMethods");
		Study study = Study.getById(studyid, fields);

		ObjectNode result = Json.newObject();
		result.put("study", JsonOutput.toJsonNode(study, "Study", fields));

		return ok(result);
	}

	/**
	 * generates participation codes. NEEDS REWRITE
	 * 
	 * @param id
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result generateCodes(String id) throws JsonValidationException, AppException {

		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "count", "reuseable");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		String userName = user.lastname + ", " + user.firstname;

		int count = JsonValidation.getInteger(json, "count", 1, 1000);
		String group = JsonValidation.getString(json, "group");
		boolean reuseable = JsonValidation.getBoolean(json, "reuseable");
		Date now = new Date();

		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner", "executionStatus", "participantSearchStatus", "validationStatus"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED)
			throw new BadRequestException("error.closed.study", "Study participant search already closed.");

		for (int num = 1; num <= count; num++) {
			ParticipationCode code = new ParticipationCode();
			code.code = CodeGenerator.nextCode(); // TODO UNIQUENESS?
			code.study = studyid;
			code.group = group;
			code.recruiter = userId;
			code.recruiterName = userName;
			code.createdAt = now;
			if (reuseable) {
				code.status = ParticipationCodeStatus.REUSEABLE;
			} else {
				code.status = ParticipationCodeStatus.UNUSED;
			}
			ParticipationCode.add(code);
		}
		String comment = count + " codes";
		if (reuseable)
			comment += ", reuseable";
		if (group != null && !"".equals(group))
			comment += ", group=" + group;
		// study.addHistory(new History(EventType.CODES_GENERATED, user,
		// comment));

		return ok();
	}

	/**
	 * list participation codes. NEEDS REWRITE
	 * 
	 * @param id
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result listCodes(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner", "executionStatus", "participantSearchStatus", "validationStatus"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			return statusWarning("study_not_validated", "Study must be validated before.");
		if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED)
			return statusWarning("participant_search_closed", "Study participant search already closed.");

		Set<ParticipationCode> codes = ParticipationCode.getByStudy(studyid);

		return ok(Json.toJson(codes));
	}

	/**
	 * start study validation
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startValidation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid,
				Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "groups", "recordQuery", "requiredInformation", "anonymous", "code"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus == StudyValidationStatus.VALIDATED)
			return badRequest("Study has already been validated.");
		if (study.validationStatus == StudyValidationStatus.VALIDATION)
			return badRequest("Validation is already in progress.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		if (study.groups == null || study.groups.size() == 0)
			return badRequest("Please define study groups before validation!");
		if (study.recordQuery == null || study.recordQuery.isEmpty())
			return badRequest("Please define record sharing query before validation!");

		// Checks

		if (study.requiredInformation.equals(InformationType.RESTRICTED)) {

			Map<String, Object> properties = new HashMap<String, Object>(study.recordQuery);
			Feature_FormatGroups.convertQueryToContents(properties);

			// if (!properties.containsKey("content")) throw new
			// BadRequestException("error.invalid.access_query", "Query does not
			// restrict content.");
			if (Query.getAnyRestrictionFromQuery(properties, "content").contains("Patient"))
				throw new BadRequestException("error.invalid.sharing", "Restricted study may not share Patient records.");

		}

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_VALIDATION_REQUESTED, userId, null, study);
		// study.addHistory(new History(EventType.VALIDATION_REQUESTED, user,
		// null));
		study.setValidationStatus(StudyValidationStatus.VALIDATION);
		AuditManager.instance.success();

		if (InstanceConfig.getInstance().getInstanceType().getStudiesValidateAutomatically()) {
			AuditManager.instance.addAuditEvent(AuditEventType.STUDY_VALIDATED, userId, null, study);
			// study.addHistory(new History(EventType.STUDY_VALIDATED, user,
			// null));
			study.setValidationStatus(StudyValidationStatus.VALIDATED);
			AuditManager.instance.success();
		} else {
			sendAdminNotificationMail(study);
		}

		return ok();
	}

	/**
	 * Helper function to notification mail to admin
	 * 
	 * @param user
	 *            new user record
	 */
	public static void sendAdminNotificationMail(Study study) throws InternalServerException {

		String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		String name = study.name;
		String code = study.code;

		AccessLog.log("send admin notification mail (study): " + code);
		Messager.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), "Midata Admin", "Study to Validate", studynotify.render(site, name, code).toString());

	}

	/**
	 * end study validation
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result endValidation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyid = new MidataId(id);

		User user = Admin.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "groups", "recordQuery", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.missing.study", "Study does not exist");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_VALIDATED, userId, null, study);
		if (!study.validationStatus.equals(StudyValidationStatus.VALIDATION))
			return badRequest("Study has already been validated.");

		// study.addHistory(new History(EventType.STUDY_VALIDATED, user, null));
		study.setValidationStatus(StudyValidationStatus.VALIDATED);
		AuditManager.instance.success();

		return ok();
	}

	/**
	 * back to draft
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result backToDraft(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyid = new MidataId(id);

		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "groups", "recordQuery", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.missing.study", "Study does not exist");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_REJECTED, userId, null, study);
		if (!study.validationStatus.equals(StudyValidationStatus.VALIDATION))
			return badRequest("Study has already been validated.");

		// study.addHistory(new History(EventType.STUDY_REJECTED, user, "Reset
		// to draft mode"));
		study.setValidationStatus(StudyValidationStatus.DRAFT);

		AuditManager.instance.success();

		return ok();
	}

	/**
	 * start participant search phase
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startParticipantSearch(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "code"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPANT_SEARCH_STARTED, userId, null, study);
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.PRE)
			return badRequest("Participants can only be searched as long as study has not stared.");
		if (study.participantSearchStatus != ParticipantSearchStatus.PRE && study.participantSearchStatus != ParticipantSearchStatus.CLOSED)
			return badRequest("Study participant search already started.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		// study.addHistory(new History(EventType.PARTICIPANT_SEARCH_STARTED,
		// user, null));
		study.setParticipantSearchStatus(ParticipantSearchStatus.SEARCHING);
		AuditManager.instance.success();

		return ok();
	}

	/**
	 * end participant search phase of a study
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result endParticipantSearch(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPANT_SEARCH_CLOSED, userId, null, study);
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING)
			throw new BadRequestException("error.closed.study", "Study participant search already closed.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		// study.addHistory(new History(EventType.PARTICIPANT_SEARCH_CLOSED,
		// user, null));
		study.setParticipantSearchStatus(ParticipantSearchStatus.CLOSED);
		AuditManager.instance.success();

		return ok();
	}

	/**
	 * start execution phase of study
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startExecution(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "code"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_STARTED, userId, null, study);
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.participantSearchStatus == ParticipantSearchStatus.PRE)
			return badRequest("Participant search must be done before.");
		if (study.executionStatus != StudyExecutionStatus.PRE)
			throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		// study.addHistory(new History(EventType.STUDY_STARTED, user, null));
		study.setExecutionStatus(StudyExecutionStatus.RUNNING);
		Market.updateActiveStatus(study);
		AuditManager.instance.success();

		return ok();
	}

	/**
	 * end execution phase of study
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result finishExecution(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("owner", "type", "executionStatus", "participantSearchStatus", "validationStatus", "createdBy", "code", "dataCreatedBefore"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_FINISHED, userId, null, study);
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING)
			throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		closeStudy(userId, study);
		// study.addHistory(new History(EventType.STUDY_FINISHED, user, null));
		study.setExecutionStatus(StudyExecutionStatus.FINISHED);
		
		Market.updateActiveStatus(study);
		
		AuditManager.instance.success();

		return ok();
	}

	public static void closeStudy(MidataId executor, Study study) throws AppException {
		Date now = new Date();
		if (study.dataCreatedBefore != null && study.dataCreatedBefore.before(now))
			return;
		study.dataCreatedBefore = now;

		Set<StudyParticipation> participants = StudyParticipation.getActiveParticipantsByStudy(study._id, Consent.ALL);
		for (StudyParticipation participant : participants) {
			if (participant.status.equals(ConsentStatus.ACTIVE)) {
				Circles.consentStatusChange(executor, participant, ConsentStatus.FROZEN);
			}
		}

		study.setDataCreatedBefore(now);
	}

	/**
	 * abort execution of study
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result abortExecution(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "type", "code", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "createdBy"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_ABORTED, userId, null, study);
		// if (study.executionStatus != StudyExecutionStatus.RUNNING) throw new
		// BadRequestException("error.invalid.status_transition", "Wrong study
		// execution status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		closeStudy(userId, study);

		// study.addHistory(new History(EventType.STUDY_ABORTED, user, null));
		study.setExecutionStatus(StudyExecutionStatus.ABORTED);
		
		Market.updateActiveStatus(study);

		AuditManager.instance.success();
		return ok();
	}

	public static StudyRelated findFreeSharingConsent(MidataId executor, MidataId researcher, Study study, String group, boolean ifDataShared) throws AppException {
		MidataId ownerId = researcher;
		Set<StudyRelated> consents = StudyRelated.getActiveByOwnerGroupAndStudy(researcher, group, study._id, Consent.ALL);
		if (consents.isEmpty() && ifDataShared)
			return null;

		StudyRelated reference = null;

		if (consents.isEmpty()) {
			StudyRelated consent = new StudyRelated();
			consent._id = new MidataId();
			consent.study = study._id;
			consent.group = group;
			consent.owner = ownerId;
			consent.name = "Study:" + study.name;
			consent.authorized = new HashSet<MidataId>();
			consent.entityType = EntityType.USERGROUP;
			consent.dateOfCreation = new Date();
			consent.lastUpdated = consent.dateOfCreation;
			consent.status = ConsentStatus.ACTIVE;
			consent.writes = WritePermissionType.UPDATE_EXISTING;

			RecordManager.instance.createAnonymizedAPS(ownerId, ownerId, consent._id, true);
			Circles.prepareConsent(consent);
			consent.add();
			Circles.addUsers(ownerId, EntityType.USERGROUP, consent, Collections.singleton(study._id));

			reference = consent;
		}
		for (StudyRelated sr : consents) {
			if (sr.authorized.size() < STUDY_CONSENT_SIZE && (sr.entityType == null || sr.entityType.equals(EntityType.USER))) {
				return sr;
			} else if (sr.entityType != null && sr.entityType.equals(EntityType.USERGROUP)) {
				reference = sr;
			}
		}
		StudyRelated consent = new StudyRelated();
		consent._id = new MidataId();
		consent.study = study._id;
		consent.group = group;
		consent.owner = ownerId;
		consent.name = "Study:" + study.name;
		consent.authorized = new HashSet<MidataId>();
		consent.dateOfCreation = new Date();
		consent.lastUpdated = consent.dateOfCreation;
		consent.status = ConsentStatus.ACTIVE;
		consent.writes = WritePermissionType.NONE;

		RecordManager.instance.createAnonymizedAPS(ownerId, study._id, consent._id, true);
		Circles.prepareConsent(consent);
		consent.add();
		RecordManager.instance.copyAPS(executor, reference._id, consent._id, ownerId);
		return consent;
	}

	public static void fixSharing(MidataId executor, MidataId researcher, Study study, String group) throws AppException {
		Set<StudyRelated> consents = StudyRelated.getActiveByOwnerGroupAndStudy(researcher, group, study._id, Consent.ALL);
		if (consents.isEmpty())
			return;

		for (StudyRelated sr : consents) {
			if (sr.entityType.equals(EntityType.USER)) {
				StudyRelated.delete(researcher, sr._id);
				AccessPermissionSet.delete(sr._id);
			}
		}

		Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(study._id, group, Sets.create());
		joinSharing(executor, researcher, study, group, false, new ArrayList<StudyParticipation>(parts), true);

	}

	public static void joinSharing(MidataId executor, MidataId researcher, Study study, String group, boolean ifDataShared, List<StudyParticipation> part) throws AppException {
		joinSharing(executor, researcher, study, group, ifDataShared, part, false);
	}

	public static void joinSharing(MidataId executor, MidataId researcher, Study study, String group, boolean ifDataShared, List<StudyParticipation> part, boolean noretry) throws AppException {
		StudyRelated sr = findFreeSharingConsent(executor, researcher, study, group, ifDataShared);
		if (sr == null)
			return;

		Set<MidataId> ids = new HashSet<MidataId>();
		Iterator<StudyParticipation> it = part.iterator();
		int remaining = part.size();
		while (sr.authorized.size() + remaining > STUDY_CONSENT_SIZE) {
			for (int i = 0; i < STUDY_CONSENT_SIZE - sr.authorized.size(); i++) {
				ids.add(it.next().owner);
				remaining--;
			}
			Circles.addUsers(executor, study._id, EntityType.USER, sr, ids);
			ids.clear();
			sr = findFreeSharingConsent(executor, researcher, study, group, ifDataShared);
		}
		while (it.hasNext())
			ids.add(it.next().owner);

		try {
			RecordManager.instance.getMeta(executor, sr._id, "_");
		} catch (Exception e) {
			if (!noretry)
				fixSharing(executor, researcher, study, group);
		}

		Circles.addUsers(executor, study._id, EntityType.USER, sr, ids);
	}

	/**
	 * share records with a group of participants of a study
	 * 
	 * @param id
	 *            ID of study
	 * @param group
	 *            name of group
	 * @return Consent that authorizes participants to view records
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result shareWithGroup(String id, String group) throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		if (group != null && (group.equals("undefined") || group.equals("null")))
			group = null;

		Study study = Study.getById(studyid, Sets.create("owner", "type", "executionStatus", "participantSearchStatus", "validationStatus", "name", "createdBy", "code", "groups"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING)
			throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");

		if (group == null) {
			for (StudyGroup grp : study.groups) {
				Set<StudyRelated> consents = StudyRelated.getActiveByOwnerGroupAndStudy(userId, grp.name, study._id, Sets.create("authorized"));

				if (consents.isEmpty()) {
					Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyid, grp.name, Sets.create());
					joinSharing(userId, userId, study, grp.name, false, new ArrayList<StudyParticipation>(parts));
				}
			}

			return ok();

		} else {
			Set<StudyRelated> consents = StudyRelated.getActiveByOwnerGroupAndStudy(userId, group, study._id, Sets.create("authorized"));

			if (consents.isEmpty()) {
				Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyid, group, Sets.create());
				joinSharing(userId, userId, study, group, false, new ArrayList<StudyParticipation>(parts));
			}

			return ok(JsonOutput.toJson(consents, "Consent", Sets.create("_id", "authorized")));
		}

	}

	/**
	 * add an application for data processing
	 * 
	 * @return status ok
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@Security.Authenticated(ResearchSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addApplication(String id, String group) throws AppException, JsonValidationException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyId = new MidataId(id);
		if (group != null && (group.equals("undefined") || group.equals("null")))
			group = null;

		Study study = Study.getById(studyId, Sets.create("owner", "type", "executionStatus", "participantSearchStatus", "validationStatus", "name", "code", "createdBy", "groups"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not exist.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		// if (study.executionStatus != StudyExecutionStatus.RUNNING) throw new
		// BadRequestException("error.invalid.status_transition", "Wrong study
		// execution status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyId, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		// if (!self.role.maySetup()) throw new
		// BadRequestException("error.notauthorized.action", "User is not
		// allowed to change study setup.");

		// validate json
		JsonNode json = request().body().asJson();

		MidataId pluginId = JsonValidation.getMidataId(json, "plugin");
		boolean restrictRead = JsonValidation.getBoolean(json, "restrictread");
		boolean shareBack = JsonValidation.getBoolean(json, "shareback");

		Plugin plugin = Plugin.getById(pluginId, Plugin.ALL_DEVELOPER);
		if (plugin == null)
			throw new BadRequestException("error.invalid.plugin", "Plugin not found.");
		if (plugin.status == PluginStatus.DELETED)
			if (plugin.status == PluginStatus.DELETED)
				throw new BadRequestException("error.invalid.plugin", "Plugin not found.");
		if (plugin.targetUserRole != UserRole.RESEARCH)
			throw new BadRequestException("error.invalid.plugin", "Wrong target role.");

		User researcher = User.getById(userId, Sets.create("apps", "password", "firstname", "lastname", "email", "language", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode",
				"accountVersion", "role", "subroles", "login", "registeredAt", "developer", "initialApp"));

		if (shareBack) {

			if (group == null) {
				for (StudyGroup grp : study.groups) {
					Set<StudyRelated> consents = StudyRelated.getActiveByOwnerGroupAndStudy(userId, grp.name, study._id, Sets.create("authorized"));

					if (consents.isEmpty()) {
						Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyId, grp.name, Sets.create());
						joinSharing(userId, userId, study, grp.name, false, new ArrayList<StudyParticipation>(parts));
					}
				}

			} else {
				Set<StudyRelated> consents = StudyRelated.getActiveByOwnerGroupAndStudy(userId, group, study._id, Sets.create("authorized"));

				if (consents.isEmpty()) {
					Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyId, group, Sets.create());
					joinSharing(userId, userId, study, group, false, new ArrayList<StudyParticipation>(parts));
				}

			}

		}

		if (plugin.type.equals("mobile")) {
			JsonValidation.validate(json, "device");
			String device = JsonValidation.getString(json, "device");

			MobileAppInstance appInstance = MobileAPI.installApp(userId, plugin._id, researcher, device, false, Collections.emptySet());
			Map<String, Object> query = appInstance.sharingQuery;
			query.put("study", studyId.toString());
			if (restrictRead && group != null)
				query.put("study-group", group);

			if (shareBack) {
				query.put("target-study", studyId.toString());
				if (group != null)
					query.put("target-study-group", group);
			}
			query.put("link-study", studyId.toString());
			if (group != null)
				query.put("link-study-group", group);

			appInstance.set(appInstance._id, "sharingQuery", query);

			HealthProvider.confirmConsent(appInstance.owner, appInstance._id);
			appInstance.status = ConsentStatus.ACTIVE;

		} else {

			Space space = null;
			space = Spaces.add(userId, plugin.defaultSpaceName, plugin._id, plugin.type, study.code + ":" + (group != null ? group : ""));

			Map<String, Object> query = new HashMap<String, Object>(plugin.defaultQuery);
			query.put("study", studyId.toString());
			if (restrictRead && group != null)
				query.put("study-group", group);

			if (shareBack) {
				query.put("target-study", studyId.toString());
				if (group != null)
					query.put("target-study-group", group);
			}
			query.put("link-study", studyId.toString());
			if (group != null)
				query.put("link-study-group", group);

			RecordManager.instance.shareByQuery(userId, userId, space._id, query);

		}
		AuditManager.instance.success();
		return ok();
	}

	/**
	 * add a task for all participants of a group of a study
	 * 
	 * @param id
	 *            ID of study
	 * @param group
	 *            name of group
	 * @return status ok
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addTask(String id, String group) throws AppException, JsonValidationException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner", "type", "executionStatus", "participantSearchStatus", "validationStatus", "name", "createdBy", "code"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED)
			throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING)
			throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");

		// validate json
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "plugin", "context", "title", "description", "pluginQuery", "frequency");

		Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyid, group, Sets.create());

		for (StudyParticipation part : parts) {
			Task task = new Task();
			task._id = new MidataId();
			task.owner = part.owner;
			task.createdBy = userId;
			task.plugin = JsonValidation.getMidataId(json, "plugin");
			task.shareBackTo = part._id;
			task.createdAt = new Date(System.currentTimeMillis());
			task.deadline = JsonValidation.getDate(json, "deadline");
			task.context = JsonValidation.getString(json, "context");
			task.title = JsonValidation.getString(json, "title");
			task.description = JsonValidation.getString(json, "description");
			task.pluginQuery = JsonExtraction.extractMap(json.get("pluginQuery"));
			task.frequency = JsonValidation.getEnum(json, "frequency", Frequency.class);
			task.done = false;
			Task.add(task);
		}

		return ok();
	}

	/**
	 * list participation consents of all participants of a study
	 * 
	 * @param id
	 *            ID of study
	 * @return list of Consents (StudyParticipation)
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(ResearchSecured.class)
	public static Result listParticipants(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyid = new MidataId(id);
		JsonNode json = request().body().asJson();

		Study study = Study.getById(studyid, Sets.create("owner", "type", "executionStatus", "participantSearchStatus", "validationStatus"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		UserGroupMember ugm = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (ugm == null)
			throw new BadRequestException("error.notauthorized.study", "Not member of study team");

		Set<String> fields = Sets.create("owner", "ownerName", "group", "recruiter", "recruiterName", "pstatus", "gender", "country", "yearOfBirth", "partName");
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		List<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(studyid, properties, fields, 1000);
		if (!ugm.role.pseudonymizedAccess()) {
			for (StudyParticipation part : participants) {
				part.partName = part.ownerName;
				part.ownerName = null;
			}
		}
		ReferenceTool.resolveOwners(participants, true);
		fields.remove("owner");
		return ok(JsonOutput.toJson(participants, "Consent", fields));
	}

	/**
	 * count participation consents of all participants of a study
	 * 
	 * @param id
	 *            ID of study
	 * @return list of Consents (StudyParticipation)
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(ResearchSecured.class)
	public static Result countParticipants(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyid = new MidataId(id);
		JsonNode json = request().body().asJson();

		Study study = Study.getById(studyid, Sets.create("owner", "type", "executionStatus", "participantSearchStatus", "validationStatus"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		UserGroupMember ugm = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (ugm == null)
			throw new BadRequestException("error.notauthorized.study", "Not member of study team");

		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		long participants = StudyParticipation.countParticipantsByStudy(studyid, properties);
		ObjectNode obj = Json.newObject();
		obj.put("total", participants);
		return ok(obj);
	}

	/**
	 * retrieve information about a participant of a study of the current
	 * research organization
	 * 
	 * @param studyidstr
	 *            ID of study
	 * @param partidstr
	 *            ID of participation
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getParticipant(String studyidstr, String partidstr) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyId = new MidataId(studyidstr);
		MidataId partId = new MidataId(partidstr);

		Study study = Study.getById(studyId, Sets.create("type", "createdAt", "createdBy", "description", "executionStatus", "name", "participantSearchStatus", "validationStatus", "infos", "owner",
				"participantRules", "recordQuery", "studyKeywords", "requiredInformation", "anonymous"));
		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		UserGroupMember ugm = UserGroupMember.getByGroupAndMember(studyId, userId);
		if (ugm == null)
			throw new BadRequestException("error.notauthorized.study", "Not member of study team");

		Set<String> participationFields = Sets.create("pstatus", "status", "group", "ownerName", "gender", "country", "yearOfBirth", "owner", "partName", "records");
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, participationFields);

		if (participation == null)
			throw new BadRequestException("error.unknown.participant", "Member does not participate in study");
		if (participation.pstatus == ParticipationStatus.CODE || participation.pstatus == ParticipationStatus.MATCH || participation.pstatus == ParticipationStatus.MEMBER_REJECTED)
			throw new BadRequestException("error.unknown.participant", "Member does not participate in study");

		if (!ugm.role.pseudonymizedAccess()) {
			participation.partName = participation.ownerName;
			participation.ownerName = null;
		}
		ReferenceTool.resolveOwners(Collections.singleton(participation), true);

		if (participation.status.equals(ConsentStatus.ACTIVE) || participation.status.equals(ConsentStatus.FROZEN)) {
			Collection<RecordsInfo> stats = RecordManager.instance.info(userId, UserRole.RESEARCH, participation._id, RecordManager.instance.createContextFromConsent(userId, participation), CMaps.map(),
					AggregationType.ALL);
			if (!stats.isEmpty())
				participation.records = stats.iterator().next().count;

		}

		ObjectNode obj = Json.newObject();
		obj.put("participation", JsonOutput.toJsonNode(participation, "Consent", participationFields));

		if (!study.anonymous && (study.requiredInformation == InformationType.DEMOGRAPHIC || !ugm.role.pseudonymizedAccess())) {
			Set<String> memberFields = Sets.create("_id", "firstname", "lastname", "address1", "address2", "city", "zip", "country", "email", "phone", "mobile");
			Member member = Member.getById(participation.owner, memberFields);
			if (member == null)
				return badRequest("Member does not exist");
			obj.put("member", JsonOutput.toJsonNode(member, "User", memberFields));
		} else {
			participation.owner = null;
		}

		return ok(obj);
	}

	/**
	 * approve participation of member in study by researcher
	 * 
	 * @param id
	 *            ID of member
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result approveParticipation(String id) throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "member");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyId = new MidataId(id);
		MidataId partId = new MidataId(JsonValidation.getString(json, "member"));
		MidataId owner = PortalSessionToken.session().getOrg();
		String comment = JsonValidation.getString(json, "comment");

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, Sets.create("name", "pstatus", "owner", "ownerName", "group"));
		Study study = Study.getById(studyId, Sets.create("name", "executionStatus", "participantSearchStatus", "createdBy", "owner", "code", "type"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
		if (participation == null)
			throw new BadRequestException("error.unknown.participant", "Member does not participate in study");

		// AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_APPROVED,
		// userId, participation, study);
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING)
			throw new BadRequestException("error.closed.study", "Study participant search already closed.");
		if (participation.pstatus != ParticipationStatus.REQUEST)
			throw new BadRequestException("error.invalid.status_transition", "Wrong participant status.");
		if (participation.group == null)
			throw new BadRequestException("error.missing.study_group", "No group assigned to participant.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyId, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.manageParticipants())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to manage participants.");

		autoApprove(null, study, userId, participation.group, Collections.singletonList(participation));

		return ok();
	}

	public static void autoApprove(MidataId app, Study study, MidataId userId, String group) throws AppException {
		Set<String> fields = Sets.create("owner", "ownerName", "group", "recruiter", "recruiterName", "pstatus", "gender", "country", "yearOfBirth", "partName");
		List<StudyParticipation> participants1 = StudyParticipation.getParticipantsByStudy(study._id, CMaps.map("pstatus", ParticipationStatus.REQUEST), fields, 0);

		autoApprove(app, study, userId, group, participants1);
	}

	public static void autoApprove(MidataId app, Study study, MidataId userId, String group, List<StudyParticipation> participants1) throws AppException {
		AccessLog.log("auto approve study=" + study._id.toString() + " executor=" + userId.toString() + " #participants=" + participants1.size());
		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING)
			throw new BadRequestException("error.closed.study", "Study participant search already closed.");
		if (study.executionStatus != StudyExecutionStatus.PRE && study.executionStatus != StudyExecutionStatus.RUNNING)
			throw new BadRequestException("error.no_alter.group", "Study is already running.");

		UserGroupMember ugmm = UserGroupMember.getByGroupAndMember(study._id, userId);
		if (ugmm == null)
			throw new BadRequestException("error.notauthorized.study", "Not member of study team");
		if (!ugmm.role.manageParticipants())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to manage participants.");

		Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByGroup(study._id);

		List<List<StudyParticipation>> parts = Lists.partition(participants1, 1000);
		for (List<StudyParticipation> participants : parts) {

			for (UserGroupMember ugm : ugms) {
				if (ugm.role.manageParticipants()) {
					controllers.research.Studies.joinSharing(userId, ugm.member, study, group, true, participants);
				}
			}

			Set<MidataId> ids = new HashSet<MidataId>();

			for (StudyParticipation participation : participants) {
				if (!group.equals(participation.group))
					AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_GROUP_ASSIGNED, app, userId, participation, study);
				participation.group = group;
				ids.add(participation._id);
			}

			StudyParticipation.setManyGroup(ids, group);

			for (StudyParticipation participation : participants) {
				if (!participation.pstatus.equals(ParticipationStatus.ACCEPTED))
					AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_APPROVED, app, userId, participation, study);
			}

			StudyParticipation.setManyStatus(ids, ParticipationStatus.ACCEPTED);

			AuditManager.instance.success();
		}

	}

	public static void autoApproveCheck(MidataId app, MidataId studyid, MidataId owner) throws AppException {
		Study study = Study.getById(studyid, Sets.create("_id", "participantSearchStatus", "executionStatus", "autoJoinGroup", "name", "code", "type"));
		if (study != null && study.autoJoinGroup != null) {
			if (study.participantSearchStatus.equals(ParticipantSearchStatus.SEARCHING)
					&& (study.executionStatus.equals(StudyExecutionStatus.PRE) || study.executionStatus.equals(StudyExecutionStatus.RUNNING))) {
				try {
					autoApprove(app, study, owner, study.autoJoinGroup);
				} catch (BadRequestException e) {
				} // We are not interested if the researcher is not allowed to
					// do it.
			}
		}
	}

	/**
	 * reject participation of member in study
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result rejectParticipation(String id) throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "member");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyId = new MidataId(id);
		MidataId partId = new MidataId(JsonValidation.getString(json, "member"));
		MidataId owner = PortalSessionToken.session().getOrg();
		String comment = JsonValidation.getString(json, "comment");

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, Sets.create(Consent.ALL, "pstatus", "ownerName", "owner", "authorized"));
		Study study = Study.getById(studyId, Sets.create("name", "type", "executionStatus", "participantSearchStatus", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
		if (participation == null)
			throw new BadRequestException("error.unknown.participant", "Member does not participate in study");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_RESEARCH_REJECTED, userId, participation, study);
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING)
			throw new BadRequestException("error.closed.study", "Study participant search already closed.");
		if (participation.pstatus != ParticipationStatus.REQUEST)
			return badRequest("Wrong participation status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyId, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.manageParticipants())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to manage participants.");

		// participation.addHistory(new
		// History(EventType.PARTICIPATION_REJECTED, user, comment));
		Circles.consentStatusChange(userId, participation, ConsentStatus.REJECTED);
		participation.setPStatus(ParticipationStatus.RESEARCH_REJECTED);
		AuditManager.instance.success();

		return ok();
	}

	/**
	 * update participation information of member in study by researcher.
	 * (Currently changes only group)
	 * 
	 * @param id
	 *            ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result updateParticipation(String id) throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "member", "group");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId studyId = new MidataId(id);
		MidataId partId = new MidataId(JsonValidation.getString(json, "member"));
		MidataId owner = PortalSessionToken.session().getOrg();
		String comment = JsonValidation.getString(json, "comment");

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, Sets.create("name", "pstatus", "owner", "ownerName"));
		Study study = Study.getById(studyId, Sets.create("name", "executionStatus", "participantSearchStatus", "code", "owner", "createdBy", "type"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
		if (participation == null)
			throw new BadRequestException("error.unknown.participant", "Member does not participate in study");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_GROUP_ASSIGNED, userId, participation, study);
		if (study.executionStatus != StudyExecutionStatus.PRE && participation.pstatus == ParticipationStatus.ACCEPTED)
			throw new BadRequestException("error.no_alter.group", "Study is already running.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyId, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.manageParticipants())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to manage participants.");

		participation.group = JsonValidation.getString(json, "group");
		// participation.addHistory(new History(EventType.GROUP_ASSIGNED, user,
		// comment));
		StudyParticipation.set(participation._id, "group", participation.group);
		AuditManager.instance.success();

		return ok();
	}

	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getRequiredInformationSetup(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		Study study = Study.getById(studyid, Sets.create("owner", "executionStatus", "participantSearchStatus", "validationStatus", "requiredInformation", "anonymous", "assistance"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		ObjectNode result = Json.newObject();
		result.put("identity", study.requiredInformation.toString());
		result.put("anonymous", study.anonymous);
		result.put("assistance", study.assistance.toString());
		return ok(result);
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result setRequiredInformationSetup(String id) throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();

		JsonValidation.validate(json, "identity", "assistance");

		InformationType inf = JsonValidation.getEnum(json, "identity", InformationType.class);
		AssistanceType assist = JsonValidation.getEnum(json, "assistance", AssistanceType.class);
		boolean anonymous = JsonValidation.getBoolean(json, "anonymous");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "requiredInformation", "anonymous", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_SETUP_CHANGED, userId, null, study);

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		if (study.validationStatus != StudyValidationStatus.DRAFT)
			throw new BadRequestException("error.no_alter.study", "Setup can only be changed as long as study is in draft phase.");

		if (anonymous && inf.equals(InformationType.DEMOGRAPHIC))
			throw new BadRequestException("error.invalid.anonymous", "Anonymous can only be set if pseudonymized.");

		if (anonymous) {
			for (UserGroupMember member : UserGroupMember.getAllActiveByGroup(study._id)) {
				if (!member.role.pseudonymizedAccess())
					throw new BadRequestException("error.invalid.anonymous", "Anonymous can only be set if no team member may access unpseudonymized.");
			}
		}

		study.setRequiredInformation(inf);
		study.setAnonymous(anonymous);
		study.setAssistance(assist);

		AuditManager.instance.success();
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result update(String id) throws AppException {
		JsonNode json = request().body().asJson();

		// JsonValidation.validate(json, "groups");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "owner", "type", "joinMethods", "infos", "executionStatus", "participantSearchStatus", "validationStatus", "requiredInformation", "anonymous", "code", "startDate",
				"endDate", "dataCreatedBefore"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_SETUP_CHANGED, userId, null, study);

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");
		if (study.validationStatus != StudyValidationStatus.DRAFT)
			return badRequest("Setup can only be changed as long as study is in draft phase.");

		if (json.has("groups")) {

			List<StudyGroup> groups = new ArrayList<StudyGroup>();
			for (JsonNode group : json.get("groups")) {
				StudyGroup grp = new StudyGroup();
				grp.name = group.get("name").asText();
				grp.description = group.get("description").asText();
				groups.add(grp);
			}

			// study.addHistory(new History(EventType.STUDY_SETUP_CHANGED, user,
			// null));
			study.setGroups(groups);

		}

		if (json.has("recordQuery")) {
			Map<String, Object> query = JsonExtraction.extractMap(json.get("recordQuery"));
			Query.validate(query, true);
			// study.addHistory(new History(EventType.STUDY_SETUP_CHANGED, user,
			// null));
			study.setRecordQuery(query);
		}

		if (json.has("termsOfUse")) {
			study.setTermsOfUse(JsonValidation.getString(json, "termsOfUse"));
		}
		if (json.has("requirements")) {
			study.setRequirements(JsonExtraction.extractEnumSet(json, "requirements", UserFeature.class));
		}
		if (json.has("startDate")) {
			study.setStartDate(JsonValidation.getDate(json, "startDate"));
		}
		if (json.has("endDate")) {
			study.setEndDate(JsonValidation.getDate(json, "endDate"));
		}

		if (json.has("dataCreatedBefore")) {

			study.setDataCreatedBefore(JsonValidation.getDate(json, "dataCreatedBefore"));
		}
		if (json.has("name")) {
			study.setName(JsonValidation.getString(json, "name"));
		}
		if (json.has("description")) {
			study.setDescription(JsonValidation.getString(json, "description"));
		}
		if (json.has("type")) {
			study.setType(JsonValidation.getEnum(json, "type", StudyType.class));
		}
		if (json.has("joinMethods")) {
			study.setJoinMethods(JsonValidation.getEnumSet(json, "joinMethods", JoinMethod.class));
		}

		AuditManager.instance.success();
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result updateNonSetup(String id) throws AppException {
		JsonNode json = request().body().asJson();

		// JsonValidation.validate(json, "groups");

		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		// MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "requiredInformation", "anonymous", "code", "startDate",
				"endDate", "dataCreatedBefore", "type"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");

		if (json.has("processFlags")) {
			study.setProcessFlags(JsonExtraction.extractStringSet(json.get("processFlags")));
		}

		if (json.has("autoJoinGroup")) {
			if (!self.role.manageParticipants())
				throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");
			String grp = JsonValidation.getStringOrNull(json, "autoJoinGroup");

			if (JsonValidation.getBoolean(json, "autoJoin")) {
				study.setAutoJoinGroup(grp);
			} else
				study.setAutoJoinGroup(null);

			if (grp != null) {
				autoApprove(null, study, userId, grp);
			}
		}
		if (json.has("infos")) {
			if (!self.role.maySetup()) 
				throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");
			
			JsonNode infos = json.get("infos");
			List<Info> result = new ArrayList<Info>();
			for (JsonNode info : infos) {
			   Info inf = new Info();
			   inf.type = JsonValidation.getEnum(info, "type", InfoType.class);
			   inf.value = JsonExtraction.extractStringMap(info.get("value"));
			   result.add(inf);
			}
			
			study.setInfos(result);
		}
		if (json.has("infosPart")) {
			if (!self.role.maySetup()) 
				throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");
			
			JsonNode infos = json.get("infosPart");
			List<Info> result = new ArrayList<Info>();
			for (JsonNode info : infos) {
			   Info inf = new Info();
			   inf.type = JsonValidation.getEnum(info, "type", InfoType.class);
			   inf.value = JsonExtraction.extractStringMap(info.get("value"));
			   result.add(inf);
			}
			
			study.setInfosPart(result);
		}
		if (json.has("infosInternal")) {
			if (!self.role.maySetup()) 
				throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");
			
			JsonNode infos = json.get("infosInternal");
			List<Info> result = new ArrayList<Info>();
			for (JsonNode info : infos) {
			   Info inf = new Info();
			   inf.type = JsonValidation.getEnum(info, "type", InfoType.class);
			   inf.value = JsonExtraction.extractStringMap(info.get("value"));
			   result.add(inf);
			}
			
			study.setInfosInternal(result);
		}


		return ok();
	}

	/**
	 * delete a Study
	 * 
	 * @return 200 ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result delete(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create("name", "type", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		// if (study.validationStatus != StudyValidationStatus.VALIDATED) return
		// badRequest("Study must be validated before.");
		// if (study.participantSearchStatus != ParticipantSearchStatus.CLOSED)
		// return badRequest("Participant search must be closed before.");
		if (study.executionStatus != StudyExecutionStatus.PRE && study.executionStatus != StudyExecutionStatus.ABORTED)
			throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");

		UserGroupMember self = UserGroupMember.getByGroupAndMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.role.maySetup())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change study setup.");

		deleteStudy(userId, study._id, false);

		return ok();
	}

	public static void deleteStudy(MidataId userId, MidataId studyId, boolean force) throws AppException {
		Study study = Study.getById(studyId, Study.ALL);

		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_DELETED, userId, null, study);

		Set<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(studyId, Sets.create("_id", "owner"));
		for (StudyParticipation part : participants) {
			if (force) {
				AccessPermissionSet.delete(part._id);
			} else {
				RecordManager.instance.deleteAPS(part._id, userId);
			}
			StudyParticipation.delete(studyId, part._id);
		}

		Set<StudyRelated> related = StudyRelated.getByStudy(studyId, Sets.create("authorized"));
		for (StudyRelated studyRelated : related) {
			if (force) {
				AccessPermissionSet.delete(studyRelated._id);
			} else {
				RecordManager.instance.deleteAPS(studyRelated._id, userId);
			}
			StudyRelated.deleteByStudyAndParticipant(studyId, studyRelated._id);
		}

		Study.delete(studyId);

		AuditManager.instance.success();
	}

}
