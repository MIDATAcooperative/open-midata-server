package utils.access;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.FormatGroup;
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
	
	/**
	 * name of the group that contains all records that cannot be mapped into a group successfully
	 */
	private final static String UNKNOWN_GROUP_NAME = "unknown";
	
	public Feature_FormatGroups(Feature next) {
		this.next = next;
	}
	
	
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> input, Query q)
			throws AppException {
		Set<String> contents = prepareFilter(q);
		if (contents != null) q.getProperties().put("content/*", contents);
		return next.lookup(input, q);
	}

	private void addChildren(String group, Set<String> groups, Set<String> exclude) throws InternalServerException {		
			FormatGroup grp = FormatGroup.getByName(group);
			if (grp != null) {
			    for (FormatGroup child : grp.children) {
			    	if (!groups.contains(child.name) && !exclude.contains(child.name)) {
			    		groups.add(child.name);
			    		addChildren(child.name, groups, exclude);
			    	}
			    }		
			}
	}
	
	private Set<String> resolveTheUnknownGroup(Query q) throws AppException {
		AccessLog.logBegin("begin resolve 'unknown' group");
		Map<String, Object> p = new HashMap<String, Object>();
		Collection<RecordsInfo> res = QueryEngine.info(q.getCache(), q.getApsId(), p, AggregationType.GROUP);
		Set<String> result = new HashSet<String>();
		for (RecordsInfo r : res) {
			if (r.groups.contains(UNKNOWN_GROUP_NAME)) result = r.contents;
		}		
		AccessLog.logEnd("end resolve 'unknown' group size="+result.size());
		return result;
	}
	
	private Set<String> prepareFilter(Query q) throws AppException {		
		
		if (q.restrictedBy("group")) {
			Set<String> contents = new HashSet<String>();
			
			Set<String> groups = new HashSet<String>();
			Set<String> included = q.getRestriction("group"); 
			groups.addAll(included);
			
			Set<String> exclude = new HashSet<String>();
			if (q.restrictedBy("group-exclude")) exclude.addAll(q.getRestriction("group-exclude"));
			
			for (String group : included) {
				addChildren(group, groups, exclude);				
			}
			
		    Set<ContentInfo> qualified = ContentInfo.getByGroups(groups);		    
		    for (ContentInfo fi : qualified) contents.add(fi.content);
		    if (groups.contains(UNKNOWN_GROUP_NAME)) contents.addAll(resolveTheUnknownGroup(q));
		    
		    return contents;
		} else if (q.restrictedBy("group-strict")) {
            Set<String> contents = new HashSet<String>();
			
			Set<String> groups = new HashSet<String>();
			Set<String> included = q.getRestriction("group-strict"); 
			groups.addAll(included);
									
		    Set<ContentInfo> qualified = ContentInfo.getByGroups(groups);		    
		    for (ContentInfo fi : qualified) contents.add(fi.content);
		    if (groups.contains(UNKNOWN_GROUP_NAME)) contents.addAll(resolveTheUnknownGroup(q));
		    
		    return contents;
		}
		
		return null;
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		Set<String> contents = prepareFilter(q);
		if (contents != null) q.getProperties().put("content/*", contents);			
		return next.query(q);		
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q) throws AppException {
		List<DBRecord> result = next.postProcess(records, q);
		if (q.returns("group")) {
			for (DBRecord record : result) {
				ContentInfo fi = ContentInfo.getByName((String) record.meta.get("content"));
	    		record.group = fi.group;
			}
		}	
		return result;
	}	

}
