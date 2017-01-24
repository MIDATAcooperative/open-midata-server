package utils.access;

import java.util.List;

import utils.exceptions.AppException;

public class Feature_SortAndLimit extends Feature {
	
    private Feature next;
	
	public Feature_SortAndLimit(Feature next) {
		this.next = next;
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		List<DBRecord> results = next.query(q);
		if (q.restrictedBy("sort") || q.restrictedBy("limit")) {
			results = QueryEngine.postProcessRecords(q.getProperties(), results);
		}
		return results;
	}

}
