package utils.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import utils.access.Query;

import models.ModelException;
import models.Record;

public class FormatRule implements Rule {

	@Override
	public boolean qualifies(Record record, List<Object> params) throws ModelException {
		if (record.format == null) return false;
		for (Object obj : params) {
			if (record.format.equals(obj)) return true;
			if (record.format.equals(Query.STREAM_TYPE) && record.name.equals(obj)) return true;
		}
		return false;
	}

	
	@Override
	public void setup(Map<String, Object> query, List<Object> params) throws ModelException {
		if (params.size() == 1) {
			query.put("format", params.get(0));
		} else query.put("format", params);		
	}


	@Override
	public void merge(List<Object> params, List<Object> params2)
			throws ModelException {
		for (Object o : params2) {
			if (!params.contains(o)) params.add(o);
		}		
	}
    
}
