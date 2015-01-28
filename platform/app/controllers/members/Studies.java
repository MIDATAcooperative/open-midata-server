package controllers.members;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import play.mvc.Result;
import play.mvc.Security;

import models.History;
import models.Member;
import models.ModelException;
import models.ParticipationCode;
import models.ResearchUser;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;
import play.libs.Json;
import controllers.APIController;
import controllers.Secured;
import com.fasterxml.jackson.databind.JsonNode;

public class Studies extends APIController {

	@APICall
	@Security.Authenticated(Secured.class)
	public static Result list() throws JsonValidationException, ModelException {
	   ObjectId user = new ObjectId(request().username());
	   
	   Set<StudyParticipation> participation = StudyParticipation.getAllByMember(user, Sets.create("study","studyName", "status","history"));
	   
	   return ok(Json.toJson(participation));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result enterCode() throws JsonValidationException, ModelException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "code");
		String codestr = JsonValidation.getString(json, "code");
		ObjectId userId = new ObjectId(request().username());
		
		User user = Member.getById(userId, Sets.create("firstname","sirname"));
		String userName = user.firstname + " " + user.sirname;
		
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
		part.study = code.study;
		part.studyName = study.name;
		part.member = userId;
		part.memberName = userName;
		part.group = code.group;
		part.recruiter = code.recruiter;		
		part.status = ParticipationStatus.REQUEST;
		
		part.history = new ArrayList<History>();
		part.shared = new HashSet<ObjectId>();
		StudyParticipation.add(part);
		
		if (code.status != ParticipationCodeStatus.REUSEABLE) {
		   code.setStatus(ParticipationCodeStatus.USED);
		}
		
		return ok();
	}
		
}
