package controllers.members;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

import models.History;
import models.Member;
import models.ModelException;
import models.ParticipationCode;
import models.Record;
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
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;
import play.libs.Json;
import controllers.APIController;
import controllers.AnyRoleSecured;
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
	   
	   Set<StudyParticipation> participation = StudyParticipation.getAllByMember(user, Sets.create("study","studyName", "pstatus"));
	   
	   return ok(Json.toJson(participation));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result search() throws JsonValidationException, ModelException {
	   ObjectId user = new ObjectId(request().username());
	   
	   JsonNode json = request().body().asJson();
	   JsonValidation.validate(json, "properties", "fields");
							   		
	   Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
	   Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
	   	   
	   Set<Study> studies = Study.getAll(user, properties, fields);
	   
	   return ok(Json.toJson(studies));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result enterCode() throws JsonValidationException, ModelException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "code");
		String codestr = JsonValidation.getString(json, "code");
		ObjectId userId = new ObjectId(request().username());
		
		Member user = Member.getById(userId, Sets.create("firstname","lastname","birthday","country","gender"));
				
		ParticipationCode code = ParticipationCode.getByCode(codestr);
		if (code == null) return inputerror("code", "notfound", "Unknown Participation Code.");
		
		StudyParticipation existing = StudyParticipation.getByStudyAndMember(code.study, userId, Sets.create("pstatus"));
		if (existing != null) {
			// Redirect to study page
			ObjectNode result = Json.newObject();
			result.put("study", code.study.toString());
			return ok(result);
		}
				
		if (code.status != ParticipationCodeStatus.UNUSED && 
			code.status != ParticipationCodeStatus.SHARED && 
			code.status != ParticipationCodeStatus.REUSEABLE) return inputerror("code","alreadyused","Participation code has expired.");
		
		Study study = Study.getByIdFromMember(code.study, Sets.create("name", "participantSearchStatus", "owner", "createdBy"));
				
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return inputerror("code", "notsearching", "Study is not searching for participants.");
		
		StudyParticipation part = createStudyParticipation(study, user, code);
				
		if (code.status != ParticipationCodeStatus.REUSEABLE) {
		   code.setStatus(ParticipationCodeStatus.USED);
		}
		
		ObjectNode result = Json.newObject();
		result.put("study", code.study.toString());
		return ok(result);
	}
	
	public static StudyParticipation createStudyParticipation(Study study, Member member, ParticipationCode code) throws ModelException {
		StudyParticipation part = new StudyParticipation();
		part._id = new ObjectId();
		part.study = study._id;
		part.studyName = study.name;
		part.owner = member._id;
		
		String userName;
		
		if (study.requiredInformation == InformationType.DEMOGRAPHIC) {
			userName = member.lastname+", "+member.firstname;	
		} else {
			userName = "Part. " + CodeGenerator.nextUniqueCode();
		}
				
		part.ownerName = userName;
		if (code != null) {
			part.group = code.group;
			part.recruiter = code.recruiter;		
			part.recruiterName = code.recruiterName;
			part.pstatus = ParticipationStatus.CODE;
		} else part.pstatus = ParticipationStatus.MATCH;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(member.birthday);
		part.yearOfBirth = cal.get(Calendar.YEAR);
		part.gender = member.gender;
		part.country = member.country;
		
		part.history = new ArrayList<History>();
		part.aps = RecordSharing.instance.createAnonymizedAPS(member._id, study.createdBy, part._id);
		
		if (code != null) {
		  History codedentererd = new History(EventType.CODE_ENTERED, part, null); 
		  part.history.add(codedentererd);
		} 
		StudyParticipation.add(part);
		
		return part;
		
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get(String id) throws JsonValidationException, ModelException {
	   ObjectId userId = new ObjectId(request().username());	
	   ObjectId studyId = new ObjectId(id);
	   	   
	   Study study = Study.getByIdFromMember(studyId, Sets.create("createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordRules","studyKeywords","requiredInformation"));
	   StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("pstatus", "history"));
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
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "birthday", "gender", "country"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "history", "memberName"));		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history", "owner", "createdBy", "name"));
		
		if (study == null) return badRequest("Study does not exist.");
		if (participation == null) {
			if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return inputerror("code", "notsearching", "Study is not searching for participants.");
			
			participation = createStudyParticipation(study, user, null);
						
			//return badRequest("Member is not allowed to participate in study.");		
		}
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return badRequest("Study is not searching for participants anymore.");
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH) return badRequest("Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.REQUEST);
		participation.addHistory(new History(EventType.PARTICIPATION_REQUESTED, participation, null));
						
		return ok();
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result noParticipation(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());		
		ObjectId studyId = new ObjectId(id);
		
		User user = Member.getById(userId, Sets.create("firstname","lastname"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "history", "memberName"));		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) return badRequest("Study does not exist.");
		if (participation == null) return badRequest("Member is not allowed to participate in study.");				
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH && participation.pstatus != ParticipationStatus.REQUEST) return badRequest("Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.MEMBER_REJECTED);
		participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, null));
						
		return ok();
	}
		
}
