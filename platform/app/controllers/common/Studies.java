package controllers.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import models.History;
import models.Member;
import models.ModelException;
import models.ParticipationCode;
import models.Research;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.EventType;
import models.enums.InformationType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.APIController;
import controllers.AnyRoleSecured;

public class Studies extends APIController {
	
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
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
			
}