package utils.access;

import java.util.List;

import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.Record;

/**
 * filter records by format
 *
 */
public class Feature_ContentFilter extends Feature {

	private Feature next;
	
	public Feature_ContentFilter(Feature next) {
		this.next = next;
	}
		

	@Override
	protected List<DBRecord> query(Query q) throws AppException {		
		return next.query(q);
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws AppException {
		List<DBRecord> result = records;
		if (q.restrictedBy("format")) result = QueryEngine.filterByMetaSet(result, "format", q.getRestriction("format"));
		if (q.restrictedBy("content")) result = QueryEngine.filterByMetaSet(result, "content", q.getRestriction("content"));
		if (q.restrictedBy("format/*")) result = QueryEngine.filterByWCFormat(result, "format", q.getRestriction("format/*"));
		if (q.restrictedBy("content/*")) result = QueryEngine.filterByWCFormat(result, "content", q.getRestriction("content/*"));
		return result;	
	}

	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q)
			throws AppException {		
		return next.lookup(record, q);
	}

}
