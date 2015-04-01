package controllers.members;

import java.util.Set;

import org.bson.types.ObjectId;

import controllers.Secured;

import models.MemberKey;
import models.ModelException;
import actions.APICall;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.json.JsonValidation.JsonValidationException;

public class HealthProvider extends Controller {
		
	@APICall
	@Security.Authenticated(Secured.class)
	public static Result list() throws ModelException {
	      
		ObjectId userId = new ObjectId(request().username());	
		Set<MemberKey> memberkeys = MemberKey.getByMember(userId);
		
		return ok(Json.toJson(memberkeys));
	}

}
