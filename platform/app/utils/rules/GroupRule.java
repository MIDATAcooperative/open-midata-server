package utils.rules;

import java.util.List;
import java.util.Map;

import utils.access.Query;
import utils.access.SingleAPSManager;

import models.FormatGroup;
import models.FormatInfo;
import models.ModelException;
import models.Record;

public class GroupRule implements Rule {

	@Override
	public boolean qualifies(Record record, List<Object> params) throws ModelException {
		if (record.group == null && record.format != null) {
		   if (record.format.equals(Query.STREAM_TYPE)) record.group = FormatInfo.getByName(record.name).group;
		   else record.group = FormatInfo.getByName(record.format).group;
		}
		if (record.group == null) return false;
		
		return qualifies(record.group, params);		
	}
	
	private boolean qualifies(String group, List<Object> params) throws ModelException {
		if (params.size() == 0) return true;
		
		for (Object obj : params) {
			if (group.equals(obj)) return true;			
		}
		FormatGroup grp = FormatGroup.getByName(group);
		if (grp == null) throw new ModelException("Missing group:" + group);
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