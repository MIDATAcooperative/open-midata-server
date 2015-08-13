package utils.access;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.FormatGroup;
import models.ContentInfo;
import models.ModelException;
import models.Record;

public class FormatGroupHandling extends QueryManager {

	private QueryManager next;
	
	public FormatGroupHandling(QueryManager next) {
		this.next = next;
	}
	
	@Override
	protected boolean lookupSingle(Record record, Query q)
			throws ModelException {
		
		boolean result = next.lookupSingle(record, q);
		
		
		return result;
	}

	private void addChildren(String group, Set<String> groups) throws ModelException {		
			FormatGroup grp = FormatGroup.getByName(group);
		    for (FormatGroup child : grp.children) {
		    	if (!groups.contains(child.name)) {
		    		groups.add(child.name);
		    		addChildren(child.name, groups);
		    	}
		    }		
	}

	@Override
	protected List<Record> query(Query q) throws ModelException {
				
		if (q.restrictedBy("group") && !q.restrictedBy("content")) {
			Set<String> groups = new HashSet<String>();
			groups.addAll(q.getRestriction("group"));
			for (String group : groups) {
				addChildren(group, groups);				
			}
			
		    Set<ContentInfo> qualified = ContentInfo.getByGroups(groups);
		    Set<String> contents = new HashSet<String>();
		    for (ContentInfo fi : qualified) contents.add(fi.content);		    
		    q.getProperties().put("content", contents);		    
		}
		
        List<Record> result = next.query(q);
        
		return result;
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q) throws ModelException {
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
