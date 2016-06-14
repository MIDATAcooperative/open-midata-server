package controllers.admin;

import models.User;
import models.enums.ContractStatus;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;

import controllers.APIController;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AdminSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for user administration. May only be used by the MIDATA admin.
 *
 */
public class Administration extends APIController {

	/**
	 * change status of target user
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result changeStatus() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "user", "status");
				
		ObjectId executorId = new ObjectId(request().username());		
		ObjectId userId = JsonValidation.getObjectId(json, "user");
		UserStatus status = JsonValidation.getEnum(json, "status", UserStatus.class);
		
		User user = User.getById(userId, Sets.create("status", "contractStatus"));
		if (user == null) throw new BadRequestException("error.unknown.user", "Unknown user");
		
		user.status = status;
		User.set(user._id, "status", user.status);
		
		if (json.has("contractStatus")) {
			user.contractStatus = JsonValidation.getEnum(json, "contractStatus", ContractStatus.class);			
			User.set(user._id, "contractStatus", user.contractStatus);
		}
		
		return ok();
	}
	
}
