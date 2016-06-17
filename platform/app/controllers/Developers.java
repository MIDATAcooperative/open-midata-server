package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import models.AccessPermissionSet;
import models.Admin;
import models.Developer;

import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import utils.access.RecordManager;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.evolution.AccountPatches;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonValidation;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * login and registration for developers
 *
 */
public class Developers extends APIController {

	/**
	 * register a new developer
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws AppException {
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language");
							
		String email = JsonValidation.getEMail(json, "email");
		if (Developer.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
				
		
		Developer user = new Developer(email);
		user._id = new ObjectId();
		user.role = UserRole.DEVELOPER;				
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
		
		user.password = Developer.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		Developer.add(user);
		
		KeyManager.instance.unlock(user._id, null);
		RecordManager.instance.createPrivateAPS(user._id, user._id);
		
		Application.sendWelcomeMail(user);
		
		return Application.loginHelper(user);		
	}
	
	/**
	 * login a developer or admin
	 * @return status ok / returns "admin" if person logged in is an admin
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result login() throws AppException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
		String email = JsonValidation.getString(json, "email");
		String password = JsonValidation.getString(json, "password");
		Developer user = Developer.getByEmail(email, Sets.create("password", "status", "contractStatus", "emailStatus", "confirmationCode", "accountVersion", "email", "role"));
		
		if (user == null) {
			Admin adminuser = Admin.getByEmail(email, Sets.create("password", "status", "contractStatus", "emailStatus", "confirmationCode", "accountVersion", "email", "role"));
			if (adminuser != null) {
				if (!Admin.authenticationValid(password, adminuser.password)) {
					throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
				}
						
				return Application.loginHelper(adminuser);
				
			}
		}
		
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		if (!Developer.authenticationValid(password, user.password)) {
			throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		}
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");
						
		return Application.loginHelper(user);
						
		// if (keytype == 0 && AccessPermissionSet.getById(user._id) == null) RecordManager.instance.createPrivateAPS(user._id, user._id);		
	}
}
