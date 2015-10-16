package controllers.research;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Consent;
import models.FilterRule;
import models.History;
import models.Info;
import models.Member;
import models.ParticipationCode;
import models.Record;
import models.Research;
import models.ResearchUser;
import models.Study;
import models.StudyGroup;
import models.StudyParticipation;
import models.StudyRelated;
import models.Task;
import models.User;
import models.enums.AssistanceType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EventType;
import models.enums.Frequency;
import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;

import org.bson.types.ObjectId;

import play.api.libs.iteratee.Enumerator;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.ResearchSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.ModelException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.APIController;
import controllers.RecordSharing;


public class Studies extends APIController {

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result create() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "description");
					
		String name = JsonValidation.getString(json, "name");
		if (Study.existsByName(name)) return inputerror("name", "exists", "A study with this name already exists.");
		
		ObjectId userId = new ObjectId(request().username());
		ObjectId research = new ObjectId(session().get("org"));
				
		Study study = new Study();
				
		study._id = new ObjectId();
		study.name = name;
		study.code = CodeGenerator.nextUniqueCode();
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
				
		study.studyKeywords = new HashSet<ObjectId>();
		
		/*StudyGroup defaultGroup = new StudyGroup();
		defaultGroup.name = "default";
		defaultGroup.description="One group for all participants.";
		study.groups*/
		
		Study.add(study);
		
		/*StudyRelated consent = new StudyRelated();
		consent._id = study._id;
		consent.type = ConsentType.STUDYRELATED;
		consent.name = "Study: "+study.name;
		consent.owner = userId;
		consent.status = ConsentStatus.ACTIVE;
		consent.authorized = new HashSet<ObjectId>();
		consent.add();
		
		RecordSharing.instance.createAnonymizedAPS(userId, userId, consent._id);*/
		
		return ok(JsonOutput.toJson(study, "Study", Study.ALL));
	}
	
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result download(String id) throws AppException {
		 ObjectId studyid = new ObjectId(id);
		 ObjectId owner = new ObjectId(session().get("org"));
		 ObjectId executorId = new ObjectId(request().username());
		   
		 Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("executionStatus","participantSearchStatus","validationStatus","history","owner"));

		 if (study == null) return badRequest("Unknown Study");

		 Set<String> fields = Sets.create("id", "ownerName",
					"app", "creator", "created", "name", "format", "content", "description", "data", "group"); 
		 List<Record> allRecords = RecordSharing.instance.list(executorId, executorId, CMaps.map("study",  study._id), fields);
		 		
		 
		 return ok(JsonOutput.toJson(allRecords, "Record" , fields));
		 
		 /*
		 try {
			    OutputStream servletOutputStream = response().getOutputStream(); // retrieve OutputStream from HttpServletResponse
			    ZipOutputStream zos = new ZipOutputStream(servletOutputStream); // create a ZipOutputStream from servletOutputStream

			    List<String[]> csvFileContents  = getContentToZIP(); // get the list of csv contents. I am assuming the CSV content is generated programmatically
			    int count = 0;
			    for (String[] entries : csvFileContents) {
			        String filename = "file-" + ++count  + ".csv";
			        ZipEntry entry = new ZipEntry(filename); // create a zip entry and add it to ZipOutputStream
			        zos.putNextEntry(entry);

			        CSVWriter writer = new CSVWriter(new OutputStreamWriter(zos));  // There is no need for staging the CSV on filesystem or reading bytes into memory. Directly write bytes to the output stream.
			        writer.writeNext(entries);  // write the contents
			        writer.flush(); // flush the writer. Very important!
			        zos.closeEntry(); // close the entry. Note : we are not closing the zos just yet as we need to add more files to our ZIP
			    }

			    zos.close(); // finally closing the ZipOutputStream to mark completion of ZIP file
			} catch (Exception e) {
			    log.error(e); // handle error
			}
		 */
	}
	
		
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result list() throws JsonValidationException, ModelException {
	   ObjectId owner = new ObjectId(session().get("org"));
	   
	   Set<String> fields = Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus");
	   Set<Study> studies = Study.getByOwner(owner, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields));
	}
		
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result get(String id) throws JsonValidationException, ModelException {
       
	   ObjectId studyid = new ObjectId(id);
	   ObjectId owner = new ObjectId(session().get("org"));

	   Set<String> fields = Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordQuery","studyKeywords","code","groups","requiredInformation", "assistance"); 
	   Study study = Study.getByIdFromOwner(studyid, owner, fields);
	   	   	   
	   return ok(JsonOutput.toJson(study, "Study", fields));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result generateCodes(String id) throws JsonValidationException, ModelException {
       
       JsonNode json = request().body().asJson();
		
	   JsonValidation.validate(json, "count", "reuseable");
	
	   ObjectId userId = new ObjectId(request().username());
	   ObjectId owner = new ObjectId(session().get("org"));
	   ObjectId studyid = new ObjectId(id);
	   
	   User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
	   String userName = user.lastname+", "+user.firstname;
		
	   
	   int count = JsonValidation.getInteger(json, "count", 1, 1000);
	   String group = JsonValidation.getString(json, "group");
	   boolean reuseable = JsonValidation.getBoolean(json, "reuseable");
	   Date now = new Date();
	   	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) return badRequest("Study does not belong to organization.");
	   if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
	   if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED) return badRequest("Study participant search already closed.");
	   	   	   
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
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result listCodes(String id) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());
	   ObjectId owner = new ObjectId(session().get("org"));
	   ObjectId studyid = new ObjectId(id);
	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) return badRequest("Study does not belong to organization.");
	   if (study.validationStatus != StudyValidationStatus.VALIDATED) return statusWarning("study_not_validated", "Study must be validated before.");
	   if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED) return statusWarning("participant_search_closed", "Study participant search already closed.");
	 
	   Set<ParticipationCode> codes = ParticipationCode.getByStudy(studyid);
	   
	   return ok(Json.toJson(codes));
	}
	
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startValidation(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus == StudyValidationStatus.VALIDATED) return badRequest("Study has already been validated.");
		if (study.validationStatus == StudyValidationStatus.VALIDATION) return badRequest("Validation is already in progress.");
		
		study.setValidationStatus(StudyValidationStatus.VALIDATED); // TODO to be changed to VALIDATION
		study.addHistory(new History(EventType.VALIDATION_REQUESTED, user, null));
						
		return ok();
	}
	
	//@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startParticipantSearch(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.PRE) return badRequest("Participants can only be searched as long as study has not stared.");
		if (study.participantSearchStatus != ParticipantSearchStatus.PRE && study.participantSearchStatus != ParticipantSearchStatus.CLOSED) return badRequest("Study participant search already started.");
		
		study.setParticipantSearchStatus(ParticipantSearchStatus.SEARCHING);
		study.addHistory(new History(EventType.PARTICIPANT_SEARCH_STARTED, user, null));
						
		return ok();
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result endParticipantSearch(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants.");
		
		study.setParticipantSearchStatus(ParticipantSearchStatus.CLOSED);
		study.addHistory(new History(EventType.PARTICIPANT_SEARCH_CLOSED, user, null));
						
		return ok();
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startExecution(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		if (study.participantSearchStatus != ParticipantSearchStatus.CLOSED) return badRequest("Participant search must be closed before.");
		if (study.executionStatus != StudyExecutionStatus.PRE) return badRequest("Wrong study execution status.");
		
		study.setExecutionStatus(StudyExecutionStatus.RUNNING);
		study.addHistory(new History(EventType.STUDY_STARTED, user, null));
						
		return ok();
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result shareWithGroup(String id, String group) throws AppException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "name"));
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING) return badRequest("Wrong study execution status.");
		
		StudyRelated consent = StudyRelated.getByGroupAndStudy(group, studyid, Sets.create("authorized"));
		
		if (consent == null) {
			consent = new StudyRelated();
			consent._id = new ObjectId();
			consent.study = studyid;
			consent.group = group;			
			consent.owner = userId;
			consent.name = "Study:"+study.name;		
			consent.authorized = new HashSet<ObjectId>();
			consent.status = ConsentStatus.ACTIVE;
						
			RecordSharing.instance.createAnonymizedAPS(userId, userId, consent._id);			
			consent.add();
		}
		
		Set<StudyParticipation> parts = StudyParticipation.getParticipantsByStudyAndGroup(studyid, group, Sets.create());
		Set<ObjectId> participants = new HashSet<ObjectId>();
		for (StudyParticipation part : parts) { participants.add(part.owner); }
		
		consent.authorized.addAll(participants);
		StudyRelated.set(consent._id, "authorized", consent.authorized);
		RecordSharing.instance.shareAPS(consent._id, userId, participants);
				
		return ok(JsonOutput.toJson(consent, "Consent", Sets.create("_id", "authorized")));		
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addTask(String id, String group) throws AppException, JsonValidationException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "name"));
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		if (study.executionStatus != StudyExecutionStatus.RUNNING) return badRequest("Wrong study execution status.");
		
		// validate json
		JsonNode json = request().body().asJson();	
		JsonValidation.validate(json, "plugin", "context", "title", "description", "pluginQuery", "frequency");

		Set<StudyParticipation> parts = StudyParticipation.getParticipantsByStudyAndGroup(studyid, group, Sets.create());
		
		for (StudyParticipation part : parts) {		
			Task task = new Task();
			task._id = new ObjectId();
			task.owner = part.owner;
			task.createdBy = userId;
			task.plugin = JsonValidation.getObjectId(json, "plugin");
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
	
	
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result listParticipants(String id) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());
	   ObjectId owner = new ObjectId(session().get("org"));
	   ObjectId studyid = new ObjectId(id);
	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) return badRequest("Study does not belong to organization.");
	   
       Set<String> fields = Sets.create("ownerName", "owner", "group", "recruiter", "recruiterName", "pstatus", "gender", "country", "yearOfBirth"); 
	   Set<StudyParticipation> participants = StudyParticipation.getParticipantsByStudy(studyid, fields);
	   
	   
	   return ok(JsonOutput.toJson(participants, "Consent", fields));
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getParticipant(String studyidstr, String memberidstr) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());	
	   ObjectId owner = new ObjectId(session().get("org"));
	   ObjectId studyId = new ObjectId(studyidstr);
	   ObjectId memberId = new ObjectId(memberidstr);
	   	   
	   Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordQuery","studyKeywords"));
	   if (study == null) return badRequest("Study does not belong to organization");
	   	   
	   Set<String> participationFields = Sets.create("pstatus", "group", "history","ownerName", "gender", "country", "yearOfBirth", "owner"); 
	   StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, memberId, participationFields);
	   if (participation == null) return badRequest("Member does not participate in study");
	   if (participation.pstatus == ParticipationStatus.CODE || 
		   participation.pstatus == ParticipationStatus.MATCH || 
		   participation.pstatus == ParticipationStatus.MEMBER_REJECTED) return badRequest("Member does not participate in study");
	   
	   if (study.requiredInformation != InformationType.DEMOGRAPHIC) { participation.owner = null; }
	   
	   ObjectNode obj = Json.newObject();
	   obj.put("participation", JsonOutput.toJsonNode(participation, "Consent", participationFields));	   
	    
	   if (study.requiredInformation == InformationType.DEMOGRAPHIC) {
		 Set<String> memberFields = Sets.create("_id", "firstname","lastname","address1","address2","city","zip","country","email", "phone","mobile"); 
	     Member member = Member.getById(memberId, memberFields);
	     if (member == null) return badRequest("Member does not exist");
	     obj.put("member", JsonOutput.toJsonNode(member, "User", memberFields));
	   }
	   
	   return ok(obj);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result approveParticipation(String id) throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "member");
		
		ObjectId userId = new ObjectId(request().username());		
		ObjectId studyId = new ObjectId(id);
		ObjectId memberId = new ObjectId(JsonValidation.getString(json, "member"));
		ObjectId owner = new ObjectId(session().get("org"));
		String comment = JsonValidation.getString(json, "comment");
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));				
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, memberId, Sets.create("pstatus", "history", "memberName"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");		
		if (participation == null) return badRequest("Member is not allowed to participate in study.");		
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants anymore.");
		if (participation.pstatus != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.ACCEPTED);
		participation.addHistory(new History(EventType.PARTICIPATION_APPROVED, user, comment));
						
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result rejectParticipation(String id) throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "member");
		
		ObjectId userId = new ObjectId(request().username());		
		ObjectId studyId = new ObjectId(id);
		ObjectId memberId = new ObjectId(JsonValidation.getString(json, "member"));
		ObjectId owner = new ObjectId(session().get("org"));
		String comment = JsonValidation.getString(json, "comment");
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));					
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, memberId, Sets.create("pstatus", "history", "memberName"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");		
		if (participation == null) return badRequest("Member is not allowed to participate in study.");		
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants anymore.");
		if (participation.pstatus != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.RESEARCH_REJECTED);
		participation.addHistory(new History(EventType.PARTICIPATION_REJECTED, user, comment));
						
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result updateParticipation(String id) throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "member", "group");
		
		ObjectId userId = new ObjectId(request().username());		
		ObjectId studyId = new ObjectId(id);
		ObjectId memberId = new ObjectId(JsonValidation.getString(json, "member"));
		ObjectId owner = new ObjectId(session().get("org"));
		String comment = JsonValidation.getString(json, "comment");
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));					
		StudyParticipation participation = StudyParticipation.getByStudyAndId(studyId, memberId, Sets.create("pstatus", "history", "memberName"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");		
		if (participation == null) return badRequest("Member is not allowed to participate in study.");		
		if (study.executionStatus != StudyExecutionStatus.PRE) return badRequest("Study is already running.");
						
		participation.group = JsonValidation.getString(json, "group");
		StudyParticipation.set(participation._id, "group", participation.group);
		participation.addHistory(new History(EventType.GROUP_ASSIGNED, user, comment));
						
		return ok();
	}
	
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getRequiredInformationSetup(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "requiredInformation", "assistance"));
		
		if (study == null) return badRequest("Study does not belong to organization.");	    
		
		ObjectNode result = Json.newObject();
		result.put("identity", study.requiredInformation.toString());
		result.put("assistance", study.assistance.toString());
		return ok(result);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result setRequiredInformationSetup(String id) throws JsonValidationException, ModelException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "identity", "assistance");
		
		InformationType inf = JsonValidation.getEnum(json, "identity", InformationType.class);
		AssistanceType assist = JsonValidation.getEnum(json, "assistance", AssistanceType.class);
		
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "requiredInformation"));
			
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.DRAFT) return badRequest("Setup can only be changed as long as study is in draft phase.");
        				
		study.setRequiredInformation(inf);
		study.setAssistance(assist);
        study.addHistory(new History(EventType.REQUIRED_INFORMATION_CHANGED, user, null));		
        
        return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result update(String id) throws JsonValidationException, ModelException {
        JsonNode json = request().body().asJson();
		
		//JsonValidation.validate(json, "groups");
						
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","lastname"));
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history", "requiredInformation"));
			
		if (study == null) return badRequest("Study does not belong to organization.");
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
			study.setRecordQuery(JsonExtraction.extractMap(json.get("recordQuery")));
			study.addHistory(new History(EventType.STUDY_SETUP_CHANGED, user, null));
		}
				        	        
        return ok();
	}
	
}
