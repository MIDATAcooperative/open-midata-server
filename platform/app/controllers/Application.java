package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.HPUser;
import models.ModelException;
import models.Member;
import models.ResearchUser;
import models.User;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.ParticipationInterest;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import play.Play;
import play.Routes;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.auth.CodeGenerator;
import utils.auth.PasswordResetToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.mails.MailUtils;
import views.html.welcome;
import views.html.registration;
import views.html.tester;
import views.html.lostpw;
import views.html.setpw;
import views.txt.mails.lostpwmail;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {

	public static Result test() {
		return ok(tester.render());
	} 
	
	public static Result welcome() {
		return ok(welcome.render());
	}
	
	public static Result registration() {
		return ok(registration.render());
	}
	
	public static Result lostpw(String role) {
		return ok(lostpw.render(role));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result requestPasswordResetToken() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "role");
		
		// validate request
		String email = JsonValidation.getEMail(json, "email");
		String role = JsonValidation.getString(json, "role");
		
		User user = null;
		switch (role) {
		case "member" : user = Member.getByEmail(email, Sets.create("name","email","password"));break;
		case "research" : user = ResearchUser.getByEmail(email, Sets.create("name","email","password"));break;
		case "provider" : user = HPUser.getByEmail(email, Sets.create("name","email","password"));break;
		default: break;		
		}
		if (user != null) {							  
		  PasswordResetToken token = new PasswordResetToken(user._id, role);
		  user.set("resettoken", token.token);
		  user.set("resettokenTs", System.currentTimeMillis());
		  String encrypted = token.encrypt();
			   
		  String site = "https://" + Play.application().configuration().getString("platform.server");
		  String url = site + "/setpw#?token=" + encrypted;
			   
		  MailUtils.sendTextMail(email, user.name, "Your Password", lostpwmail.render(site,url));
		}
				
		return ok();
	}
	
	public static Result setpw() {
		return ok(setpw.render());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result setPasswordWithToken() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "token", "password");
		
		// validate request
		PasswordResetToken passwordResetToken = PasswordResetToken.decrypt(json.get("token").asText());
		if (passwordResetToken == null) return badRequest("Missing or bad password token.");
		
		ObjectId userId = passwordResetToken.userId;
		String token = passwordResetToken.token;
		String role = passwordResetToken.role;
		String password = JsonValidation.getPassword(json, "password");
		
		User user = null;
		switch (role) {
		case "member" : user = Member.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
		case "research" : user = ResearchUser.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
		case "provider" : user = HPUser.getById(userId, Sets.create("resettoken","password","resettokenTs"));break;
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
					
		return ok();		
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public static Result authenticate() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
		// validate request
		String email = json.get("email").asText();
		String password = json.get("password").asText();
		Map<String, String> emailQuery = new ChainedMap<String, String>().put("email", email).get();
		Member user;
	
	    if (!Member.exists(emailQuery)) {
		  return badRequest("Invalid user or password.");
		} else {
		  user = Member.get(emailQuery, new ChainedSet<String>().add("password").get());
		  if (!Member.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		  }
		}
	
		// user authenticated
		session().clear();
		session("id", user._id.toString());
		session("role","member");
		return ok(routes.News.index().url());
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "firstname", "sirname", "gender", "city", "zip", "country", "address1");
		
		// validate request
		String email = json.get("email").asText();
		String firstName = json.get("firstname").asText();
		String lastName = json.get("sirname").asText();
		String password = json.get("password").asText();
		
		if (Member.existsByEMail(email)) {
		  return badRequest("A user with this email address already exists.");
		}
		
		// create the user
		Member user = new Member();
		user._id = new ObjectId();
		user.email = email;
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
		user.sirname = JsonValidation.getString(json, "sirname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.birthday = JsonValidation.getDate(json, "birthday");
		user.ssn = JsonValidation.getString(json, "ssn");
						
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
		user.confirmationCode = CodeGenerator.nextCode();
		user.partInterest = ParticipationInterest.UNSET;
		
		user.visible = new HashMap<String, Set<ObjectId>>();
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
		
		Member.add(user);
		
		session().clear();
		session("id", user._id.toString());
		session("role","member");
		return ok(routes.News.index().url());
	}

	public static Result logout() {
		session().clear();
		return redirect(routes.Application.welcome());
	}

	public static Result portalRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter(
				"portalRoutes",
				controllers.routes.javascript.ResearchFrontend.studyoverview(),
				controllers.routes.javascript.MemberFrontend.studydetails()
				));
	}
	
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter(
				"jsRoutes",
				// Application
				controllers.routes.javascript.Application.welcome(),
				controllers.routes.javascript.Application.authenticate(),
				controllers.routes.javascript.Application.register(),
				controllers.routes.javascript.Application.requestPasswordResetToken(),
				controllers.routes.javascript.Application.setPasswordWithToken(),
				// Apps
				controllers.routes.javascript.Apps.details(),
				controllers.routes.javascript.Apps.get(),
				controllers.routes.javascript.Apps.install(),
				controllers.routes.javascript.Apps.uninstall(),
				controllers.routes.javascript.Apps.isInstalled(),
				controllers.routes.javascript.Apps.getUrl(),
				controllers.routes.javascript.Apps.requestAccessTokenOAuth2(),
				controllers.routes.javascript.Apps.getRequestTokenOAuth1(),
				controllers.routes.javascript.Apps.requestAccessTokenOAuth1(),
				// Visualizations
				controllers.routes.javascript.Visualizations.details(),
				controllers.routes.javascript.Visualizations.get(),
				controllers.routes.javascript.Visualizations.install(),
				controllers.routes.javascript.Visualizations.uninstall(),
				controllers.routes.javascript.Visualizations.isInstalled(),
				controllers.routes.javascript.Visualizations.getUrl(),
				// News
				controllers.routes.javascript.News.get(),
				controllers.routes.javascript.News.hide(),
				// Messages
				controllers.routes.javascript.Messages.details(),
				controllers.routes.javascript.Messages.get(),
				controllers.routes.javascript.Messages.send(),
				controllers.routes.javascript.Messages.move(),
				controllers.routes.javascript.Messages.remove(),
				controllers.routes.javascript.Messages.delete(),
				// Records
				controllers.routes.javascript.Records.filter(),
				controllers.routes.javascript.Records.details(),
				controllers.routes.javascript.Records.create(),
				controllers.routes.javascript.Records.importRecords(),
				controllers.routes.javascript.Records.get(),
				controllers.routes.javascript.Records.getVisibleRecords(),
				controllers.routes.javascript.Records.search(),
				controllers.routes.javascript.Records.updateSpaces(),
				controllers.routes.javascript.Records.updateSharing(),
				controllers.routes.javascript.Records.showInSpaces(),
				controllers.routes.javascript.Records.shareWithCircles(),
				controllers.routes.javascript.Records.getFile(),
				// Circles
				controllers.routes.javascript.Circles.get(),
				controllers.routes.javascript.Circles.add(),
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.addUsers(),
				controllers.routes.javascript.Circles.removeMember(),
				// Spaces
				controllers.routes.javascript.Spaces.get(),
				controllers.routes.javascript.Spaces.add(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.addRecords(),
				controllers.routes.javascript.Spaces.getToken(),
				// Users
				controllers.routes.javascript.Users.get(),
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				controllers.routes.javascript.Users.loadContacts(),
				controllers.routes.javascript.Users.complete(),
				controllers.routes.javascript.Users.clearPushed(),
				controllers.routes.javascript.Users.clearShared(),
				//Research
				controllers.research.routes.javascript.Researchers.register(),
				controllers.research.routes.javascript.Researchers.login(),
				controllers.research.routes.javascript.Studies.create(),
				controllers.research.routes.javascript.Studies.list(),
				controllers.research.routes.javascript.Studies.get(),
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
				controllers.research.routes.javascript.Studies.getRequiredInformationSetup(),
				controllers.research.routes.javascript.Studies.setRequiredInformationSetup(),
				
				controllers.members.routes.javascript.Studies.list(),
				controllers.members.routes.javascript.Studies.enterCode(),
				controllers.members.routes.javascript.Studies.get(),
				controllers.members.routes.javascript.Studies.requestParticipation(),
				controllers.members.routes.javascript.Studies.noParticipation(),
				//Healthcare Providers
				controllers.providers.routes.javascript.Providers.register(),
				controllers.providers.routes.javascript.Providers.login(),
				controllers.providers.routes.javascript.Providers.search(),
				controllers.providers.routes.javascript.Providers.getMember(),
				// Market
				controllers.routes.javascript.Market.registerApp(),
				controllers.routes.javascript.Market.registerVisualization(),
				// Global search
				controllers.routes.javascript.GlobalSearch.index(),
				controllers.routes.javascript.GlobalSearch.search(),
				controllers.routes.javascript.GlobalSearch.complete()));						        
		        
	}

}
