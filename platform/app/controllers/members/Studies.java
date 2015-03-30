package controllers.members;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import play.mvc.Result;
import play.mvc.Security;

import models.History;
import models.Member;
import models.ModelException;
import models.ParticipationCode;
import models.Research;
import models.ResearchUser;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.EventType;
import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.StudyValidationStatus;
import utils.auth.CodeGenerator;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;
import play.libs.Json;
import controllers.APIController;
import controllers.RecordSharing;
import controllers.Secured;
import controllers.research.ResearchSecured;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Studies extends APIController {

	@APICall
	@Security.Authenticated(Secured.class)
	public static Result list() throws JsonValidationException, ModelException {
	   ObjectId user = new ObjectId(request().username());
	   
	   Set<StudyParticipation> participation = StudyParticipation.getAllByMember(user, Sets.create("study","studyName", "status"));
	   
	   return ok(Json.toJson(participation));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result enterCode() throws JsonValidationException, ModelException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "code");
		String codestr = JsonValidation.getString(json, "code");
		ObjectId userId = new ObjectId(request().username());
		
		Member user = Member.getById(userId, Sets.create("firstname","sirname","birthday","country","gender"));
				
		ParticipationCode code = ParticipationCode.getByCode(codestr);
		if (code == null) return inputerror("code", "notfound", "Unknown Participation Code.");
		
		StudyParticipation existing = StudyParticipation.getByStudyAndMember(code.study, userId, Sets.create("status"));
		if (existing != null) {
			// Redirect to study page
		}
				
		if (code.status != ParticipationCodeStatus.UNUSED && 
			code.status != ParticipationCodeStatus.SHARED && 
			code.status != ParticipationCodeStatus.REUSEABLE) return inputerror("code","alreadyused","Participation code has expired.");
		
		Study study = Study.getByIdFromMember(code.study, Sets.create("name", "participantSearchStatus"));
				
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return inputerror("code", "notsearching", "Study is not searching for participants.");
		
		StudyParticipation part = new StudyParticipation();
		part._id = new ObjectId();
		part.study = code.study;
		part.studyName = study.name;
		part.member = userId;
		
		String userName;
		
		if (study.requiredInformation == InformationType.DEMOGRAPHIC) {
			userName = user.sirname+", "+user.firstname;	
		} else {
			userName = "Part. " + CodeGenerator.nextUniqueCode();
		}
				
		part.memberName = userName;
		part.group = code.group;
		part.recruiter = code.recruiter;		
		part.recruiterName = code.recruiterName;
		part.status = ParticipationStatus.CODE;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(user.birthday);
		part.yearOfBirth = cal.get(Calendar.YEAR);
		part.gender = user.gender;
		part.country = user.country;
		
		part.history = new ArrayList<History>();
		part.aps = RecordSharing.instance.createAnonymizedAPS(userId, study.owner, part._id);
		
		History codedentererd = new History(EventType.CODE_ENTERED, part, null); 
		part.history.add(codedentererd);
		StudyParticipation.add(part);
		
		if (code.status != ParticipationCodeStatus.REUSEABLE) {
		   code.setStatus(ParticipationCodeStatus.USED);
		}
		
		ObjectNode result = Json.newObject();
		result.put("study", code.study.toString());
		return ok(result);
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result get(String id) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());	
	   ObjectId studyId = new ObjectId(id);
	   	   
	   Study study = Study.getByIdFromMember(studyId, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordRules","studyKeywords","requiredInformation"));
	   StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "history"));
	   Research research = Research.getById(study.owner, Sets.create("name", "description"));
	   
	   ObjectNode obj = Json.newObject();
	   obj.put("study", Json.toJson(study));
	   obj.put("participation", Json.toJson(participation));
	   obj.put("research", Json.toJson(research));
	   
	   return ok(obj);
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result requestParticipation(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());		
		ObjectId studyId = new ObjectId(id);
		
		User user = Member.getById(userId, Sets.create("firstname","sirname"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "history", "memberName"));		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");
		if (participation == null) return badRequest("Member is not allowed to participate in study.");		
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants anymore.");
		if (participation.status != ParticipationStatus.CODE && participation.status != ParticipationStatus.MATCH) return badRequest("Wrong participation status.");
		
		participation.setStatus(ParticipationStatus.REQUEST);
		participation.addHistory(new History(EventType.PARTICIPATION_REQUESTED, participation, null));
						
		return ok();
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result noParticipation(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());		
		ObjectId studyId = new ObjectId(id);
		
		User user = Member.getById(userId, Sets.create("firstname","sirname"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "history", "memberName"));		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");
		if (participation == null) return badRequest("Member is not allowed to participate in study.");				
		if (participation.status != ParticipationStatus.CODE && participation.status != ParticipationStatus.MATCH && participation.status != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setStatus(ParticipationStatus.MEMBER_REJECTED);
		participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, null));
						
		return ok();
	}
		
}
