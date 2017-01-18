package controllers;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Developer;
import models.HPUser;
import models.Member;
import models.MidataId;
import models.ResearchUser;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.ParticipationInterest;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.Routes;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.auth.PasswordResetToken;
import utils.auth.PortalSessionToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.evolution.AccountPatches;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.mails.MailUtils;
import views.html.apstest;
import views.html.tester;
import views.txt.mails.lostpwmail;
import views.txt.mails.welcome;
import views.txt.mails.adminnotify;

/**
 * Member login, registration and password reset functions 
 *
 */
public class Application extends APIController {

	public final static long MAX_TIME_UNTIL_EMAIL_CONFIRMATION = 1000l * 60l * 60l * 24l;
	public final static long MAX_TRIAL_DURATION = 1000l * 60l * 60l * 24l * 30l;
	/**
	 * for debugging only : displays API call test page
	 * @return
	 */
	public static Result test() {
		return ok(tester.render());
	}

	/**
	 * for debugging : displays APS viewer
	 * @return
	 */
	public static Result test2() {
		return ok(apstest.render());
	} 
			
	/**
	 * Handling of OPTIONS requests
	 * @param all dummy parameter
	 * @return status ok
	 */
	@APICall
	public static Result checkPreflight(String all) {				
		return ok();
	}

	/**
	 * request sending a password reset token by email
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class) 
	@APICall
	public static Result requestPasswordResetToken() throws JsonValidationException, InternalServerException {
		// validate input
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "role");				
		String email = JsonValidation.getEMail(json, "email");
		String role = JsonValidation.getString(json, "role");
		
		// execute
		User user = null;
		switch (role) {
		case "member" : user = Member.getByEmail(email, Sets.create("firstname", "lastname","email","password"));break;
		case "research" : user = ResearchUser.getByEmail(email, Sets.create("firstname", "lastname","email","password"));break;
		case "provider" : user = HPUser.getByEmail(email, Sets.create("firstname", "lastname","email","password"));break;
		case "developer" : user = Developer.getByEmail(email, Sets.create("firstname", "lastname","email","password"));break;
		default: break;		
		}
		if (user != null) {							  
		  PasswordResetToken token = new PasswordResetToken(user._id, role);
		  user.set("resettoken", token.token);
		  user.set("resettokenTs", System.currentTimeMillis());
		  String encrypted = token.encrypt();
			   
		  String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		  String url = site + "/#/portal/setpw?token=" + encrypted;
			   
		  MailUtils.sendTextMail(email, user.firstname+" "+user.lastname, "Your Password", lostpwmail.render(site,url));
		}
			
		// response
		return ok();
	}
	
	/**
	 * request sending the welcome mail
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result requestWelcomeMail() throws JsonValidationException, InternalServerException {
		
		// execute
		MidataId userId = new MidataId(request().username());
		User user = User.getById(userId, Sets.create("firstname", "lastname", "email", "emailStatus", "status", "role"));
		
		if (user != null && user.emailStatus.equals(EMailStatus.UNVALIDATED)) {							  
		   sendWelcomeMail(user);
		}
			
		// response
		return ok();
	}
	
	/**
	 * Helper function to send welcome mail
	 * @param user user record which sould receive the mail
	 */
	public static void sendWelcomeMail(User user) throws InternalServerException {
	   if (user.developer == null) {
		   PasswordResetToken token = new PasswordResetToken(user._id, user.role.toString());
		   user.set("resettoken", token.token);
		   user.set("resettokenTs", System.currentTimeMillis());
		   String encrypted = token.encrypt();
				   
		   String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		   String url1 = site + "/#/portal/confirm/" + encrypted;
		   String url2 = site + "/#/portal/reject/" + encrypted;
		   AccessLog.log("send welcome mail: "+user.email);	   
	  	   MailUtils.sendTextMail(user.email, user.firstname+" "+user.lastname, "Welcome to MIDATA", welcome.render(site, url1, url2));
	   } else {
		   user.emailStatus = EMailStatus.VALIDATED;
		   User.set(user._id, "emailStatus", user.emailStatus);
	   }
	}
	
