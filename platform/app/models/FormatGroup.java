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

public class FormatGroup extends Model {
	
	private @NotMaterialized static final String collection = "formatgroups";
	private @NotMaterialized static Map<String, FormatGroup> cache;
		
	public String name;
	public String label;
	public String parent;
	
	public @NotMaterialized List<FormatGroup> children;
	public @NotMaterialized List<String> formats;
	
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
