package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.ModelException;
import models.Member;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchResult;
import views.html.details.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class Users extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result details(String userIdString) {
		return ok(user.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(Secured.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get users
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Member> users;
		try {
			users = new ArrayList<Member>(Member.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(users);
		return ok(Json.toJson(users));
	}

	@Security.Authenticated(Secured.class)
	public static Result getCurrentUser() {
		return ok(Json.toJson(new ObjectId(request().username())));
	}

	@Security.Authenticated(Secured.class)
	public static Result search(String query) {
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
		Set<String> fields = new ChainedSet<String>().add("name").get();
		List<Member> users;
		try {
			users = new ArrayList<Member>(Member.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(users);
		return ok(Json.toJson(users));
	}

	/**
	 * Prefetch contacts for completion suggestions.
	 */
	@Security.Authenticated(Secured.class)
	public static Result loadContacts() {
		ObjectId userId = new ObjectId(request().username());
		Set<ObjectId> contactIds = new HashSet<ObjectId>();
		Set<Member> contacts;
		try {
			Set<Circle> circles = Circle.getAll(new ChainedMap<String, ObjectId>().put("owner", userId).get(), new ChainedSet<String>()
					.add("members").get());
			for (Circle circle : circles) {
				contactIds.addAll(circle.members);
			}
			contacts = Member.getAll(new ChainedMap<String, Set<ObjectId>>().put("_id", contactIds).get(),
					new ChainedSet<String>().add("name").add("email").get());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Set<ObjectNode> jsonContacts = new HashSet<ObjectNode>();
		for (Member contact : contacts) {
			ObjectNode node = Json.newObject();
			node.put("value", contact.name + " (" + contact.email + ")");
			String[] split = contact.name.split(" ");
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
	@Security.Authenticated(Secured.class)
	public static Result complete(String query) {
		return ok(Json.toJson(Search.complete(Type.USER, query)));
	}

	/**
	 * Make the owner's records visible to a set of users.
	 */
	/*
	static void makeVisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds) throws ModelException {
		// get the visible fields for the owner from all users
		Map<String, Set<ObjectId>> properties = new ChainedMap<String, Set<ObjectId>>().put("_id", userIds).get();
		Set<String> fields = new ChainedSet<String>().add("visible." + ownerId.toString()).add("shared").get();
		Set<Member> users = Member.getAll(properties, fields);
		for (Member user : users) {
			if (user.visible.containsKey(ownerId.toString())) {
				Set<ObjectId> visibleRecords = user.visible.get(ownerId.toString());
				int originalSize = visibleRecords.size();
				visibleRecords.addAll(recordIds);

				// only update the field if some records were not visible before
				if (visibleRecords.size() > originalSize) {
					Member.set(user._id, "visible." + ownerId.toString(), visibleRecords);
				}
			} else {
				Member.set(user._id, "visible." + ownerId.toString(), recordIds);
			}

			// also update shared field if it has changed
			int originalSize = user.shared.size();
			user.shared.addAll(recordIds);
			if (user.shared.size() > originalSize) {
				Member.set(user._id, "shared", user.shared);
			}
		}
	}*/

	/**
	 * Make the owner's records invisible to a set of users, if these records are not shared with them via another circle.
	 *//*
	static void makeInvisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds) throws ModelException {
		// get the owner's circles
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", ownerId).get();
		Set<String> fields = new ChainedSet<String>().add("members").add("shared").get();
		Set<Circle> circles = Circle.getAll(properties, fields);
		Users.makeInvisible(ownerId, recordIds, userIds, circles);
	}*/

	/**
	 * Use this if the owner's circles have already been loaded.
	 *//*
	static void makeInvisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds, Set<Circle> circles) throws ModelException {
		for (ObjectId userId : userIds) {
			// get the records that are still shared with this user
			Set<ObjectId> stillSharedRecords = new HashSet<ObjectId>();
			for (Circle circle : circles) {
				if (circle.members.contains(userId)) {
					stillSharedRecords.addAll(circle.shared);
				}
			}

			// update visible field if visible records have changed
			recordIds.removeAll(stillSharedRecords);
			if (recordIds.size() > 0) {
				Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(),
						new ChainedSet<String>().add("visible." + ownerId.toString()).add("shared").get());
				Set<ObjectId> visibleRecords = user.visible.get(ownerId.toString());
				visibleRecords.removeAll(recordIds);
				Member.set(userId, "visible." + ownerId.toString(), visibleRecords);

				// also update shared field if it has changed
				int originalSize = user.shared.size();
				user.shared.removeAll(recordIds);
				if (user.shared.size() < originalSize) {
					Member.set(userId, "shared", user.shared);
				}
			}
		}
	}*/

	/**
	 * Add a record to the list of pushed records of the given user.
	 */
	static void pushRecord(ObjectId userId, ObjectId recordId) throws ModelException {
		Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>().add("pushed").get());
		user.pushed.add(recordId);
		Member.set(user._id, "pushed", user.pushed);
	}

	/**
	 * Remove a record from the list of pushed records of the given user.
	 */
	static void pullRecord(ObjectId userId, ObjectId recordId) throws ModelException {
		Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>().add("pushed").get());
		user.pushed.remove(recordId);
		Member.set(user._id, "pushed", user.pushed);
	}

	/**
	 * Clear the list of pushed records of the current user.
	 */
	@Security.Authenticated(Secured.class)
	public static Result clearPushed() {
		ObjectId userId = new ObjectId(request().username());
		try {
			Member.set(userId, "pushed", new HashSet<ObjectId>());
			Member.set(userId, "login", DateTimeUtils.now());
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Clear the list of shared records of the current user.
	 */
	@Security.Authenticated(Secured.class)
	public static Result clearShared() {
		ObjectId userId = new ObjectId(request().username());
		try {
			Member.set(userId, "shared", new HashSet<ObjectId>());
			Member.set(userId, "login", DateTimeUtils.now());
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Get a user's authorization tokens for an app.
	 */
	static Map<String, String> getTokens(ObjectId userId, ObjectId appId) throws ModelException {
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
	static void setTokens(ObjectId userId, ObjectId appId, Map<String, String> tokens) throws ModelException {
		Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>().add("tokens").get());
		user.tokens.put(appId.toString(), tokens);
		Member.set(userId, "tokens", user.tokens);
	}
}
