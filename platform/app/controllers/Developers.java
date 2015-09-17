package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import models.Developer;

import models.ModelException;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Result;
import utils.auth.CodeGenerator;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

public class Developers extends APIController {

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result register() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1");
							
		String email = JsonValidation.getEMail(json, "email");
		if (Developer.existsByEMail(email)) return inputerror("email", "exists", "A user with this email address already exists.");
				
		
		Developer user = new Developer(email);
		user._id = new ObjectId();
		user.role = UserRole.DEVELOPER;				
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
		
		user.password = Developer.encrypt(JsonValidation.getPassword(json, "password"));		
		user.registeredAt = new Date();		
		
		user.status = UserStatus.NEW;		
		user.contractStatus = ContractStatus.NEW;		
		user.confirmationCode = CodeGenerator.nextCode();
		
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
				
		Developer.add(user);
		RecordSharing.instance.createPrivateAPS(user._id, user._id);
		
		KeyManager.instance.unlock(user._id, "12345");
		
		session().clear();
		session("id", user._id.toString());
		session("role", "developer");		
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result login() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "email", "password");
		
		String email = JsonValidation.getString(json, "email");
		String password = JsonValidation.getString(json, "password");
		Developer user = Developer.getByEmail(email, Sets.create("email","password","provider"));
		
		if (user == null) return badRequest("Invalid user or password.");
		if (!Developer.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		}
		
		
		//user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		//Developer.set(user._id, "publicKey", user.publicKey);
		
		KeyManager.instance.unlock(user._id, "12345");
		
		//RecordSharing.instance.createPrivateAPS(user._id, user._id);
		
		
		// user authenticated
		session().clear();
		session("id", user._id.toString());
		session("role", "developer");		
		return ok();
	}
}
