package controllers.admin;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import models.AccessPermissionSet;
import models.Admin;
import models.Circle;
import models.Consent;
import models.HealthcareProvider;
import models.History;
import models.MidataId;
import models.Research;
import models.Space;
import models.Study;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.EventType;
import models.enums.Gender;
import models.enums.MessageReason;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.auth.AdminSecured;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;

/**
 * functions for user administration. May only be used by the MIDATA admin.
 *
 */
public class Administration extends APIController {

	/**
	 * change status of target user
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result changeStatus() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "user", "status");
				
		MidataId executorId = new MidataId(request().username());		
		MidataId userId = JsonValidation.getMidataId(json, "user");
		UserStatus status = JsonValidation.getEnum(json, "status", UserStatus.class);
		
		User user = User.getById(userId, Sets.create("status", "contractStatus", "agbStatus", "subroles", "confirmedAt", "emailStatus", "history"));
		
		User admin = User.getById(executorId, Sets.create("firstname", "lastname", "role"));
		
		if (user == null) throw new BadRequestException("error.unknown.user", "Unknown user");
		
		UserStatus oldstatus = user.status;
		user.status = status;
		User.set(user._id, "status", user.status);
		
		if (user.status.equals(UserStatus.ACTIVE) && !oldstatus.equals(UserStatus.ACTIVE)) {			
			Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.ACCOUNT_UNLOCK, null, Collections.singleton(user._id), null, new HashMap<String, String>());			
		}
		
		if (user.status != oldstatus && user.status == UserStatus.DELETED) {
			User.set(user._id, "searchable", false);
			user.addHistory(new History(EventType.ACCOUNT_DELETED, admin, null));
		}
		
		if (json.has("contractStatus")) {
			ContractStatus old = user.contractStatus;
			user.contractStatus = JsonValidation.getEnum(json, "contractStatus", ContractStatus.class);			
			User.set(user._id, "contractStatus", user.contractStatus);
			if (old == user.contractStatus) {
			} else if (user.contractStatus == ContractStatus.PRINTED) {
			  user.addHistory(new History(EventType.CONTRACT_SEND, admin, "Midata contract"));				
			
			} else {
			  user.addHistory(new History(EventType.ADMIN_ACCOUNT_CHANGE, admin, "contract status "+old.toString()+" to "+user.contractStatus.toString()));
			}
		}
		
		if (json.has("agbStatus")) {
			ContractStatus old = user.agbStatus;
			user.agbStatus = JsonValidation.getEnum(json, "agbStatus", ContractStatus.class);			
			User.set(user._id, "agbStatus", user.agbStatus);
			if (old == user.agbStatus) {
			} else if (user.agbStatus == ContractStatus.PRINTED) {
			   user.addHistory(new History(EventType.CONTRACT_SEND, admin, "AGB"));							
			} else {
			   user.addHistory(new History(EventType.ADMIN_ACCOUNT_CHANGE, admin, "agb status "+old.toString()+" to "+user.agbStatus.toString()));
			}
		}
		
		if (json.has("emailStatus")) {
			EMailStatus old = user.emailStatus;
			user.emailStatus = JsonValidation.getEnum(json, "emailStatus", EMailStatus.class);
			User.set(user._id, "emailStatus", user.emailStatus);
			if (old != user.emailStatus) {
			  user.addHistory(new History(EventType.ADMIN_ACCOUNT_CHANGE, admin, "email status "+old.toString()+" to "+user.emailStatus.toString()));
			}
		}
		
		Application.checkAccount(user);
		
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
		user._id = new MidataId();
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
		
		user.apps = new HashSet<MidataId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<MidataId>();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		Admin.add(user);
					
		Application.sendWelcomeMail(user);
				
		return ok();		
	}
	
	/**
	 * add a comment for a user
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result addComment() throws AppException {
		requireSubUserRole(SubUserRole.USERADMIN);
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "user", "comment");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");
		String comment = JsonValidation.getString(json, "comment");
		
		MidataId executorId = new MidataId(request().username());				
		User admin = User.getById(executorId, Sets.create("firstname", "lastname", "role"));
	
		
		User targetUser = User.getById(userId, Sets.create("history"));		
		targetUser.addHistory(new History(EventType.INTERNAL_COMMENT, admin, comment));
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result adminWipeAccount() throws JsonValidationException, AppException {
		requireSubUserRole(SubUserRole.USERADMIN);
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "user");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");
		
		User selected = User.getById(userId, Sets.create("status"));
		if (!selected.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.invalid.status",  "User must have status deleted to be wiped.");
		
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			AccessPermissionSet.delete(space._id);			
			Space.delete(userId, space._id);
		}
		
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map(), Consent.ALL);
		for (Consent consent : consents) {			
			AccessPermissionSet.delete(consent._id);
			Circle.delete(userId, consent._id);
		}
		
		Set<Consent> consents2 = Consent.getAllByAuthorized(userId);
		for (Consent consent : consents2) {
			consent = Consent.getByIdAndAuthorized(consent._id, userId, Sets.create("authorized"));
			consent.authorized.remove(userId);
			Consent.set(consent._id, "authorized", consent.authorized);			
		}
		
		
		
		if (getRole().equals(UserRole.RESEARCH)) {
			Set<Study> studies = Study.getByOwner(PortalSessionToken.session().org, Sets.create("_id"));
			
			for (Study study : studies) {
				controllers.research.Studies.deleteStudy(userId, study._id, true);
			}
			
			Research.delete(PortalSessionToken.session().org);			
			
		}
		
		if (getRole().equals(UserRole.PROVIDER)) {
			HealthcareProvider.delete(PortalSessionToken.session().org);
		}
		
		KeyManager.instance.deleteKey(userId);
		User.delete(userId);
		return ok();
	}
}
