package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model for record groups. The "group" of a record determines where it is located in the tree of records.
 *
 */
public class FormatGroup extends Model {
	
	private @NotMaterialized static final String collection = "formatgroups";
	private @NotMaterialized static Map<String, FormatGroup> cache;

	/**
	 * the internal name of this group
	 */
	public String name;
	
	/**
	 * the public label that should be shown in the frontend
	 */
	public String label;
	
	/**
	 * the internal name of the parent of this group. This information is used to build the tree.
	 */
	public String parent;
	
	/**
	 * list of children of this group. The list is derived from the parent field.
	 */
	public @NotMaterialized List<FormatGroup> children;		
	
	public static void add(FormatGroup record) throws InternalServerException {
		Model.insert(collection, record);
	}
	
	public static FormatGroup getByName(String name) throws InternalServerException {
		if (cache == null) load();
		return cache.get(name);
	}
	
	public static Collection<FormatGroup> getAll() throws InternalServerException {
		if (cache == null) load();
		return cache.values();
	}
	
	public static void load() throws InternalServerException {
		Set<FormatGroup> groups = Model.getAll(FormatGroup.class, collection, Collections.EMPTY_MAP, Sets.create("name", "label", "parent"));
		
		cache = new HashMap<String, FormatGroup>();
		for (FormatGroup group : groups) {
			cache.put(group.name, group);
			group.children = new ArrayList<FormatGroup>();
		}
		
		for (FormatGroup group : groups) {
		    if (group.parent != null) cache.get(group.parent).children.add(group);
		}
	}
	
}
