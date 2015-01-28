package controllers.research;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import models.FilterRule;
import models.History;
import models.Info;
import models.ModelException;
import models.ParticipationCode;
import models.ResearchUser;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
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
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

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
		
		return ok();
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
		   
		Study study = Study.getByIdFromOwner(studyid, owner, Sets.create("owner","executionStatus", "participantSearchStatus","validationStatus", "history"));
		if (study == null) return badRequest("Study does not belong to organization.");
		if (study.validationStatus != StudyValidationStatus.VALIDATED) return badRequest("Study must be validated before.");
		if (study.participantSearchStatus != ParticipantSearchStatus.PRE) return badRequest("Study participant search already started.");
		
		study.setParticipantSearchStatus(ParticipantSearchStatus.SEARCHING);
		
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
	   

	   Set<StudyParticipation> participants = StudyParticipation.getAllByStudy(studyid, Sets.create("memberName", "group", "status"));
	   
	   return ok(Json.toJson(participants));
	}
}
