/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers.providers;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

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
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.EntityType;
import models.enums.Gender;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.UserGroupTools;
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
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.FHIRServlet;
import utils.fhir.OrganizationResourceProvider;
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
	public Result register(Request request) throws AppException {
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language", "pub", "priv_pw", "recovery");
					
		String name = JsonValidation.getStringOrNull(json, "name");
		if (name != null && HealthcareProvider.existsByName(name)) return inputerror("name", "exists", "A healthcare provider with this name already exists.");
		
		String email = JsonValidation.getEMail(json, "email");
		if (HPUser.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
		
		/*HealthcareProvider provider = new HealthcareProvider();
		
		provider._id = new MidataId();
		provider.name = name;
		provider.description = JsonValidation.getStringOrNull(json, "description");
		provider.url = JsonValidation.getStringOrNull(json, "url");
		*/
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
		
		Application.developerRegisteredAccountCheck(request, user, json);
		Terms.addAgreedToDefaultTerms(user);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		AccessContext context = ContextManager.instance.createInitialSession(user._id, UserRole.PROVIDER, null);
		
		String pub = JsonValidation.getString(json, "pub");
		String pk = JsonValidation.getString(json, "priv_pw");
		Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
			  		        	      		  		
		user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);		  
		KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  
		String handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);
			  
		user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(user._id, null);								
				
		HPUser.add(user);
			  
		KeyManager.instance.newFutureLogin(user);	
		PWRecovery.storeRecoveryData(user._id, recover);
				
		RecordManager.instance.createPrivateAPS(null, user._id, user._id);		
		
		if (name != null) {
		   HealthcareProvider provider = UserGroupTools.createOrUpdateOrganizationUserGroup(context, new MidataId(), name, JsonValidation.getStringOrNull(json, "description"), null, true);		
		   OrganizationResourceProvider.updateFromHP(context, provider);
		}
		
		Application.sendWelcomeMail(user, null);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) Application.sendAdminNotificationMail(user);
		
		return OAuth2.loginHelper(request, new ExtendedSessionToken().forUser(user).withSession(handle), json, null, context);
		
	}
	
	public static void register(AccessContext context, HPUser user, HealthcareProvider provider, User executingUser) throws AppException {
		
		if (provider != null && HealthcareProvider.existsByName(provider.name)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider with this name already exists.");			
		if (HPUser.existsByEMail(user.email)) {
			AuditManager.instance.addAuditEvent(AuditEventType.TRIED_USER_REREGISTRATION, HPUser.getByEmail(user.email, User.PUBLIC));
			throw new JsonValidationException("error.exists.user", "email", "exists", "A user with this email address already exists.");
		}
				
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
			provider = UserGroupTools.createOrUpdateOrganizationUserGroup(context, provider._id, provider.name, provider.description, null, true);		
			OrganizationResourceProvider.updateFromHP(context, provider);			
		}
		HPUser.add(user);
					
		RecordManager.instance.createPrivateAPS(null, user._id, user._id);		
		
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
	public Result registerOther(Request request) throws AppException {
		
		requireSubUserRole(request, SubUserRole.MASTER);
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "country", "language");
							
		String email = JsonValidation.getEMail(json, "email");
			
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
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
						
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request.attrs().get(play.mvc.Security.USERNAME)), user);
		AccessContext context = portalContext(request);
		register(context, user ,null, executingUser);
			
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
	public Result login(Request request) throws AppException {
		JsonNode json = request.body().asJson();
			
		JsonValidation.validate(json, "email", "password");
			
	    ExtendedSessionToken token = new ExtendedSessionToken();
			
		token.created = System.currentTimeMillis();                               
		token.userRole = UserRole.PROVIDER;                
											    				
		return OAuth2.loginHelper(request, token, json, null, null);						
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
	public Result search(Request request) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request.body().asJson();
		
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
		//if (memberKeys.isEmpty() && removeIfNoConsents) return ok();
				
		boolean activeConsent = false;
		for (Consent consent : memberKeys) {
			if (consent.isSharingData()) activeConsent = true;
		}
		
		memberFields = activeConsent 
				   ? memberFields
				   : Sets.create("_id", "email");
		
		
		ObjectNode obj = Json.newObject();
		obj.set("member", JsonOutput.toJsonNode(result, "User", memberFields));
		obj.set("consents", JsonOutput.toJsonNode(memberKeys, "Consent", Consent.ALL));
		
		return ok(Json.toJson(obj));
	}
	
	/**
	 * healthcare provider search for MIDATA members by MIDATAID and birthday.
	 * @return Member and list of consents
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result searchOrganization(Request request) throws JsonValidationException, AppException {
	
		JsonNode json = request.body().asJson();
		String name = JsonValidation.getStringOrNull(json, "name");
		String city = JsonValidation.getStringOrNull(json, "city");
		OrganizationResourceProvider provider = ((OrganizationResourceProvider) FHIRServlet.getProvider("Organization")); 
		List<Organization> orgs = provider.search(portalContext(request), name, city);
		StringBuffer out = new StringBuffer("[");
		boolean first = true;
		for (Organization org : orgs) {
		   if (first) first=false; else out.append(",");			  
		   out.append(provider.serialize(org));
		}
		out.append("]");
		return ok(out.toString()).as("application/json");
	}
	
    /**
     * return list of all patients of current healthcare provider	
     * @return list of Members
     * @throws JsonValidationException
     * @throws InternalServerException
     */
	@Security.Authenticated(ProviderSecured.class)	
	@APICall
	public Result list(Request request) throws JsonValidationException, InternalServerException {
		
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));

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
	public Result getMember(Request request, String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId memberId = MidataId.from(id);
		
		Collection<Consent> memberKeys = Circles.getConsentsAuthorized(userId, CMaps.map("type", ConsentType.HEALTHCARE).map("owner", memberId), Consent.ALL);		
		if (memberKeys.isEmpty()) throw new BadRequestException("error.notauthorized.account", "You are not authorized.");
		
		Set<HCRelated> backconsent = HCRelated.getByAuthorizedAndOwner(memberId,  userId);
		
		boolean activeConsent = false;
		for (Consent consent : memberKeys) {
			if (consent.isSharingData()) activeConsent = true;
		}
		
		Set<String> memberFields = activeConsent 
				   ? Sets.create("_id", "firstname","birthday", "lastname","city","zip","country","email","phone","mobile","ssn","address1","address2")
				   : Sets.create("_id", "email");
		Member result = Member.getById(memberId, memberFields);
		if (result==null) throw new BadRequestException("error.unknown.user", "Member does not exist.");
		
		ObjectNode obj = Json.newObject();
		obj.set("member", JsonOutput.toJsonNode(result, "User", memberFields));
		obj.set("consents", JsonOutput.toJsonNode(memberKeys, "Consent", Consent.ALL));
		obj.set("backwards", JsonOutput.toJsonNode(backconsent, "Consent", Sets.create("_id", "name", "owner") ));
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
	public Result getVisualizationToken(Request request) throws JsonValidationException, InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request.body().asJson();
						
		JsonValidation.validate(json, "consent");
		
		//MidataId memberId = JsonValidation.getMidataId(json, "member");
		MidataId consentId = JsonValidation.getMidataId(json, "consent");
		//MemberKey memberKey = MemberKey.getByIdAndOwner(consentId, memberId, Sets.create());

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, consentId, userId, getRole());
		return ok(spaceToken.encrypt(request));
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getOrganization(String id) throws AppException {
			
		//MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId providerid = MidataId.from(id);
						
		HealthcareProvider provider = HealthcareProvider.getById(providerid, HealthcareProvider.ALL);
		if (provider == null) return notFound();						
		return ok(JsonOutput.toJson(provider, "HealthcareProvider", HealthcareProvider.ALL));		
	}

	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ProviderSecured.class)
	public Result updateOrganization(Request request, String id) throws AppException {
		requireSubUserRole(request, SubUserRole.MASTER);
		
		AccessContext context = portalContext(request);
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "_id", "name");
					
		String name = JsonValidation.getString(json, "name");
		MidataId providerid = MidataId.parse(id);
		//String description = JsonValidation.getString(json, "description");
				
		if (!UserGroupTools.accessorIsMemberOfGroup(context, providerid, Permission.SETUP)) {
			throw new BadRequestException("error.notauthorized.action", "Tried to change other healthcare provider organization!");
		}
				
		if (HealthcareProvider.existsByName(name, providerid)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider organization with this name already exists.");			
		
		HealthcareProvider provider = HealthcareProvider.getById(providerid, HealthcareProvider.ALL);

		if (provider == null) throw new InternalServerException("error.internal", "Healthcare provider organization not found.");
		
		MidataId parent = JsonValidation.getMidataId(json, "parent");
		
		if (parent != null && !parent.equals(provider.parent)) {
			if (!UserGroupTools.accessorIsMemberOfGroup(context, parent, Permission.SETUP)) {
				throw new BadRequestException("error.notauthorized.action", "Tried to change other healthcare provider organization!");
			}	
		}
				
	   provider = UserGroupTools.createOrUpdateOrganizationUserGroup(context, provider._id, name, JsonValidation.getStringOrNull(json, "description"), parent, false);		
	   OrganizationResourceProvider.updateFromHP(context, provider);		
		
		return ok();		
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ProviderSecured.class)
	public Result createOrganization(Request request) throws AppException {
		requireSubUserRole(request, SubUserRole.MASTER);
		
		AccessContext context = portalContext(request);
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "name");
					
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getStringOrNull(json, "description");
		
		String manager = JsonValidation.getStringOrNull(json, "manager");
		EntityType managerType = JsonValidation.getEnum(json, "managerType", EntityType.class);
		MidataId managerId = null;
		
		MidataId parent = JsonValidation.getMidataId(json, "parent");
		
		if (HealthcareProvider.existsByName(name)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider organization with this name already exists.");			
		
		if (managerType == EntityType.ORGANIZATION) {
			  managerId = parent;
		} else if (managerType == EntityType.USER) {
			if (manager == null) {
				managerId = context.getAccessor();
			} else {
			    HPUser user = HPUser.getByEmail(manager, Sets.create("_id"));
			    if (user == null) throw new JsonValidationException("error.unknown.user", "manager", "unknown", "No practitioner with this name exists.");
			    managerId = user._id;
			}
		}
		
		HealthcareProvider provider = UserGroupTools.createOrUpdateOrganizationUserGroup(context, new MidataId(), name, description, parent, managerId.equals(context.getAccessor()));
				
		
		if (!managerId.equals(context.getAccessor())) UserGroupTools.createUserGroupMember(context, managerId, managerType, ResearcherRole.MANAGER(), provider._id);
		
		OrganizationResourceProvider.updateFromHP(context, provider);
		
		return ok(JsonOutput.toJson(provider, "HealthcareProvider", HealthcareProvider.ALL)).as("application/json");		
	}
	
}
