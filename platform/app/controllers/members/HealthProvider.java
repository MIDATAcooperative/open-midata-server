package controllers.members;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import controllers.APIController;
import controllers.Circles;
import controllers.MobileAPI;
import models.HPUser;
import models.MemberKey;
import models.MidataId;
import models.User;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.UserRole;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.auth.MemberSecured;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
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
	      
		MidataId userId = new MidataId(request().username());	
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
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result search() throws AppException, JsonValidationException {
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "fields");
		
		// get users
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "provider");		
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		Rights.chk("HealthProvider.search", getRole(), properties, fields);

		if (fields.contains("name")) { fields.add("firstname"); fields.add("lastname"); } 
						
		if (properties.containsKey("name") || properties.containsKey("city")) {
			String name = properties.containsKey("name") ? properties.get("name").toString() : null;
			String city = properties.containsKey("city") ? properties.get("city").toString() : null;
			properties.remove("name");
			properties.remove("city");
			if (name != null && city != null) {
			properties = CMaps.and(CMaps.or(CMaps.map("firstname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE)), CMaps.map("lastname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE))),
					               CMaps.or(CMaps.map("city", Pattern.compile("^"+city+"$", Pattern.CASE_INSENSITIVE)), CMaps.map("zip", Pattern.compile("^"+city+"$", Pattern.CASE_INSENSITIVE))),
					               CMaps.map("keywordsLC", name.toLowerCase())).map(properties);
			} else if (name != null) {
				properties = CMaps.and(
						        CMaps.or(CMaps.map("firstname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE)), CMaps.map("lastname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE))),
						        CMaps.map("keywordsLC", name.toLowerCase())		
						      ).map(properties);
			} else {
				properties = CMaps.or(
						       CMaps.and(
						    		 CMaps.map("keywordsLC", city.toLowerCase()), 
						             CMaps.map("city", Pattern.compile("^"+city+"$", Pattern.CASE_INSENSITIVE))
						       ), CMaps.map("zip", city)).map(properties);
			}
		}
		properties.put("role", UserRole.PROVIDER);
		
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
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result confirmConsent() throws AppException, JsonValidationException {
		
		MidataId userId = new MidataId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "consent");
		
		MidataId consentId = JsonValidation.getMidataId(json, "consent");		
		confirmConsent(userId, consentId);				
		return ok();
	}
	
    public static void confirmConsent(MidataId userId, MidataId consentId) throws AppException, JsonValidationException {
										
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Sets.create("status", "owner", "authorized", "confirmDate", "type"));
		if (target.status.equals(ConsentStatus.UNCONFIRMED)) {
			if (target.type.equals(ConsentType.EXTERNALSERVICE)) {
				MobileAPI.confirmMobileConsent(userId, consentId);
			}			
			target.setConfirmDate(new Date());
			target.setStatus(ConsentStatus.ACTIVE);
			Circles.consentStatusChange(userId, target);
		} else throw new BadRequestException("error.invalid.status_transition", "Wrong status");			
	}
	
	
	/**
	 * reject a consent created by a health provider
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result rejectConsent() throws AppException, JsonValidationException {
		
		MidataId userId = new MidataId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "consent");
		
		MidataId consentId = JsonValidation.getMidataId(json, "consent");
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Sets.create("owner", "authorized", "status", "confirmDate"));
		if (target.status.equals(ConsentStatus.UNCONFIRMED) || target.status.equals(ConsentStatus.ACTIVE)) {
			target.setConfirmDate(new Date());
			target.setStatus(ConsentStatus.REJECTED);
			Circles.consentStatusChange(userId, target);
		} else throw new BadRequestException("error.invalid.status_transition", "Wrong status");
	
		return ok();
	}
	
	

}
