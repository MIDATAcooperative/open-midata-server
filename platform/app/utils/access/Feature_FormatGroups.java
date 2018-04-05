package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.ContentCode;
import models.RecordGroup;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

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
			
	
	public static Set<String> resolveContentNames(String groupSystem, Set<String> groups) throws AppException {
		
		Set<String> result = new HashSet<String>();
		for (String grp : groups) {
			if (grp.startsWith("cnt:")) {
				result.add(grp.substring(4));
			} else {
				RecordGroup recGroup = RecordGroup.getBySystemPlusName(groupSystem,grp);
				if (recGroup == null) throw new BadRequestException("error.unknown.group", "Unknown group:'"+grp+"', group-system:'"+groupSystem+"'");
				if (recGroup.contents != null) result.addAll(recGroup.contents);
			}
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
    
    public static void convertQueryToContents(Map<String, Object> properties) throws BadRequestException, AppException {
    	
    	if (properties.containsKey("group")) {
    		String groupSystem = properties.containsKey("group-system") ? properties.get("group-system").toString() : "v1";
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
    	} else if (properties.containsKey("code")) {
    		 Set<String> codes = Query.getRestriction(properties.get("code"), "code");
			 Set<String> contents = new HashSet<String>();
			 for (String code : codes) {
				 String content = ContentCode.getContentForSystemCode(code);
				 if (content == null) throw new BadRequestException("error.unknown.code", "Unknown code '"+code+"' in restriction.");
				 contents.add(content);
			 }
			 properties.put("content", contents);
    	}
    	if (properties.containsKey("$or")) {
    		Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) properties.get("$or");
    		for (Map<String, Object> part : parts) convertQueryToContents(part);
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
    				contents.removeAll(group.contents);
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
    		
    		for (String content : contents) include.add("cnt:"+content);
    		
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
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		Set<String> contents = prepareFilter(q);
		DBIterator<DBRecord> result = null;
		if (contents != null) {
		  	result = QueryEngine.combineIterator(q, CMaps.map("content", contents), next);					
		} else {
		   result = next.iterator(q);
		}
		
		if (q.returns("group")) {
			String system = q.getStringRestriction("group-system");
			if (system == null) system = "v1";
			
			return new SetRecordGroupIterator(result, system);			
		}	
		return result;
	}

	static class SetRecordGroupIterator implements DBIterator<DBRecord> {

		private DBIterator<DBRecord> chain;
		private String system;
		
		SetRecordGroupIterator(DBIterator<DBRecord> chain, String system) {
			this.chain = chain;
			this.system = system;
		}
		
		@Override
		public boolean hasNext() throws AppException {
			return chain.hasNext();
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord record = chain.next();
		
			BasicBSONObject meta = record.meta;
			   
			if (meta == null) AccessLog.log("NO META");
			record.group = RecordGroup.getGroupForSystemAndContent(system, (String) meta.get("content"));
		
			return record;
		}

		@Override
		public String toString() {
			return "set-group("+chain.toString()+")";
		}
		
		
		
	}
	
	
	public static boolean mayAccess(Map<String, Object> properties, String content, String format) throws AppException {
		if (properties.containsKey("$or")) {
			Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) properties.get("$or");
			boolean found = false;
			for (Map<String, Object> part : parts) {
				if (mayAccess(part, content, format)) found = true;;
			}
			if (!found) return false;
		}
		
		Object formatObj = properties.get("format");
		if (formatObj != null && format != null) { 
		  Set<String> fmts = Query.getRestriction(formatObj, "format");
		  if (!fmts.contains(format)) return false;
		}
		if (content == null) return true;
		
		convertQueryToContents(properties);
		Object contentObj = properties.get("content");
		if (contentObj != null) {
			Set<String> contents = Query.getRestriction(contentObj, "content");
			if (!contents.contains(content)) return false;
		}
		return true;
	}
	
}
