package controllers.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import models.Admin;
import models.Developer;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;

import controllers.APIController;
import controllers.Application;
import controllers.Market;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.RecordManager;
import utils.auth.AdminSecured;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
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
		
		User user = User.getById(userId, Sets.create("status", "contractStatus", "agbStatus", "subroles"));
		if (user == null) throw new BadRequestException("error.unknown.user", "Unknown user");
		
		user.status = status;
		User.set(user._id, "status", user.status);
		
		if (json.has("contractStatus")) {
			user.contractStatus = JsonValidation.getEnum(json, "contractStatus", ContractStatus.class);			
			User.set(user._id, "contractStatus", user.contractStatus);
		}
		
		if (json.has("agbStatus")) {
			user.agbStatus = JsonValidation.getEnum(json, "agbStatus", ContractStatus.class);			
			User.set(user._id, "agbStatus", user.agbStatus);
		}
		
		if ()
		
		return ok();
	}
	
	/**
	 * register a new administrator
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result register() throws AppException {
		requireSubUserRole(SubUserRole.SUPERADMIN);
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language", "subroles");
							
		String email = JsonValidation.getEMail(json, "email");
		if (Admin.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
				
		
		Admin user = new Admin(email);
		user._id = new ObjectId();
		user.role = UserRole.ADMIN;		
		user.subroles = JsonValidation.getEnumSet(json, "subroles", SubUserRole.class);
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.language = JsonValidation.getString(json, "language");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		
		user.password = Admin.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;	
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		Admin.add(user);
					
		Application.sendWelcomeMail(user);
				
		return ok();		
	}
	
}
