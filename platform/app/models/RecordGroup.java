/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	public @NotMaterialized static final Set<String> ALL = Sets.create("system", "name", "label", "parent", "deleted", "lastUpdated");

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
	
	public boolean deleted;
	
	public long lastUpdated;
	
	/**
	 * list of children of this group. The list is derived from the parent field.
	 */
	public @NotMaterialized List<RecordGroup> children;		
	
	public static void add(RecordGroup recordGroup) throws InternalServerException {
		Model.insert(collection, recordGroup);
	}
	
	public boolean exists() throws InternalServerException {
		return Model.exists(RecordGroup.class, collection, CMaps.map("system", system).map("name", name).map("_id", CMaps.map("$ne", _id)).map("deleted", CMaps.map("$ne", true)));
	}
			
	public static void upsert(RecordGroup recordGroup) throws InternalServerException {
	    Model.upsert(collection, recordGroup);
	}
	  
	public static void delete(MidataId recordGroupId) throws InternalServerException {
		Model.set(RecordGroup.class, collection, recordGroupId, "lastUpdated", System.currentTimeMillis());
	    Model.set(RecordGroup.class, collection, recordGroupId, "deleted", true);
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
	
	public static Set<RecordGroup> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(RecordGroup.class, collection, properties, fields);
  }
	
	public static void invalidate() {
		cache = null;
	}
	
	public static void load() throws AppException {		
		
		Map<String, Map<String, String>> newSystemToContentToGroup = new HashMap<String, Map<String, String>>();
				
		Set<RecordGroup> groups = Model.getAll(RecordGroup.class, collection, CMaps.map("deleted", CMaps.map("$ne", true)), Sets.create("system", "name", "label", "contents", "parent"));
		
		Map<String, RecordGroup> newCache = new HashMap<String, RecordGroup>();
		for (RecordGroup group : groups) {
			newCache.put(group.system+":"+group.name, group);
			group.children = new ArrayList<RecordGroup>();	
			if (group.contents==null) group.contents = new HashSet<String>();
		}
		
		for (RecordGroup group : groups) {
			AccessLog.log(group.name);
		    if (group.parent != null) newCache.get(group.system+":"+group.parent).children.add(group);
		}
		
		Set<GroupContent> groupContent = GroupContent.getAll();
		
		// Convert
		Set<String> existing = new HashSet<String>();
		for (GroupContent grpContent : groupContent) {
			existing.add(grpContent.system+":"+grpContent.name+":"+grpContent.content);
		}
		for (RecordGroup group : groups) {
			for (String c : group.contents) {
				if (!existing.contains(group.system+":"+group.name+":"+c)) {
					GroupContent gc = new GroupContent();
					gc._id = new MidataId();
					gc.system = group.system;
					gc.name = group.name;
					gc.content = c;
					GroupContent.add(gc);
					existing.add(gc.system+":"+gc.name+":"+gc.content);
				}
			}
		}
		// End convert
		
		
		for (GroupContent grpContent : groupContent) {
			newCache.get(grpContent.system+":"+grpContent.name).contents.add(grpContent.content);
		}
				
		Set<ContentInfo> allci = ContentInfo.getAll(CMaps.map("deleted", CMaps.map("$ne", true)), Sets.create("content", "group","label","defaultCode"));
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
