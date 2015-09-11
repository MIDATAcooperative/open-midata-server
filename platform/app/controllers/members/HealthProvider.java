package controllers.members;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Secured;

import models.HPUser;
import models.Member;
import models.MemberKey;
import models.ModelException;
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
import utils.collections.Sets;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class HealthProvider extends Controller {
		
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result list() throws ModelException {
	      
		ObjectId userId = new ObjectId(request().username());	
		Set<MemberKey> memberkeys = MemberKey.getByOwner(userId);
		
		return ok(Json.toJson(memberkeys));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result search() throws ModelException {
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get users
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		properties.put("role", UserRole.PROVIDER);
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));

		if (fields.contains("name")) { fields.add("firstname"); fields.add("sirname"); } 
						
		List<HPUser> users = new ArrayList<HPUser>(HPUser.getAll(properties, fields));
		
		if (fields.contains("name")) {
			for (User user : users) user.name = (user.firstname + " "+ user.sirname).trim();
		}
				
		Collections.sort(users);
		return ok(Json.toJson(users));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result confirmConsent() throws ModelException, JsonValidationException {
		
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "consent");
		
		ObjectId consentId = JsonValidation.getObjectId(json, "consent");
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Sets.create("status", "confirmDate"));
		if (target.status.equals(ConsentStatus.UNCONFIRMED)) {
			target.setConfirmDate(new Date());
			target.setStatus(ConsentStatus.ACTIVE);
		} else return badRequest("Wrong status");
	
		return ok();
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result rejectConsent() throws ModelException, JsonValidationException {
		
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "consent");
		
		ObjectId consentId = JsonValidation.getObjectId(json, "consent");
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Sets.create("status", "confirmDate"));
		if (target.status.equals(ConsentStatus.UNCONFIRMED)) {
			target.setConfirmDate(new Date());
			target.setStatus(ConsentStatus.REJECTED);
		} else return badRequest("Wrong status");
	
		return ok();
	}
	
	

}
