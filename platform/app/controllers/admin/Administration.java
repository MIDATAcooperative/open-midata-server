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

package controllers.admin;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import controllers.APIController;
import controllers.Application;
import controllers.Users;
import models.AccessPermissionSet;
import models.Admin;
import models.Circle;
import models.Consent;
import models.Developer;
import models.HealthcareProvider;
import models.KeyInfoExtern;
import models.KeyRecoveryData;
import models.KeyRecoveryProcess;
import models.Member;
import models.MidataAuditEvent;
import models.MidataId;
import models.Plugin;
import models.Space;
import models.Study;
import models.StudyParticipation;
import models.SubscriptionData;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.MessageReason;
import models.enums.ParticipationStatus;
import models.enums.SecondaryAuthType;
import models.enums.StudyExecutionStatus;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.stats.InstanceStats;
import models.stats.UsageStats;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.AccessContext;
import utils.access.DBRecord;
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.access.index.IndexPageModel;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.FutureLogin;
import utils.auth.KeyManager;
import utils.auth.PasswordResetToken;
import utils.auth.PortalSessionToken;
import utils.auth.PreLoginSecured;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
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
import utils.messaging.ServiceHandler;
import utils.stats.UsageStatsRecorder;
import utils.sync.Instances;

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
	public Result changeStatus(Request request) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "user", "status");
				
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));		
		MidataId userId = JsonValidation.getMidataId(json, "user");
		UserStatus status = JsonValidation.getEnum(json, "status", UserStatus.class);
		
		User user = User.getByIdAlsoDeleted(userId, User.ALL_USER_INTERNAL); //Sets.create("status", "contractStatus", "agbStatus", "subroles", "confirmedAt", "emailStatus"));
		
		
		
		if (user == null) throw new BadRequestException("error.unknown.user", "Unknown user");
		
		UserStatus oldstatus = user.status;
		
		//if (status == UserStatus.PRECREATED && oldstatus != UserStatus.PRECREATED) throw new BadRequestException("error.invalid.status_transition", "Invalid status change");
		
		user.status = status;
		User.set(user._id, "status", user.status);
		
		if (user.status.equals(UserStatus.ACTIVE) && !oldstatus.equals(UserStatus.ACTIVE)) {			
			Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.ACCOUNT_UNLOCK, null, Collections.singleton(user._id), null, new HashMap<String, String>());			
		}
		
		if (user.status != oldstatus && user.status == UserStatus.ACTIVE) {
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_STATUS_CHANGE_ACTIVE, null, executorId, user);
		}
		
		if (user.status != oldstatus && user.status == UserStatus.BLOCKED) {
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_STATUS_CHANGE_BLOCKED, null, executorId, user);
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
		
		if (json.has("authType")) {
			SecondaryAuthType old = user.authType;
			user.authType = JsonValidation.getEnum(json, "authType", SecondaryAuthType.class);
			User.set(user._id, "authType", user.authType);
			if (old != user.authType) {
				if (old == null) old = SecondaryAuthType.NONE;
				AuditManager.instance.addAuditEvent(AuditEventType.USER_ACCOUNT_CHANGE_BY_ADMIN, null, executorId, user, "2FA type "+old.toString()+" to "+user.authType.toString());
			}
		}
		
		if (json.has("subroles") && !executorId.equals(user._id)) {
			Set<SubUserRole> roles = JsonValidation.getEnumSet(json, "subroles", SubUserRole.class);
			user.subroles = roles;
			User.set(user._id, "subroles", user.subroles);
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
	public Result register(Request request) throws AppException {
		requireSubUserRole(request, SubUserRole.SUPERADMIN);
		
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "country", "language", "subroles");
							
		String email = JsonValidation.getEMail(json, "email");
		
		User existing = Developer.getByEmail(email, User.ALL_USER);		
				
		Admin executingUser = Admin.getById(executorId, User.ALL_USER);
						
		Admin user;
		if (existing != null) {
			user = Admin.getById(existing._id, User.ALL_USER_INTERNAL);
			AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request.attrs().get(play.mvc.Security.USERNAME)), user);
			user.role = UserRole.ADMIN;
			user.subroles = JsonValidation.getEnumSet(json, "subroles", SubUserRole.class);
			if (user.authType == SecondaryAuthType.NONE) {
				user.authType = null;
				user.set("authType", user.authType);
			}
			user.set("role", user.role);
			user.set("subroles", user.subroles);
			AuditManager.instance.success();
			
			ObjectNode obj = Json.newObject();
			obj.put("_id", user._id.toString());			
			return ok(obj);
		} else {
			user = new Admin(email);
			user._id = new MidataId();
			user.role = UserRole.ADMIN;		
			user.subroles = JsonValidation.getEnumSet(json, "subroles", SubUserRole.class);
			//user.address1 = JsonValidation.getString(json, "address1");
			//user.address2 = JsonValidation.getString(json, "address2");
			//user.city = JsonValidation.getString(json, "city");
			//user.zip  = JsonValidation.getString(json, "zip");
			user.country = JsonValidation.getString(json, "country");
			user.firstname = JsonValidation.getString(json, "firstname"); 
			user.lastname = JsonValidation.getString(json, "lastname");
			user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
			user.language = JsonValidation.getString(json, "language");
			//user.phone = JsonValidation.getString(json, "phone");
			user.mobile = JsonValidation.getString(json, "mobile");
			
			//user.password = Admin.encrypt(JsonValidation.getPassword(json, "password"));		
			user.registeredAt = new Date();		
			
			user.status = UserStatus.ACTIVE;		
			user.contractStatus = ContractStatus.REQUESTED;	
			user.agbStatus = ContractStatus.REQUESTED;
			user.emailStatus = EMailStatus.UNVALIDATED;
			user.confirmationCode = CodeGenerator.nextCode();
			if (user.mobile != null && user.mobile.length() > 0) user.authType = SecondaryAuthType.SMS;
			
			AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, null, new MidataId(request.attrs().get(play.mvc.Security.USERNAME)), user);
			
			user.apps = new HashSet<MidataId>();		
			user.visualizations = new HashSet<MidataId>();
			
			user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
			user.security = AccountSecurityLevel.KEY;
					
			Admin.add(user);
							
			RecordManager.instance.createPrivateAPS(context.getCache(), user._id, user._id);						
			//PatientResourceProvider.updatePatientForAccount(user._id);
			
			Application.sendWelcomeMail(user, executingUser);
				
			AuditManager.instance.success();
			ObjectNode obj = Json.newObject();
			obj.put("_id", user._id.toString());			
			return ok(obj);	
		}
	}
	
	/**
	 * add a comment for a user
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result addComment(Request request) throws AppException {
		requireSubUserRole(request, SubUserRole.USERADMIN);
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "user", "comment");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");
		String comment = JsonValidation.getString(json, "comment");
		
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));				
		
		User targetUser = User.getByIdAlsoDeleted(userId, User.ALL_USER);
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
	public Result changeUserEmail(Request request) throws AppException {
		
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "user", "email");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");
		String email = JsonValidation.getEMail(json, "email");
		
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
		//Check authorization except for change self
		if (!executorId.equals(userId)) {
		  requireSubUserRole(request, SubUserRole.USERADMIN);
		}
		
		User targetUser = User.getByIdAlsoDeleted(userId, User.ALL_USER);
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
	@Security.Authenticated(PreLoginSecured.class)
	public Result changeBirthday(Request request) throws AppException {	
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "user", "birthday");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");			
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
		//Check authorization except for change self
		if (!executorId.equals(userId)) {
		  requireSubUserRole(request, SubUserRole.USERADMIN);
		}				
				
		Member user = Member.getById(userId, Sets.create("_id", "birthday", "firstname", "lastname", "email", "role", "flags")); 
				
		if (!PortalSessionToken.session().is2FAVerified(user)) {
		   throw new InternalServerException("error.internal", "birthday change tried without verification");	  
		}
		
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
	public Result adminWipeAccount(Request request) throws JsonValidationException, AppException {
		requireSubUserRole(request, SubUserRole.USERADMIN);
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "user");
							
		MidataId userId = JsonValidation.getMidataId(json, "user");
		MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
		User selected = User.getByIdAlsoDeleted(userId, User.ALL_USER);
		if (!selected.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.invalid.status",  "User must have status deleted to be wiped.");
		
		AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.USER_ACCOUNT_DELETED).withActorUser(executorId).withModifiedUser(selected));
		
		Users.doAccountWipe(portalContext(request), userId);
		
		AuditManager.instance.success();
		
			/*if (!User.exists(CMaps.map("organization", PortalSessionToken.session().org))) {
			  Research.delete(PortalSessionToken.session().org);			
		}*/
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result searchAuditLog(Request request) throws JsonValidationException, AppException {
		JsonNode json = request.body().asJson();					
		JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));		
		
		Rights.chk("MidataAuditEvent.search", getRole(), properties, fields);
		
		List<MidataAuditEvent> events = null;
	    //properties.put("noAdminView", CMaps.map("$ne", true));
		
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
	public Result deleteStudy(Request request, String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId owner = PortalSessionToken.session().getOrgId();
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
	public Result getStats(Request request) throws AppException {
		JsonNode json = request.body().asJson();					
		JsonValidation.validate(json, "properties");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id");
		
		properties = CMaps.map("date", CMaps.map("$gte", new Date(System.currentTimeMillis() - 1000l*60l*60l*24l*7l)));
		
		Set<InstanceStats> stats = InstanceStats.getAll(properties);		
		return ok(JsonOutput.toJson(stats, "InstanceStats", InstanceStats.ALL_FIELDS));
		
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result getUsageStats(Request request) throws AppException {
		JsonNode json = request.body().asJson();					
		JsonValidation.validate(json, "properties");
		
		UsageStatsRecorder.flush();
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id", "object");
						
		Set<UsageStats> stats = UsageStats.getAll(properties);		
		return ok(JsonOutput.toJson(stats, "UsageStats", UsageStats.ALL));		
	}
		
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result getSystemHealth() throws AppException {
		ObjectNode obj = Json.newObject();
		
		ArrayNode memberinfo = Json.newArray();
		CurrentClusterState state = Cluster.get(Instances.system()).state();
		for (akka.cluster.Member member : state.getMembers()) {
			ObjectNode info = Json.newObject();
			info.put("address", member.address().toString());
			info.put("status", member.status().toString());
			memberinfo.add(info);
		}
		
		obj.put("servicekey", ServiceHandler.keyAvailable());
		obj.set("cluster", memberinfo);
		return ok(obj);
	}
	
}
