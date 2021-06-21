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

package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.AccessPermissionSet;
import models.AccountStats;
import models.Circle;
import models.Consent;
import models.Developer;
import models.HPUser;
import models.HealthcareProvider;
import models.KeyInfoExtern;
import models.KeyRecoveryData;
import models.KeyRecoveryProcess;
import models.Member;
import models.MidataId;
import models.Research;
import models.ResearchUser;
import models.Space;
import models.StudyParticipation;
import models.SubscriptionData;
import models.User;
import models.UserGroupMember;
import models.enums.AccountActionFlags;
import models.enums.AccountNotifications;
import models.enums.AuditEventType;
import models.enums.ConsentType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.ParticipationStatus;
import models.enums.SecondaryAuthType;
import models.enums.SubUserRole;
import models.enums.UserFeature;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.AccessContext;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.ExtendedSessionToken;
import utils.auth.FutureLogin;
import utils.auth.KeyManager;
import utils.auth.MemberSecured;
import utils.auth.PortalSessionToken;
import utils.auth.PreLoginSecured;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.OrganizationResourceProvider;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;

/**
 * user related functions
 *
 */
public class Users extends APIController {
	
	public final static int MAX_CONTACTS_SIZE = 300;
	
	/**
	 * retrieve a list of users matching some criteria
	 * allowed restrictions and returned fields depend heavily on user role
	 * @return list of users
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result get() throws AppException, JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "fields");
		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));		
		ObjectIdConversion.convertMidataIds(properties, "_id", "developer", "organization", "provider");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		Map<String, Object> properties_mod = new HashMap<String, Object>(properties);
		// check authorization
		
		if (!getRole().equals(UserRole.ADMIN) && 
			!(getRole().equals(UserRole.RESEARCH) && properties.containsKey("role") && properties.get("role").equals("RESEARCH")) &&
			!(getRole().equals(UserRole.PROVIDER) && properties.containsKey("role") && properties.get("role").equals("PROVIDER")) &&
			!properties.containsKey("_id") && !properties.containsKey("developer") && !properties.containsKey("organization")) properties.put("searchable", true);
		boolean postcheck = false;		
		if (!getRole().equals(UserRole.ADMIN) && !properties.containsKey("email") && !properties.containsKey("midataID") && !properties.containsKey("_id") && !properties.containsKey("developer") && !properties.containsKey("organization") && !properties.containsKey("provider")) {
			throw new AuthException("error.notauthorized.action", "Search must be restricted");
		}
		UserRole role = null;
		
		
		
		if (properties.containsKey("_id") && properties.get("_id").toString().equals(request().attrs().get(play.mvc.Security.USERNAME))) {
		  Rights.chk("Users.getSelf", getRole(), properties, fields);
		} else if (properties.containsKey("role") && !(properties.get("role") instanceof Collection)) {
		  role = UserRole.valueOf(properties.get("role").toString());
		  if (Rights.existsAction("Users.get"+role, getRole())) {
		    Rights.chk("Users.get"+role.toString(), getRole(), properties, fields);
		  } else {
			Rights.chk("Users.get", getRole(), properties, fields);
		  }		  
		} else if (fields.contains("role")) {
			// Check later
			postcheck = true;			
		} else {		
		  Rights.chk("Users.get", getRole(), properties, fields);
		  
		}

		if (!getRole().equals(UserRole.ADMIN)) properties_mod.put("status", User.NON_DELETED);
		
		// execute		
		if (fields.contains("name")) { fields.add("firstname"); fields.add("lastname"); }	
		
		if (properties.containsKey("email")) {
			properties_mod.put("emailLC", properties.get("email").toString().toLowerCase());
			properties_mod.remove("email");
		}
		if (properties.get("lastname") instanceof String ) {
			String lastname = (String) properties.get("lastname");
			properties_mod.put("keywordsLC", lastname.toLowerCase());
			properties_mod.put("lastname", Pattern.compile("^"+lastname+"$", Pattern.CASE_INSENSITIVE));
		}
		
		List<User> users;		
		if (role != null && role == UserRole.DEVELOPER) {
		  properties_mod.put("role", EnumSet.of(UserRole.DEVELOPER, UserRole.ADMIN));
		  users = new ArrayList<User>(Developer.getAll(properties_mod, fields, 100));
		} else if (role != null && role == UserRole.RESEARCH) {
		  users = new ArrayList<User>(ResearchUser.getAll(properties_mod, fields, 100));	
		} else if (role != null && role == UserRole.PROVIDER) {
		  users = new ArrayList<User>(HPUser.getAll(properties_mod, fields));	
		} else {
		  users = new ArrayList<User>(Member.getAll(properties_mod, fields, 100));
		}
		
		if (postcheck) {
			for (User mem : users) {
				if (Rights.existsAction("Users.get"+mem.getRole().toString(), getRole())) {
				  Rights.chk("Users.get"+mem.getRole().toString(), getRole(), properties, fields);
				}  else {
				  Rights.chk("Users.get", getRole(), properties, fields);
				}
			}
		}
		
		if (fields.contains("name")) {
			for (User user : users) user.name = (user.firstname + " "+ user.lastname).trim();
		}
		
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields)).as("application/json");
	}
		
	/**
	 * get ID of currently logged in user
	 * @return
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result getCurrentUser() {
						
		ObjectNode obj = Json.newObject();	
		PortalSessionToken session = PortalSessionToken.session();
		obj.put("role", session.getRole().toString());
		obj.put("user", session.getOwnerId().toString());
		if (session.orgId != null) obj.put("org", session.orgId.toString());
																
		return ok(obj);
	}

    /**
     * text search for users
     * @param query search string
     * @return list of users (json)
     * @throws AppException
     */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result search(String query) throws AppException {
		
		requireUserFeature(UserFeature.EMAIL_VERIFIED);
		
		Set<String> fields =  Sets.create("firstname", "lastname", "name", "role");
		Set<Member> result = Member.getAll(CMaps.map("emailLC", query.toLowerCase()).map("searchable", true).map("status", User.NON_DELETED).map("role", UserRole.MEMBER), fields);
		
		for (Member member : result) {
			member.name = member.firstname+" "+member.lastname;
		}
		
		List<Member> users = new ArrayList<Member>(result);
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields));
	}

	/**
	 * Prefetch contacts for completion suggestions.
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result loadContacts() throws InternalServerException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		Set<MidataId> contactIds = new HashSet<MidataId>();
		Set<Member> contacts;
	
		Set<Circle> circles = Circle.getAll(CMaps.map("owner", userId).map("type",Sets.createEnum(ConsentType.CIRCLE, ConsentType.HEALTHCARE, ConsentType.REPRESENTATIVE)).map("status",Consent.NOT_DELETED), Sets.create("authorized"));
		for (Circle circle : circles) {
			contactIds.addAll(circle.authorized);
		}
		if (contactIds.size() > MAX_CONTACTS_SIZE) return ok();
		
		contacts = Member.getAll(CMaps.map("_id", contactIds).map("role", UserRole.MEMBER).map("status", User.NON_DELETED),Sets.create("firstname","lastname","email","role"));
			
		return ok(JsonOutput.toJson(contacts, "User", Sets.create("firstname","lastname","email","role")));
	}

	/**
	 * Suggest users that complete the given query.
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result complete(String query) {
		return ok(Json.toJson(Collections.EMPTY_LIST)); //Search.complete(Type.USER, query)));
	}
	
	
	/**
	 * Updates the address information of a user.
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(PreLoginSecured.class)
	public Result updateAddress() throws AppException {
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "user");//, "firstname", "lastname", "gender", "city", "zip", "country", "address1");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");			
		MidataId executorId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		//Check authorization except for change self
		if (!executorId.equals(userId)) {
		  requireSubUserRole(SubUserRole.USERADMIN);
		} 
		 			
		//String email = JsonValidation.getEMail(json, "email");
		String firstName = JsonValidation.getString(json, "firstname");
		String lastName = JsonValidation.getString(json, "lastname");
					
		User user = User.getById(userId, User.ALL_USER_INTERNAL);
		
		if (!PortalSessionToken.session().is2FAVerified(user)) {
		  throw new InternalServerException("error.internal", "address change tried without verification");	  
		}
				
		//user.email = email;
		//user.emailLC = email.toLowerCase();
		if (json.has("country")) { 
						
			if (user.city != null && user.city.length()>0) {
				JsonValidation.validate(json, "user", "firstname", "lastname", "city", "zip", "country", "address1");
			} else {
				JsonValidation.validate(json, "user", "firstname", "lastname", "country");	
			}
			
			user.name = firstName + " " + lastName;
			user.address1 = JsonValidation.getStringOrNull(json, "address1");
			user.address2 = JsonValidation.getStringOrNull(json, "address2");
			user.city = JsonValidation.getStringOrNull(json, "city");
			user.zip = JsonValidation.getStringOrNull(json, "zip");
			
			user.country = JsonValidation.getString(json, "country");
			user.firstname = JsonValidation.getString(json, "firstname");
			user.lastname = JsonValidation.getString(json, "lastname");
			if (json.has("gender")) {
			  user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
			}
		}
		if (json.has("phone") || json.has("mobile")) {
		  if (user.mobile != null) JsonValidation.validate(json, "mobile");
		  user.phone = JsonValidation.getStringOrNull(json, "phone");
		  String mobile = JsonValidation.getStringOrNull(json, "mobile");
		  if (mobile==null || !mobile.equals(user.mobile)) {
			  user.mobileStatus = EMailStatus.UNVALIDATED;
			  user.mobile = mobile;  
		  }
		  
		}
		if (json.has("authType")) {
			
			user.authType = JsonValidation.getEnum(json, "authType", SecondaryAuthType.class);
			
			if (user.authType.equals(SecondaryAuthType.NONE) && InstanceConfig.getInstance().getInstanceType().is2FAMandatory(getRole())) {
				throw new JsonValidationException("error.missing.auth_type", "authType", "missing", "Two factor authentication is mandantory");
			}
			
			user.mobile = JsonValidation.getString(json, "mobile");
			if (JsonValidation.getBoolean(json, "emailnotify")) {
				user.notifications = AccountNotifications.LOGIN;
			} else {
				user.notifications = AccountNotifications.NONE;
			}
		}
		
		if (json.has("country")) {
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_ADDRESS_CHANGE, user);
		} else if (json.has("phone")) {
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_PHONE_CHANGE, user);
		} else {
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_SETTINGS_CHANGE, user);
		}
		
		//User.set(user._id, "email", user.email);
		//User.set(user._id, "emailLC", user.emailLC);
		User.set(user._id, "name", user.name);
		User.set(user._id, "address1", user.address1);
		User.set(user._id, "address2", user.address2);
		User.set(user._id, "city", user.city);
		User.set(user._id, "zip", user.zip);
		User.set(user._id, "phone", user.phone);
		User.set(user._id, "mobile", user.mobile);
		User.set(user._id, "mobileStatus", user.mobileStatus);
		User.set(user._id, "country", user.country);
		User.set(user._id, "firstname", user.firstname);
		User.set(user._id, "lastname", user.lastname);
		User.set(user._id, "gender", user.gender);
		User.set(user._id, "authType", user.authType);	
		User.set(user._id, "notifications", user.notifications);
		
		user.updateKeywords(true);
		
		if (user.role.equals(UserRole.MEMBER)) {	
			
			if (!executorId.equals(userId)) {
			   user.addFlag(AccountActionFlags.UPDATE_FHIR);
			} else {
			   PatientResourceProvider.updatePatientForAccount(user._id);
			}
					  
		}
		
		AuditManager.instance.success();
		
		return ok();		
	}
	
	
	
	
	/**
	 * Update user settings like language and public settings
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result updateSettings() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "language");
		
		boolean searchable = JsonValidation.getBoolean(json, "searchable");
		
		if (searchable) {
			requireUserFeature(UserFeature.EMAIL_VERIFIED);			
		}
		
		String language = JsonValidation.getString(json, "language");
					
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		SecondaryAuthType authType = JsonValidation.getEnum(json, "authType", SecondaryAuthType.class);
						
		if (authType.equals(SecondaryAuthType.NONE) && InstanceConfig.getInstance().getInstanceType().is2FAMandatory(getRole())) {
			throw new JsonValidationException("error.missing.auth_type", "authType", "missing", "Two factor authentication is mandantory");
		}
		
		AccountNotifications sendMail = JsonValidation.getEnum(json, "notifications", AccountNotifications.class);
		
		User user = User.getById(userId, Sets.create("_id", "notifications")); 
		
		User.set(user._id, "searchable", searchable);
		User.set(user._id, "language", language);
		User.set(user._id, "authType", authType);
		User.set(user._id, "notifications", sendMail);
						
		PatientResourceProvider.updatePatientForAccount(userId);
		
		if (authType.equals(SecondaryAuthType.SMS)) {
			requireUserFeature(UserFeature.PHONE_ENTERED);
		}
		
		return ok();		
	}
	
	/**
	 * Request MIDATA membership of current user
	 * @return 200 ok
	 * @throws AppException
	 */	
	@APICall
	@Security.Authenticated(PreLoginSecured.class)
	public Result requestMembership() throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		return requestMembershipHelper(userId);
	}
	
	public static Result requestMembershipHelper(MidataId userId) throws AppException {			
		
		Member user = Member.getById(userId, Sets.create("_id", "status", "role", "subroles",  "emailStatus", "confirmedAt", "contractStatus", "agbStatus", "lastname", "firstname")); 
		if (user == null) throw new InternalServerException("error.internal", "User record not found.");
		//if (user.subroles.contains(SubUserRole.TRIALUSER) || user.subroles.contains(SubUserRole.NONMEMBERUSER) || user.subroles.contains(SubUserRole.STUDYPARTICIPANT) || user.subroles.contains(SubUserRole.APPUSER)) {
		//	
		//} else throw new BadRequestException("invalid.status_transition", "No membership request required.");
		
		AuditManager.instance.addAuditEvent(AuditEventType.CONTRACT_REQUESTED, user);
		
		if (user.agbStatus.equals(ContractStatus.NEW)) {
			user.agbStatus = ContractStatus.REQUESTED;
			Member.set(user._id, "agbStatus", ContractStatus.REQUESTED);
		} else if (user.contractStatus.equals(ContractStatus.NEW)) {
			user.contractStatus = ContractStatus.REQUESTED;
			Member.set(user._id, "contractStatus", ContractStatus.REQUESTED);
		}								
		
		AuditManager.instance.success();
		
		if (InstanceConfig.getInstance().getInstanceType().getAutoGrandMembership()) {
			
			if (user.confirmedAt == null) {
				user.confirmedAt = new Date(System.currentTimeMillis());			
	    	    user.set("confirmedAt", user.confirmedAt);
			}
			
			if (user.agbStatus == ContractStatus.REQUESTED) {				
			   user.agbStatus = ContractStatus.SIGNED;
			   Member.set(user._id, "agbStatus", ContractStatus.SIGNED);
			} else {
			   user.contractStatus = ContractStatus.SIGNED;
			   Member.set(user._id, "contractStatus", ContractStatus.SIGNED);
			}
			Application.checkAccount(user);
		}
				
		return ok();
	}
	
	/**
	 * completely wipe account of current user. This is only working on test instances - other instances will throw an exception.
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result accountWipe() throws AppException {
		if (!InstanceConfig.getInstance().getInstanceType().getAccountWipeAvailable()) throw new InternalServerException("error.internal", "Only allowed on demo server");
		
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "_id", "password");
		
		String password = JsonValidation.getString(json, "password");
		String passwordHash = JsonValidation.getString(json, "passwordHash");
		String reason = JsonValidation.getStringOrNull(json, "reason");
		MidataId check = JsonValidation.getMidataId(json, "_id");
		if (!check.equals(userId)) throw new InternalServerException("error.internal", "Session mismatch for wipe account.");
		
		User user = User.getById(userId, User.FOR_LOGIN);
		if (!user.authenticationValid(password) && !user.authenticationValid(passwordHash)) {
			throw new BadRequestException("accountwipe.error",  "Invalid password.");
		}
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_ACCOUNT_DELETED, userId);
		
		doAccountWipe(portalContext(), userId);
			
		AuditManager.instance.success();
		
		if (reason != null) {
			Messager.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), "Midata Admin", "["+InstanceConfig.getInstance().getPortalServerDomain()+"]: Account Deletion", "Reason given by user: "+reason);
		}
		
		return ok();
	}
	
	public static void doAccountWipe(AccessContext context, MidataId userId) throws AppException {
		MidataId executorId = context.getAccessor();
        SubscriptionData.deleteByOwner(userId);
		
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			if (executorId.equals(userId)) {
			  RecordManager.instance.deleteAPS(space._id, userId);
			} else AccessPermissionSet.delete(space._id);		
			
			Space.delete(userId, space._id);
		}
		
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map("type", Sets.createEnum(ConsentType.CIRCLE, ConsentType.EXTERNALSERVICE, ConsentType.HCRELATED, ConsentType.HEALTHCARE, ConsentType.API, ConsentType.REPRESENTATIVE)), Consent.ALL, Integer.MAX_VALUE);
		for (Consent consent : consents) {
			if (executorId.equals(userId)) {
			RecordManager.instance.deleteAPS(consent._id, userId);
			} else AccessPermissionSet.delete(consent._id);
			Circle.delete(userId, consent._id);
		}
		
		Set<StudyParticipation> studies = StudyParticipation.getAllByMember(userId, Sets.create("_id", "study", "status", "pstatus"));
		for (StudyParticipation study : studies) {
			if (study.pstatus == ParticipationStatus.MEMBER_REJECTED || study.pstatus == ParticipationStatus.MEMBER_RETREATED || study.pstatus == ParticipationStatus.RESEARCH_REJECTED) continue;
			try {
			  controllers.members.Studies.retreatParticipation(context, userId, study.study);
			} catch (Exception e) {}			
		}
		
		Set<Consent> consents2 = Consent.getAllByAuthorized(userId);
		for (Consent consent : consents2) {
			consent = Consent.getByIdAndAuthorized(consent._id, userId, Sets.create("authorized"));
			consent.authorized.remove(userId);
			Consent.set(consent._id, "authorized", consent.authorized);	
			Consent.set(consent._id, "lastUpdated", new Date());
		}
		
		Set<UserGroupMember> ugs = UserGroupMember.getAllByMember(userId);
		for (UserGroupMember ug : ugs) {
			AccessPermissionSet.delete(ug._id);
			ug.delete();
		}
		
		if (executorId.equals(userId)) {
		  RecordManager.instance.clearIndexes(userId);
		}
				
        KeyRecoveryProcess.delete(userId);
        KeyRecoveryData.delete(userId);
        FutureLogin.delete(userId);
		KeyManager.instance.deleteKey(userId);
		KeyInfoExtern.delete(userId);
		AccessPermissionSet.delete(userId);
						
		User user = User.getByIdAlsoDeleted(userId, User.ALL_USER_INTERNAL);
		if (user != null) {
			if (user.role == UserRole.PROVIDER) {
				HPUser hp = HPUser.getById(userId, Sets.create("provider"));
				user.delete();
				if (!User.exists(CMaps.map("provider", hp.provider).map("status", User.NON_DELETED))) {
					OrganizationResourceProvider.deleteOrganization(executorId, hp.provider);
					HealthcareProvider.delete(hp.provider);
				}			
			} else
			if (user.role == UserRole.RESEARCH) {
				ResearchUser ru = ResearchUser.getById(userId, Sets.create("organization"));
				user.delete();
				if (!User.exists(CMaps.map("organization", ru.organization).map("status", User.NON_DELETED))) {
				  OrganizationResourceProvider.deleteOrganization(executorId, ru.organization);
				  Research.delete(ru.organization);	
				}
			} else {
				user.delete();
			} 						
		}
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result getAccountStats() throws AppException {
								
		PortalSessionToken session = PortalSessionToken.session();
		AccountStats stats = RecordManager.instance.getStats(session.ownerId);
																	
		return ok(JsonOutput.toJson(stats, "AccountStats", AccountStats.ALL));
	}
}
