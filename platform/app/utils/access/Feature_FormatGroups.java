package utils.access;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.FormatGroup;
import models.ContentInfo;
import models.Record;

public class Feature_FormatGroups extends Feature {

	private Feature next;
	
	public Feature_FormatGroups(Feature next) {
		this.next = next;
	}
	
	
	
	@Override
	protected List<Record> lookup(List<Record> input, Query q)
			throws AppException {
		Set<String> contents = prepareFilter(q);
		if (contents != null) q.getProperties().put("content/*", contents);
		return next.lookup(input, q);
	}

	private void addChildren(String group, Set<String> groups, Set<String> exclude) throws InternalServerException {		
			FormatGroup grp = FormatGroup.getByName(group);
		    for (FormatGroup child : grp.children) {
		    	if (!groups.contains(child.name) && !exclude.contains(child.name)) {
		    		groups.add(child.name);
		    		addChildren(child.name, groups, exclude);
		    	}
		    }		
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
		    
		    return contents;
		} else if (q.restrictedBy("group-strict")) {
            Set<String> contents = new HashSet<String>();
			
			Set<String> groups = new HashSet<String>();
			Set<String> included = q.getRestriction("group-strict"); 
			groups.addAll(included);
									
		    Set<ContentInfo> qualified = ContentInfo.getByGroups(groups);		    
		    for (ContentInfo fi : qualified) contents.add(fi.content);
		    
		    return contents;
		}
		
		return null;
	}

	@Override
	protected List<Record> query(Query q) throws AppException {
		Set<String> contents = prepareFilter(q);
		if (contents != null) q.getProperties().put("content/*", contents);			
		return next.query(q);		
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q) throws AppException {
		List<Record> result = next.postProcess(records, q);
		if (q.returns("group")) {
			for (Record record : result) {
				ContentInfo fi = ContentInfo.getByName(record.content);
	    		record.group = fi.group;
			}
		}	
		return result;
	}	

}
