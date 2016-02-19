package controllers.members;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.APIController;
import controllers.Circles;

import models.HPUser;
import models.Member;
import models.MemberKey;
import models.User;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.UserRole;
import actions.APICall;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.Rights;
import utils.auth.MemberSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions about interaction with health providers
 *
 */
public class HealthProvider extends APIController {
		
	/**
	 * returns all consents of a member with health providers 
	 * @return list of consents (MemberKeys)
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result list() throws InternalServerException {
	      
		ObjectId userId = new ObjectId(request().username());	
		Set<MemberKey> memberkeys = MemberKey.getByOwner(userId);
		
		return ok(JsonOutput.toJson(memberkeys, "Consent", Sets.create("owner", "organization", "authorized", "status", "confirmDate", "aps", "comment", "name")));
	}
	
	/**
	 * search for health providers matching some criteria
	 * @return list of users
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result search() throws AppException, JsonValidationException {
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "fields");
		
		// get users
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		properties.put("role", UserRole.PROVIDER);
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		Rights.chk("HealthProvider.search", getRole(), properties, fields);

		if (fields.contains("name")) { fields.add("firstname"); fields.add("lastname"); } 
						
		List<HPUser> users = new ArrayList<HPUser>(HPUser.getAll(properties, fields));
		
		if (fields.contains("name")) {
			for (User user : users) user.name = (user.firstname + " "+ user.lastname).trim();
		}
				
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields));
	}
	
	/**
	 * accept a consent created by a health provider
	 * @return status ok
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result confirmConsent() throws AppException, JsonValidationException {
		
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "consent");
		
		ObjectId consentId = JsonValidation.getObjectId(json, "consent");
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Sets.create("status", "confirmDate"));
		if (target.status.equals(ConsentStatus.UNCONFIRMED)) {
			target.setConfirmDate(new Date());
			target.setStatus(ConsentStatus.ACTIVE);
			Circles.consentStatusChange(userId, target);
		} else return badRequest("Wrong status");
	
		return ok();
	}
	
	/**
	 * reject a consent created by a health provider
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result rejectConsent() throws AppException, JsonValidationException {
		
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "consent");
		
		ObjectId consentId = JsonValidation.getObjectId(json, "consent");
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Sets.create("status", "confirmDate"));
		if (target.status.equals(ConsentStatus.UNCONFIRMED)) {
			target.setConfirmDate(new Date());
			target.setStatus(ConsentStatus.REJECTED);
			Circles.consentStatusChange(userId, target);
		} else return badRequest("Wrong status");
	
		return ok();
	}
	
	

}
