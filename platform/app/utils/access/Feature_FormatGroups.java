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
		
	public Feature_FormatGroups(Feature next) {
		this.next = next;
	}
	
	
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> input, Query q)
			throws AppException {
		Set<String> contents = prepareFilter(q);
		if (contents != null) {
			Map<String, Object> combined = Feature_QueryRedirect.combineQuery(q.getProperties(), CMaps.map("content", contents));
			if (combined == null) return Collections.EMPTY_LIST;
		  	return next.lookup(input, new Query(q, combined));
		} else {
		  return next.lookup(input, q);
		}
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
		
	private Set<String> resolveContentNames(Query q, Set<String> groups) throws AppException {
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
	}
	
	private Set<String> prepareFilter(Query q) throws AppException {		
		
		if (q.restrictedBy("group")) {
						
			Set<String> groups = new HashSet<String>();
			Set<String> included = q.getRestriction("group"); 
			groups.addAll(included);
			
			Set<String> exclude = new HashSet<String>();
			if (q.restrictedBy("group-exclude")) exclude.addAll(q.getRestriction("group-exclude"));
			
			for (String group : included) {
				addChildren(group, groups, exclude);				
			}
			
			return resolveContentNames(q, groups);
		    		  		    
		    
		} else if (q.restrictedBy("group-strict")) {           
			
			Set<String> groups = new HashSet<String>();
			Set<String> included = q.getRestriction("group-strict"); 
			groups.addAll(included);
									
			return resolveContentNames(q, groups);			
		}
		
		return null;
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		Set<String> contents = prepareFilter(q);
		if (contents != null) {
			Map<String, Object> combined = Feature_QueryRedirect.combineQuery(q.getProperties(), CMaps.map("content", contents));
			if (combined == null) return Collections.EMPTY_LIST;
		  	return next.query(new Query(q, combined));					
		} else {
		   return next.query(q);
		}
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
