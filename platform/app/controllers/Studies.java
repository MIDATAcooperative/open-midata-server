package controllers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import models.History;
import models.Member;
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

import models.MidataId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

import utils.auth.AnyRoleSecured;
import utils.auth.Rights;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * functions about studies.
 *
 */
public class Studies extends APIController {
	
	/**
	 * search for studies matching some criteria
	 * @return list of studies
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result search() throws JsonValidationException, InternalServerException, AuthException {
	   MidataId user = new MidataId(request().username());	   
	   JsonNode json = request().body().asJson();
	   JsonValidation.validate(json, "properties", "fields");
							   		
	   Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
	   Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
	   
	   Rights.chk("Studies.search", getRole(), properties, fields);	   	   
	   Set<Study> studies = Study.getAll(user, properties, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields));
	}	
			
}
