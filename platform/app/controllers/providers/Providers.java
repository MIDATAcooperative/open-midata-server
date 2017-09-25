package controllers.providers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import controllers.Users;
import models.HCRelated;
import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
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
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.ProviderSecured;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for healthcare providers
 *
 */
public class Providers extends APIController {

	/**
	 * register a new healthcare provider
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws AppException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language");
					
		String name = JsonValidation.getString(json, "name");
		if (HealthcareProvider.existsByName(name)) return inputerror("name", "exists", "A healthcare provider with this name already exists.");
		
		String email = JsonValidation.getEMail(json, "email");
		if (HPUser.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
		
		HealthcareProvider provider = new HealthcareProvider();
		
		provider._id = new MidataId();
		provider.name = name;
		//research.description = JsonValidation.getString(json, "description");
		
		HPUser user = new HPUser(email);
		user._id = new MidataId();
		user.role = UserRole.PROVIDER;		
		user.subroles.add(SubUserRole.MANAGER);
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
		
		user.password = HPUser.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.REQUESTED;
		user.agbStatus = ContractStatus.REQUESTED;	
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
		
		Application.developerRegisteredAccountCheck(user, json);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		HealthcareProvider.add(provider);
		user.provider = provider._id;
		HPUser.add(user);
		
		//KeyManager.instance.unlock(user._id, null);
		
		RecordManager.instance.createPrivateAPS(user._id, user._id);		
		
		Application.sendWelcomeMail(user);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
		
		return Application.loginHelper(user);		
	}
	
	/**
	 * healthcare provider login
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
		HPUser user = HPUser.getByEmail(email, Sets.create("firstname", "lastname", "email", "password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "provider", "role", "subroles", "login", "registeredAt", "developer", "keywordsLC"));
		
		if (user == null) throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user);
		if (!HPUser.authenticationValid(password, user.password)) {
			throw new BadRequestException("error.invalid.credentials", "Invalid user or password.");
		}
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");

		if (user.keywordsLC == null || user.keywordsLC.isEmpty()) {
			User user2 = User.getById(user._id, User.ALL_USER);
			user2.updateKeywords(true);
		}
		
		return Application.loginHelper(user);					
	}
	
	/**
	 * healthcare provider search for MIDATA members by MIDATAID and birthday.
	 * @return Member and list of consents
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@Security.Authenticated(ProviderSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result search() throws JsonValidationException, InternalServerException {
		MidataId userId = new MidataId(request().username());
		JsonNode json = request().body().asJson();
			
		JsonValidation.validate(json, "midataID", "birthday");
		
		String midataID = JsonValidation.getString(json, "midataID");
		Date birthday = JsonValidation.getDate(json, "birthday");
		
		Set<String> memberFields = Sets.create("firstname","birthday", "lastname","city","zip","country","email","phone","mobile","ssn","address1","address2");
		Member result = Member.getByMidataIDAndBirthday(midataID, birthday, memberFields);
		if (result == null) return ok();
		
		HPUser hpuser = HPUser.getById(userId, Sets.create("provider", "firstname", "lastname", "email"));
		
		//MemberKeys.getOrCreate(hpuser, result);
		Set<MemberKey> memberKeys = MemberKey.getByOwnerAndAuthorizedPerson(result._id, userId);
		
		ObjectNode obj = Json.newObject();
		obj.put("member", JsonOutput.toJsonNode(result, "User", memberFields));
		obj.put("consents", JsonOutput.toJsonNode(memberKeys, "Consent", MemberKey.ALL));
		
		return ok(Json.toJson(obj));
	}
	
    /**
     * return list of all patients of current healthcare provider	
     * @return list of Members
     * @throws JsonValidationException
     * @throws InternalServerException
     */
	@Security.Authenticated(ProviderSecured.class)	
	@APICall
	public static Result list() throws JsonValidationException, InternalServerException {
		
		MidataId userId = new MidataId(request().username());

		Set<MemberKey> memberKeys = MemberKey.getByAuthorizedPerson(userId, Sets.create("owner"));
		Set<MidataId> ids = new HashSet<MidataId>();
		for (MemberKey key : memberKeys) ids.add(key.owner);
		Set<String> fields = Sets.create("_id", "firstname","birthday", "lastname"); 
		Set<Member> result = Member.getAll(CMaps.map("_id", ids).map("status", User.NON_DELETED), fields, 0);
		
		return ok(JsonOutput.toJson(result, "User", fields));
	}
	
	/**
	 * retrieve information about a specific patient (Member) of the current healthcare provider
	 * @param id ID of member
	 * @return Member, Consents with current Healthcare Provider, Consent for Healthcare Provider to share data with patient.  
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@Security.Authenticated(ProviderSecured.class)	
	@APICall
	public static Result getMember(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId memberId = new MidataId(id);
		
		Set<MemberKey> memberKeys = MemberKey.getByOwnerAndAuthorizedPerson(memberId, userId);
		if (memberKeys.isEmpty()) throw new BadRequestException("error.notauthorized.account", "You are not authorized.");
		
		Set<HCRelated> backconsent = HCRelated.getByAuthorizedAndOwner(memberId,  userId);
		
		Set<String> memberFields = Sets.create("_id", "firstname","birthday", "lastname","city","zip","country","email","phone","mobile","ssn","address1","address2");
		Member result = Member.getById(memberId, memberFields);
		if (result==null) throw new BadRequestException("error.unknown.user", "Member does not exist.");
		
		ObjectNode obj = Json.newObject();
		obj.put("member", JsonOutput.toJsonNode(result, "User", memberFields));
		obj.put("consents", JsonOutput.toJsonNode(memberKeys, "Consent", MemberKey.ALL));
		obj.put("backwards", JsonOutput.toJsonNode(backconsent, "Consent", Sets.create("_id", "name", "owner") ));
		return ok(obj);
	}
	
	/**
	 * TODO check functionality
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@Security.Authenticated(ProviderSecured.class)
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public static Result getVisualizationToken() throws JsonValidationException, InternalServerException {
		MidataId userId = new MidataId(request().username());
		JsonNode json = request().body().asJson();
						
		JsonValidation.validate(json, "consent");
		
		//MidataId memberId = JsonValidation.getMidataId(json, "member");
		MidataId consentId = JsonValidation.getMidataId(json, "consent");
		//MemberKey memberKey = MemberKey.getByIdAndOwner(consentId, memberId, Sets.create());

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, consentId, userId);
		return ok(spaceToken.encrypt(request()));
	}
	
}
