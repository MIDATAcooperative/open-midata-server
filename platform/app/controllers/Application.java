package controllers;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Admin;
import models.Developer;
import models.HPUser;
import models.KeyInfoExtern;
import models.Member;
import models.MidataId;
import models.Plugin;
import models.ResearchUser;
import models.User;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.MessageReason;
import models.enums.ParticipationInterest;
import models.enums.SecondaryAuthType;
import models.enums.SubUserRole;
import models.enums.UsageAction;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import play.routing.JavaScriptReverseRouter;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.ExtendedSessionToken;
import utils.auth.FutureLogin;
import utils.auth.KeyManager;
import utils.auth.PasswordResetToken;
import utils.auth.PortalSessionToken;
import utils.auth.PreLoginSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.evolution.PostLoginActions;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;
import utils.messaging.SMSUtils;
import utils.stats.UsageStatsRecorder;
import views.txt.mails.adminnotify;
import views.txt.mails.lostpwmail;
import utils.auth.auth2factor.Authenticator;

/**
 * Member login, registration and password reset functions 
 *
 */
public class Application extends APIController {

	public final static long MAX_TIME_UNTIL_EMAIL_CONFIRMATION = 1000l * 60l * 60l * 24l;
	public final static long EMAIL_TOKEN_LIFETIME = 1000l * 60l * 60l * 24l *3l;
	
	// public final static long MAX_TRIAL_DURATION = 1000l * 60l * 60l * 24l * 30l;
			
	/**
	 * Handling of OPTIONS requests
	 * @param all dummy parameter
	 * @return status ok
	 */
	@APICall
	public Result checkPreflight(String all) {				
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
	public Result requestPasswordResetToken() throws AppException {
		// validate input
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "role");				
		String email = JsonValidation.getEMail(json, "email");
		String role = JsonValidation.getString(json, "role");
		
		// execute
		User user = null;
		switch (role) {
		case "member" : user = Member.getByEmail(email, Sets.create("firstname", "lastname","email","password", "role", "security","resettoken","resettokenTs"));break;
		case "research" : user = ResearchUser.getByEmail(email, Sets.create("firstname", "lastname","email","password", "role", "security","resettoken","resettokenTs"));break;
		case "provider" : user = HPUser.getByEmail(email, Sets.create("firstname", "lastname","email","password", "role", "security","resettoken","resettokenTs"));break;
		case "developer" : 
			user = Developer.getByEmail(email, Sets.create("firstname", "lastname","email","password", "role", "security","resettoken","resettokenTs"));
			if (user == null) user = Admin.getByEmail(email, Sets.create("firstname", "lastname","email","password", "role", "security","resettoken","resettokenTs"));
			break;
		default: break;		
		}
		if (user != null) {		
		  AuditManager.instance.addAuditEvent(AuditEventType.USER_PASSWORD_CHANGE_REQUEST, user._id);
		  PasswordResetToken token;
		  if (user.resettoken != null && user.resettokenTs > 0 && System.currentTimeMillis() - user.resettokenTs < EMAIL_TOKEN_LIFETIME - 1000l * 60l * 60l) {
			  token = new PasswordResetToken(user._id, role, user.resettoken);
		  } else {		  
			  token = new PasswordResetToken(user._id, role);
			  user.set("resettoken", token.token);
			  user.set("resettokenTs", System.currentTimeMillis());
		  }
		  String encrypted = token.encrypt();
			   
		  String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		  String url = site + "/#/portal/setpw?token=" + encrypted;
		  if (user.security != AccountSecurityLevel.KEY_EXT_PASSWORD) url +="&ns=1";
		  
		  Map<String,String> replacements = new HashMap<String, String>();
		  replacements.put("site", site);
		  replacements.put("password-link", url);
		   				
		  if (!Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.PASSWORD_FORGOTTEN, null, Collections.singleton(user._id), null, replacements)) {			  		  		 
		    Messager.sendTextMail(email, user.firstname+" "+user.lastname, "Your Password", lostpwmail.render(site,url).toString());
		  }		
		  AuditManager.instance.success();
		}
			
