package utils.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_ProcessFilters extends Feature {

	private Feature next;
	
	public Feature_ProcessFilters(Feature next) {
		this.next = next;
	}
	
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		List<DBRecord> result = next.query(q);
		return QueryEngine.postProcessRecordsFilter(q, result);
	}
	
		
}