	/**
	 * Helper function to notification mail to admin
	 * @param user new user record
	 */
	public static void sendAdminNotificationMail(User user) throws InternalServerException {
	   if (user.status == UserStatus.NEW) {		   
		   String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		   String email = user.email;
		   String role = user.role.toString();
		   
		   AccessLog.log("send admin notification mail: "+user.email);	   
	  	   MailUtils.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), user.firstname+" "+user.lastname, "New MIDATA User", adminnotify.render(site, email, role));
	   }
	}
	
	/**
	 * confirms a email account for a new MIDATA user
	 * @return status ok
	 * @throws AppException	 
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result confirmAccountEmail() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "token" ,"mode");
		EMailStatus wanted = JsonValidation.getEnum(json, "mode", EMailStatus.class);
		
		// check status
		PasswordResetToken passwordResetToken = PasswordResetToken.decrypt(json.get("token").asText());
		if (passwordResetToken == null) throw new BadRequestException("error.missing.token", "Missing or bad token.");
		
		// execute
		MidataId userId = passwordResetToken.userId;
		String token = passwordResetToken.token;
		String role = passwordResetToken.role;		
		
		User user = User.getById(userId, Sets.create("status", "role", "subroles", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "resettoken","password","resettokenTs", "registeredAt", "confirmedAt", "developer"));
		
		if (user!=null && !user.emailStatus.equals(EMailStatus.VALIDATED)) {							
		       if (user.resettoken != null 		    		    
		    		   && user.resettoken.equals(token)
		    		   && System.currentTimeMillis() - user.resettokenTs < 1000 * 60 * 15) {	   
			   
		           user.set("resettoken", null);	
		           user.emailStatus = wanted;
			       user.set("emailStatus", wanted);			       
		       } else throw new BadRequestException("error.expired.token", "Token has already expired. Please request a new one.");
		       
		       checkAccount(user);
		       
		       return loginHelper(user);
		} else if (user != null) {
			throw new BadRequestException("error.already_done.email_verification", "E-Mail has already been verified.");
		}
					
		// response
		return ok();		
	}
	
	/**
	 * confirms a postal address for a new MIDATA user and activates the account
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result confirmAccountAddress() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "confirmationCode");
				
		MidataId userId = new MidataId(request().username());
		String confirmationCode = JsonValidation.getString(json, "confirmationCode");
		
		User user = User.getById(userId, Sets.create("firstname", "lastname", "email", "confirmationCode", "emailStatus", "contractStatus", "agbStatus", "status", "role", "subroles", "registeredAt", "confirmedAt", "developer"));
		
		
		if (user!=null && user.confirmationCode != null && user.confirmedAt == null) {
			/*if (user.role.equals(UserRole.PROVIDER)) {
				user = HPUser.getById(userId, Sets.create("firstname", "lastname", "email", "confirmationCode", "emailStatus", "contractStatus", "agbStatus", "status", "role", "subroles", "provider"));
			} else if (user.role.equals(UserRole.RESEARCH)) {
				user = ResearchUser.getById(userId, Sets.create("firstname", "lastname", "email", "confirmationCode", "emailStatus", "contractStatus", "agbStatus", "status", "role", "subroles", "organization"));
			}*/
			
		
	       if (user.confirmationCode.equals(confirmationCode)) {
	    	   user.confirmedAt = new Date(System.currentTimeMillis());
	    	   user.set("confirmedAt", user.confirmedAt);	    	   			           			       
	       } else throw new BadRequestException("error.invalid.confirmation_code", "Bad confirmation code");
		}
		
		checkAccount(user);
					
		// response
		return loginHelper(user);		
	}
	
	public static void checkAccount(User user) throws AppException {
		if (user.subroles.contains(SubUserRole.TRIALUSER) && 
			user.emailStatus.equals(EMailStatus.VALIDATED) &&
			user.agbStatus.equals(ContractStatus.SIGNED) &&
			(user.confirmedAt != null || !InstanceConfig.getInstance().getInstanceType().confirmationCodeRequired())) {
			user.subroles.remove(SubUserRole.TRIALUSER);
			user.subroles.add(SubUserRole.NONMEMBERUSER);
			user.set("subroles", user.subroles);
		}
		
		if (user.subroles.contains(SubUserRole.STUDYPARTICIPANT) && 
			user.emailStatus.equals(EMailStatus.VALIDATED) &&
			user.agbStatus.equals(ContractStatus.SIGNED) &&
			(user.confirmedAt != null || !InstanceConfig.getInstance().getInstanceType().confirmationCodeRequired())) {
			user.subroles.remove(SubUserRole.STUDYPARTICIPANT);
			user.subroles.add(SubUserRole.NONMEMBERUSER);
			user.set("subroles", user.subroles);
		}
		
		if (user.subroles.contains(SubUserRole.APPUSER) && 
				user.emailStatus.equals(EMailStatus.VALIDATED) &&
				user.agbStatus.equals(ContractStatus.SIGNED) &&
				(user.confirmedAt != null || !InstanceConfig.getInstance().getInstanceType().confirmationCodeRequired())) {
				user.subroles.remove(SubUserRole.APPUSER);
				user.subroles.add(SubUserRole.NONMEMBERUSER);
				user.set("subroles", user.subroles);
		}
		
		if (user.subroles.contains(SubUserRole.NONMEMBERUSER) &&
			user.contractStatus.equals(ContractStatus.SIGNED)) {
			user.subroles.remove(SubUserRole.NONMEMBERUSER);
			user.subroles.add(SubUserRole.MEMBEROFCOOPERATIVE);
			user.set("subroles", user.subroles);
		}
		
		if (user.status.equals(UserStatus.NEW) && 
			user.emailStatus.equals(EMailStatus.VALIDATED) &&
			user.agbStatus.equals(ContractStatus.SIGNED)) {
			user.status = UserStatus.ACTIVE;
			user.set("status", user.status);
		}
				
	}
	


	/**
	 * set a new password for a user account by using a password reset token
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result setPasswordWithToken() throws JsonValidationException, AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "token", "password");
		
		// check status
		PasswordResetToken passwordResetToken = PasswordResetToken.decrypt(json.get("token").asText());
		if (passwordResetToken == null) throw new BadRequestException("error.missing.token", "Missing or bad password token.");
		
		// execute
		MidataId userId = passwordResetToken.userId;
		String token = passwordResetToken.token;
		String role = passwordResetToken.role;
		String password = JsonValidation.getPassword(json, "password");
		
		User user = null;
		switch (role) {
		case "member" : user = Member.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
		case "research" : user = ResearchUser.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
		case "provider" : user = HPUser.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
		case "developer" : user = Developer.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
		default: break;		
		}
		if (user!=null) {				
				
		       if (user.resettoken != null 		    		    
		    		   && user.resettoken.equals(token)
		    		   && System.currentTimeMillis() - user.resettokenTs < 1000 * 60 * 15) {	   
			   
		           user.set("resettoken", null);		       
			       user.set("password", Member.encrypt(password));
		       } else throw new BadRequestException("error.expired.token", "Password reset token has already expired.");
		}
					
		// response
		return ok();		
	}
	
	/**
	 * set a new password for a user account by providing the old password
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result changePassword() throws JsonValidationException, AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "oldPassword", "password");
		MidataId userId = new MidataId(request().username());
		
		String oldPassword = JsonValidation.getString(json, "oldPassword");
		String password = JsonValidation.getPassword(json, "password");
		
		User user = User.getById(userId, Sets.create("password"));
		if (!Member.authenticationValid(oldPassword, user.password)) throw new BadRequestException("error.invalid.password_old","Bad password.");
		
		user.set("password", Member.encrypt(password));
		       			
		// response
		return ok();		
	}
	
	/**
	 * sets or changes a passphrase for a user
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result changePassphrase() throws AppException {
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "oldPassphrase", "passphrase");
		MidataId userId = new MidataId(request().username());
		
		String oldPassphrase = JsonValidation.getStringOrNull(json, "oldPassphrase");
		String passphrase = JsonValidation.getPassword(json, "passphrase");
		
		KeyManager.instance.unlock(userId, oldPassphrase);
		
		// This is a dummy query to check if provided passphrase works
		try {
		  RecordManager.instance.list(userId, userId, CMaps.map("format","zzzzzz"), Sets.create("name"));
		} catch (InternalServerException e) { throw new BadRequestException("error.passphrase_old", "Old passphrase not correct."); }
		
		KeyManager.instance.changePassphrase(userId, passphrase);
		
		return ok();
	}
	
	
	
	/**
	 * login function for MIDATA members
	 * @return status ok
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public static Result authenticate() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "password");	
		String email = JsonValidation.getEMail(json, "email");
		String password = JsonValidation.getString(json, "password");
		
		// check status
		Member user = Member.getByEmail(email , Sets.create("password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer"));
		if (user == null) throw new BadRequestException("error.invalid.credentials",  "Invalid user or password.");
		if (!Member.authenticationValid(password, user.password)) {
			throw new BadRequestException("error.invalid.credentials",  "Invalid user or password.");
		}
			 
		return loginHelper(user);
	
	}
	
	/**
	 * Helper function for all login / registration type functions.
	 * Returns correct response to login request
	 * @param user the user to be logged in. May have any role.
	 * @return
	 * @throws AppException
	 */
	public static Result loginHelper(User user) throws AppException {
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");
		
		if (user.emailStatus.equals(EMailStatus.UNVALIDATED) && user.registeredAt.before(new Date(System.currentTimeMillis() - MAX_TIME_UNTIL_EMAIL_CONFIRMATION))) {
			user.status = UserStatus.TIMEOUT;			
		}
		
		Date endTrial = new Date(System.currentTimeMillis() - MAX_TRIAL_DURATION);
		if (user.subroles.contains(SubUserRole.TRIALUSER) && user.registeredAt.before(endTrial)) {
			if (user.agbStatus.equals(ContractStatus.NEW)) {
				Users.requestMembershipHelper(user._id);				
			}
						
			user.status = UserStatus.TIMEOUT;
		}
		if (user.status.equals(UserStatus.NEW)&&  user.subroles.contains(SubUserRole.TRIALUSER) && !InstanceConfig.getInstance().getInstanceType().getTrialAccountsMayLogin()) {
			user.status = UserStatus.TIMEOUT;
		}
		
		PortalSessionToken token = null;
		String handle = KeyManager.instance.login(PortalSessionToken.LIFETIME);
		
		if (user instanceof HPUser) {
		   token = new PortalSessionToken(handle, user._id, user.role, ((HPUser) user).provider, user.developer);		  
		} else if (user instanceof ResearchUser) {
		   token = new PortalSessionToken(handle, user._id, user.role, ((ResearchUser) user).organization, user.developer);		  
		} else {
		   token = new PortalSessionToken(handle, user._id, user.role, null, user.developer);
		}
		
		ObjectNode obj = Json.newObject();
		obj.put("sessionToken", token.encrypt(request()));
		
		if (user.status.equals(UserStatus.TIMEOUT) || (!user.status.equals(UserStatus.ACTIVE) && InstanceConfig.getInstance().getInstanceType().getUsersNeedValidation())) {
		  obj.put("status", user.status.toString());
		  obj.put("contractStatus", user.contractStatus.toString());
		  obj.put("agbStatus", user.agbStatus.toString());
		  obj.put("emailStatus", user.emailStatus.toString());
		  obj.put("confirmationCode", user.confirmationCode == null);
		} else {						
		  int keytype = KeyManager.instance.unlock(user._id, null);		
		  if (keytype == 0) AccountPatches.check(user);
				
		  obj.put("keyType", keytype);
		  obj.put("role", user.role.toString().toLowerCase());
		  obj.put("subroles", Json.toJson(user.subroles));
		  obj.put("lastLogin", Json.toJson(user.login));
		}
	    User.set(user._id, "login", new Date());
		return ok(obj);
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result downloadToken() throws AppException {
		PortalSessionToken current = PortalSessionToken.session();
		PortalSessionToken token = new PortalSessionToken(current.getHandle(), current.getUserId(), current.getRole(), current.getOrg(), current.getDeveloper());
		
		ObjectNode obj = Json.newObject();
		obj.put("token", token.encrypt(request(), 1000 * 10));
		
		return ok(obj);
	}
	
	/**
	 * provide passphrase
	 * @return status ok
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result providePassphrase() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "passphrase");
		MidataId userId = new MidataId(request().username());
		
		String passphrase = JsonValidation.getString(json, "passphrase");
		
		KeyManager.instance.unlock(userId, passphrase);
		
		try {
		  RecordManager.instance.list(userId, userId, CMaps.map("format","zzzzzzz"), Sets.create("name"));
		} catch (InternalServerException e) {
		  throw new BadRequestException("error.invalid.passphrase", "Bad Passphrase");
		}
					
		User user = User.getById(userId , Sets.create("status", "accountVersion"));
		AccountPatches.check(user);
		
		// response
		return ok();
	}

	/**
	 * register a new MIDATA member
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1", "language");
		String email = JsonValidation.getEMail(json, "email");
		String firstName = JsonValidation.getString(json, "firstname");
		String lastName = JsonValidation.getString(json, "lastname");
		String password = JsonValidation.getPassword(json, "password");

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
		user.subroles = EnumSet.of(SubUserRole.TRIALUSER);		
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
		
		registerSetDefaultFields(user);				
		developerRegisteredAccountCheck(user, json);		
		registerCreateUser(user);		
		
		sendWelcomeMail(user);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) sendAdminNotificationMail(user);
		
		return loginHelper(user);		
	}
	
	/**
	 * Sets fields for a new account holder user account
	 * @param user the new user
	 * @throws AppException
	 */
	public static void registerSetDefaultFields(Member user) throws AppException {
		user._id = new MidataId();
		do {
			  user.midataID = CodeGenerator.nextUniqueCode();
		} while (Member.existsByMidataID(user.midataID));
		
		user.role = UserRole.MEMBER;
		user.registeredAt = new Date();
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;	
		user.agbStatus = ContractStatus.NEW;
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		user.partInterest = ParticipationInterest.UNSET;
							
		user.apps = new HashSet<MidataId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<MidataId>();
		
		user.login = new Date();
		user.news = new HashSet<MidataId>();
	}
	
	/**
	 * actually creates a new user and creates the corresponding APS and key
	 * @param user the new user
	 * @throws AppException
	 */
	public static void registerCreateUser(Member user) throws AppException {
		user.security = AccountSecurityLevel.KEY;		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);								
		Member.add(user);
		KeyManager.instance.login(60000);
		KeyManager.instance.unlock(user._id, null);
		
		user.myaps = RecordManager.instance.createPrivateAPS(user._id, user._id);
		Member.set(user._id, "myaps", user.myaps);
		
		PatientResourceProvider.updatePatientForAccount(user._id);
	}
	
	/**
	 * checks for a new user if it has been registered by a developer and skips validation if allowed
	 * @param newuser the new user
	 * @param json the json parameters from the registration request
	 * @throws JsonValidationException
	 * @throws AuthException
	 */
	public static void developerRegisteredAccountCheck(User newuser, JsonNode json) throws JsonValidationException, AuthException {
		if (InstanceConfig.getInstance().getInstanceType().developersMayRegisterTestUsers()) {		  
		   newuser.developer = JsonValidation.getMidataId(json, "developer");
		   if (newuser.developer != null) {
		     if (!newuser.developer.equals(PortalSessionToken.decrypt(request()).userId)) throw new AuthException("error.internal", "You need to be logged in as this developer");
		     newuser.status = UserStatus.ACTIVE;
		   }
		}
	}

	/**
	 * logout current user
	 * @return status ok
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result logout() {
		
		// execute
		KeyManager.instance.logout();
		
		// reponse
		return ok();		
	}
	
	/**
	 * retrieves a list of URLs to all functions used by the MIDATA portal
	 * @return javascript file with URL routes
	 */
	@APICall
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter(
				"jsRoutes",
				// Application
				
				controllers.routes.javascript.Application.authenticate(),
				controllers.routes.javascript.Application.register(),
				controllers.routes.javascript.QuickRegistration.register(),
				controllers.routes.javascript.Application.requestPasswordResetToken(),
				controllers.routes.javascript.Application.setPasswordWithToken(),
				controllers.routes.javascript.Application.changePassword(),
				controllers.routes.javascript.Application.changePassphrase(),
				controllers.routes.javascript.Application.providePassphrase(),
				controllers.routes.javascript.Application.confirmAccountEmail(),
				controllers.routes.javascript.Application.confirmAccountAddress(),
				controllers.routes.javascript.Application.requestWelcomeMail(),
				controllers.routes.javascript.Application.downloadToken(),
				
				// Apps										
				controllers.routes.javascript.Plugins.getUrlForConsent(),
				controllers.routes.javascript.Plugins.requestAccessTokenOAuth2(),
				controllers.routes.javascript.Plugins.getRequestTokenOAuth1(),
				controllers.routes.javascript.Plugins.requestAccessTokenOAuth1(),
				// Visualizations				
				controllers.routes.javascript.Plugins.get(),
				controllers.routes.javascript.Plugins.getInfo(),
				controllers.routes.javascript.Plugins.install(),
				controllers.routes.javascript.Plugins.uninstall(),
				controllers.routes.javascript.Plugins.isInstalled(),
				controllers.routes.javascript.Plugins.isAuthorized(),
				controllers.routes.javascript.Plugins.getUrl(),
				
				// News
				controllers.routes.javascript.News.get(),
				controllers.routes.javascript.News.add(),
				controllers.routes.javascript.News.delete(),
				controllers.routes.javascript.News.update(),
				
				controllers.routes.javascript.FormatAPI.listGroups(),
				controllers.routes.javascript.FormatAPI.updateGroup(),
				controllers.routes.javascript.FormatAPI.deleteGroup(),
				controllers.routes.javascript.FormatAPI.createGroup(),
				controllers.routes.javascript.FormatAPI.listFormats(),				
				controllers.routes.javascript.FormatAPI.listContents(),
				controllers.routes.javascript.FormatAPI.updateContent(),
				controllers.routes.javascript.FormatAPI.deleteContent(),
				controllers.routes.javascript.FormatAPI.createContent(),
				controllers.routes.javascript.FormatAPI.listCodes(),
				controllers.routes.javascript.FormatAPI.updateCode(),
				controllers.routes.javascript.FormatAPI.deleteCode(),
				controllers.routes.javascript.FormatAPI.createCode(),
				
				// Records					
				controllers.routes.javascript.Records.get(),				
				controllers.routes.javascript.Records.getRecords(),
				controllers.routes.javascript.Records.getInfo(),	
				controllers.routes.javascript.Records.getSharingDetails(),			
				controllers.routes.javascript.Records.updateSharing(),
				controllers.routes.javascript.Records.share(),	
				controllers.routes.javascript.Records.getFile(),
				controllers.routes.javascript.Records.getRecordUrl(),
				controllers.routes.javascript.Records.delete(),
				controllers.routes.javascript.Records.fixAccount(),
				// Circles
				controllers.routes.javascript.Circles.get(),
				controllers.routes.javascript.Circles.add(),
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.addUsers(),
				controllers.routes.javascript.Circles.removeMember(),
				controllers.routes.javascript.Circles.listConsents(),
				controllers.routes.javascript.Circles.joinByPasscode(),
				// Spaces
				controllers.routes.javascript.Spaces.get(),
				controllers.routes.javascript.Spaces.add(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.addRecords(),
				controllers.routes.javascript.Spaces.getUrl(),
				controllers.routes.javascript.Spaces.regetUrl(),	
				// Users
				controllers.routes.javascript.Users.get(),		
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				controllers.routes.javascript.Users.loadContacts(),
				controllers.routes.javascript.Users.complete(),
				controllers.routes.javascript.Users.updateSettings(),
				controllers.routes.javascript.Users.updateAddress(),
				controllers.routes.javascript.Users.requestMembership(),
				
				controllers.routes.javascript.Tasking.add(),
				controllers.routes.javascript.Tasking.list(),
				controllers.routes.javascript.Tasking.execute(),
				
				//Research
				controllers.research.routes.javascript.Researchers.register(),
				controllers.research.routes.javascript.Researchers.login(),
				controllers.research.routes.javascript.Studies.create(),
				controllers.research.routes.javascript.Studies.list(),
				controllers.research.routes.javascript.Studies.listAdmin(),
				controllers.research.routes.javascript.Studies.get(),
				controllers.research.routes.javascript.Studies.getAdmin(),
				controllers.research.routes.javascript.Studies.update(),
				controllers.research.routes.javascript.Studies.updateParticipation(),
				controllers.research.routes.javascript.Studies.download(),
				controllers.research.routes.javascript.Studies.listCodes(),
				controllers.research.routes.javascript.Studies.generateCodes(),
				controllers.research.routes.javascript.Studies.startValidation(),
				controllers.research.routes.javascript.Studies.endValidation(),
				controllers.research.routes.javascript.Studies.startParticipantSearch(),
				controllers.research.routes.javascript.Studies.endParticipantSearch(),
				controllers.research.routes.javascript.Studies.startExecution(),
				controllers.research.routes.javascript.Studies.finishExecution(),
				controllers.research.routes.javascript.Studies.abortExecution(),
				controllers.research.routes.javascript.Studies.delete(),
				controllers.research.routes.javascript.Studies.listParticipants(),
				controllers.research.routes.javascript.Studies.getParticipant(),
				controllers.research.routes.javascript.Studies.approveParticipation(),
				controllers.research.routes.javascript.Studies.rejectParticipation(),
				controllers.research.routes.javascript.Studies.shareWithGroup(),
				controllers.research.routes.javascript.Studies.addTask(),
				controllers.research.routes.javascript.Studies.getRequiredInformationSetup(),
				controllers.research.routes.javascript.Studies.setRequiredInformationSetup(),
				
				controllers.members.routes.javascript.Studies.list(),
				controllers.routes.javascript.Studies.search(),
				controllers.members.routes.javascript.Studies.enterCode(),
				controllers.members.routes.javascript.Studies.get(),
				controllers.members.routes.javascript.Studies.requestParticipation(),
				controllers.members.routes.javascript.Studies.updateParticipation(),
				controllers.members.routes.javascript.Studies.noParticipation(),
				controllers.members.routes.javascript.HealthProvider.list(),
				controllers.members.routes.javascript.HealthProvider.search(),
				controllers.members.routes.javascript.HealthProvider.confirmConsent(),
				controllers.members.routes.javascript.HealthProvider.rejectConsent(),
				//Healthcare Providers
				controllers.providers.routes.javascript.Providers.register(),
				controllers.providers.routes.javascript.Providers.login(),
				controllers.providers.routes.javascript.Providers.search(),
				controllers.providers.routes.javascript.Providers.list(),
				controllers.providers.routes.javascript.Providers.getMember(),
				controllers.providers.routes.javascript.Providers.getVisualizationToken(),
				
				
				
				// Developers
				controllers.routes.javascript.Developers.register(),
				controllers.routes.javascript.Developers.login(),
				controllers.routes.javascript.Developers.resetTestAccountPassword(),
				
				controllers.admin.routes.javascript.Administration.register(),
				controllers.admin.routes.javascript.Administration.changeStatus(),
				controllers.admin.routes.javascript.Administration.addComment(),
				// Market				
				controllers.routes.javascript.Market.registerPlugin(),
				controllers.routes.javascript.Market.updatePlugin(),
				controllers.routes.javascript.Market.deletePlugin(),
				controllers.routes.javascript.Market.deletePluginDeveloper(),
				controllers.routes.javascript.Market.updatePluginStatus(),
				// Global search				
				//controllers.routes.javascript.GlobalSearch.search(),
				//controllers.routes.javascript.GlobalSearch.complete(),
				
				// UserGroups
				controllers.routes.javascript.UserGroups.search(),
				controllers.routes.javascript.UserGroups.createUserGroup(),
				controllers.routes.javascript.UserGroups.addMembersToUserGroup(),
				controllers.routes.javascript.UserGroups.listUserGroupMembers(),
				
		        // Portal
		        controllers.routes.javascript.PortalConfig.getConfig(),
		        controllers.routes.javascript.PortalConfig.setConfig()));
				        
		        
	}

}
