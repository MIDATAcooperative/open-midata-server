package controllers.research;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bson.types.ObjectId;

import models.Member;
import models.Research;
import models.ResearchUser;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.UserRole;
import models.enums.UserStatus;
 
import com.fasterxml.jackson.databind.JsonNode;

import controllers.APIController;
import controllers.KeyManager;
import controllers.routes;

import actions.APICall; 
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result; 
import utils.access.RecordSharing;
import utils.auth.CodeGenerator;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.exceptions.ModelException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * login and registration functions for researchers
 *
 */
public class Researchers extends APIController {

	/**
	 * register a new researcher
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws ModelException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "email", "description", "firstname", "lastname", "gender", "city", "zip", "country", "address1");
					
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
		user.role = UserRole.RESEARCH;
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip  = JsonValidation.getString(json, "zip");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname"); 
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		
		user.password = ResearchUser.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		
		Research.add(research);
		user.organization = research._id;
		ResearchUser.add(user);
		
		KeyManager.instance.unlock(user._id, "12345");
		
		RecordSharing.instance.createPrivateAPS(user._id, user._id);		
		
		session().clear();
		session("id", user._id.toString());
		session("role", UserRole.RESEARCH.toString());
		session("org", research._id.toString());
		
		return ok();
	}
	
	/**
	 * login a researcher
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws ModelException
	 */
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
		KeyManager.instance.unlock(user._id, "12345");
		
		session().clear();
		session("id", user._id.toString());
		session("role", UserRole.RESEARCH.toString());
		session("org", user.organization.toString());
		return ok();
	}
		
}
