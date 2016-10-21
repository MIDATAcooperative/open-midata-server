package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.Frequency;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;


/**
 * Data model class for a task that a member should do
 *
 */
public class Task extends Model {

	private static String collection = "tasks";
	
	/**
	 * owner id of the task.
	 * 
	 * The owner of the task is the user that should do the action described by this task.
	 */
	public MidataId owner;
	
	/**
	 * id of creator of this task
	 */
	public MidataId createdBy;
	
	/**
	 * id of plugin that should be used to fulfill the task
	 */
	public MidataId plugin;
	
	/**
	 * id of consent where records created by the plugin specified in the plugin field should be shared into
	 */
	public MidataId shareBackTo;
	
	/**
	 * date of creation of this task
	 */
	public Date createdAt;
	
	/**
	 * deadline for this task
	 */
	public Date deadline;
	
	/**
	 * name of dashboard where the task should be done
	 */
	public String context;
	
	/**
	 * title of this task
	 */
	public String title;
	
	/**
	 * textual description of this task
	 */
	public String description;
	
	/**
	 * query for the space created for the given plugin
	 */
	public Map<String, Object> pluginQuery;
	
	/**
	 * query that needs to be non empty for this task to be done
	 */
	public Map<String, Object> confirmQuery;
	
	/**
	 * how often should this task be done by the user
	 */
	public Frequency frequency;
	
	/**
	 * is this task (currently) done
	 */
	public boolean done;
	
	public static void add(Task task) throws InternalServerException {
		Model.insert(collection, task);
	}
	
	public static Set<Task> getAllByOwner(MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(Task.class, collection, CMaps.map("owner", owner), fields);
	}
	
	public static void set(MidataId taskId, String field, Object value) throws InternalServerException {
		Model.set(Task.class, collection, taskId, field, value);
	}
	
	public static Task getByIdAndOwner(MidataId taskId, MidataId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Task.class, collection, CMaps.map("_id", taskId).map("owner", ownerId), fields);
	}

	public static void inactivateTask(Task task) throws InternalServerException {
		Model.delete(Task.class, collection, CMaps.map("_id", task._id));		
	}
}
