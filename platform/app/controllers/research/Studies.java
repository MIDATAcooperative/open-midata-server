package controllers.research;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import controllers.APIController;
import controllers.Circles;
import models.AccessPermissionSet;
import models.Admin;
import models.Consent;
import models.FilterRule;
import models.History;
import models.Info;
import models.Member;
import models.MidataId;
import models.ParticipationCode;
import models.Record;
import models.ResearchUser;
import models.Study;
import models.StudyGroup;
import models.StudyParticipation;
import models.StudyRelated;
import models.Task;
import models.User;
import models.enums.AssistanceType;
import models.enums.ConsentStatus;
import models.enums.EventType;
import models.enums.Frequency;
import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.Query;
import utils.access.RecordManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.PortalSessionToken;
import utils.auth.ResearchSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions about studies to be used by researchers
 *
 */
public class Studies extends APIController {

	/**
	 * create a new study 
	 * @return Study
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result create() throws JsonValidationException, InternalServerException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "description");
					
		String name = JsonValidation.getString(json, "name");
		if (Study.existsByName(name)) return inputerror("name", "exists", "A study with this name already exists.");
		
		MidataId userId = new MidataId(request().username());
		MidataId research = PortalSessionToken.session().getOrg();
				
		if (research == null) throw new InternalServerException("error.internal", "No organization associated with session.");
		Study study = new Study();
				
		study._id = new MidataId();
		study.name = name;
		do {
		  study.code = CodeGenerator.nextUniqueCode();
		} while (Study.existsByCode(study.code));
		study.description = JsonValidation.getString(json, "description");
		
		study.createdAt = new Date();
		study.createdBy = userId;
		study.owner = research;
		
		study.validationStatus = StudyValidationStatus.DRAFT;
		study.participantSearchStatus = ParticipantSearchStatus.PRE;
		study.executionStatus = StudyExecutionStatus.PRE;
				
		study.history = new ArrayList<History>();
		study.infos = new ArrayList<Info>();
		study.participantRules = new HashSet<FilterRule>();
		study.recordQuery = new HashMap<String, Object>();
		study.requiredInformation = InformationType.RESTRICTED;
		study.assistance = AssistanceType.NONE;
		study.groups = new ArrayList<StudyGroup>();		
				
		study.studyKeywords = new HashSet<MidataId>();			
		
		Study.add(study);			
		
		return ok(JsonOutput.toJson(study, "Study", Study.ALL));
	}
	
	/**
	 * download data about a study of the current research organization
	 * @param id ID of study
	 * @return not yet: (ZIP file) 
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result download(String id) throws AppException, IOException {
		 MidataId studyid = new MidataId(id);
		 MidataId owner = PortalSessionToken.session().getOrg();
		 MidataId executorId = new MidataId(request().username());
		   
		 Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("executionStatus","participantSearchStatus","validationStatus","history","owner","groups"));

		 if (study == null) throw new BadRequestException("error.unknown.study", "Unknown Study");

		 setAttachmentContentDisposition("study.zip");
				 		 		
		 ByteArrayOutputStream servletOutputStream = new ByteArrayOutputStream();
			    ZipOutputStream zos = new ZipOutputStream(servletOutputStream); // create a ZipOutputStream from servletOutputStream

			    ZipEntry entry = new ZipEntry("participants.json");			    
		        zos.putNextEntry(entry);

		        Writer output = new OutputStreamWriter(zos);			   
			    for (StudyGroup group : study.groups) {
					 Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(study._id, group.name, Sets.create("ownerName", "yearOfBirth", "country", "gender", "group"));
					 			 			 
					 //for (StudyParticipation part : parts) {						 
					 output.append(JsonOutput.toJson(parts, "Consent", Sets.create("ownerName", "yearOfBirth", "country", "gender", "group")));						 						 						 			
					 //}					 
				 }			    
			     output.flush();
			     zos.closeEntry();
			     
			     for (StudyGroup group : study.groups) {
			     entry = new ZipEntry("records-"+group.name+".json");
			     zos.putNextEntry(entry);
			     output = new OutputStreamWriter(zos);
			     
				 Set<String> fields = Sets.create( 
							"owner", "ownerName", "app", "creator", "created", "name", "format", "content", "description", "data"); 
				 List<Record> allRecords = RecordManager.instance.list(executorId, executorId, CMaps.map("study", study._id).map("study-group", group.name), fields);
						 
				 output.append(JsonOutput.toJson(allRecords, "Record" , fields));			     
			    
			     output.flush();
			     zos.closeEntry();
			     }
			    

			    zos.close(); // finally closing the ZipOutputStream to mark completion of ZIP file
			    
			    return ok(servletOutputStream.toByteArray());
		 
	}
	
	/**
	 * list all studies of current research organization	
	 * @return list of studies
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result list() throws JsonValidationException, InternalServerException {
	   MidataId owner = PortalSessionToken.session().getOrg();
	   
	   Set<String> fields = Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus");
	   Set<Study> studies = Study.getByOwner(owner, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields));
	}
	
	/**
	 * list all studies waiting for validation	
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
	   Set<String> fields = Sets.create("createdAt","createdBy","description","name");
	   Set<Study> studies = Study.getAll(null, properties, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields));
	}

	/**
	 * retrieve one study of current research organization
	 * @param id ID of study
	 * @return Study
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result get(String id) throws JsonValidationException, InternalServerException {
       
	   MidataId studyid = new MidataId(id);
	   MidataId owner = PortalSessionToken.session().getOrg();

	   Set<String> fields = Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordQuery","studyKeywords","code","groups","requiredInformation", "assistance"); 
	   Study study = Study.getByIdFromOwner(studyid, owner, fields);
	   	   	   
	   return ok(JsonOutput.toJson(study, "Study", fields));
	}
	
	/**
	 * retrieve one study (for validation by admin)
	 * @param id ID of study
	 * @return Study
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result getAdmin(String id) throws JsonValidationException, InternalServerException {
       
	   MidataId studyid = new MidataId(id);
	   
	   Set<String> fields = Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordQuery","studyKeywords","code","groups","requiredInformation", "assistance"); 
	   Study study = Study.getByIdFromMember(studyid, fields);
	   	   	   
	   ObjectNode result = Json.newObject();
	   result.put("study", JsonOutput.toJsonNode(study, "Study", fields));
	   
	   return ok(result);	   
	}
	
	/**
	 * generates participation codes. NEEDS REWRITE
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
	
	   MidataId userId = new MidataId(request().username());
	   MidataId owner = PortalSessionToken.session().getOrg();
	   MidataId studyid = new MidataId(id);
	   
	   User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
	   String userName = user.lastname+", "+user.firstname;
		
	   
	   int count = JsonValidation.getInteger(json, "count", 1, 1000);
	   String group = JsonValidation.getString(json, "group");
	   boolean reuseable = JsonValidation.getBoolean(json, "reuseable");
	   Date now = new Date();
	   	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
	   if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
	   if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED) throw new BadRequestException("error.closed.study", "Study participant search already closed.");
	   	   	   
	   for (int num=1 ; num <= count; num++) {
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
	   String comment = count+" codes";
	   if (reuseable) comment += ", reuseable";
	   if (group!=null && ! "".equals(group)) comment +=", group="+group;
	   study.addHistory(new History(EventType.CODES_GENERATED, user, comment));
	   
	   return ok();
	}
	
	/**
	 * list participation codes. NEEDS REWRITE
	 * @param id
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result listCodes(String id) throws JsonValidationException, AppException {
	   MidataId userId = new MidataId(request().username());
	   MidataId owner = PortalSessionToken.session().getOrg();
	   MidataId studyid = new MidataId(id);
	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
	   if (study.validationStatus != StudyValidationStatus.VALIDATED) return statusWarning("study_not_validated", "Study must be validated before.");
	   if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED) return statusWarning("participant_search_closed", "Study participant search already closed.");
	 
	   Set<ParticipationCode> codes = ParticipationCode.getByStudy(studyid);
	   
	   return ok(Json.toJson(codes));
	}
	
	/**
	 * start study validation
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startValidation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history","groups","recordQuery"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus == StudyValidationStatus.VALIDATED) return badRequest("Study has already been validated.");
		if (study.validationStatus == StudyValidationStatus.VALIDATION) return badRequest("Validation is already in progress.");
		
		if (study.groups == null || study.groups.size() == 0) return badRequest("Please define study groups before validation!");
		if (study.recordQuery == null || study.recordQuery.isEmpty()) return badRequest("Please define record sharing query before validation!");
		
		
		study.setValidationStatus(StudyValidationStatus.VALIDATION); 
		study.addHistory(new History(EventType.VALIDATION_REQUESTED, user, null));
		
		if (InstanceConfig.getInstance().getInstanceType().getStudiesValidateAutomatically()) {
		   study.setValidationStatus(StudyValidationStatus.VALIDATED); 
		   study.addHistory(new History(EventType.STUDY_VALIDATED, user, null));
		}
						
		return ok();
	}
	
	/**
	 * end study validation
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result endValidation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());		
		MidataId studyid = new MidataId(id);
		
		User user = Admin.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromMember(studyid, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history","groups","recordQuery"));
		
		if (study == null) throw new BadRequestException("error.missing.study", "Study does not exist");
		if (!study.validationStatus.equals(StudyValidationStatus.VALIDATION)) return badRequest("Study has already been validated.");
							
		study.setValidationStatus(StudyValidationStatus.VALIDATED); 
		study.addHistory(new History(EventType.STUDY_VALIDATED, user, null));
						
		return ok();
	}
	
	/**
	 * start participant search phase
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startParticipantSearch(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.PRE) return badRequest("Participants can only be searched as long as study has not stared.");
		if (study.participantSearchStatus != ParticipantSearchStatus.PRE && study.participantSearchStatus != ParticipantSearchStatus.CLOSED) return badRequest("Study participant search already started.");
		
		study.setParticipantSearchStatus(ParticipantSearchStatus.SEARCHING);
		study.addHistory(new History(EventType.PARTICIPANT_SEARCH_STARTED, user, null));
						
		return ok();
	}
	
	/**
	 * end participant search phase of a study
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result endParticipantSearch(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new BadRequestException("error.closed.study", "Study participant search already closed.");
		
		study.setParticipantSearchStatus(ParticipantSearchStatus.CLOSED);
		study.addHistory(new History(EventType.PARTICIPANT_SEARCH_CLOSED, user, null));
						
		return ok();
	}
	
	/**
	 * start execution phase of study
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startExecution(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.participantSearchStatus != ParticipantSearchStatus.CLOSED) return badRequest("Participant search must be closed before.");
		if (study.executionStatus != StudyExecutionStatus.PRE) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
		
		study.setExecutionStatus(StudyExecutionStatus.RUNNING);
		study.addHistory(new History(EventType.STUDY_STARTED, user, null));
						
		return ok();
	}
	
	/**
	 * end execution phase of study
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result finishExecution(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");		
		if (study.executionStatus != StudyExecutionStatus.RUNNING) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
		
		study.setExecutionStatus(StudyExecutionStatus.FINISHED);
		study.addHistory(new History(EventType.STUDY_FINISHED, user, null));
						
		return ok();
	}
	
	/**
	 * abort execution of study
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result abortExecution(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");		
		if (study.executionStatus != StudyExecutionStatus.RUNNING) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
		
		study.setExecutionStatus(StudyExecutionStatus.ABORTED);
		study.addHistory(new History(EventType.STUDY_ABORTED, user, null));
						
		return ok();
	}
	
	/**
	 * share records with a group of participants of a study
	 * @param id ID of study
	 * @param group name of group 
	 * @return Consent that authorizes participants to view records
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result shareWithGroup(String id, String group) throws AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "name"));
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
		
		StudyRelated consent = StudyRelated.getByGroupAndStudy(group, studyid, Sets.create("authorized"));
		
		if (consent == null) {
			consent = new StudyRelated();
			consent._id = new MidataId();
			consent.study = studyid;
			consent.group = group;			
			consent.owner = userId;
			consent.name = "Study:"+study.name;		
			consent.authorized = new HashSet<MidataId>();
			consent.status = ConsentStatus.ACTIVE;
						
			RecordManager.instance.createAnonymizedAPS(userId, userId, consent._id, true);
			Circles.prepareConsent(consent);
			consent.add();
		}
		
		Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(studyid, group, Sets.create());
		Set<MidataId> participants = new HashSet<MidataId>();
		for (StudyParticipation part : parts) { participants.add(part.owner); }
		
		consent.authorized.addAll(participants);
		StudyRelated.set(consent._id, "authorized", consent.authorized);
		RecordManager.instance.shareAPS(consent._id, userId, participants);
				
		return ok(JsonOutput.toJson(consent, "Consent", Sets.create("_id", "authorized")));		
	}
	
	/**
	 * add a task for all participants of a group of a study
	 * @param id ID of study
	 * @param group name of group
	 * @return status ok
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addTask(String id, String group) throws AppException, JsonValidationException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "name"));
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) throw new BadRequestException("error.notvalidated.study", "Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
		
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
	 * @param id ID of study
	 * @return list of Consents (StudyParticipation)
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result listParticipants(String id) throws JsonValidationException, AppException {
	   MidataId userId = new MidataId(request().username());
	   MidataId owner = PortalSessionToken.session().getOrg();
	   MidataId studyid = new MidataId(id);
	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
	   
       Set<String> fields = Sets.create("ownerName", "group", "recruiter", "recruiterName", "pstatus", "gender", "country", "yearOfBirth"); 
	   Set<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(studyid, fields);
	   
	   
	   return ok(JsonOutput.toJson(participants, "Consent", fields));
	}
	
	/**
	 * retrieve information about a participant of a study of the current research organization
	 * @param studyidstr ID of study
	 * @param partidstr ID of participation
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getParticipant(String studyidstr, String partidstr) throws JsonValidationException, AppException {
	   //MidataId userId = new MidataId(request().username());	
	   MidataId owner = PortalSessionToken.session().getOrg();
	   MidataId studyId = new MidataId(studyidstr);
	   MidataId partId = new MidataId(partidstr);
	   	   
	   Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordQuery","studyKeywords"));
	   if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
	   	   
	   Set<String> participationFields = Sets.create("pstatus", "status", "group", "history","ownerName", "gender", "country", "yearOfBirth", "owner"); 
	   StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, participationFields);
	   if (participation == null) throw new BadRequestException("error.unknown.participant", "Member does not participate in study");
	   if (participation.pstatus == ParticipationStatus.CODE || 
		   participation.pstatus == ParticipationStatus.MATCH || 
		   participation.pstatus == ParticipationStatus.MEMBER_REJECTED) throw new BadRequestException("error.unknown.participant", "Member does not participate in study");
	   
	   if (study.requiredInformation != InformationType.DEMOGRAPHIC) { participation.owner = null; }
	   
	   ObjectNode obj = Json.newObject();
	   obj.put("participation", JsonOutput.toJsonNode(participation, "Consent", participationFields));	   
	    
	   if (study.requiredInformation == InformationType.DEMOGRAPHIC) {
		 Set<String> memberFields = Sets.create("_id", "firstname","lastname","address1","address2","city","zip","country","email", "phone","mobile"); 
	     Member member = Member.getById(participation.owner, memberFields);
	     if (member == null) return badRequest("Member does not exist");
	     obj.put("member", JsonOutput.toJsonNode(member, "User", memberFields));
	   }
	   
	   return ok(obj);
	}
	
	/**
	 * approve participation of member in study by researcher
	 * @param id ID of member
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
		
		MidataId userId = new MidataId(request().username());		
		MidataId studyId = new MidataId(id);
		MidataId partId = new MidataId(JsonValidation.getString(json, "member"));
		MidataId owner = PortalSessionToken.session().getOrg();
		String comment = JsonValidation.getString(json, "comment");
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));				
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, Sets.create("pstatus", "history", "ownerName"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Unknown Study");	
		if (participation == null) throw new BadRequestException("error.unknown.participant", "Member does not participate in study");	
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new BadRequestException("error.closed.study", "Study participant search already closed.");
		if (participation.pstatus != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.ACCEPTED);
		participation.addHistory(new History(EventType.PARTICIPATION_APPROVED, user, comment));
						
		return ok();
	}
	
	/**
	 * reject participation of member in study
	 * @param id ID of study
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
		
		MidataId userId = new MidataId(request().username());		
		MidataId studyId = new MidataId(id);
		MidataId partId = new MidataId(JsonValidation.getString(json, "member"));
		MidataId owner = PortalSessionToken.session().getOrg();
		String comment = JsonValidation.getString(json, "comment");
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));					
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, Sets.create(Consent.ALL, "pstatus", "history", "ownerName", "owner", "authorized"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Unknown Study");	
		if (participation == null) throw new BadRequestException("error.unknown.participant", "Member does not participate in study");	
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new BadRequestException("error.closed.study", "Study participant search already closed.");
		if (participation.pstatus != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.RESEARCH_REJECTED);		
		participation.addHistory(new History(EventType.PARTICIPATION_REJECTED, user, comment));
		Circles.consentStatusChange(userId, participation, ConsentStatus.REJECTED);
						
		return ok();
	}
	
	/**
	 * update participation information of member in study by researcher. (Currently changes only group)
	 * @param id ID of study
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
		
		MidataId userId = new MidataId(request().username());		
		MidataId studyId = new MidataId(id);
		MidataId partId = new MidataId(JsonValidation.getString(json, "member"));
		MidataId owner = PortalSessionToken.session().getOrg();
		String comment = JsonValidation.getString(json, "comment");
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));					
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, partId, Sets.create("pstatus", "history", "ownerName"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Unknown Study");
		if (participation == null) throw new BadRequestException("error.unknown.participant", "Member does not participate in study");	
		if (study.executionStatus != StudyExecutionStatus.PRE) return badRequest("Study is already running.");
						
		participation.group = JsonValidation.getString(json, "group");
		StudyParticipation.set(participation._id, "group", participation.group);
		participation.addHistory(new History(EventType.GROUP_ASSIGNED, user, comment));
						
		return ok();
	}
	
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getRequiredInformationSetup(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
			
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "requiredInformation", "assistance"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");  
		
		ObjectNode result = Json.newObject();
		result.put("identity", study.requiredInformation.toString());
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
		
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "requiredInformation"));
			
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.DRAFT) throw new BadRequestException("error.no_alter.study", "Setup can only be changed as long as study is in draft phase.");
        				
		study.setRequiredInformation(inf);
		study.setAssistance(assist);
        study.addHistory(new History(EventType.REQUIRED_INFORMATION_CHANGED, user, null));		
        
        return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result update(String id) throws AppException {
        JsonNode json = request().body().asJson();
		
		//JsonValidation.validate(json, "groups");
						
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "requiredInformation"));
			
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.DRAFT) return badRequest("Setup can only be changed as long as study is in draft phase.");
        				
		if (json.has("groups")) {
			List<StudyGroup> groups = new ArrayList<StudyGroup>();
			for (JsonNode group : json.get("groups")) {
				StudyGroup grp = new StudyGroup();
				grp.name = group.get("name").asText();
				grp.description = group.get("description").asText();
				groups.add(grp);
			}
			
			study.setGroups(groups);
			study.addHistory(new History(EventType.STUDY_SETUP_CHANGED, user, null));
		}
		
		if (json.has("recordQuery")) {
			Map<String, Object> query = JsonExtraction.extractMap(json.get("recordQuery"));
			Query.validate(query, true);			
			study.setRecordQuery(query);
			study.addHistory(new History(EventType.STUDY_SETUP_CHANGED, user, null));
		}
				        	        
        return ok();
	}
	
	/**
	 * delete a Study
	 * @return 200 ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result delete(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");
		//if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		//if (study.participantSearchStatus != ParticipantSearchStatus.CLOSED) return badRequest("Participant search must be closed before.");
		if (study.executionStatus != StudyExecutionStatus.PRE) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
	
		deleteStudy(userId, study._id, false);
		
		return ok();
	}
	
	public static void deleteStudy(MidataId userId, MidataId studyId, boolean force) throws AppException {
		
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
			StudyRelated.delete(studyId, studyRelated._id);
		}
		
		Study.delete(studyId);
	}
}
