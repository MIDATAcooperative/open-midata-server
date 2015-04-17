package controllers.members;

import java.util.Date;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Secured;

import models.MemberKey;
import models.ModelException;
import models.enums.MemberKeyStatus;
import actions.APICall;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class HealthProvider extends Controller {
		
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result list() throws ModelException {
	      
		ObjectId userId = new ObjectId(request().username());	
		Set<MemberKey> memberkeys = MemberKey.getByMember(userId);
		
		return ok(Json.toJson(memberkeys));
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result confirmMemberKey() throws ModelException, JsonValidationException {
		
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "provider");
		
		ObjectId providerId = JsonValidation.getObjectId(json, "provider");
		MemberKey target = MemberKey.getByMemberAndProvider(userId, providerId);
		if (target.status.equals(MemberKeyStatus.UNCONFIRMED)) {
			target.setConfirmDate(new Date());
			target.setStatus(MemberKeyStatus.CONFIRMED);
		} else return badRequest("Wrong status");
	
		return ok();
	}
	
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result rejectMemberKey() throws ModelException, JsonValidationException {
		
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "provider");
		
		ObjectId providerId = JsonValidation.getObjectId(json, "provider");
		MemberKey target = MemberKey.getByMemberAndProvider(userId, providerId);
		if (target.status.equals(MemberKeyStatus.UNCONFIRMED)) {
			target.setConfirmDate(new Date());
			target.setStatus(MemberKeyStatus.REJECTED);
		} else return badRequest("Wrong status");
	
		return ok();
	}
	
	

}
