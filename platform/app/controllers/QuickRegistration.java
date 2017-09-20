package controllers;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import actions.MobileCall;
import controllers.members.HealthProvider;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Study;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.EventType;
import models.enums.Gender;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationInterest;
import models.enums.SubUserRole;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Result;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class QuickRegistration extends APIController {

	/**
	 * register a new MIDATA member and prepare the account for a study
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "password", "firstname", "lastname", "gender", "country", "app", "language");
		
		
		String appName = JsonValidation.getString(json, "app");
		String studyName = null;
		Study study = null;
		boolean confirmStudy =  JsonValidation.getBoolean(json, "confirmStudy");
		
		if (json.has("study")) { studyName = JsonValidation.getString(json, "study"); }
						
		Plugin app = Plugin.getByFilename(appName, Plugin.ALL_PUBLIC);
		if (app == null) throw new BadRequestException("error.invalid.appcode", "Unknown code for app.");
  
		if (app.unlockCode != null) {
			JsonValidation.validate(json, "unlockCode");
			String code = JsonValidation.getString(json, "unlockCode");
			if (!app.unlockCode.toUpperCase().equals(code.toUpperCase())) throw new JsonValidationException("error.invalid.unlock_code", "unlockCode", "invalid", "Invalid unlock code");
		}
		Set<UserFeature> requirements = InstanceConfig.getInstance().getInstanceType().defaultRequirementsOAuthLogin(UserRole.MEMBER);
		if (app.requirements != null) requirements.addAll(app.requirements);
		
		if (app.linkedStudy != null && app.mustParticipateInStudy && !confirmStudy) {
			throw new JsonValidationException("error.missing.study_accept", "confirmStudy", "mustaccept", "Study belonging to app must be accepted.");
		}
		if (app.linkedStudy != null && confirmStudy) {
			Set<UserFeature> studyReq = controllers.members.Studies.precheckRequestParticipation(null, app.linkedStudy);
			if (studyReq != null) requirements.addAll(studyReq);
		}
		
		if (studyName != null) {
			study = Study.getByCodeFromMember(studyName, Study.ALL);
						
			if (study == null) throw new BadRequestException("error.invalid.code", "Unknown code for study.");
			
			Set<UserFeature> studyReq = controllers.members.Studies.precheckRequestParticipation(null, study._id);
			if (studyReq != null) requirements.addAll(studyReq);
		}

		
		if (requirements != null 
				&& (requirements.contains(UserFeature.ADDRESS_ENTERED) || requirements.contains(UserFeature.ADDRESS_VERIFIED))) {
			JsonValidation.validate(json, "city", "zip", "country", "address1");	
		}
		
		if (requirements != null 
				&& (requirements.contains(UserFeature.PHONE_ENTERED) || requirements.contains(UserFeature.PHONE_VERIFIED))) {
			JsonValidation.validate(json, "mobile");	
		}
						
		String email = JsonValidation.getEMail(json, "email");
		String firstName = JsonValidation.getString(json, "firstname");
		String lastName = JsonValidation.getString(json, "lastname");
		String password = JsonValidation.getPassword(json, "password");
		String device = JsonValidation.getStringOrNull(json, "device");

		// check status
		if (Member.existsByEMail(email)) {
		  throw new BadRequestException("error.exists.user", "A user with this email address already exists.");
		}
		
		// create the user
		Member user = new Member();
		
		user.email = email;
		user.emailLC = email.toLowerCase();
		user.name = firstName + " " + lastName;
		
		user.password = Member.encrypt(password);
						
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.birthday = JsonValidation.getDate(json, "birthday");
		user.language = JsonValidation.getString(json, "language");
		user.ssn = JsonValidation.getString(json, "ssn");
		
        Application.registerSetDefaultFields(user);		
		
		if (study != null) {
		  user.subroles = EnumSet.of(SubUserRole.STUDYPARTICIPANT);
		} else {
		  user.subroles = EnumSet.of(SubUserRole.APPUSER);
		}
		
		user.initialApp = app._id;
		if (study != null) user.initialStudy = study._id;
									
		user.status = UserStatus.NEW;	
		
		user.agreedToTerms(app.termsOfUse, user.initialApp);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user, app._id);
		
		Application.registerCreateUser(user);
				
		Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, requirements);
		
		Circles.fetchExistingConsents(user._id, user.emailLC);
		Application.sendWelcomeMail(app._id, user);
		
		if (notok == null || notok.isEmpty()) {
		
			if (study != null) controllers.members.Studies.requestParticipation(user._id, study._id, user.initialApp);
			
			if (device != null) {
			   MobileAppInstance appInstance = MobileAPI.installApp(user._id, app._id, user, device, true, confirmStudy);
			}
					
			return Application.loginHelper(user);
		} else {
			return Application.loginHelperResult(user, notok);
		}
	}
	
	/**
	 * register a new MIDATA member from an app prepare the account for usage of that app
	 * @return status ok
	 * @throws AppException	
	 */
	/*@BodyParser.Of(BodyParser.Json.class)
	@MobileCall
	public static Result registerFromApp() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "password", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "birthday", "appname", "secret", "device", "language");
				
		String appName = JsonValidation.getString(json, "appname");
		String secret = JsonValidation.getString(json,  "secret");
		String phrase = JsonValidation.getString(json, "device");
				
		Plugin app = Plugin.getByFilename(appName, Sets.create("type", "name", "secret", "status", "defaultQuery"));
		if (app == null) throw new BadRequestException("error.invalid.appcode", "Unknown code for app.");
		
		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
		if (app.secret == null || !app.secret.equals(secret)) throw new BadRequestException("error.unknown.app", "Unknown app");
				
		String email = JsonValidation.getEMail(json, "email");
		String firstName = JsonValidation.getString(json, "firstname");
		String lastName = JsonValidation.getString(json, "lastname");
		String password = JsonValidation.getPassword(json, "password");

	
		if (Member.existsByEMail(email)) {
		  throw new BadRequestException("error.exists.user", "A user with this email address already exists.");
		}
		
		
		Member user = new Member();		
		user.email = email;
		user.emailLC = email.toLowerCase();
		user.name = firstName + " " + lastName;
		
		user.password = Member.encrypt(password);
		
		Application.registerSetDefaultFields(user);
		
		user.subroles = EnumSet.of(SubUserRole.APPUSER);
		
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.birthday = JsonValidation.getDate(json, "birthday");
		user.language = JsonValidation.getString(json, "language");
		user.ssn = JsonValidation.getString(json, "ssn");
		
		user.initialApp = app._id;		
						
		user.status = UserStatus.NEW;		
		Application.registerCreateUser(user);								
		Application.sendWelcomeMail(app._id,user);
		Circles.fetchExistingConsents(user._id, user.emailLC);
		
		MobileAppInstance appInstance = MobileAPI.installApp(user._id, app._id, user, phrase, true, false);		
		appInstance.status = ConsentStatus.ACTIVE;
		
		
		Map<String, Object> meta = RecordManager.instance.getMeta(user._id, appInstance._id, "_app").toMap();			
		
		return MobileAPI.authResult(user._id, appInstance, meta, phrase);		
	}*/
}
