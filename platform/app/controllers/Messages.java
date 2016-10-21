package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Message;
import models.MidataId;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for a message system. Needs to be rewritten in a secure way. Not used.
 *
 */
public class Messages extends Controller {
	

	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
	
		JsonValidation.validate(json, "properties", "fields");
		
		// get messages
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "sender", "receivers");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Message> messages = new ArrayList<Message>(Message.getAll(properties, fields));
		
		Collections.sort(messages);
		return ok(Json.toJson(messages));
	}

	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result send() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "receivers", "title", "content");
		

		// validate receivers
		Set<MidataId> receiverIds = ObjectIdConversion
				.toMidataIds(JsonExtraction.extractStringSet(json.get("receivers")));
		Set<User> users = User.getAllUser(CMaps.map("_id",  receiverIds), Sets.create("messages.inbox"));
							
		if (receiverIds.size() != users.size()) {
			return badRequest("One or more users could not be found.");
		}

		// create message
		Message message = new Message();
		message._id = new MidataId();
		message.sender = new MidataId(request().username());
		message.receivers = receiverIds;
		message.created = new Date();
		message.title = JsonValidation.getString(json, "title");
		message.content = JsonValidation.getString(json, "content");
		
		Message.add(message);
		
		// add to inbox and search index of receivers
		for (User user : users) {
			user.messages.get("inbox").add(message._id);			
		}
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)	
	@APICall
	public static Result move(String messageIdString, String from, String to) throws InternalServerException {
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId messageId = new MidataId(messageIdString);
		try {
			if (!User.exists(new ChainedMap<String, MidataId>().put("_id", userId).put("messages." + from, messageId).get())) {
				return badRequest("No message with this id exists.");
			}
		} catch (InternalServerException e) {
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
	public static Result remove(String messageIdString) throws InternalServerException {
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId messageId = new MidataId(messageIdString);
		
		if (!User.exists(new ChainedMap<String, MidataId>().put("_id", userId).put("messages.trash", messageId).get())) {
			return badRequest("No message with this id exists.");
		}
		
		// remove message from trash folder and user's search index		
		User user = User.getById(userId, Sets.create("messages.trash"));
		user.messages.get("trash").remove(messageId);
		User.set(userId, "messages.trash", user.messages.get("trash"));
		//Search.delete(userId, "message", messageId);
		
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)	
	@APICall
	public static Result delete(String messageIdString) throws InternalServerException {
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId messageId = new MidataId(messageIdString);
		
		if (!Message.exists(new ChainedMap<String, MidataId>().put("_id", messageId).get())) {
		   return badRequest("No message with this id exists.");
		}
		
		// remove the message from the user's inbox		
		Message.delete(userId, messageId);
		
		return ok();
	}
}
