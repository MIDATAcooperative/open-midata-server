package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Developer;
import models.HPUser;
import models.Member;
import models.ResearchUser;
import models.User;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.ParticipationInterest;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import play.Play;
import play.Routes;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
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
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.mails.MailUtils;
import views.html.apstest;
import views.html.tester;
import views.txt.mails.lostpwmail;
import views.txt.mails.welcome;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Member login, registration and password reset functions 
 *
 */
public class Application extends APIController {

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
			   
		  String site = "https://" + Play.application().configuration().getString("portal.server");
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
		ObjectId userId = new ObjectId(request().username());
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
	   PasswordResetToken token = new PasswordResetToken(user._id, user.role.toString());
	   user.set("resettoken", token.token);
	   user.set("resettokenTs", System.currentTimeMillis());
	   String encrypted = token.encrypt();
			   
	   String site = "https://" + Play.application().configuration().getString("portal.server");
	   String url1 = site + "/#/portal/confirm/" + encrypted;
	   String url2 = site + "/#/portal/reject/" + encrypted;
			   
  	   MailUtils.sendTextMail(user.email, user.firstname+" "+user.lastname, "Welcome to MIDATA", welcome.render(site, url1, url2));
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
		ObjectId userId = passwordResetToken.userId;
		String token = passwordResetToken.token;
		String role = passwordResetToken.role;		
		
		User user = User.getById(userId, Sets.create("status", "role", "contractStatus", "emailStatus", "confirmationCode", "resettoken","password","resettokenTs"));
		
		if (user!=null && !user.emailStatus.equals(EMailStatus.VALIDATED)) {							
		       if (user.resettoken != null 		    		    
		    		   && user.resettoken.equals(token)
		    		   && System.currentTimeMillis() - user.resettokenTs < 1000 * 60 * 15) {	   
			   
		           user.set("resettoken", null);	
		           user.emailStatus = wanted;
			       user.set("emailStatus", wanted);			       
		       } else throw new BadRequestException("error.expired.token", "Token has already expired. Please request a new one.");
		       
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
				
		ObjectId userId = new ObjectId(request().username());
		String confirmationCode = JsonValidation.getString(json, "confirmationCode");
		
		User user = User.getById(userId, Sets.create("firstname", "lastname", "email", "confirmationCode", "emailStatus", "contractStatus", "status", "role"));
		
		
		if (user!=null && user.confirmationCode != null && user.emailStatus.equals(EMailStatus.VALIDATED) && user.contractStatus.equals(ContractStatus.SIGNED) && user.status.equals(UserStatus.NEW)) {
			if (user.role.equals(UserRole.PROVIDER)) {
				user = HPUser.getById(userId, Sets.create("firstname", "lastname", "email", "confirmationCode", "emailStatus", "contractStatus", "status", "role", "provider"));
			} else if (user.role.equals(UserRole.RESEARCH)) {
				user = ResearchUser.getById(userId, Sets.create("firstname", "lastname", "email", "confirmationCode", "emailStatus", "contractStatus", "status", "role", "organization"));
			}
			
		
	       if (user.confirmationCode.equals(confirmationCode)) {
	    	   user.status = UserStatus.ACTIVE;
	           user.set("status", user.status);			           			       
	       } else throw new BadRequestException("error.invalid.confirmation_code", "Bad confirmation code");
		}
					
		// response
		return loginHelper(user);		
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
		ObjectId userId = passwordResetToken.userId;
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
		ObjectId userId = new ObjectId(request().username());
		
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
		ObjectId userId = new ObjectId(request().username());
		
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
		Member user = Member.getByEmail(email , Sets.create("password", "status", "contractStatus", "emailStatus", "confirmationCode", "accountVersion", "role"));
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
				
		PortalSessionToken token = null;
		
		if (user instanceof HPUser) {
		   token = new PortalSessionToken(user._id, user.role, ((HPUser) user).provider);		  
		} else if (user instanceof ResearchUser) {
		   token = new PortalSessionToken(user._id, user.role, ((ResearchUser) user).organization);		  
		} else {
		   token = new PortalSessionToken(user._id, user.role, null);
		}
		
		ObjectNode obj = Json.newObject();
		obj.put("sessionToken", token.encrypt(request()));
		
		if (!user.status.equals(UserStatus.ACTIVE) && !Play.application().configuration().getBoolean("demoserver", false)) {
		  obj.put("status", user.status.toString());
		  obj.put("contractStatus", user.contractStatus.toString());
		  obj.put("emailStatus", user.emailStatus.toString());
		  obj.put("confirmationCode", user.confirmationCode == null);
		} else {						
		  int keytype = KeyManager.instance.unlock(user._id, null);		
		  if (keytype == 0) AccountPatches.check(user);
				
		  obj.put("keyType", keytype);
		  obj.put("role", user.role.toString().toLowerCase());
		}
	
		return ok(obj);
	}
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result downloadToken() throws AppException {
		PortalSessionToken current = PortalSessionToken.session();
		PortalSessionToken token = new PortalSessionToken(current.getUserId(), current.getRole(), current.getOrg());
		
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
		ObjectId userId = new ObjectId(request().username());
		
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
		user._id = new ObjectId();
		user.email = email;
		user.name = firstName + " " + lastName;
		
		user.password = Member.encrypt(password);
		do {
		  user.midataID = CodeGenerator.nextUniqueCode();
		} while (Member.existsByMidataID(user.midataID));
		user.role = UserRole.MEMBER;
		
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
						
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;	
		user.emailStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		user.partInterest = ParticipationInterest.UNSET;
							
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		user.messages = new HashMap<String, Set<ObjectId>>();
		user.messages.put("inbox", new HashSet<ObjectId>());
		user.messages.put("archive", new HashSet<ObjectId>());
		user.messages.put("trash", new HashSet<ObjectId>());
		user.login = DateTimeUtils.now();
		user.news = new HashSet<ObjectId>();
		//user.pushed = new HashSet<ObjectId>();
		//user.shared = new HashSet<ObjectId>();
		
		user.security = AccountSecurityLevel.KEY;
		
		switch (user.security) {
		case KEY: user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);break;
		default: user.publicKey = null;
		}
						
