package controllers.admin;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import models.InstanceStats;
import models.Member;
import models.MidataAuditEvent;
import models.MidataId;
import models.Plugin;
import models.Space;
import models.Study;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.EventType;
import models.enums.Gender;
import models.enums.MessageReason;
import models.enums.StudyExecutionStatus;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.DBRecord;
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexPageModel;
import utils.audit.AuditManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.auth.PasswordResetToken;
import utils.auth.PortalSessionToken;
import utils.auth.PreLoginSecured;
import utils.auth.ResearchSecured;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
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
		
		User user = User.getById(userId, User.ALL_USER_INTERNAL); //Sets.create("status", "contractStatus", "agbStatus", "subroles", "confirmedAt", "emailStatus"));
		
		
		
		if (user == null) throw new BadRequestException("error.unknown.user", "Unknown user");
		
		UserStatus oldstatus = user.status;
		user.status = status;
		User.set(user._id, "status", user.status);
		
		if (user.status.equals(UserStatus.ACTIVE) && !oldstatus.equals(UserStatus.ACTIVE)) {			
			Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.ACCOUNT_UNLOCK, null, Collections.singleton(user._id), null, new HashMap<String, String>());			
		}
		
		if (user.status != oldstatus && user.status == UserStatus.DELETED) {
			AuditManager.instance.addAuditEvent(AuditEventType.USER_ACCOUNT_DELETED, null, executorId, user);
			User.set(user._id, "searchable", false);			
		}
		
		if (json.has("contractStatus")) {
			ContractStatus old = user.contractStatus;
			user.contractStatus = JsonValidation.getEnum(json, "contractStatus", ContractStatus.class);			
			User.set(user._id, "contractStatus", user.contractStatus);
			if (old == user.contractStatus) {
			} else if (user.contractStatus == ContractStatus.PRINTED) {
			  AuditManager.instance.addAuditEvent(AuditEventType.CONTRACT_SEND, null, executorId, user);
			  //user.addHistory(new History(EventType.CONTRACT_SEND, admin, "Midata contract"));				
			
			} else {
			  AuditManager.instance.addAuditEvent(AuditEventType.USER_ACCOUNT_CHANGE_BY_ADMIN, null, executorId, user, "contract status "+old.toString()+" to "+user.contractStatus.toString());
			  //user.addHistory(new History(EventType.ADMIN_ACCOUNT_CHANGE, admin, "contract status "+old.toString()+" to "+user.contractStatus.toString()));
			}
		}
		
		if (json.has("agbStatus")) {
			ContractStatus old = user.agbStatus;
			user.agbStatus = JsonValidation.getEnum(json, "agbStatus", ContractStatus.class);			
			User.set(user._id, "agbStatus", user.agbStatus);
			if (old == user.agbStatus) {
			} else if (user.agbStatus == ContractStatus.PRINTED) {
			  AuditManager.instance.addAuditEvent(AuditEventType.CONTRACT_SEND, null, executorId, user);
			  //user.addHistory(new History(EventType.CONTRACT_SEND, admin, "AGB"));							
			} else {
			   AuditManager.instance.addAuditEvent(AuditEventType.USER_ACCOUNT_CHANGE_BY_ADMIN, null, executorId, user, "agb status "+old.toString()+" to "+user.agbStatus.toString());
			  // user.addHistory(new History(EventType.ADMIN_ACCOUNT_CHANGE, admin, "agb status "+old.toString()+" to "+user.agbStatus.toString()));
			}
		}
		
		if (json.has("emailStatus")) {
			EMailStatus old = user.emailStatus;
			user.emailStatus = JsonValidation.getEnum(json, "emailStatus", EMailStatus.class);
			User.set(user._id, "emailStatus", user.emailStatus);
			if (old != user.emailStatus) {
			   AuditManager.instance.addAuditEvent(AuditEventType.USER_ACCOUNT_CHANGE_BY_ADMIN, null, executorId, user, "email status "+old.toString()+" to "+user.emailStatus.toString());
			 // user.addHistory(new History(EventType.ADMIN_ACCOUNT_CHANGE, admin, "email status "+old.toString()+" to "+user.emailStatus.toString()));
			}
		}
		
		Application.checkAccount(user);
		
		AuditManager.instance.success();
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
		
		MidataId executorId = new MidataId(request().username());
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language", "subroles");
							
		String email = JsonValidation.getEMail(json, "email");
		if (Admin.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
				
		Admin executingUser = Admin.getById(executorId, User.ALL_USER);
		
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
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request().username()), user);
		
		user.apps = new HashSet<MidataId>();		
		user.visualizations = new HashSet<MidataId>();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		Admin.add(user);
							
		Application.sendWelcomeMail(user, executingUser);
			
		AuditManager.instance.success();
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
		
		User targetUser = User.getById(userId, User.ALL_USER);
		AuditManager.instance.addAuditEvent(AuditEventType.INTERNAL_COMMENT, null, executorId, targetUser, comment);
		AuditManager.instance.success();
		//targetUser.addHistory(new History(EventType.INTERNAL_COMMENT, admin, comment));
		
		return ok();
	}
	
	/**
	 * change email address for user
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result changeUserEmail() throws AppException {
		
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "user", "email");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");
		String email = JsonValidation.getEMail(json, "email");
		
		MidataId executorId = new MidataId(request().username());
		
		//Check authorization except for change self
		if (!executorId.equals(userId)) {
		  requireSubUserRole(SubUserRole.USERADMIN);
		}
		
		User targetUser = User.getById(userId, User.ALL_USER);
		if (targetUser.email != null && targetUser.email.equals(email)) return ok();
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_EMAIL_CHANGE, null, executorId, targetUser, targetUser.email + " -> "+email);
		
		if (User.exists(CMaps.map("emailLC", email.toLowerCase()).map("role", targetUser.role))) {
			throw new BadRequestException("error.exists.user", "A person with this email already exists.");
		}
		
		String oldEmail = targetUser.email;								
		
		PasswordResetToken token = new PasswordResetToken(targetUser._id, targetUser.role.toString(), true);
		targetUser.set("resettoken", token.token);
		targetUser.set("resettokenTs", System.currentTimeMillis());
		String encrypted = token.encrypt();
			
		String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		Map<String,String> replacements = new HashMap<String, String>();
		replacements.put("old-email", oldEmail);
		replacements.put("new-email", email);
		replacements.put("confirm-url", site + "/#/portal/confirm/" + encrypted);
		replacements.put("reject-url", site + "/#/portal/reject/" + encrypted);
		replacements.put("token", token.token);
		   		
		if (oldEmail != null) Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.EMAIL_CHANGED_OLDADDRESS, null, Collections.singleton(targetUser._id), null, replacements);
		
		targetUser.email = email;
		targetUser.emailLC = email.toLowerCase();
		targetUser.emailStatus = EMailStatus.UNVALIDATED;
		
		if (oldEmail != null) targetUser.set("previousEMail", oldEmail);
		targetUser.set("email", targetUser.email);
		targetUser.set("emailLC", targetUser.emailLC);
		targetUser.set("emailStatus", targetUser.emailStatus);
		
		if (oldEmail != null) {
			Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.EMAIL_CHANGED_NEWADDRESS, null, Collections.singleton(targetUser._id), null, replacements);		
		} else { 			
 		    Application.sendWelcomeMail(targetUser, null);
		}
		
		AuditManager.instance.success();
		//targetUser.addHistory(new History(EventType.INTERNAL_COMMENT, admin, comment));
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result changeBirthday() throws AppException {	
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "user", "birthday");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");			
		MidataId executorId = new MidataId(request().username());
		
		//Check authorization except for change self
		if (!executorId.equals(userId)) {
		  requireSubUserRole(SubUserRole.USERADMIN);
		}				
				
		Member user = Member.getById(userId, Sets.create("_id", "birthday", "firstname", "lastname", "email", "role", "flags")); 
						
		Date birthDay = JsonValidation.getDate(json, "birthday");
		if (user != null && birthDay != null && !birthDay.equals(user.birthday)) {
			
			AuditManager.instance.addAuditEvent(AuditEventType.USER_BIRTHDAY_CHANGE, user);
			
			user.birthday = birthDay;
			User.set(user._id, "birthday", user.birthday);	
										
			if (!executorId.equals(userId)) {
			   user.addFlag(AccountActionFlags.UPDATE_FHIR);
			} else {
		       PatientResourceProvider.updatePatientForAccount(user._id);
			}
		    AuditManager.instance.success();
		
		}
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
		
		User selected = User.getById(userId, User.ALL_USER);
		if (!selected.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.invalid.status",  "User must have status deleted to be wiped.");
		
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			AccessPermissionSet.delete(space._id);			
			Space.delete(userId, space._id);
		}
		
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map("type", Sets.createEnum(ConsentType.CIRCLE, ConsentType.EXTERNALSERVICE, ConsentType.HCRELATED, ConsentType.HEALTHCARE)), Consent.ALL, Integer.MAX_VALUE);
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
		
		Set<UserGroupMember> ugs = UserGroupMember.getAllByMember(userId);
		for (UserGroupMember ug : ugs) {
			AccessPermissionSet.delete(ug._id);
			ug.delete();
		}
							
		if (getRole().equals(UserRole.PROVIDER)) {
			HealthcareProvider.delete(PortalSessionToken.session().org);
		}
		
		KeyManager.instance.deleteKey(userId);
		selected.delete();
		
		/*if (!User.exists(CMaps.map("organization", PortalSessionToken.session().org))) {
			  Research.delete(PortalSessionToken.session().org);			
		}*/
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result searchAuditLog() throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();					
		JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));		
		
		Rights.chk("MidataAuditEvent.search", getRole(), properties, fields);
		
		List<MidataAuditEvent> events = null;
	
		
		events = MidataAuditEvent.getAll(properties, fields, 1000);
							
		
		//Collections.sort(circles);
		return ok(JsonOutput.toJson(events, "MidataAuditEvent", fields));
	}
	
	/**
	 * delete a Study
	 * @return 200 ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result deleteStudy(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());
		MidataId owner = PortalSessionToken.session().getOrg();
		MidataId studyid = new MidataId(id);
		
		
		Study study = Study.getById(studyid, Sets.create("name", "owner","executionStatus", "participantSearchStatus","validationStatus", "createdBy", "code"));
		
		if (study == null) throw new BadRequestException("error.missing.study", "Study not found.");
		if (study.executionStatus != StudyExecutionStatus.PRE && study.executionStatus != StudyExecutionStatus.ABORTED) throw new BadRequestException("error.invalid.status_transition", "Wrong study execution status.");
	
		controllers.research.Studies.deleteStudy(userId, study._id, false);
		
		return ok();
	}
	
	public static void createStats() throws AppException {
		InstanceStats stats = new InstanceStats();
		stats._id = new MidataId();
		stats.date = new Date();
		
		stats.recordCount = DBRecord.count();
		stats.vRecordCount = VersionedDBRecord.vcount();
		stats.indexPageCount = IndexPageModel.count();
		stats.appCount = Plugin.count();
		stats.runningStudyCount = Study.count(); 
		stats.groupCount = UserGroup.count();
		stats.auditEventCount = MidataAuditEvent.count();
		stats.userCount = new HashMap<String, Long>();
		for (UserRole role : UserRole.values()) {
			stats.userCount.put(role.toString(), User.count(role));
		}
		
		stats.languages = new HashMap<String, Long>();
		String langs[] = new String[] { "en", "de", "fr", "it" };
		for (String language : langs) {
			stats.languages.put(language, User.countLanguage(language));
		}
		
		stats.consentCount = new HashMap<String, Long>();
		for (ConsentType type : ConsentType.values()) {
			stats.consentCount.put(type.toString(), Consent.count(type));
		}
		
		stats.add();
		
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public static Result getStats() throws AppException {
		JsonNode json = request().body().asJson();					
		JsonValidation.validate(json, "properties");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id");
		
		properties = CMaps.map("date", CMaps.map("$gte", new Date(System.currentTimeMillis() - 1000l*60l*60l*24l*7l)));
		
		Set<InstanceStats> stats = InstanceStats.getAll(properties);		
		return ok(JsonOutput.toJson(stats, "InstanceStats", InstanceStats.ALL_FIELDS));
		
	}
	
	
}
