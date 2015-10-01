package controllers.admin;

import models.ModelException;
import models.User;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class Administration extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result changeStatus() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "user", "status");
				
		ObjectId executorId = new ObjectId(request().username());		
		ObjectId userId = JsonValidation.getObjectId(json, "user");
		UserStatus status = JsonValidation.getEnum(json, "status", UserStatus.class);
		
		User user = User.getById(userId, Sets.create("status"));
		if (user == null) return badRequest("Unknown user");
		
		user.status = status;
		User.set(user._id, "status", user.status);
		
		return ok();
	}
	
}
