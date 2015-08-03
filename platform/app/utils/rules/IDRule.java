package utils.rules;

import java.util.List;
import java.util.Map;

import models.ModelException;
import models.Record;

public class IDRule implements Rule {

	@Override
	public boolean qualifies(Record record, List<Object> params) throws ModelException {
		if (params.size() == 0) return true;		
		for (Object obj : params) {
			if (record._id.toString().equals(obj)) return true;			
		}
		return false;
	}

	
	@Override
	public void setup(Map<String, Object> query, List<Object> params) throws ModelException {
		/*if (params.size() == 1) {
			query.put("format", params.get(0));
		} else*/ query.put("_id", params);		
	}


	@Override
	public void merge(List<Object> params, List<Object> params2)
			throws ModelException {
		for (Object o : params2) {
			if (!params.contains(o)) params.add(o);
		}		
	}
    
}