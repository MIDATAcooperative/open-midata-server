package controllers.providers;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.mvc.Result;

import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
import models.ModelException;
import models.Space;
import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.auth.CodeGenerator;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.defaultpages.badRequest;
import actions.APICall;
import controllers.APIController;
import controllers.KeyManager;
import controllers.MemberKeys;
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
		user.role = UserRole.PROVIDER;		
		user.subrole = SubUserRole.MANAGER;
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
		
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		
		user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
		user.security = AccountSecurityLevel.KEY;
		
		HealthcareProvider.add(provider);
		user.provider = provider._id;
		HPUser.add(user);
		
		KeyManager.instance.unlock(user._id, "12345");
		
		session().clear();
		session("id", user._id.toString());
		session("role", "provider");
		session("org", provider._id.toString());
		
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
		HPUser user = HPUser.getByEmail(email, Sets.create("email","password","provider"));
		
		if (user == null) return badRequest("Invalid user or password.");
		if (!HPUser.authenticationValid(password, user.password)) {
			return badRequest("Invalid user or password.");
		}
		
		KeyManager.instance.unlock(user._id, "12345");
		// user authenticated
		session().clear();
		session("id", user._id.toString());
		session("role", "provider");
		session("org", user.provider.toString());
		return ok();
	}
	
	@Security.Authenticated(ProviderSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result search() throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
			
		JsonValidation.validate(json, "midataID", "birthday");
		
		String midataID = JsonValidation.getString(json, "midataID");
		Date birthday = JsonValidation.getDate(json, "birthday");
		
		Member result = Member.getByMidataIDAndBirthday(midataID, birthday, Sets.create("firstname","birthday", "sirname","city","zip","country","email","phone","mobile","ssn","address1","address2"));
		HPUser hpuser = HPUser.getById(userId, Sets.create("provider", "firstname", "sirname"));
		
		MemberKeys.getOrCreate(hpuser, result);
		
		return ok(Json.toJson(result));
	}
	
	@Security.Authenticated(ProviderSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result list() throws JsonValidationException, ModelException {
		JsonNode json = request().body().asJson();
		
		ObjectId userId = new ObjectId(request().username());
		//JsonValidation.validate(json, "midataID", "birthday");
		Set<MemberKey> memberKeys = MemberKey.getByAuthorizedPerson(userId, Sets.create("owner"));
		Set<ObjectId> ids = new HashSet<ObjectId>();
		for (MemberKey key : memberKeys) ids.add(key.owner);
		Set<Member> result = Member.getAll(CMaps.map("_id", ids), Sets.create("_id", "firstname","birthday", "sirname"));
		
		return ok(Json.toJson(result));
	}
	
	@Security.Authenticated(ProviderSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result getMember(String id) throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId memberId = new ObjectId(id);
		
		MemberKey memberKey = MemberKey.getByOwnerAndAuthorizedPerson(memberId, userId);
		
		Member result = Member.getById(memberId, Sets.create("firstname","birthday", "sirname","city","zip","country","email","phone","mobile","ssn","address1","address2"));
		if (result==null) return badRequest("Member does not exist.");
		
		ObjectNode obj = Json.newObject();
		obj.put("member", Json.toJson(result));
		if (memberKey != null) obj.put("memberkey", Json.toJson(memberKey));
		return ok(obj);
	}
	
	@Security.Authenticated(ProviderSecured.class)
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	public static Result getVisualizationToken() throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());
		JsonNode json = request().body().asJson();
						
		JsonValidation.validate(json, "member");
		ObjectId memberId = JsonValidation.getObjectId(json, "member");
		MemberKey memberKey = MemberKey.getByOwnerAndAuthorizedPerson(memberId, userId);

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(memberKey.aps, userId);
		return ok(spaceToken.encrypt());
	}
}
