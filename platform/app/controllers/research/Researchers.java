package controllers.research;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

import models.Member;
import models.ModelException;
import models.Research;
import models.ResearchUser;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.UserStatus;
 
import com.fasterxml.jackson.databind.JsonNode;

import controllers.APIController;
import controllers.routes;

import actions.APICall; 
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result; 
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class Researchers extends APIController {

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "description", "firstname", "sirname", "gender", "city", "zip", "country", "address1");
					
		String name = JsonValidation.getString(json, "name");
		if (Research.existsByName(name)) return inputerror("name", "exists", "A research organization with this name already exists.");
		
		String email = JsonValidation.getEMail(json, "email");
		if (ResearchUser.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
		
		Research research = new Research();
		
		research._id = new ObjectId();
		research.name = name;
		research.description = JsonValidation.getString(json, "description");
		
		ResearchUser user = new ResearchUser(email);
		user._id = new ObjectId();
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.sirname = JsonValidation.getString(json, "sirname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		
		user.password = ResearchUser.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
		user.confirmationCode = "ABCD";
		
		Research.add(research);
		user.organization = research._id;
		ResearchUser.add(user);
		
		session().clear();
		session("id", user._id.toString());
		session("role", "research");
		session("org", research._id.toString());
		
		return ok(routes.ResearchFrontend.messages().url());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result login() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
		String email = JsonValidation.getString(json, "email");
		String password = JsonValidation.getString(json, "password");
		ResearchUser user = ResearchUser.getByEmail(email, Sets.create("email","password","organization"));
		
		if (user == null) return badRequest("Invalid user or password.");
		if (!ResearchUser.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		}
		
		// user authenticated
		session().clear();
		session("id", user._id.toString());
		session("role", "research");
		session("org", user.organization.toString());
		return ok(routes.ResearchFrontend.messages().url());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email");
		
		ResearchUser user = ResearchUser.getByEmail(JsonValidation.getEMail(json, "email"), Sets.create("gender", "firstname"));
		
		return ok(Json.toJson(user));
	}
}
