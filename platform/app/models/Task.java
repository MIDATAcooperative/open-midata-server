package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.Frequency;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.exceptions.InternalServerException;


public class Task extends Model {

	private static String collection = "tasks";
	
	public ObjectId owner;
	public ObjectId createdBy;
	public ObjectId plugin;
	public ObjectId shareBackTo;
	public Date createdAt;
	public Date deadline;
	public String context;
	public String title;
	public String description;
	public Map<String, Object> pluginQuery;
	public Map<String, Object> confirmQuery;
	public Frequency frequency;
	public boolean done;
	
	public static void add(Task task) throws InternalServerException {
		Model.insert(collection, task);
	}
	
	public static Set<Task> getAllByOwner(ObjectId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(Task.class, collection, CMaps.map("owner", owner), fields);
	}
	
	public static void set(ObjectId taskId, String field, Object value) throws InternalServerException {
		Model.set(Task.class, collection, taskId, field, value);
	}
	
	public static Task getByIdAndOwner(ObjectId taskId, ObjectId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Task.class, collection, CMaps.map("_id", taskId).map("owner", ownerId), fields);
	}

	public static void inactivateTask(Task task) throws InternalServerException {
		Model.delete(Task.class, collection, CMaps.map("_id", task._id));		
	}
}
