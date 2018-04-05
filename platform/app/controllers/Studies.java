package controllers;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.MidataId;
import models.Study;
import models.enums.UserRole;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.auth.Rights;
import utils.db.ObjectIdConversion;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;


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
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result search() throws JsonValidationException, InternalServerException, AuthException {
	   //MidataId user = new MidataId(request().username());	   
	   JsonNode json = request().body().asJson();
	   JsonValidation.validate(json, "properties", "fields");
							   		
	   Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
	   ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "createdBy", "studyKeywords");
	   Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
	   
	   Rights.chk("Studies.search", UserRole.MEMBER, properties, fields);	   	   
	   Set<Study> studies = Study.getAll(null, properties, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields)).as("application/json");
	}	
			
}