		// response
		return ok();
	}
	
	/**
	 * request sending the welcome mail
	 * @return status ok
	 * @throws AppException
	 */	
	@APICall	
	@BodyParser.Of(BodyParser.Json.class) 
	public Result requestWelcomeMail() throws AppException {
		
		JsonNode json = request().body().asJson();	
		JsonValidation.validate(json, "userId");				
		MidataId userId = JsonValidation.getMidataId(json, "userId");
		User user = User.getById(userId, Sets.create("firstname", "lastname", "email", "emailStatus", "status", "role"));
		
		if (user != null && (user.emailStatus.equals(EMailStatus.UNVALIDATED) || user.emailStatus.equals(EMailStatus.EXTERN_VALIDATED)) ) {							  
		   sendWelcomeMail(user, null);
		}
			
		// response
		return ok();
	}
	
	/**
	 * Helper function to send welcome mail
	 * @param user user record which sould receive the mail
	 */
	public static void sendWelcomeMail(User user, User executingUser) throws AppException {
		sendWelcomeMail(RuntimeConstants.instance.portalPlugin, user, executingUser);
	}
	
	
	public static void sendWelcomeMail(MidataId sourcePlugin, User user, User executingUser) throws AppException {
	   if (user.developer == null) {
		   if (user.email == null) return;
		   PasswordResetToken token = new PasswordResetToken(user._id, user.role.toString(), true);
		   user.set("resettoken", token.token);
		   user.set("resettokenTs", System.currentTimeMillis());
		   String encrypted = token.encrypt();
	
		   String site = "https://" + InstanceConfig.getInstance().getPortalServerDomain();
		   Map<String,String> replacements = new HashMap<String, String>();
		   replacements.put("site", site);
		   replacements.put("confirm-url", site + "/#/portal/confirm/" + encrypted);
		   replacements.put("reject-url", site + "/#/portal/reject/" + encrypted);
		   replacements.put("token", token.token);
		   
		   if (executingUser != null) {
			   replacements.put("executor-firstname", executingUser.firstname);
			   replacements.put("executor-lastname", executingUser.lastname);
			   replacements.put("executor-email", executingUser.email);
		   }
		   
		   AccessLog.log("send welcome mail: "+user.email);
		   if (executingUser == null) {
			   if (!Messager.sendMessage(sourcePlugin, MessageReason.REGISTRATION, null, Collections.singleton(user._id), null, replacements)) {
				   Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.REGISTRATION, user.role.toString(), Collections.singleton(user._id), null, replacements);
			   }	  	   
		   } else {
			   if (!Messager.sendMessage(sourcePlugin, MessageReason.REGISTRATION_BY_OTHER_PERSON, null, Collections.singleton(user._id), null, replacements)) {
				   if (!Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.REGISTRATION_BY_OTHER_PERSON, user.role.toString(), Collections.singleton(user._id), null, replacements)) {
					   if (!Messager.sendMessage(sourcePlugin, MessageReason.REGISTRATION, null, Collections.singleton(user._id), null, replacements)) {
						   Messager.sendMessage(RuntimeConstants.instance.portalPlugin, MessageReason.REGISTRATION, user.role.toString(), Collections.singleton(user._id), null, replacements);
					   }	
				   }
			   }	  	   
		   }
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
		   String email = user.getPublicIdentifier();
		   String role = user.role.toString();
		   
		   AccessLog.log("send admin notification mail: "+user.getPublicIdentifier());	   
	  	   Messager.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), "Midata Admin", "New MIDATA User", adminnotify.render(site, email, role).toString());
	   }
	}
			
	
	/**
	 * confirms a email account for a new MIDATA user
	 * @return status ok
	 * @throws AppException	 
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result confirmAccountEmail() throws AppException {
						
		// validate 
		JsonNode json = request().body().asJson();		
		
		EMailStatus wanted = json.has("mode") ? JsonValidation.getEnum(json, "mode", EMailStatus.class) : null;
		String password = json.has("password") ? JsonValidation.getPassword(json, "password") : null;
		
		MidataId userId;
		String token;
		String role;
		String handle = null;
		
		if (json.has("token")) {				
			// check status
			PasswordResetToken passwordResetToken = PasswordResetToken.decrypt(json.get("token").asText());
			if (passwordResetToken == null) throw new BadRequestException("error.missing.token", "Missing or bad token.");
			
			// execute
			userId = passwordResetToken.userId;
			token = passwordResetToken.token;
			role = passwordResetToken.role.toUpperCase();	
			
			ExtendedSessionToken stoken = new ExtendedSessionToken();
			stoken.userRole = UserRole.valueOf(role);
			stoken.ownerId = userId;
			stoken.created = System.currentTimeMillis();
			if (password==null) stoken.securityToken = "-";
			stoken.set();
			
		} else {
			JsonValidation.validate(json, "userId", "code", "role");
			userId = JsonValidation.getMidataId(json, "userId");
			token = JsonValidation.getString(json, "code");
			role = JsonValidation.getString(json, "role").toUpperCase();
			
			PortalSessionToken tk = PortalSessionToken.decrypt(request());
		    
		    if (tk != null) {
			    try {
			      KeyManager.instance.continueSession(tk.getHandle());
			      handle = tk.getHandle();
			    } catch (AppException e) { return unauthorized(); }
		    } else {
		    	return unauthorized();
		    }
		}
		
		
		User user = User.getById(userId, Sets.create(User.FOR_LOGIN, "resettoken", "resettokenTs", "registeredAt", "confirmedAt", "previousEMail"));
		if (user == null)  throw new BadRequestException("error.unknown.user", "User not found");
		
		if (user!=null && password != null) {				
			 AuditManager.instance.addAuditEvent(AuditEventType.USER_PASSWORD_CHANGE, userId);
		       if (user.resettoken != null 		    		    
		    		   && user.resettoken.equals(token)
		    		   && System.currentTimeMillis() - user.resettokenTs < EMAIL_TOKEN_LIFETIME) {	   
			   
		    	   if (handle == null) {
		    		   handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);		    	   
		    	       int keytype = KeyManager.instance.unlock(user._id, null);
		    	       
		               if (keytype == KeyManager.KEYPROTECTION_FAIL || keytype == KeyManager.KEYPROTECTION_AESKEY) {
		        	     PWRecovery.startRecovery(user, json);	
		        	     user.addFlag(AccountActionFlags.KEY_RECOVERY);
		        	     new ExtendedSessionToken().forUser(user).set();
		               } else {
		            	 new ExtendedSessionToken().forUser(user).withSession(handle).set();
		        	     PWRecovery.changePassword(user, json);		        	   		        	   
		               }
		    	   } else PWRecovery.changePassword(user, json);
		    	   		
		    	   if (user.emailStatus == EMailStatus.UNVALIDATED && wanted == null) {
		    		   // Implicit confirmation of email address by having received password reset mail
		    		   AuditManager.instance.addAuditEvent(AuditEventType.USER_EMAIL_CONFIRMED, user);			       		    	   		         
		               user.emailStatus = EMailStatus.VALIDATED;
			           user.set("emailStatus", EMailStatus.VALIDATED);		 
		    	   }
		    	   
		    	   user.set("resettoken", null);	
		       } else throw new BadRequestException("error.expired.token", "Password reset token has already expired.");
		}
		
		
		if (wanted != null) {
			if (user!=null && !user.emailStatus.equals(EMailStatus.VALIDATED)) {
				if (user.password == null) {					
					return OAuth2.loginHelper();	
				}
			       if (user.resettoken != null 		    		    
			    		   && user.resettoken.equals(token)
			    		   && System.currentTimeMillis() - user.resettokenTs < EMAIL_TOKEN_LIFETIME) {	   
				   
			    	   
			    	   if (wanted == EMailStatus.REJECTED) {
			    		   if (user.previousEMail != null) {
			    			   AuditManager.instance.addAuditEvent(AuditEventType.USER_EMAIL_REJECTED, user);
			    			   user.email = user.previousEMail;
			    			   user.emailLC = user.email.toLowerCase();
			    			   wanted = EMailStatus.VALIDATED;
			    			   user.set("email", user.email);
			    			   user.set("emailLC", user.emailLC);
			    		   } else {
				    		   AuditManager.instance.addAuditEvent(AuditEventType.USER_EMAIL_REJECTED, user);
				    		   user.status = UserStatus.BLOCKED;
					    	   user.set("status", user.status);
			    		   }
				       } else {
				    	   AuditManager.instance.addAuditEvent(AuditEventType.USER_EMAIL_CONFIRMED, user);
				       }
			    	   		          
			           user.emailStatus = wanted;
				       user.set("emailStatus", wanted);				       
				       
			       } else if (user!=null && user.emailStatus.equals(EMailStatus.UNVALIDATED) && user.resettoken != null 
			    		   && user.resettoken.equals(token)) {
			    	     sendWelcomeMail(user, null);
			    	     throw new BadRequestException("error.expired.tokenresent", "Token has already expired. A new one has been requested.");
			       } else throw new BadRequestException("error.expired.token", "Token has already expired. Please request a new one.");
			       
			       checkAccount(user);
			       
			       
			       
			} else if (user != null) {
				throw new BadRequestException("error.already_done.email_verification", "E-Mail has already been verified.");
			}
		}
		
			
		AuditManager.instance.success();	
		
		return OAuth2.loginHelper();	
				
	}
	
	/**
	 * confirms a postal address for a new MIDATA user and activates the account
	 * @return status ok
	 * @throws AppException	
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(PreLoginSecured.class)
	public Result confirmAccountAddress() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "confirmationCode");
				
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		String confirmationCode = JsonValidation.getString(json, "confirmationCode");
		
		User user = User.getById(userId, User.FOR_LOGIN);
		
		
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
		return OAuth2.loginHelper();		
	}
	
	public static void checkAccount(User user) throws AppException {
		/*if (user.subroles.contains(SubUserRole.TRIALUSER) && 
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
			*/	
	}
	


	/**
	 * set a new password for a user account by using a password reset token
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result setPasswordWithToken() throws JsonValidationException, AppException {
		return confirmAccountEmail();				
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
	public Result changePassword() throws JsonValidationException, AppException {
		return new PWRecovery().changePassword();
		// validate 
		/*JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "oldPassword", "password");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		String oldPassword = JsonValidation.getString(json, "oldPassword");
		String password = JsonValidation.getPassword(json, "password");
		
		User user = User.getById(userId, User.ALL_USER_INTERNAL);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_PASSWORD_CHANGE, user);
		if (!user.authenticationValid(oldPassword)) throw new BadRequestException("error.invalid.password_old","Bad password.");
		
		user.set("password", Member.encrypt(password));
		       			
		// response
		AuditManager.instance.success();
		return ok();*/		
	}
	
	/**
	 * sets or changes a passphrase for a user
	 * @return
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result changePassphrase() throws AppException {
		requireUserFeature(UserFeature.ADDRESS_VERIFIED);
		
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "oldPassphrase", "passphrase");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		String oldPassphrase = JsonValidation.getStringOrNull(json, "oldPassphrase");
		String passphrase = JsonValidation.getPassword(json, "passphrase");
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_PASSPHRASE_CHANGE, userId);
		
		KeyManager.instance.unlock(userId, oldPassphrase);
		
		// This is a dummy query to check if provided passphrase works
		try {
		  RecordManager.instance.list(userId, getRole(), RecordManager.instance.createContextFromAccount(userId), CMaps.map("format","zzzzzz"), Sets.create("name"));
		} catch (InternalServerException e) { throw new BadRequestException("error.passphrase_old", "Old passphrase not correct."); }
		
		KeyManager.instance.changePassphrase(userId, passphrase);
		
		AuditManager.instance.success();
		
		return ok();
	}
	
	
	
	/**
	 * login function for MIDATA members
	 * @return status ok
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public Result authenticate() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "password");	
			
		ExtendedSessionToken token = new ExtendedSessionToken();
		
		token.created = System.currentTimeMillis();                               
	    token.userRole = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;                
										    				
		return OAuth2.loginHelper(token, json, null, null);
		
		
		
		// check status
		/*Member user = Member.getByEmail(email , User.FOR_LOGIN);
		if (user == null) {
			Set<User> alts = User.getAllUser(CMaps.map("emailLC", email.toLowerCase()).map("status", User.NON_DELETED).map("role", Sets.create(UserRole.DEVELOPER.toString(), UserRole.RESEARCH.toString(), UserRole.PROVIDER.toString())), Sets.create("role"));
			if (!alts.isEmpty()) {				
			  throw new BadRequestException("error.invalid.credentials_hint",  "Invalid user or password.");
			} else {
			  throw new BadRequestException("error.invalid.credentials",  "Invalid user or password.");
			}
		}
		
		if (user.publicExtKey == null) {
			if (!json.has("nonHashed")) return ok("compatibility-mode");
			password = JsonValidation.getString(json, "nonHashed");
		}
							
		if (user.publicExtKey == null || sessionToken != null) AuditManager.instance.addAuditEvent(AuditEventType.USER_AUTHENTICATION, user);
		
		Result checkrecovery = PWRecovery.checkAuthentication(null, user, password, sessionToken);
		if (checkrecovery != null) return checkrecovery;
					
		return loginHelper(user, sessionToken, securityToken, false);					
	*/
	}
	
	/*
	public static boolean verifyUser(MidataId userId) throws AppException {
		User user = User.getById(userId , Sets.create("password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer"));
		return verifyUser(user);		
	}
	
	public static boolean verifyUser(User user) throws AppException {		
		if (user == null) return false;
		if (loginHelperPreconditionsFailed(user)) return false;
		return true;
	}
	*/
	
	public static Set<UserFeature> loginHelperPreconditionsFailed(User user, Set<UserFeature> required) throws AppException {
        if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED) || user.status.equals(UserStatus.WIPED)) throw new BadRequestException("error.blocked.user", "User is not allowed to log in.");
		
		if (user.emailStatus.equals(EMailStatus.UNVALIDATED) && user.registeredAt.before(new Date(System.currentTimeMillis() - MAX_TIME_UNTIL_EMAIL_CONFIRMATION)) && !InstanceConfig.getInstance().getInstanceType().disableEMailValidation()) {
			user.status = UserStatus.TIMEOUT;			
			return Collections.singleton(UserFeature.EMAIL_VERIFIED);
		}
		
						
		Set<UserFeature> missing = null;
		if (required != null) {
			for (UserFeature feature : required) {
				if (!feature.isSatisfiedBy(user)) {
					if (missing == null) missing = new HashSet<UserFeature>();
					missing.add(feature);
				}
			}
		}
				
		
		return missing;
	}
	
	public static Result loginHelperResult(PortalSessionToken token, User user, Set<UserFeature> missing) throws AppException {
		ObjectNode obj = Json.newObject();
		obj.put("status", user.status.toString());
		obj.put("contractStatus", user.contractStatus.toString());		
		obj.put("agbStatus", user.agbStatus.toString());
		obj.put("emailStatus", user.emailStatus.toString());
		obj.put("mobileStatus", user.mobileStatus == null ? EMailStatus.UNVALIDATED.toString() : user.mobileStatus.toString());
		ArrayNode ar = obj.putArray("requirements");
		for (UserFeature feature : missing) ar.add(feature.toString());
		obj.put("confirmationCode", user.confirmedAt != null);
		obj.put("role", user.role.toString().toLowerCase());
		obj.put("userId", user._id.toString());
		obj.set("user", JsonOutput.toJsonNode(user, "User", User.ALL_USER));
		token.setRemoteAddress(request());
		obj.put("sessionToken", token.encrypt());
		
		/*
		if (user.status.equals(UserStatus.ACTIVE) || user.status.equals(UserStatus.NEW)) {			
		   String handle = KeyManager.instance.currentHandleOptional(user._id);
		   if (handle != null) {
			   PortalSessionToken token = null;		   	
			   token = new PortalSessionToken(handle, user._id, UserRole.ANY, null, user.developer);
			   token.setRemoteAddress(request());
			   obj.put("sessionToken", token.encrypt());
			   //KeyManager.instance.unlock(user._id, null);
			   KeyManager.instance.persist(user._id);
		   }
		}*/
					
		AuditManager.instance.success();
		return ok(obj);
	}
	
	public static Result loginChallenge(PortalSessionToken token, User user) throws InternalServerException {
		FutureLogin fl = FutureLogin.getById(user._id);
		
		KeyInfoExtern key = KeyInfoExtern.getById(user._id);
		
		ObjectNode obj = Json.newObject();
		
		obj.put("challenge", Base64.getEncoder().encodeToString(Arrays.copyOfRange(fl.extPartEnc, 4, fl.extPartEnc.length)));
		obj.put("keyEncrypted", key.privateKey);
		obj.put("pub", user.publicExtKey);
		obj.put("recoverKey", user.recoverKey);
		obj.put("userid", user._id.toString());
		if (token != null) obj.put("sessionToken", token.encrypt());
		
		return ok(obj);
	}
					
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result downloadToken() throws AppException {
		PortalSessionToken current = PortalSessionToken.session();
		PortalSessionToken token = new PortalSessionToken(current.getHandle(), current.getOwnerId(), current.getRole(), current.getOrgId(), current.getDeveloperId());
		
		ObjectNode obj = Json.newObject();
		token.setRemoteAddress(request());
		token.setTimeout(1000 * 10);
		obj.put("token", token.encrypt());
		
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
	public Result providePassphrase() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "passphrase");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		String passphrase = JsonValidation.getString(json, "passphrase");
		
		KeyManager.instance.unlock(userId, passphrase);
		KeyManager.instance.persist(userId);
		
		try {
		  RecordManager.instance.list(userId, getRole(),  RecordManager.instance.createContextFromAccount(userId), CMaps.map("format","zzzzzzz"), Sets.create("name"));
		} catch (InternalServerException e) {
		  throw new BadRequestException("error.invalid.passphrase", "Bad Passphrase");
		}
					
		User user = User.getById(userId , Sets.create("firstname", "lastname", "email", "role", "password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer"));
		user = PostLoginActions.check(user);
		
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
	public Result register() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "country", "language","password");
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
		user.subroles = EnumSet.noneOf(SubUserRole.class);		
		user.address1 = JsonValidation.getStringOrNull(json, "address1");
		user.address2 = JsonValidation.getStringOrNull(json, "address2");
		user.city = JsonValidation.getStringOrNull(json, "city");
		user.zip  = JsonValidation.getStringOrNull(json, "zip");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.birthday = JsonValidation.getDate(json, "birthday");
		user.language = JsonValidation.getString(json, "language");
		user.ssn = JsonValidation.getString(json, "ssn");										
		//user.authType = SecondaryAuthType.NONE;	
		
		registerSetDefaultFields(user);				
		developerRegisteredAccountCheck(user, json);
		
		AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, user);
		
		String handle;
		if (json.has("priv_pw")) {
		  String pub = JsonValidation.getString(json, "pub");
		  String pk = JsonValidation.getString(json, "priv_pw");
		  Map<String, String> recover = JsonExtraction.extractStringMap(json.get("recovery"));
		  		        	      		  		
		  user.publicExtKey = KeyManager.instance.readExternalPublicKey(pub);
		  
		  KeyManager.instance.saveExternalPrivateKey(user._id, pk);		  
		  handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);
		  
		  user.security = AccountSecurityLevel.KEY_EXT_PASSWORD;		
		  user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(user._id, null);
		  user.recoverKey = JsonValidation.getStringOrNull(json, "recoverKey");
		  Member.add(user);
		  
		  KeyManager.instance.newFutureLogin(user);	
		  PWRecovery.storeRecoveryData(user._id, recover);
			
		  user.myaps = RecordManager.instance.createPrivateAPS(user._id, user._id);
		  Member.set(user._id, "myaps", user.myaps);
			
		  PatientResourceProvider.updatePatientForAccount(user._id);
		  		  		  		  
		} else {
		  handle = registerCreateUser(user);		
		}
		Circles.fetchExistingConsents(user._id, user.emailLC);
		
		sendWelcomeMail(user, null);
		if (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister() && user.developer == null) sendAdminNotificationMail(user);
		UsageStatsRecorder.protokoll(RuntimeConstants.instance.portalPlugin, "portal", UsageAction.REGISTRATION);
		
		return OAuth2.loginHelper(new ExtendedSessionToken().forUser(user).withSession(handle), json, null, user._id);				
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
		user.mobileStatus = EMailStatus.UNVALIDATED;
		user.confirmationCode = CodeGenerator.nextCode();
		user.partInterest = ParticipationInterest.UNSET;
							
		user.apps = new HashSet<MidataId>();	
		user.visualizations = new HashSet<MidataId>();
		
		user.login = new Date();
		user.news = new HashSet<MidataId>();
				
		Terms.addAgreedToDefaultTerms(user);
	}
	
	/**
	 * actually creates a new user and creates the corresponding APS and key
	 * @param user the new user
	 * @throws AppException
	 */
	public static String registerCreateUser(Member user) throws AppException {
		user.security = AccountSecurityLevel.KEY;		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);								
		Member.add(user);
		String handle = KeyManager.instance.login(PortalSessionToken.LIFETIME, true);
		KeyManager.instance.unlock(user._id, null);	
		
		user.myaps = RecordManager.instance.createPrivateAPS(user._id, user._id);
		Member.set(user._id, "myaps", user.myaps);
		
		PatientResourceProvider.updatePatientForAccount(user._id);
		
		return handle;
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
			  PortalSessionToken token = PortalSessionToken.decrypt(request());
			  if (token == null) throw new AuthException("error.internal", "You need to be logged in as this developer");
		     if (!newuser.developer.equals(token.ownerId)) throw new AuthException("error.internal", "You need to be logged in as this developer");
		     newuser.status = UserStatus.ACTIVE;
		     newuser.authType = SecondaryAuthType.NONE;
		   }
		}
	}

	/**
	 * logout current user
	 * @return status ok
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result logout() {
		
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
	public Result javascriptRoutes() {		
          return ok(JavaScriptReverseRouter.create("jsRoutes", 	
				// Application
				
				controllers.routes.javascript.Application.authenticate(),
				controllers.routes.javascript.Application.register(),
				controllers.routes.javascript.QuickRegistration.register(),
				controllers.routes.javascript.Application.requestPasswordResetToken(),
				controllers.routes.javascript.Application.setPasswordWithToken(),
				controllers.routes.javascript.PWRecovery.changePassword(),
				controllers.routes.javascript.Application.changePassphrase(),
				controllers.routes.javascript.Application.providePassphrase(),
				controllers.routes.javascript.Application.confirmAccountEmail(),
				controllers.routes.javascript.Application.confirmAccountAddress(),
				controllers.routes.javascript.Application.requestWelcomeMail(),
				controllers.routes.javascript.Application.downloadToken(),
				
				// Apps										
				//controllers.routes.javascript.Plugins.getUrlForConsent(),
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
				controllers.routes.javascript.Plugins.addMissingPlugins(),
				
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
				controllers.routes.javascript.FormatAPI.searchContents(),
				controllers.routes.javascript.FormatAPI.searchCoding(),
				controllers.routes.javascript.FormatAPI.searchCodingPortal(),
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
				controllers.routes.javascript.Records.getFile(),
				controllers.routes.javascript.Records.getRecordUrl(),
				controllers.routes.javascript.Records.delete(),
				controllers.routes.javascript.Records.fixAccount(),
				controllers.routes.javascript.Records.downloadAccountData(),
				controllers.routes.javascript.Records.shareRecord(),
				controllers.routes.javascript.Records.unshareRecord(),
				// Circles
				controllers.routes.javascript.Circles.get(),
				controllers.routes.javascript.Circles.add(),
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.addUsers(),
				controllers.routes.javascript.Circles.removeMember(),
				controllers.routes.javascript.Circles.listConsents(),
				controllers.routes.javascript.Circles.listApps(),
				controllers.routes.javascript.Circles.joinByPasscode(),
				// Spaces
				controllers.routes.javascript.Spaces.get(),
				controllers.routes.javascript.Spaces.add(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.addRecords(),
				controllers.routes.javascript.Spaces.getUrl(),
				controllers.routes.javascript.Spaces.regetUrl(),
				controllers.routes.javascript.Spaces.reset(),
				// Users
				controllers.routes.javascript.Users.get(),		
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				controllers.routes.javascript.Users.loadContacts(),
				controllers.routes.javascript.Users.complete(),
				controllers.routes.javascript.Users.updateSettings(),
				controllers.routes.javascript.Users.updateAddress(),
				controllers.routes.javascript.Users.requestMembership(),						
				
				//Research
				controllers.research.routes.javascript.Researchers.register(),
				controllers.research.routes.javascript.Researchers.registerOther(),
				controllers.research.routes.javascript.Researchers.login(),
				controllers.research.routes.javascript.Researchers.getOrganization(),
				controllers.research.routes.javascript.Researchers.updateOrganization(),
				controllers.research.routes.javascript.Studies.create(),
				controllers.research.routes.javascript.Studies.cloneToNew(),
				controllers.research.routes.javascript.Studies.list(),
				controllers.research.routes.javascript.Studies.listAdmin(),
				controllers.research.routes.javascript.Studies.get(),
				controllers.research.routes.javascript.Studies.getAdmin(),
				controllers.research.routes.javascript.Studies.update(),
				controllers.research.routes.javascript.Studies.updateNonSetup(),
				controllers.research.routes.javascript.Studies.updateParticipation(),
				controllers.research.routes.javascript.Studies.download(),
				controllers.research.routes.javascript.Studies.downloadFHIR(),
				controllers.research.routes.javascript.Studies.listCodes(),
				controllers.research.routes.javascript.Studies.generateCodes(),
				controllers.research.routes.javascript.Studies.startValidation(),
				controllers.research.routes.javascript.Studies.endValidation(),
				controllers.research.routes.javascript.Studies.backToDraft(),
				controllers.research.routes.javascript.Studies.startParticipantSearch(),
				controllers.research.routes.javascript.Studies.endParticipantSearch(),
				controllers.research.routes.javascript.Studies.startExecution(),
				controllers.research.routes.javascript.Studies.finishExecution(),
				controllers.research.routes.javascript.Studies.abortExecution(),
				controllers.research.routes.javascript.Studies.delete(),
				controllers.research.routes.javascript.Studies.listParticipants(),
				controllers.research.routes.javascript.Studies.countParticipants(),
				controllers.research.routes.javascript.Studies.getParticipant(),
				controllers.research.routes.javascript.Studies.approveParticipation(),
				controllers.research.routes.javascript.Studies.rejectParticipation(),	
				controllers.research.routes.javascript.Studies.shareWithGroup(),
				controllers.research.routes.javascript.Studies.addApplication(),		
				controllers.research.routes.javascript.Studies.getRequiredInformationSetup(),
				controllers.research.routes.javascript.Studies.setRequiredInformationSetup(),
				
				controllers.members.routes.javascript.Studies.list(),
				controllers.routes.javascript.Studies.search(),
				controllers.members.routes.javascript.Studies.enterCode(),
				controllers.members.routes.javascript.Studies.get(),
				controllers.members.routes.javascript.Studies.requestParticipation(),
				controllers.members.routes.javascript.Studies.updateParticipation(),
				controllers.members.routes.javascript.Studies.noParticipation(),
				controllers.members.routes.javascript.Studies.retreatParticipation(),
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
				controllers.providers.routes.javascript.Providers.registerOther(),				
				controllers.providers.routes.javascript.Providers.getOrganization(),
				controllers.providers.routes.javascript.Providers.updateOrganization(),
								
				// Developers
				controllers.routes.javascript.Developers.register(),
				controllers.routes.javascript.Developers.login(),
				controllers.routes.javascript.Developers.resetTestAccountPassword(),
				
				controllers.admin.routes.javascript.Administration.register(),
				controllers.admin.routes.javascript.Administration.changeStatus(),
				controllers.admin.routes.javascript.Administration.changeUserEmail(),
				controllers.admin.routes.javascript.Administration.changeBirthday(),
				controllers.admin.routes.javascript.Administration.addComment(),
				controllers.admin.routes.javascript.Administration.adminWipeAccount(), 
				controllers.admin.routes.javascript.Administration.deleteStudy(),
				controllers.admin.routes.javascript.Administration.getStats(),
				controllers.admin.routes.javascript.Administration.getUsageStats(),
				controllers.admin.routes.javascript.Administration.getSystemHealth(),
				controllers.routes.javascript.PWRecovery.getUnfinished(),
				controllers.routes.javascript.PWRecovery.storeRecoveryShare(),
				controllers.routes.javascript.PWRecovery.finishRecovery(),
				controllers.routes.javascript.PWRecovery.requestServiceKeyRecovery(),
				// Market				
				controllers.routes.javascript.Market.registerPlugin(),
				controllers.routes.javascript.Market.updatePlugin(),
				controllers.routes.javascript.Market.deletePlugin(),
				controllers.routes.javascript.Market.deletePluginDeveloper(),
				controllers.routes.javascript.Market.updatePluginStatus(),
				controllers.routes.javascript.Market.updateDefaultSubscriptions(),
				controllers.routes.javascript.Market.getPluginStats(),
				controllers.routes.javascript.Market.deletePluginStats(),
				controllers.routes.javascript.Market.importPlugin(),
				controllers.routes.javascript.Market.exportPlugin(),
				controllers.routes.javascript.Market.uploadIcon(),
				controllers.routes.javascript.Market.deleteIcon(),
				controllers.routes.javascript.Market.getIcon(),
				controllers.routes.javascript.Market.getStudyAppLinks(),
				controllers.routes.javascript.Market.insertStudyAppLink(),
				controllers.routes.javascript.Market.deleteStudyAppLink(),
				controllers.routes.javascript.Market.validateStudyAppLink(),
				controllers.routes.javascript.Market.setSubscriptionDebug(),
				controllers.routes.javascript.Market.addReview(),
				controllers.routes.javascript.Market.getReviews(),
				controllers.routes.javascript.Market.getSoftwareChangeLog(),
				controllers.routes.javascript.Market.updateLicence(),
				controllers.routes.javascript.Market.addLicence(),
				controllers.routes.javascript.Market.searchLicenses(),
								
				// UserGroups
				controllers.routes.javascript.UserGroups.search(),
				controllers.routes.javascript.UserGroups.createUserGroup(),
				controllers.routes.javascript.UserGroups.deleteUserGroup(), 
				controllers.routes.javascript.UserGroups.editUserGroup(),
				controllers.routes.javascript.UserGroups.addMembersToUserGroup(),
				controllers.routes.javascript.UserGroups.deleteUserGroupMembership(),
				controllers.routes.javascript.UserGroups.listUserGroupMembers(),
				
				controllers.routes.javascript.Terms.get(),
				controllers.routes.javascript.Terms.search(),
				controllers.routes.javascript.Terms.add(),
				controllers.routes.javascript.Terms.agreedToTerms(),
				
		        // Portal
		        controllers.routes.javascript.PortalConfig.getConfig(),
		        controllers.routes.javascript.PortalConfig.setConfig())).as("text/javascript");
				        
		        
	}

}
