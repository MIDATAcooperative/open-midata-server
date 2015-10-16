package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Developer;
import models.HPUser;
import models.Member;
import models.Record;
import models.ResearchUser;
import models.Space;
import models.User;
import models.enums.APSSecurityLevel;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.ParticipationInterest;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import play.Play;
import play.Routes;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.access.AccessLog;
import utils.auth.CodeGenerator;
import utils.auth.PasswordResetToken;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.evolution.AccountPatches;
import utils.exceptions.ModelException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.mails.MailUtils;
import views.html.tester;
import views.html.apstest;
import views.txt.mails.lostpwmail;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends APIController {

	public static Result test() {
		return ok(tester.render());
	}
	
	public static Result test2() {
		return ok(apstest.render());
	} 
			
	public static Result checkPreflight(String all) {		
		String host = request().getHeader("Origin");		
		if (host.startsWith("http://localhost:") || host.equals("https://demo.midata.coop")) {
		    response().setHeader("Access-Control-Allow-Origin", host);
		} else response().setHeader("Access-Control-Allow-Origin", "https://demo.midata.coop");
        response().setHeader("Allow", "*");
        response().setHeader("Access-Control-Allow-Credentials", "true");
        response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
        response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie");
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class) 
	@APICall
	public static Result requestPasswordResetToken() throws JsonValidationException, ModelException {
		// validate input
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "role");				
		String email = JsonValidation.getEMail(json, "email");
		String role = JsonValidation.getString(json, "role");
		
		// execute
		User user = null;
		switch (role) {
		case "member" : user = Member.getByEmail(email, Sets.create("name","email","password"));break;
		case "research" : user = ResearchUser.getByEmail(email, Sets.create("name","email","password"));break;
		case "provider" : user = HPUser.getByEmail(email, Sets.create("name","email","password"));break;
		case "developer" : user = Developer.getByEmail(email, Sets.create("name","email","password"));break;
		default: break;		
		}
		if (user != null) {							  
		  PasswordResetToken token = new PasswordResetToken(user._id, role);
		  user.set("resettoken", token.token);
		  user.set("resettokenTs", System.currentTimeMillis());
		  String encrypted = token.encrypt();
			   
		  String site = "https://" + Play.application().configuration().getString("platform.server");
		  String url = site + "/setpw#?token=" + encrypted;
			   
		  MailUtils.sendTextMail(email, user.firstname+" "+user.lastname, "Your Password", lostpwmail.render(site,url));
		}
			
		// response
		return ok();
	}
	
		
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result setPasswordWithToken() throws JsonValidationException, ModelException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "token", "password");
		
		// check status
		PasswordResetToken passwordResetToken = PasswordResetToken.decrypt(json.get("token").asText());
		if (passwordResetToken == null) return badRequest("Missing or bad password token.");
		
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
		       } else return badRequest("Password reset token has already expired.");
		}
					
		// response
		return ok();		
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public static Result authenticate() throws JsonValidationException, ModelException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "password");	
		String email = json.get("email").asText();
		String password = json.get("password").asText();
		
		// check status
		Member user = Member.getByEmail(email , Sets.create("password", "status", "accountVersion"));
		if (user == null) return badRequest("Invalid user or password.");
		if (!Member.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		}
		if (user.status.equals(UserStatus.BLOCKED) || user.status.equals(UserStatus.DELETED)) return badRequest("User is not allowed to log in.");
			    	    	
		// execute
		session().clear();
		session("id", user._id.toString());
		session("role", UserRole.MEMBER.toString());
		KeyManager.instance.unlock(user._id, "12345");
		
		AccountPatches.check(user);
		
		// response
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws JsonValidationException, ModelException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1");				
		String email = json.get("email").asText();
		String firstName = json.get("firstname").asText();
		String lastName = json.get("lastname").asText();
		String password = json.get("password").asText();

		// check status
		if (Member.existsByEMail(email)) {
		  return badRequest("A user with this email address already exists.");
		}
		
		// create the user
		Member user = new Member();
		user._id = new ObjectId();
		user.email = email;
		user.name = firstName + " " + lastName;
		
		user.password = Member.encrypt(password);
		user.midataID = CodeGenerator.nextUniqueCode();
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
		user.ssn = JsonValidation.getString(json, "ssn");
						
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
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
		user.pushed = new HashSet<ObjectId>();
		user.shared = new HashSet<ObjectId>();
		
		user.security = AccountSecurityLevel.KEY;
		
		switch (user.security) {
		case KEY: user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);break;
		default: user.publicKey = null;
		}
						
		Member.add(user);
		
		KeyManager.instance.unlock(user._id, "12345");
		
		user.myaps = RecordSharing.instance.createPrivateAPS(user._id, user._id);
		Member.set(user._id, "myaps", user.myaps);
		
		session().clear();
		session("id", user._id.toString());
		session("role",UserRole.MEMBER.toString());
		
		// reponse
		return ok();
	}

	@APICall
	public static Result logout() {
		// execute
		session().clear();
		
		// reponse
		return ok();		
	}
	
	
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter(
				"jsRoutes",
				// Application
				
				controllers.routes.javascript.Application.authenticate(),
				controllers.routes.javascript.Application.register(),
				controllers.routes.javascript.Application.requestPasswordResetToken(),
				controllers.routes.javascript.Application.setPasswordWithToken(),
				// Apps								
				controllers.routes.javascript.Apps.getUrl(),
				controllers.routes.javascript.Apps.getPreviewUrl(),
				controllers.routes.javascript.Apps.getUrlForConsent(),
				controllers.routes.javascript.Plugins.requestAccessTokenOAuth2(),
				controllers.routes.javascript.Plugins.getRequestTokenOAuth1(),
				controllers.routes.javascript.Plugins.requestAccessTokenOAuth1(),
				// Visualizations				
				controllers.routes.javascript.Plugins.get(),
				controllers.routes.javascript.Plugins.install(),
				controllers.routes.javascript.Plugins.uninstall(),
				controllers.routes.javascript.Plugins.isInstalled(),
				controllers.routes.javascript.Plugins.isAuthorized(),
				controllers.routes.javascript.Plugins.getUrl(),
				
				// News
				controllers.routes.javascript.News.get(),
				controllers.routes.javascript.News.hide(),
				// Messages				
				controllers.routes.javascript.Messages.get(),
				controllers.routes.javascript.Messages.send(),
				controllers.routes.javascript.Messages.move(),
				controllers.routes.javascript.Messages.remove(),
				controllers.routes.javascript.Messages.delete(),
				controllers.routes.javascript.FormatAPI.listGroups(),
				
				// Records					
				controllers.routes.javascript.Records.get(),				
				controllers.routes.javascript.Records.getRecords(),
				controllers.routes.javascript.Records.getInfo(),	
				controllers.routes.javascript.Records.getSharingDetails(),
				controllers.routes.javascript.Records.search(),				
				controllers.routes.javascript.Records.updateSharing(),
				controllers.routes.javascript.Records.share(),	
				controllers.routes.javascript.Records.getFile(),
				controllers.routes.javascript.Records.getRecordUrl(),
				controllers.routes.javascript.Records.delete(),
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
				controllers.routes.javascript.Spaces.getPreviewUrl(),
				controllers.routes.javascript.Spaces.getPreviewUrlFromSetup(),
				// Users
				controllers.routes.javascript.Users.get(),		
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				controllers.routes.javascript.Users.loadContacts(),
				controllers.routes.javascript.Users.complete(),
				
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
				controllers.research.routes.javascript.Studies.listParticipants(),
				controllers.research.routes.javascript.Studies.getParticipant(),
				controllers.research.routes.javascript.Studies.approveParticipation(),
				controllers.research.routes.javascript.Studies.rejectParticipation(),
				controllers.research.routes.javascript.Studies.shareWithGroup(),
				controllers.research.routes.javascript.Studies.addTask(),
				controllers.research.routes.javascript.Studies.getRequiredInformationSetup(),
				controllers.research.routes.javascript.Studies.setRequiredInformationSetup(),
				
				controllers.members.routes.javascript.Studies.list(),
				controllers.common.routes.javascript.Studies.search(),
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
				// Global search				
				controllers.routes.javascript.GlobalSearch.search(),
				controllers.routes.javascript.GlobalSearch.complete(),
		        // Portal
		        controllers.routes.javascript.PortalConfig.getConfig(),
		        controllers.routes.javascript.PortalConfig.setConfig()));
		        
	}

}
