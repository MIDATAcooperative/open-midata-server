package controllers.research;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import models.FilterRule;
import models.History;
import models.Info;
import models.ModelException;
import models.Study;
import models.enums.ParticipantSearchStatus;
import models.enums.StudyExecutionStatus;
import models.enums.StudyValidationStatus;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
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
		
		study.validationStatus = StudyValidationStatus.DRAFT;
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
	
}
