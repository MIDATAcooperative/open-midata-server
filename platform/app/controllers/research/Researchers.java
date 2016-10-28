package controllers.research;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import models.MidataId;
import models.Research;
import models.ResearchUser;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * login and registration functions for researchers
 *
 */
public class Researchers extends APIController {

	/**
	 * register a new researcher
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws AppException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "description", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language");
					
		String name = JsonValidation.getString(json, "name");
		if (Research.existsByName(name)) return inputerror("name", "exists", "A research organization with this name already exists.");
		
		String email = JsonValidation.getEMail(json, "email");
		if (ResearchUser.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
		
		Research research = new Research();
		
		research._id = new MidataId();
		research.name = name;
		research.description = JsonValidation.getString(json, "description");
		
		ResearchUser user = new ResearchUser(email);
		user._id = new MidataId();
		user.role = UserRole.RESEARCH;
		user.subroles = EnumSet.noneOf(SubUserRole.class);
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
		
		user.password = ResearchUser.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
		user.apps = new HashSet<MidataId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<MidataId>();
		
		Application.developerRegisteredAccountCheck(user, json);
		
		Research.add(research);
		user.organization = research._id;
		ResearchUser.add(user);
		
		KeyManager.instance.unlock(user._id, null);
		
		RecordManager.instance.createPrivateAPS(user._id, user._id);		
		
		Application.sendWelcomeMail(user);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
		
		return Application.loginHelper(user);		
	}
	
	/**
	 * login a researcher
	 * @return status ok
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
		ResearchUser user = ResearchUser.getByEmail(email, Sets.create("password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "email", "organization", "role", "subroles", "login", "registeredAt", "developer"));
		
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		if (!ResearchUser.authenticationValid(password, user.password)) {
			throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		}
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");
		
		return Application.loginHelper(user); 
				
	}
		
}