		Member.add(user);
		
		KeyManager.instance.unlock(user._id, null);
		
		user.myaps = RecordManager.instance.createPrivateAPS(user._id, user._id);
		Member.set(user._id, "myaps", user.myaps);
		
		sendWelcomeMail(user);
		
		return loginHelper(user);		
	}

	/**
	 * logout current user
	 * @return status ok
	 */
	@APICall
	public static Result logout() {
		
		// execute
		//session().clear();
		
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
				// Messages				
				controllers.routes.javascript.Messages.get(),
				controllers.routes.javascript.Messages.send(),
				controllers.routes.javascript.Messages.move(),
				controllers.routes.javascript.Messages.remove(),
				controllers.routes.javascript.Messages.delete(),
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
				controllers.routes.javascript.Spaces.getToken(),
				controllers.routes.javascript.Spaces.getUrl(),
				controllers.routes.javascript.Spaces.regetUrl(),
				controllers.routes.javascript.Spaces.getPreviewUrl(),
				controllers.routes.javascript.Spaces.getPreviewUrlFromSetup(),
				// Users
				controllers.routes.javascript.Users.get(),		
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				controllers.routes.javascript.Users.loadContacts(),
				controllers.routes.javascript.Users.complete(),
				controllers.routes.javascript.Users.updateSettings(),
				controllers.routes.javascript.Users.updateAddress(),
				
				controllers.routes.javascript.Tasking.add(),
				controllers.routes.javascript.Tasking.list(),
				controllers.routes.javascript.Tasking.execute(),
				
				//Research
				controllers.research.routes.javascript.Researchers.register(),
				controllers.research.routes.javascript.Researchers.login(),
				controllers.research.routes.javascript.Studies.create(),
				controllers.research.routes.javascript.Studies.list(),
				controllers.research.routes.javascript.Studies.get(),
				controllers.research.routes.javascript.Studies.update(),
				controllers.research.routes.javascript.Studies.updateParticipation(),
				controllers.research.routes.javascript.Studies.download(),
				controllers.research.routes.javascript.Studies.listCodes(),
				controllers.research.routes.javascript.Studies.generateCodes(),
				controllers.research.routes.javascript.Studies.startValidation(),
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
				
				controllers.admin.routes.javascript.Administration.changeStatus(),
				// Market				
				controllers.routes.javascript.Market.registerPlugin(),
				controllers.routes.javascript.Market.updatePlugin(),
				controllers.routes.javascript.Market.deletePlugin(),
				controllers.routes.javascript.Market.updatePluginStatus(),
				// Global search				
				//controllers.routes.javascript.GlobalSearch.search(),
				//controllers.routes.javascript.GlobalSearch.complete(),
		        // Portal
		        controllers.routes.javascript.PortalConfig.getConfig(),
		        controllers.routes.javascript.PortalConfig.setConfig()));
		        
	}

}
