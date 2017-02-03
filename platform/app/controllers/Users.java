package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Circle;
import models.Consent;
import models.Developer;
import models.HealthcareProvider;
import models.History;
import models.Member;
import models.MidataId;
import models.Research;
import models.Space;
import models.Study;
import models.User;
import models.enums.ContractStatus;
import models.enums.EventType;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.MemberSecured;
import utils.auth.PortalSessionToken;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * user related functions
 *
 */
public class Users extends APIController {
	
	/**
	 * retrieve a list of users matching some criteria
	 * allowed restrictions and returned fields depend heavily on user role
	 * @return list of users
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result get() throws AppException, JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "properties", "fields");
		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "developer");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		// check authorization
		
		if (!getRole().equals(UserRole.ADMIN) && !properties.containsKey("_id") && !properties.containsKey("developer")) properties.put("searchable", true);
		boolean postcheck = false;		
		if (!getRole().equals(UserRole.ADMIN) && !properties.containsKey("email") && !properties.containsKey("midataID") && !properties.containsKey("_id") && !properties.containsKey("developer")) {
			throw new AuthException("error.notauthorized.action", "Search must be restricted");
		}
		UserRole role = null;
		
		
		
		if (properties.containsKey("_id") && properties.get("_id").toString().equals(request().username())) {
		  Rights.chk("Users.getSelf", getRole(), properties, fields);
		} else if (properties.containsKey("role")) {
		  role = UserRole.valueOf(properties.get("role").toString());
		  if (Rights.existsAction("Users.get"+role, getRole())) {
		    Rights.chk("Users.get"+role.toString(), getRole(), properties, fields);
		  } else {
			Rights.chk("Users.get", getRole(), properties, fields);
		  }		  
		} else if (fields.contains("role")) {
			// Check later
			postcheck = true;			
		} else {		
		  Rights.chk("Users.get", getRole(), properties, fields);
		  
		}

		if (!getRole().equals(UserRole.ADMIN)) properties.put("status", User.NON_DELETED);
		
		// execute		
		if (fields.contains("name")) { fields.add("firstname"); fields.add("lastname"); }	
		
		if (properties.containsKey("email")) {
			properties.put("emailLC", properties.get("email").toString().toLowerCase());
			properties.remove("email");
		}				
		
		List<User> users;
		if (role != null && role == UserRole.DEVELOPER) {
		  users = new ArrayList<User>(Developer.getAll(properties, fields, 100));
		} else {
		  users = new ArrayList<User>(Member.getAll(properties, fields, 100));
		}
		
		if (postcheck) {
			for (User mem : users) {
				if (Rights.existsAction("Users.get"+mem.getRole().toString(), getRole())) {
				  Rights.chk("Users.get"+mem.getRole().toString(), getRole(), properties, fields);
				}  else {
				  Rights.chk("Users.get", getRole(), properties, fields);
				}
			}
		}
		
		if (fields.contains("name")) {
			for (User user : users) user.name = (user.firstname + " "+ user.lastname).trim();
		}
		
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields));
	}
		
	/**
	 * get ID of currently logged in user
	 * @return
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result getCurrentUser() {
						
		ObjectNode obj = Json.newObject();								
		obj.put("role", PortalSessionToken.session().getRole().toString());
		obj.put("user", PortalSessionToken.session().getUserId().toString());
																
		return ok(obj);
	}

    /**
     * text search for users
     * @param query search string
     * @return list of users (json)
     * @throws AppException
     */
	@Security.Authenticated(MemberSecured.class)
	@APICall
	public static Result search(String query) throws AppException {
		
		forbidSubUserRole(SubUserRole.TRIALUSER, SubUserRole.NONMEMBERUSER);
		forbidSubUserRole(SubUserRole.STUDYPARTICIPANT, SubUserRole.NONMEMBERUSER);
		forbidSubUserRole(SubUserRole.APPUSER, SubUserRole.NONMEMBERUSER);
		
		Set<String> fields =  Sets.create("firstname", "lastname", "name");
		Set<Member> result = Member.getAll(CMaps.map("email", query).map("searchable", true).map("status", User.NON_DELETED).map("role", UserRole.MEMBER), fields);
		
		for (Member member : result) {
			member.name = member.firstname+" "+member.lastname;
		}
		
		List<Member> users = new ArrayList<Member>(result);
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields));
	}

	/**
	 * Prefetch contacts for completion suggestions.
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result loadContacts() throws InternalServerException {
		MidataId userId = new MidataId(request().username());
		Set<MidataId> contactIds = new HashSet<MidataId>();
		Set<Member> contacts;
	
		Set<Circle> circles = Circle.getAll(CMaps.map("owner", userId), Sets.create("authorized"));
		for (Circle circle : circles) {
			contactIds.addAll(circle.authorized);
		}
		contacts = Member.getAll(CMaps.map("_id", contactIds),Sets.create("firstname","lastname","email"));
	
		Set<ObjectNode> jsonContacts = new HashSet<ObjectNode>();
		for (Member contact : contacts) {
			ObjectNode node = Json.newObject();
			node.put("value", contact.firstname+" "+contact.lastname + " (" + contact.email + ")");
			String[] split = (contact.firstname+" "+contact.lastname).split(" ");
			String[] tokens = new String[split.length + 1];
			System.arraycopy(split, 0, tokens, 0, split.length);
			tokens[tokens.length - 1] = contact.email;
			node.put("tokens", Json.toJson(tokens));
			node.put("id", contact._id.toString());
			jsonContacts.add(node);
		}
		return ok(Json.toJson(jsonContacts));
	}

	/**
	 * Suggest users that complete the given query.
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result complete(String query) {
		return ok(Json.toJson(Collections.EMPTY_LIST)); //Search.complete(Type.USER, query)));
	}


	/**
	 * Get a user's authorization tokens for an app.
	 */
	protected static Map<String, String> getTokens(MidataId userId, MidataId appId) throws InternalServerException {
		Member user = Member.get(new ChainedMap<String, MidataId>().put("_id", userId).get(), new ChainedSet<String>().add("tokens").get());
		if (user.tokens.containsKey(appId.toString())) {
			return user.tokens.get(appId.toString());
		} else {
			return new HashMap<String, String>();
		}
	}

	/**
	 * Set authorization tokens, namely the access and refresh token.
	 */
	protected static void setTokens(MidataId userId, MidataId appId, Map<String, String> tokens) throws InternalServerException {
		Member user = Member.get(new ChainedMap<String, MidataId>().put("_id", userId).get(), new ChainedSet<String>().add("tokens").get());
		user.tokens.put(appId.toString(), tokens);
		Member.set(userId, "tokens", user.tokens);
	}
	
	/**
	 * Updates the address information of a user.
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result updateAddress() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "email", "firstname", "lastname", "gender", "city", "zip", "country", "address1");
		String email = JsonValidation.getEMail(json, "email");
		String firstName = JsonValidation.getString(json, "firstname");
		String lastName = JsonValidation.getString(json, "lastname");
		
		MidataId userId = new MidataId(request().username());
				
		User user = User.getById(userId, User.ALL_USER); 
		
		user.email = email;
		user.emailLC = email.toLowerCase();
		user.name = firstName + " " + lastName;
		user.address1 = JsonValidation.getString(json, "address1");
		user.address2 = JsonValidation.getString(json, "address2");
		user.city = JsonValidation.getString(json, "city");
		user.zip = JsonValidation.getString(json, "zip");
		user.phone = JsonValidation.getString(json, "phone");
		user.mobile = JsonValidation.getString(json, "mobile");
		user.country = JsonValidation.getString(json, "country");
		user.firstname = JsonValidation.getString(json, "firstname");
		user.lastname = JsonValidation.getString(json, "lastname");
		user.gender = JsonValidation.getEnum(json, "gender", Gender.class);
		
		User.set(user._id, "email", user.email);
		User.set(user._id, "emailLC", user.emailLC);
		User.set(user._id, "name", user.name);
		User.set(user._id, "address1", user.address1);
		User.set(user._id, "address2", user.address2);
		User.set(user._id, "city", user.city);
		User.set(user._id, "zip", user.zip);
		User.set(user._id, "phone", user.phone);
		User.set(user._id, "mobile", user.mobile);
		User.set(user._id, "country", user.country);
		User.set(user._id, "firstname", user.firstname);
		User.set(user._id, "lastname", user.lastname);
		User.set(user._id, "gender", user.gender);		
		
		user.updateKeywords(true);
		
		if (user.role.equals(UserRole.MEMBER)) {		  
		  PatientResourceProvider.updatePatientForAccount(user._id);
		}
		
		user.addHistory(new History(EventType.CONTACT_ADDRESS_CHANGED, user, null));
		
		return ok();		
	}
	
	
	
	/**
	 * Update user settings like language and public settings
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result updateSettings() throws AppException {
		// validate 
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "language");
		
		boolean searchable = JsonValidation.getBoolean(json, "searchable");
		
		if (searchable) {
			forbidSubUserRole(SubUserRole.TRIALUSER, SubUserRole.NONMEMBERUSER);
			forbidSubUserRole(SubUserRole.STUDYPARTICIPANT, SubUserRole.NONMEMBERUSER);
			forbidSubUserRole(SubUserRole.APPUSER, SubUserRole.NONMEMBERUSER);
		}
		
		String language = JsonValidation.getString(json, "language");
					
		MidataId userId = new MidataId(request().username());
				
		User user = User.getById(userId, Sets.create("_id")); 
		
		User.set(user._id, "searchable", searchable);
		User.set(user._id, "language", language);
		
		return ok();		
	}
	
	/**
	 * Request MIDATA membership of current user
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public static Result requestMembership() throws AppException {
		MidataId userId = new MidataId(request().username());
		return requestMembershipHelper(userId);
	}
	
	public static Result requestMembershipHelper(MidataId userId) throws AppException {			
		
		Member user = Member.getById(userId, Sets.create("_id", "status", "role", "subroles", "history", "emailStatus", "confirmedAt", "contractStatus", "agbStatus", "lastname", "firstname")); 
		if (user == null) throw new InternalServerException("error.internal", "User record not found.");
		if (user.subroles.contains(SubUserRole.TRIALUSER) || user.subroles.contains(SubUserRole.NONMEMBERUSER) || user.subroles.contains(SubUserRole.STUDYPARTICIPANT) || user.subroles.contains(SubUserRole.APPUSER)) {
			
		} else throw new BadRequestException("invalid.status_transition", "No membership request required.");
		
		
		
		if (user.agbStatus.equals(ContractStatus.NEW)) {
			user.agbStatus = ContractStatus.REQUESTED;
			Member.set(user._id, "agbStatus", ContractStatus.REQUESTED);
		} else if (user.contractStatus.equals(ContractStatus.NEW)) {
			user.contractStatus = ContractStatus.REQUESTED;
			Member.set(user._id, "contractStatus", ContractStatus.REQUESTED);
		}
						
		user.addHistory(new History(EventType.MEMBERSHIP_REQUEST, user, null));
		
		if (InstanceConfig.getInstance().getInstanceType().getAutoGrandMembership()) {
			
			if (user.confirmedAt == null) {
				user.confirmedAt = new Date(System.currentTimeMillis());			
	    	    user.set("confirmedAt", user.confirmedAt);
			}
			
			if (user.agbStatus == ContractStatus.REQUESTED) {				
			   user.agbStatus = ContractStatus.SIGNED;
			   Member.set(user._id, "agbStatus", ContractStatus.SIGNED);
			} else {
			   user.contractStatus = ContractStatus.SIGNED;
			   Member.set(user._id, "contractStatus", ContractStatus.SIGNED);
			}
			Application.checkAccount(user);
		}
				
		return ok();
	}
	
	/**
	 * completely wipe account of current user. This is only working on test instances - other instances will throw an exception.
	 * @return 200 ok
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result accountWipe() throws AppException {
		if (!InstanceConfig.getInstance().getInstanceType().getAccountWipeAvailable()) throw new InternalServerException("error.internal", "Only allowed on demo server");
		
		MidataId userId = new MidataId(request().username());
						
		Set<Space> spaces = Space.getAllByOwner(userId, Space.ALL);
		for (Space space : spaces) {
			RecordManager.instance.deleteAPS(space._id, userId);
			Space.delete(userId, space._id);
		}
		
		Set<Consent> consents = Consent.getAllByOwner(userId, CMaps.map(), Consent.ALL);
		for (Consent consent : consents) {
			RecordManager.instance.deleteAPS(consent._id, userId);
			Circle.delete(userId, consent._id);
		}
		
		Set<Consent> consents2 = Consent.getAllByAuthorized(userId);
		for (Consent consent : consents2) {
			consent = Consent.getByIdAndAuthorized(consent._id, userId, Sets.create("authorized"));
			consent.authorized.remove(userId);
			Consent.set(consent._id, "authorized", consent.authorized);			
		}
		
		RecordManager.instance.wipe(userId, CMaps.map("owner", "self"));
		RecordManager.instance.wipe(userId, CMaps.map("owner", "self").map("streams", "true"));
		
		if (getRole().equals(UserRole.RESEARCH)) {
			Set<Study> studies = Study.getByOwner(PortalSessionToken.session().org, Sets.create("_id"));
			
			for (Study study : studies) {
				controllers.research.Studies.deleteStudy(userId, study._id);
			}
			
			Research.delete(PortalSessionToken.session().org);			
			
		}
		
		if (getRole().equals(UserRole.PROVIDER)) {
			HealthcareProvider.delete(PortalSessionToken.session().org);
		}
		
		User.delete(userId);
		
		return ok();
	}
}
