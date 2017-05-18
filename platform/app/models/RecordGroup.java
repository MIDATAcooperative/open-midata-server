package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.AccessLog;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * data model for record groups. The "group" of a record determines where it is located in the tree of records.
 *
 */
public class RecordGroup extends Model {
	
	private @NotMaterialized static final String collection = "formatgroups";
	private @NotMaterialized static volatile Map<String, RecordGroup> cache;
	private @NotMaterialized static Map<String, Map<String, String>> systemToContentToGroup;

	/**
	 * the name of the group system this group belongs to
	 */
	public String system;
	
	/**
	 * the internal name of this group
	 */
	public String name;
	
	/**
	 * the public label that should be shown in the frontend (map from language to label)
	 */
	public Map<String, String> label;
	
	/**
	 * the internal name of the parent of this group. This information is used to build the tree.
	 */
	public String parent;
	
	/**
	 * all MIDATA content names that are in this group (if group has no children)
	 */
	public Set<String> contents;
	
	/**
	 * list of children of this group. The list is derived from the parent field.
	 */
	public @NotMaterialized List<RecordGroup> children;		
	
	public static void add(RecordGroup recordGroup) throws InternalServerException {
		Model.insert(collection, recordGroup);
	}
			
	public static void upsert(RecordGroup recordGroup) throws InternalServerException {
	    Model.upsert(collection, recordGroup);
	}
	  
	public static void delete(MidataId recordGroupId) throws InternalServerException {			
	    Model.delete(RecordGroup.class, collection, CMaps.map("_id", recordGroupId));
	}
	
	public static RecordGroup getBySystemPlusName(String system, String name) throws AppException {		
		if (cache == null) load();		
		return cache.get(system+":"+name);
	}
	
	public static String getGroupForSystemAndContent(String groupSystem, String content) throws AppException {
		if (cache == null) load();
		Map<String, String> contentToGroup = systemToContentToGroup.get(groupSystem);
		if (contentToGroup == null) return null;
		return contentToGroup.get(content);
	}
	
	public static Collection<RecordGroup> getAll() throws AppException {
		if (cache == null) load();
		return cache.values();
	}
	
	public static void invalidate() {
		cache = null;
	}
	
	public static void load() throws AppException {		
		
		Map<String, Map<String, String>> newSystemToContentToGroup = new HashMap<String, Map<String, String>>();
				
		Set<RecordGroup> groups = Model.getAll(RecordGroup.class, collection, Collections.EMPTY_MAP, Sets.create("system", "name", "label", "contents", "parent"));
		
		Map<String, RecordGroup> newCache = new HashMap<String, RecordGroup>();
		for (RecordGroup group : groups) {
			newCache.put(group.system+":"+group.name, group);
			group.children = new ArrayList<RecordGroup>();						
		}
		
		for (RecordGroup group : groups) {
			AccessLog.log(group.name);
		    if (group.parent != null) newCache.get(group.system+":"+group.parent).children.add(group);
		}
				
		Set<ContentInfo> allci = ContentInfo.getAll(new HashMap<String, Object>(), Sets.create("content", "group","label","defaultCode"));
		for (ContentInfo ci : allci) {
			if (ci.label == null) {
			   ContentCode cc = ContentCode.getBySystemCode(ci.defaultCode);
			   if (cc != null && cc.display != null) {
				   ci.label = new HashMap<String, String>();
				   ci.label.put("en", cc.display);
				   Model.set(ContentInfo.class, "contentinfo", ci._id, "label", ci.label);
			   } else {
				   ci.label = new HashMap<String, String>();
				   ci.label.put("en", ci.content);
				   Model.set(ContentInfo.class, "contentinfo", ci._id, "label", ci.label);
			   }
			}		
		}
		
		ContentInfo.clear();
		
		for (RecordGroup group : groups) {		
			Map<String, String> contentToGroup = newSystemToContentToGroup.get(group.system);
			if (contentToGroup == null) {
				contentToGroup = new HashMap<String, String>();
				newSystemToContentToGroup.put(group.system, contentToGroup);
			}
			if (group.contents != null) {
				for (String content : group.contents) {					
					ContentInfo.getByName(content);					
					contentToGroup.put(content, group.name);
				}
			}
		}
		
		cache = newCache;
		systemToContentToGroup = newSystemToContentToGroup;
		
	}
	
	
	
}
