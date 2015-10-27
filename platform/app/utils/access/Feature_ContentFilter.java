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
	protected List<Record> query(Query q) throws AppException {		
		return next.query(q);
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws AppException {
		return QueryEngine.filterByFormat(records, q.restrictedBy("format") ? q.getRestriction("format") : null, q.restrictedBy("content") ? q.getRestriction("content") : null, q.restrictedBy("content/*") ? q.getRestriction("content/*") : null);	
	}

	@Override
	protected List<Record> lookup(List<Record> record, Query q)
			throws AppException {		
		return next.lookup(record, q);
	}

}
