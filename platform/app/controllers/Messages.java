package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Message;
import models.ModelException;
import models.Member;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.search.Search;
import utils.search.SearchException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;


public class Messages extends Controller {
	

	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
	
		JsonValidation.validate(json, "properties", "fields");
		
		// get messages
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Message> messages = new ArrayList<Message>(Message.getAll(properties, fields));
		
		Collections.sort(messages);
		return ok(Json.toJson(messages));
	}

	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result send() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "receivers", "title", "content");
		

		// validate receivers
		Set<ObjectId> receiverIds = ObjectIdConversion
				.castToObjectIds(JsonExtraction.extractSet(json.get("receivers")));
		Set<User> users = User.getAllUser(CMaps.map("_id",  receiverIds), Sets.create("messages.inbox"));
							
		if (receiverIds.size() != users.size()) {
			return badRequest("One or more users could not be found.");
		}

		// create message
		Message message = new Message();
		message._id = new ObjectId();
		message.sender = new ObjectId(request().username());
		message.receivers = receiverIds;
		message.created = DateTimeUtils.now();
		message.title = json.get("title").asText();
		message.content = json.get("content").asText();
		
		Message.add(message);
		
		// add to inbox and search index of receivers
		for (User user : users) {
			user.messages.get("inbox").add(message._id);
			try {
				User.set(user._id, "messages.inbox", user.messages.get("inbox"));
				Search.add(user._id, "message", message._id, message.title, message.content);
			} catch (ModelException e) {
				return badRequest(e.getMessage());
			} catch (SearchException e) {
				return badRequest(e.getMessage());
			}
		}
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)	
	@APICall
	public static Result move(String messageIdString, String from, String to) throws ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId messageId = new ObjectId(messageIdString);
		try {
			if (!User.exists(new ChainedMap<String, ObjectId>().put("_id", userId).put("messages." + from, messageId).get())) {
				return badRequest("No message with this id exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// update the respective message folders		
		User user = User.getById(userId, Sets.create("messages." + from, "messages." + to));
		user.messages.get(from).remove(messageId);
		user.messages.get(to).add(messageId);
		User.set(userId, "messages." + from, user.messages.get(from));
		User.set(userId, "messages." + to, user.messages.get(to));
		
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)	
	@APICall
	public static Result remove(String messageIdString) throws ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId messageId = new ObjectId(messageIdString);
		
		if (!User.exists(new ChainedMap<String, ObjectId>().put("_id", userId).put("messages.trash", messageId).get())) {
			return badRequest("No message with this id exists.");
		}
		
		// remove message from trash folder and user's search index		
		User user = User.getById(userId, Sets.create("messages.trash"));
		user.messages.get("trash").remove(messageId);
		User.set(userId, "messages.trash", user.messages.get("trash"));
		Search.delete(userId, "message", messageId);
		
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)	
	@APICall
	public static Result delete(String messageIdString) throws ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId messageId = new ObjectId(messageIdString);
		
		if (!Message.exists(new ChainedMap<String, ObjectId>().put("_id", messageId).get())) {
		   return badRequest("No message with this id exists.");
		}
		
		// remove the message from the user's inbox		
		Message.delete(userId, messageId);
		
		return ok();
	}
}
