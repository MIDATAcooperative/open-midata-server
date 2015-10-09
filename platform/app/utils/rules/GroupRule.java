package utils.rules;

import java.util.List;
import java.util.Map;

import utils.access.AccessLog;
import utils.access.Query;
import utils.access.SingleAPSManager;
import utils.exceptions.ModelException;

import models.FormatGroup;
import models.ContentInfo;
import models.Record;

public class GroupRule implements Rule {

	@Override
	public boolean qualifies(Record record, List<Object> params) throws ModelException {
		if (record.group == null && record.content != null) {
		   record.group = ContentInfo.getByName(record.content).group;
		}
		if (record.group == null) return false;
		
		return qualifies(record.group, params);		
	}
	
	private boolean qualifies(String group, List<Object> params) throws ModelException {
		if (params.size() == 0) return true;
		
		for (Object obj : params) {
			//AccessLog.debug(group+" vs "+obj+" = "+group.equals(obj));
			if (group.equals(obj)) return true;			
		}
		FormatGroup grp = FormatGroup.getByName(group);
		if (grp == null) throw new ModelException("error.internal", "Missing group:" + group);
		if (grp != null && grp.parent != null) return qualifies(grp.parent, params);
		return false;
	}

	
	@Override
	public void setup(Map<String, Object> query, List<Object> params) throws ModelException {
		/*if (params.size() == 1) {
			query.put("group", params.get(0));
		} else*/ query.put("group", params);		
	}


	@Override
	public void merge(List<Object> params, List<Object> params2)
			throws ModelException {
		for (Object o : params2) {
			if (!params.contains(o)) params.add(o);
		}		
	}
    
}