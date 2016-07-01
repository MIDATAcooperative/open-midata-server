package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

import models.RecordGroup;
import models.ContentInfo;
import models.Record;
import models.RecordsInfo;
import models.enums.AggregationType;

/**
 * filter records by group. add group information to record while querying
 *
 */
public class Feature_FormatGroups extends Feature {

	private Feature next;
		
	public Feature_FormatGroups(Feature next) {
		this.next = next;
	}
	
	

	private static void addChildren(String groupSystem, String group, Set<String> groups, Set<String> exclude) throws AppException {		
			RecordGroup grp = RecordGroup.getBySystemPlusName(groupSystem, group);
			if (grp != null) {
			    for (RecordGroup child : grp.children) {
			    	if (!groups.contains(child.name) && !exclude.contains(child.name)) {
			    		groups.add(child.name);
			    		addChildren(groupSystem, child.name, groups, exclude);
			    	}
			    }		
			}
	}
		
	/*private Set<String> resolveContentNames(Query q, Set<String> groups) throws AppException {
		List<DBRecord> records = next.query(new Query(CMaps.map(q.getProperties()).map("streams", "true").map("flat", "true"), Sets.create("content"), q.getCache(), q.getApsId(), true ));
		Set<String> result = new HashSet<String>();
		for (DBRecord rec : records) {
			String content = (String) rec.meta.get("content");
			ContentInfo fi = ContentInfo.getByName(content);
            if (groups.contains(fi.group)) {
            	result.add(content);            	
            }
		}	
		return result;
	}*/
	
	public static Set<String> resolveContentNames(String groupSystem, Set<String> groups) throws AppException {
		
		Set<String> result = new HashSet<String>();
		for (String grp : groups) {
			RecordGroup recGroup = RecordGroup.getBySystemPlusName(groupSystem,grp);
			if (recGroup == null) throw new BadRequestException("error.unknown.group", "Unknown group:'"+grp+"', group-system:'"+groupSystem+"'");
			if (recGroup.contents != null) result.addAll(recGroup.contents);
		}	
		
		return result;
	}
	
    public static Set<String> resolveContentNames(String groupSystem, Set<String> included, Set<String> exclude) throws AppException {
		
		Set<String> groups = new HashSet<String>();		
		groups.addAll(included);
				
		
		for (String group : included) {
			addChildren(groupSystem, group, groups, exclude);				
		}
		
		return resolveContentNames(groupSystem, groups);
	}
    
    public static void convertQueryToContents(String groupSystem, Map<String, Object> properties) throws BadRequestException, AppException {
    	if (properties.containsKey("group")) {
    		Set<String> include = Query.getRestriction(properties.get("group"), "group");
    		Set<String> exclude;
    		if (properties.containsKey("group-exclude")) {
    			exclude = Query.getRestriction(properties.get("group-exclude"), "group-exclude");
    		} else {
    			exclude = new HashSet<String>();
    		}
    		Set<String> contents = resolveContentNames(groupSystem, include, exclude);
    		properties.put("content", contents);
    		properties.remove("group");
    		properties.remove("group-exclude");
    	}    			    	
    }
    
    public static void convertQueryToGroups(String groupSystem, Map<String, Object> properties) throws BadRequestException, AppException {
    	if (properties.containsKey("content")) {
    		Set<String> contents = Query.getRestriction(properties.get("content"), "content");
    		Set<String> include = new HashSet<String>();
    		
    		Map<String, Integer> counts = new HashMap<String, Integer>();
    		for (String content : contents) {
    			String group =  RecordGroup.getGroupForSystemAndContent(groupSystem, content);
    			if (group != null) {
	    			if (counts.containsKey(group)) {
	    				counts.put(group, counts.get(group) + 1);
	    			} else {
	    				counts.put(group, 1);
	    			}
    			}
    		}
    		
    		for (String grp : counts.keySet()) {
    			RecordGroup group = RecordGroup.getBySystemPlusName(groupSystem, grp);
    			if (group.contents != null && group.contents.size() == counts.get(grp)) {
    				include.add(grp);
    			}
    		}
    		
    		int cancel = 10;
    		boolean redo = false;
    		do {
    			
    		redo = false;
    		cancel--;
    		
    		counts.clear();
    		
    		for (String grp : include) {
    			AccessLog.log("included group:"+grp);
    			RecordGroup group = RecordGroup.getBySystemPlusName(groupSystem, grp);
    			if (group.parent != null) {
    				if (counts.containsKey(group.parent)) {
        				counts.put(group.parent, counts.get(group.parent) + 1);
        			} else {
        				counts.put(group.parent, 1);
        			}	
    			}
    		}
    		
    		for (String grp : counts.keySet()) {
    			RecordGroup group = RecordGroup.getBySystemPlusName(groupSystem, grp);
    			if (group.children != null && group.children.size() == counts.get(grp)) {
    				AccessLog.log("add group:"+grp);
    				
    				include.add(grp);
    				for (RecordGroup g : group.children) {
    					AccessLog.log("remove:"+g.name);
    					include.remove(g.name);
    				}
    				redo = true;
    			}
    		}
    		
    		} while (redo && cancel > 0);
    		if (cancel <= 0) properties.put("error", "true");
    		
    		properties.put("group", include);
    		properties.put("group-system", groupSystem);
    		properties.remove("content");
    	}
    }
		
	private Set<String> prepareFilter(Query q) throws AppException {		
		
		if (q.restrictedBy("group")) {
			String groupSystem = q.getStringRestriction("group-system");
			if (groupSystem == null) groupSystem = "v1";
			
			Set<String> exclude = new HashSet<String>();
			if (q.restrictedBy("group-exclude")) exclude.addAll(q.getRestriction("group-exclude"));
									
			return resolveContentNames(groupSystem, q.getRestriction("group"), exclude);
		    		  		    
		    
		} else if (q.restrictedBy("group-strict")) {           
			String groupSystem = q.getStringRestriction("group-system");
			if (groupSystem == null) groupSystem = "v1";
			
			Set<String> groups = new HashSet<String>();
			Set<String> included = q.getRestriction("group-strict"); 
			groups.addAll(included);
									
			return resolveContentNames(groupSystem, groups);			
		}
		
		return null;
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		Set<String> contents = prepareFilter(q);
		List<DBRecord> result = null;
		if (contents != null) {
			Map<String, Object> combined = Feature_QueryRedirect.combineQuery(q.getProperties(), CMaps.map("content", contents));
			if (combined == null) return Collections.EMPTY_LIST;
		  	result = next.query(new Query(q, combined));					
		} else {
		   result = next.query(q);
		}
		
		if (q.returns("group")) {
			String system = q.getStringRestriction("group-system");
			if (system == null) system = "v1";
			
			for (DBRecord record : result) {
	    		record.group = RecordGroup.getGroupForSystemAndContent(system, (String) record.meta.get("content"));
			}
		}	
		return result;
	}	
	
}
