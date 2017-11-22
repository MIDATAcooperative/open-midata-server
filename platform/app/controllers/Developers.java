package controllers;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Admin;
import models.Developer;
import models.Member;
import models.MidataId;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.DeveloperSecured;
import utils.auth.KeyManager;
import utils.auth.PasswordResetToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonValidation;

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
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language", "reason");
							
		String email = JsonValidation.getEMail(json, "email");
		if (Developer.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
				
		
		Developer user = new Developer(email);
		user._id = new MidataId();
		user.role = UserRole.DEVELOPER;
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
		
		user.reason = JsonValidation.getString(json, "reason");
		user.coach = JsonValidation.getString(json, "coach");
		
		user.password = Developer.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;	
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		Developer.add(user);
		
		//KeyManager.instance.unlock(user._id, null);
		RecordManager.instance.createPrivateAPS(user._id, user._id);
		
		Application.sendWelcomeMail(user, null);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
		
		Market.correctOwners();
		
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
		Developer user = Developer.getByEmail(email, Sets.create("firstname", "lastname", "email", "password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "email", "role", "subroles", "login", "registeredAt"));
		
		if (user == null) {
			Admin adminuser = Admin.getByEmail(email, Sets.create("firstname", "lastname", "email", "password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "email", "role", "subroles", "login", "registeredAt"));
			if (adminuser != null) {
				AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, adminuser);
				if (!Admin.authenticationValid(password, adminuser.password)) {
					throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
				}
						
				return Application.loginHelper(adminuser);
				
			}
		}
		
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user);
		if (!Developer.authenticationValid(password, user.password)) {
			throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		}
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED) || user.status.equals(UserStatus.WIPED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");
						
		return Application.loginHelper(user);
						
		// if (keytype == 0 && AccessPermissionSet.getById(user._id) == null) RecordManager.instance.createPrivateAPS(user._id, user._id);		
	}
	
	/**
	 * Creates a change password link for a test account
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(DeveloperSecured.class)
	public static Result resetTestAccountPassword() throws AppException {
		JsonNode json = request().body().asJson();
			
		MidataId developerId = new MidataId(request().username());
		MidataId targetUserId = JsonValidation.getMidataId(json, "user");
		User target = User.getById(targetUserId, Sets.create("developer", "role", "password"));		
		if (target == null || !target.developer.equals(developerId)) throw new BadRequestException("error.unknown.user", "No test user");
		
		PasswordResetToken token = new PasswordResetToken(target._id, target.role.toString().toLowerCase());
		target.set("resettoken", token.token);
		target.set("resettokenTs", System.currentTimeMillis());
		String encrypted = token.encrypt();
			   		
		String url = "/#/portal/setpw?token=" + encrypted;
						
		return ok(url);		
	}
}
