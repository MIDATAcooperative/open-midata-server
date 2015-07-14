package utils.access;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.FormatGroup;
import models.FormatInfo;
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
		if (result && q.returns("group")) {
			FormatInfo fi = FormatInfo.getByName(record.format);
    		record.group = fi.group;
		}
		
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
				
		if (q.restrictedBy("group") && !q.restrictedBy("format")) {
			Set<String> groups = new HashSet<String>();
			groups.addAll(q.getRestriction("group"));
			for (String group : groups) {
				addChildren(group, groups);				
			}
			
		    Set<FormatInfo> qualified = FormatInfo.getByGroups(groups);
		    Set<String> formats = new HashSet<String>();
		    for (FormatInfo fi : qualified) formats.add(fi.format);
		    q.getProperties().put("format", formats);		    
		}
		
        List<Record> result = next.query(q);
        
        if (q.returns("group")) {
        	for (Record r : result) {
        		FormatInfo fi = FormatInfo.getByName(r.format);
        		r.group = fi.group;
        	}
        }
		return result;
	}

}
