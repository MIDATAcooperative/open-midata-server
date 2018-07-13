package controllers.research;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import models.MidataId;
import models.Research;
import models.ResearchUser;
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
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.ResearchSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonOutput;
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
		String email = JsonValidation.getEMail(json, "email");
		
		
		Research research = new Research();
		
		research._id = new MidataId();
		research.name = name;
		research.description = JsonValidation.getString(json, "description");
		
		ResearchUser user = new ResearchUser(email);
		
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
				
		user._id = new MidataId();
		Application.developerRegisteredAccountCheck(user, json);		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		register(user, research, null);
		return Application.loginHelper(user);		
	}
	
	/**
	 * register a new research
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result registerOther() throws AppException {
		
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "country", "language");
							
		String email = JsonValidation.getEMail(json, "email");
			
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		User executingUser = User.getById(executorId, User.ALL_USER);
		
	    ResearchUser user = new ResearchUser(email);
		
	    user._id = new MidataId();
		user.address1 = JsonValidation.getStringOrNull(json, "address1");
		user.address2 = JsonValidation.getStringOrNull(json, "address2");
		user.city = JsonValidation.getStringOrNull(json, "city");
		user.zip  = JsonValidation.getStringOrNull(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.language = JsonValidation.getString(json, "language");
		user.phone = JsonValidation.getStringOrNull(json, "phone");
		user.mobile = JsonValidation.getStringOrNull(json, "mobile");
		user.organization = PortalSessionToken.session().org;
		if (user.organization == null) throw new InternalServerException("error.internal", "No organization in session for register researcher!");
		user.status = UserStatus.ACTIVE;
						
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request().attrs().get(play.mvc.Security.USERNAME)), user);
		register(user ,null, executingUser);
			
		AuditManager.instance.success();
		return ok();		
	}
	
	
	public static void register(ResearchUser user, Research research, User executingUser) throws AppException {
					
		if (research != null && Research.existsByName(research.name)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A research organization with this name already exists.");			
		if (ResearchUser.existsByEMail(user.email)) throw new JsonValidationException("error.exists.user", "email", "exists", "A user with this email address already exists.");
				
		if (user._id == null) user._id = new MidataId();
		user.role = UserRole.RESEARCH;
		user.subroles = EnumSet.noneOf(SubUserRole.class);
		user.registeredAt = new Date();				
		if (user.status == null) user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;
		user.agbStatus = ContractStatus.REQUESTED;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
				
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		if (research != null) {
		  Research.add(research);
		  user.organization = research._id;
		}
		ResearchUser.add(user);
					
		RecordManager.instance.createPrivateAPS(user._id, user._id);		
		
		Application.sendWelcomeMail(user, executingUser);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
						
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
		ResearchUser user = ResearchUser.getByEmail(email, Sets.create(User.FOR_LOGIN, "organization"));
		
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user);
		
		if (!user.authenticationValid(password)) {
			throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		}
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED) || user.status.equals(UserStatus.WIPED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");
		
		return Application.loginHelper(user); 
				
	}
	

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getOrganization(String id) throws AppException {
			
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId researchid = MidataId.from(id);
						
		Research research = Research.getById(researchid, Research.ALL);
		if (research == null) return notFound();						
		return ok(JsonOutput.toJson(research, "Research", Research.ALL));		
	}

	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public static Result updateOrganization(String id) throws AppException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "_id", "name", "description");
					
		String name = JsonValidation.getString(json, "name");				
		String description = JsonValidation.getString(json, "description");
				
		MidataId researchid = PortalSessionToken.session().getOrg();
		
		if (!researchid.equals(JsonValidation.getMidataId(json, "_id"))) throw new InternalServerException("error.internal", "Tried to change other research organization!");
		
		if (Research.existsByName(name, researchid)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A research organization with this name already exists.");			
		
		Research research = Research.getById(researchid, Research.ALL);

		if (research == null) throw new InternalServerException("error.internal", "Research organization not found.");
		
		research.name = name;
		research.description = description;
		research.set("name", research.name);
		research.set("description", research.description);						
		
		return ok();		
	}
		
}
