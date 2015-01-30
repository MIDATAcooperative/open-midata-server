package controllers.research;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import models.FilterRule;
import models.History;
import models.Info;
import models.Member;
import models.ModelException;
import models.ParticipationCode;
import models.Research;
import models.ResearchUser;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.EventType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.CodeGenerator;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.defaultpages.badRequest;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.APIController;
import controllers.routes;


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
		study.description = JsonValidation.getString(json, "description");
		
		study.createdAt = new Date();
		study.createdBy = userId;
		study.owner = research;
		
		study.validationStatus = StudyValidationStatus.VALIDATED; //StudyValidationStatus.DRAFT;
		study.participantSearchStatus = ParticipantSearchStatus.PRE;
		study.executionStatus = StudyExecutionStatus.PRE;
				
		study.history = new ArrayList<History>();
		study.infos = new ArrayList<Info>();
		study.participantRules = new HashSet<FilterRule>();
		study.recordRules = new HashSet<FilterRule>();
		
		study.studyKeywords = new HashSet<ObjectId>();		
		
		Study.add(study);
		
		return ok(Json.toJson(study));
	}
		
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result list() throws JsonValidationException, ModelException {
	   ObjectId owner = new ObjectId(session().get("org"));
	   
	   Set<Study> studies = Study.getByOwner(owner, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus"));
	   
	   return ok(Json.toJson(studies));
	}
		
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result get(String id) throws JsonValidationException, ModelException {
       
	   ObjectId studyid = new ObjectId(id);
	   ObjectId owner = new ObjectId(session().get("org"));
	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordRules","studyKeywords"));
	   	   	   
	   return ok(Json.toJson(study));
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
	   
	   User user = ResearchUser.getById(userId, Sets.create("firstname","sirname"));
	   String userName = user.firstname + " " + user.sirname;
		
	   
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
	   if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
	   if (study.participantSearchStatus == ParticipantSearchStatus.CLOSED) return badRequest("Study participant search already closed.");
	 
	   Set<ParticipationCode> codes = ParticipationCode.getByStudy(studyid);
	   
	   return ok(Json.toJson(codes));
	}
	
	//@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result startParticipantSearch(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId owner = new ObjectId(session().get("org"));
		ObjectId studyid = new ObjectId(id);
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","sirname"));
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
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","sirname"));
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
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","sirname"));
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
	public static Result listParticipants(String id) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());
	   ObjectId owner = new ObjectId(session().get("org"));
	   ObjectId studyid = new ObjectId(id);
	   
	   Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
	   if (study == null) return badRequest("Study does not belong to organization.");
	   

	   Set<StudyParticipation> participants = StudyParticipation.getAllByStudy(studyid, Sets.create("member", "memberName", "group", "status"));
	   
	   return ok(Json.toJson(participants));
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result getParticipant(String studyidstr, String memberidstr) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());	
	   ObjectId owner = new ObjectId(session().get("org"));
	   ObjectId studyId = new ObjectId(studyidstr);
	   ObjectId memberId = new ObjectId(memberidstr);
	   	   
	   Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordRules","studyKeywords"));
	   if (study == null) return badRequest("Study does not belong to organization");
	   
	   StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, memberId, Sets.create("status", "group", "history"));
	   if (participation == null) return badRequest("Member does not participate in study");
	   if (participation.status == ParticipationStatus.CODE || 
		   participation.status == ParticipationStatus.MATCH || 
		   participation.status == ParticipationStatus.MEMBER_REJECTED) return badRequest("Member does not participate in study");
	   
	   Member member = Member.getById(memberId, Sets.create("firstname","sirname","address1","address2","city","zip","country","phone","mobile"));
	   if (member == null) return badRequest("Member does not exist");
	   	   
	   ObjectNode obj = Json.newObject();
	   obj.put("member", Json.toJson(member));
	   obj.put("participation", Json.toJson(participation));	   
	   
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
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","sirname"));		
		User member = Member.getById(memberId, Sets.create("firstname","sirname"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, memberId, Sets.create("status", "history"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");
		if (member == null) return badRequest("Member does not exist.");
		if (participation == null) return badRequest("Member is not allowed to participate in study.");		
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants anymore.");
		if (participation.status != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setStatus(ParticipationStatus.ACCEPTED);
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
		
		User user = ResearchUser.getById(userId, Sets.create("firstname","sirname"));		
		User member = Member.getById(memberId, Sets.create("firstname","sirname"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, memberId, Sets.create("status", "history"));		
		Study study = Study.getByIdFromOwner(studyId, owner, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");
		if (member == null) return badRequest("Member does not exist.");
		if (participation == null) return badRequest("Member is not allowed to participate in study.");		
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants anymore.");
		if (participation.status != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setStatus(ParticipationStatus.RESEARCH_REJECTED);
		participation.addHistory(new History(EventType.PARTICIPATION_REJECTED, user, comment));
						
		return ok();
	}
	
	
}
