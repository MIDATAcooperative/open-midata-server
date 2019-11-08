package controllers.providers;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import controllers.Circles;
import controllers.OAuth2;
import controllers.PWRecovery;
import controllers.Terms;
import models.Consent;
import models.HCRelated;
import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
import models.MidataId;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentType;
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
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.ExtendedSessionToken;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.ProviderSecured;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
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
	public Result register() throws AppException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language", "pub", "priv_pw", "recovery");
					
		String name = JsonValidation.getString(json, "name");
		if (HealthcareProvider.existsByName(name)) return inputerror("name", "exists", "A healthcare provider with this name already exists.");
		
		String email = JsonValidation.getEMail(json, "email");
		if (HPUser.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
		
		HealthcareProvider provider = new HealthcareProvider();
		
		provider._id = new MidataId();
		provider.name = name;
		provider.description = JsonValidation.getStringOrNull(json, "description");
		provider.url = JsonValidation.getStringOrNull(json, "url");
		//research.description = JsonValidation.getString(json, "description");
		
		HPUser user = new HPUser(email);
		user._id = new MidataId();
		user.role = UserRole.PROVIDER;		
		user.subroles.add(SubUserRole.MASTER);
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
		//user.authType = SecondaryAuthType.SMS;
		
		Application.developerRegisteredAccountCheck(user, json);
		Terms.addAgreedToDefaultTerms(user);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
			  		        	      		  		
		user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);		  
		KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  
		String handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);
			  
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(user._id, null);								
		
		HealthcareProvider.add(provider);
		user.provider = provider._id;
		HPUser.add(user);
			  
		KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, recover);
				
		RecordManager.instance.createPrivateAPS(user._id, user._id);		
				
		Application.sendWelcomeMail(user, null);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
		
		return OAuth2.loginHelper(new ExtendedSessionToken().forUser(user).withSession(handle), json, null, user._id);
		
	}
	
	public static void register(HPUser user, HealthcareProvider provider, User executingUser) throws AppException {
		
		if (provider != null && HealthcareProvider.existsByName(provider.name)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider with this name already exists.");			
		if (HPUser.existsByEMail(user.email)) throw new JsonValidationException("error.exists.user", "email", "exists", "A user with this email address already exists.");
				
		if (user._id == null) user._id = new MidataId();
		user.role = UserRole.PROVIDER;
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
		
		if (provider != null) {
			HealthcareProvider.add(provider);
		    user.provider = provider._id;
		}
		HPUser.add(user);
					
		RecordManager.instance.createPrivateAPS(user._id, user._id);		
		
		Application.sendWelcomeMail(user, executingUser);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
						
	}
	
	/**
	 * register a new research
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ProviderSecured.class)
	public Result registerOther() throws AppException {
		
		requireSubUserRole(SubUserRole.MASTER);
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "country", "language");
							
		String email = JsonValidation.getEMail(json, "email");
			
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		User executingUser = User.getById(executorId, User.ALL_USER);
		
	    HPUser user = new HPUser(email);
		
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
		user.provider = PortalSessionToken.session().orgId;
		if (user.provider == null) throw new InternalServerException("error.internal", "No organization in session for register provider!");
		user.status = UserStatus.ACTIVE;
		
		//user.authType = SecondaryAuthType.SMS;
						
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request().attrs().get(play.mvc.Security.USERNAME)), user);
		register(user ,null, executingUser);
			
		AuditManager.instance.success();
		return ok();		
	}
	
	/**
	 * healthcare provider login
	 * @return status ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result login() throws AppException {
		JsonNode json = request().body().asJson();
			
		JsonValidation.validate(json, "email", "password");
			
	    ExtendedSessionToken token = new ExtendedSessionToken();
			
		token.created = System.currentTimeMillis();                               
		token.userRole = UserRole.PROVIDER;                
											    				
		return OAuth2.loginHelper(token, json, null, null);						
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
	public Result search() throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request().body().asJson();
		
		Member result = null;
		boolean removeIfNoConsents = false;
		Set<String> memberFields = Sets.create("firstname","birthday", "lastname","city","zip","country","email","phone","mobile","ssn","address1","address2", "searchable");
		
		if (json.has("email")) {
			String email = JsonValidation.getString(json, "email");
			result = Member.getByEmail(email, memberFields);
			if (result!=null && !result.searchable) removeIfNoConsents = true;
		} else {		
			JsonValidation.validate(json, "midataID", "birthday");
			
			String midataID = JsonValidation.getString(json, "midataID");
			Date birthday = JsonValidation.getDate(json, "birthday");
					
			result = Member.getByMidataIDAndBirthday(midataID, birthday, memberFields);
		}
		if (result == null) return ok();
		
		HPUser hpuser = HPUser.getById(userId, Sets.create("provider", "firstname", "lastname", "email"));
		
		//MemberKeys.getOrCreate(hpuser, result);
		Collection<Consent> memberKeys = Circles.getConsentsAuthorized(userId, CMaps.map("type", ConsentType.HEALTHCARE).map("owner", result._id), Consent.ALL);
		if (memberKeys.isEmpty() && removeIfNoConsents) return ok();
				
		ObjectNode obj = Json.newObject();
		obj.set("member", JsonOutput.toJsonNode(result, "User", memberFields));
		obj.set("consents", JsonOutput.toJsonNode(memberKeys, "Consent", Consent.ALL));
		
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
	public Result list() throws JsonValidationException, InternalServerException {
		
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));

		Set<MemberKey> memberKeys = MemberKey.getByAuthorizedPerson(userId, Sets.create("owner"), Circles.RETURNED_CONSENT_LIMIT);
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
	public Result getMember(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId memberId = new MidataId(id);
		
		Collection<Consent> memberKeys = Circles.getConsentsAuthorized(userId, CMaps.map("type", ConsentType.HEALTHCARE).map("owner", memberId), Consent.ALL);		
		if (memberKeys.isEmpty()) throw new BadRequestException("error.notauthorized.account", "You are not authorized.");
		
		Set<HCRelated> backconsent = HCRelated.getByAuthorizedAndOwner(memberId,  userId);
		
		Set<String> memberFields = Sets.create("_id", "firstname","birthday", "lastname","city","zip","country","email","phone","mobile","ssn","address1","address2");
		Member result = Member.getById(memberId, memberFields);
		if (result==null) throw new BadRequestException("error.unknown.user", "Member does not exist.");
		
		ObjectNode obj = Json.newObject();
		obj.put("member", JsonOutput.toJsonNode(result, "User", memberFields));
		obj.put("consents", JsonOutput.toJsonNode(memberKeys, "Consent", Consent.ALL));
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
	public Result getVisualizationToken() throws JsonValidationException, InternalServerException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request().body().asJson();
						
		JsonValidation.validate(json, "consent");
		
		//MidataId memberId = JsonValidation.getMidataId(json, "member");
		MidataId consentId = JsonValidation.getMidataId(json, "consent");
		//MemberKey memberKey = MemberKey.getByIdAndOwner(consentId, memberId, Sets.create());

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, consentId, userId, getRole());
		return ok(spaceToken.encrypt(request()));
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getOrganization(String id) throws AppException {
			
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId providerid = MidataId.from(id);
						
		HealthcareProvider provider = HealthcareProvider.getById(providerid, HealthcareProvider.ALL);
		if (provider == null) return notFound();						
		return ok(JsonOutput.toJson(provider, "HealthcareProvider", HealthcareProvider.ALL));		
	}

	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ProviderSecured.class)
	public Result updateOrganization(String id) throws AppException {
		requireSubUserRole(SubUserRole.MASTER);
		
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "_id", "name");
					
		String name = JsonValidation.getString(json, "name");				
		//String description = JsonValidation.getString(json, "description");
				
		MidataId providerid = PortalSessionToken.session().getOrgId();
		
		if (!providerid.equals(JsonValidation.getMidataId(json, "_id"))) throw new InternalServerException("error.internal", "Tried to change other healthcare provider organization!");
		
		if (HealthcareProvider.existsByName(name, providerid)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider organization with this name already exists.");			
		
		HealthcareProvider provider = HealthcareProvider.getById(providerid, HealthcareProvider.ALL);

		if (provider == null) throw new InternalServerException("error.internal", "Healthcare provider organization not found.");
		
		provider.name = name;	
		provider.description = JsonValidation.getStringOrNull(json, "description");
		provider.url = JsonValidation.getStringOrNull(json, "url");
		provider.setMultiple(Sets.create("name", "description", "url"));
							
		return ok();		
	}
	
}
