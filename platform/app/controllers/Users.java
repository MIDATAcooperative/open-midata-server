package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Member;
import models.User;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
import utils.auth.AnyRoleSecured;
import utils.auth.Rights;
import utils.auth.MemberSecured;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchResult;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
		// check authorization
		boolean postcheck = false;		
		if (properties.containsKey("_id") && properties.get("_id").toString().equals(request().username())) {
		  Rights.chk("Users.getSelf", getRole(), properties, fields);
		} else if (properties.containsKey("role")) {
		  UserRole role = UserRole.valueOf(properties.get("role").toString());
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

		// execute		
		if (fields.contains("name")) { fields.add("firstname"); fields.add("lastname"); }		
		List<Member> users = new ArrayList<Member>(Member.getAll(properties, fields));
		
		if (postcheck) {
			for (Member mem : users) {
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
		return ok(Json.toJson(new ObjectId(request().username())));
	}

    /**
     * text search for users
     * @param query search string
     * @return list of users (json)
     * @throws InternalServerException
     */
	@Security.Authenticated(MemberSecured.class)
	@APICall
	public static Result search(String query) throws InternalServerException {
		// TODO use caching/incremental retrieval of results (scrolls)
		List<SearchResult> searchResults = Search.search(Type.USER, query);
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		for (SearchResult searchResult : searchResults) {
			userIds.add(new ObjectId(searchResult.id));
		}

		// remove own entry, if present
		userIds.remove(new ObjectId(request().username()));

		// get name for ids
		Map<String, Set<ObjectId>> properties = new ChainedMap<String, Set<ObjectId>>().put("_id", userIds).get();
		Set<String> fields = Sets.create("name", "firstname", "lastname");
		List<Member> users = new ArrayList<Member>(Member.getAll(properties, fields));		
		for (User user : users) user.name = (user.firstname + " "+ user.lastname).trim();
		
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields));
	}

	/**
	 * Prefetch contacts for completion suggestions.
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result loadContacts() throws InternalServerException {
		ObjectId userId = new ObjectId(request().username());
		Set<ObjectId> contactIds = new HashSet<ObjectId>();
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
		return ok(Json.toJson(Search.complete(Type.USER, query)));
	}


	/**
	 * Get a user's authorization tokens for an app.
	 */
	protected static Map<String, String> getTokens(ObjectId userId, ObjectId appId) throws InternalServerException {
		Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>().add("tokens").get());
		if (user.tokens.containsKey(appId.toString())) {
			return user.tokens.get(appId.toString());
		} else {
			return new HashMap<String, String>();
		}
	}

	/**
	 * Set authorization tokens, namely the access and refresh token.
	 */
	protected static void setTokens(ObjectId userId, ObjectId appId, Map<String, String> tokens) throws InternalServerException {
		Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>().add("tokens").get());
		user.tokens.put(appId.toString(), tokens);
		Member.set(userId, "tokens", user.tokens);
	}
}
