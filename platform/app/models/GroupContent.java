package models;

import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class GroupContent extends Model {

	private @NotMaterialized static final String collection = "groupcontent";
	public @NotMaterialized static final Set<String> ALL = Sets.create("system", "name", "content", "deleted", "lastUpdated");
	
	/**
	 * the system of the group
	 */
    public String system;
	
	/**
	 * the internal name of the group
	 */
	public String name;
	
	/**
	 * the Midata content code that belongs to that group
	 */
	public String content;
	
	public long lastUpdated;
	
	public boolean deleted;
	
	public static void add(GroupContent groupContent) throws InternalServerException {
		Model.insert(collection, groupContent);
	}
	
	public static void upsert(GroupContent groupContent) throws InternalServerException {
		Model.upsert(collection, groupContent);
	}
	
	public static GroupContent getPrevious(GroupContent content) throws InternalServerException {
		return Model.get(GroupContent.class, collection, CMaps.map("system", content.system).map("name", content.name).map("content", content.content), ALL);
	}
	
	public static Set<GroupContent> getAll() throws InternalServerException {
		return Model.getAll(GroupContent.class, GroupContent.collection, CMaps.map("deleted", CMaps.map("$ne", true)), GroupContent.ALL);
	}
	
	public static Set<GroupContent> getByContent(String content) throws InternalServerException {
		return Model.getAll(GroupContent.class, GroupContent.collection, CMaps.map("content", content).map("deleted", CMaps.map("$ne", true)), GroupContent.ALL);
	}
	
	public static Set<GroupContent> getAllChanged() throws InternalServerException {
		return Model.getAll(GroupContent.class, GroupContent.collection, CMaps.map("lastUpdated", CMaps.map("$gt", 0)), GroupContent.ALL);
	}
	
	public void delete() throws InternalServerException {
		Model.set(GroupContent.class, collection, _id, "lastUpdated", System.currentTimeMillis());
		Model.set(GroupContent.class, collection, _id, "deleted", true);	    
	}
	
}
