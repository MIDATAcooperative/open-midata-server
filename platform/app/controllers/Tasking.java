package controllers;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Plugin;
import models.RecordsInfo;
import models.Space;
import models.Task;
import models.enums.AggregationType;
import models.enums.Frequency;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for managing user tasks
 *
 */
public class Tasking extends APIController {
	
	/**
	 * create a new task for another user
	 * @return
	 * @throws InternalServerException
	 * @throws JsonValidationException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result add() throws InternalServerException, JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();
		ObjectId userId = new ObjectId(request().username());
		JsonValidation.validate(json, "owner", "plugin", "shareBackTo", "context", "title", "description", "pluginQuery", "confirmQuery", "frequency");
		
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
		task.confirmQuery = JsonExtraction.extractMap(json.get("confirmQuery"));
		task.frequency = JsonValidation.getEnum(json, "frequency", Frequency.class);
		task.done = false;	
		Task.add(task);		
				
		return ok();
	}
	
	/**
	 * check if tasks are done
	 * @param who ID of user whos task shall be checked
	 * @param task task to be checked
	 * @throws AppException
	 */
	public static void check(ObjectId who, Task task) throws AppException {
		Date dateLimit = new Date(System.currentTimeMillis());
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		if (!task.done && task.confirmQuery != null) {
			task.confirmQuery.put("owner", "self");
			switch (task.frequency) {
			case DAILY: dateLimit = cal.getTime();break;
			case MONTHLY: cal.set(Calendar.DAY_OF_MONTH, 1);dateLimit = cal.getTime();break;
			case WEEKLY: cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);dateLimit = cal.getTime(); break;
			case YEARLY: cal.set(Calendar.DAY_OF_YEAR, 1);dateLimit = cal.getTime(); break;
			case ONCE: dateLimit = task.createdAt;
			}
			Collection<RecordsInfo> info = RecordManager.instance.info(who, task.shareBackTo, task.confirmQuery, AggregationType.ALL);
			if (info.size() == 1) {
				RecordsInfo recInf = info.iterator().next();
				if (recInf.count > 0 && recInf.newest.after(dateLimit)) task.done = true;
			}
		}
	}
	
	/**
	 * retrieve list of tasks for current user
	 * @return list of tasks
	 * @throws AppException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result list() throws AppException {
		
		ObjectId userId = new ObjectId(request().username());		
		Set<Task> tasks = Task.getAllByOwner(userId, Sets.create("owner", "createdBy", "plugin", "shareBackTo", "createdAt", "deadline", "context", "title", "description", "pluginQuery", "confirmQuery", "frequency", "done"));
		
		for (Task task : tasks) {
			check(userId, task);
			if (task.done && task.frequency.equals(Frequency.ONCE)) {
				Task.inactivateTask(task);
			}
		}
		return ok(Json.toJson(tasks));
	}
	
	/**
	 * lets a user execute one of her tasks
	 * @param taskIdStr ID of task
	 * @return space to be shown
	 * @throws AppException
	 */
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result execute(String taskIdStr) throws AppException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId taskId = new ObjectId(taskIdStr);
		
		Task task = Task.getByIdAndOwner(taskId, userId, Sets.create("owner", "createdBy", "plugin", "shareBackTo", "createdAt", "deadline", "context", "title", "description", "pluginQuery", "confirmQuery", "frequency", "done"));
		Plugin plugin = Plugin.getById(task.plugin, Sets.create("defaultQuery"));
		if (plugin == null) return badRequest("Unknown plugin in task.");
		
		Set<Space> spaces = Space.getAll(CMaps.map("owner", userId).map("context", task.context).map("visualization", task.plugin).map("autoShare", task.shareBackTo), Sets.create("name", "owner", "visualization", "app", "order", "type", "context", "autoShare"));
		
		
		Space space = null;
		if (!spaces.isEmpty()) space = spaces.iterator().next();
		
		if (space == null) space = Spaces.add(userId, task.title, task.plugin, null, task.context);
		
		if (task.shareBackTo != null) {
			if (space.autoShare == null) space.autoShare = new HashSet<ObjectId>();
			if (!space.autoShare.contains(task.shareBackTo)) {
				space.autoShare.add(task.shareBackTo);
				Space.set(space._id, "autoShare", space.autoShare);
			}
		}
		
		if (task.pluginQuery != null) {			  
			  RecordManager.instance.shareByQuery(userId, userId, space._id, task.pluginQuery);
		} else {					
			  RecordManager.instance.shareByQuery(userId, userId, space._id, plugin.defaultQuery);			  
		}
				
		return ok(Json.toJson(space));
	}
	

}
