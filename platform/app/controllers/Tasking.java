package controllers;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Plugin;
import models.Space;
import models.Task;
import models.enums.Frequency;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.Sets;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class Tasking extends Controller {
	
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result add() throws ModelException, JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();
		ObjectId userId = new ObjectId(request().username());
		JsonValidation.validate(json, "owner", "plugin", "shareBackTo", "context", "title", "description", "pluginQuery", "frequency");
		
		Task task = new Task();
		task._id = new ObjectId();
		task.owner = JsonValidation.getObjectId(json, "owner");
		task.createdBy = userId;
		task.plugin = JsonValidation.getObjectId(json, "plugin");
		task.shareBackTo = JsonValidation.getObjectId(json, "shareBackTo");
		task.createdAt = new Date(System.currentTimeMillis());
		task.deadline = JsonValidation.getDate(json, "deadline");
		task.context = JsonValidation.getString(json, "context");
		task.title = JsonValidation.getString(json, "title");
		task.description = JsonValidation.getString(json, "description");
		task.pluginQuery = JsonExtraction.extractMap(json.get("pluginQuery"));
		task.frequency = JsonValidation.getEnum(json, "frequency", Frequency.class);
		task.done = false;	
		Task.add(task);		
				
		return ok();
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result list() throws ModelException {
		
		ObjectId userId = new ObjectId(request().username());
		
		Set<Task> tasks = Task.getAllByOwner(userId, Sets.create("owner", "createdBy", "plugin", "shareBackTo", "createdAt", "deadline", "context", "title", "description", "pluginQuery", "confirmQuery", "frequency", "done"));
		return ok(Json.toJson(tasks));
	}
	
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result execute(String taskIdStr) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId taskId = new ObjectId(taskIdStr);
		
		Task task = Task.getByIdAndOwner(taskId, userId, Sets.create("owner", "createdBy", "plugin", "shareBackTo", "createdAt", "deadline", "context", "title", "description", "pluginQuery", "confirmQuery", "frequency", "done"));
		Plugin plugin = Plugin.getById(task.plugin, Sets.create("defaultQuery"));
		if (plugin == null) return badRequest("Unknown plugin in task.");
		
		Space space = Spaces.add(userId, task.title, task.plugin, null, task.context);
		
		if (task.shareBackTo != null) {
			if (space.autoShare == null) space.autoShare = new HashSet<ObjectId>();
			space.autoShare.add(task.shareBackTo);
			Space.set(space._id, "autoShare", space.autoShare);
		}
		
		if (task.pluginQuery != null) {			  
			  RecordSharing.instance.shareByQuery(userId, userId, space._id, task.pluginQuery);
		} else {					
			  RecordSharing.instance.shareByQuery(userId, userId, space._id, plugin.defaultQuery);			  
		}
				
		return ok(Json.toJson(space));
	}
	

}
