package controllers.providers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import play.mvc.Result;

import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.ModelException;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.UserStatus;
import utils.auth.CodeGenerator;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.defaultpages.badRequest;
import actions.APICall;
import controllers.APIController;
import controllers.routes;
import actions.APICall; 
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import controllers.providers.ProviderSecured;
import play.mvc.Result;
import play.mvc.Security;


public class Providers extends APIController {

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "firstname", "sirname", "gender", "city", "zip", "country", "address1");
					
		String name = JsonValidation.getString(json, "name");
		if (HealthcareProvider.existsByName(name)) return inputerror("name", "exists", "A healthcare provider with this name already exists.");
		
		String email = JsonValidation.getEMail(json, "email");
		if (HPUser.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
		
		HealthcareProvider provider = new HealthcareProvider();
		
		provider._id = new ObjectId();
		provider.name = name;
		//research.description = JsonValidation.getString(json, "description");
		
		HPUser user = new HPUser(email);
		user._id = new ObjectId();
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.sirname = JsonValidation.getString(json, "sirname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		
		user.password = HPUser.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
		user.confirmationCode = CodeGenerator.nextCode();
		
		HealthcareProvider.add(provider);
		user.provider = provider._id;
		HPUser.add(user);
		
		session().clear();
		session("id", user._id.toString());
		session("role", "provider");
		session("org", provider._id.toString());
		
		return ok(routes.ProviderFrontend.messages().url());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result login() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
		String email = JsonValidation.getString(json, "email");
		String password = JsonValidation.getString(json, "password");
		HPUser user = HPUser.getByEmail(email, Sets.create("email","password","provider"));
		
		if (user == null) return badRequest("Invalid user or password.");
		if (!HPUser.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		}
		
		// user authenticated
		session().clear();
		session("id", user._id.toString());
		session("role", "provider");
		session("org", user.provider.toString());
		return ok(routes.ProviderFrontend.messages().url());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result search() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "firstname", "sirname");
		
		Map<String, Object> criteria = 
				CMaps.mapNotEmpty("firstname", JsonValidation.getString(json, "firstname"))
				     .mapNotEmpty("sirname", JsonValidation.getString(json, "sirname"))
				     .mapNotEmpty("ssn", JsonValidation.getString(json, "ssn"))
				     .mapNotEmpty("city", JsonValidation.getString(json, "city"))
				     .mapNotEmpty("zip", JsonValidation.getString(json, "zip"))
				     .mapNotEmpty("country", JsonValidation.getString(json, "country"))
				     .mapNotEmpty("email", JsonValidation.getString(json, "email"));
		
		Set<Member> results = Member.getAll(criteria, Sets.create("firstname","birthday", "sirname","city","zip","country","email","phone","mobile","ssn","address1"));
		
		return ok(Json.toJson(results));
	}
	
	@Security.Authenticated(ProviderSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result getMember(String id) throws JsonValidationException, ModelException {
		
		ObjectId memberId = new ObjectId(id);
		
		Member result = Member.getById(memberId, Sets.create("firstname","birthday", "sirname","city","zip","country","email","phone","mobile","ssn","address1","address2"));
		if (result==null) return badRequest("Member does not exist.");
		
		return ok(Json.toJson(result));
	}
}
