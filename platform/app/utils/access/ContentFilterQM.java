package utils.access;

import java.util.List;

import utils.exceptions.AppException;
import utils.exceptions.ModelException;

import models.Record;

public class ContentFilterQM extends QueryManager {

	private QueryManager next;
	
	public ContentFilterQM(QueryManager next) {
		this.next = next;
	}
		

	@Override
	protected List<Record> query(Query q) throws AppException {		
		return next.query(q);
	}

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws AppException {
		return ComplexQueryManager.filterByFormat(records, q.restrictedBy("format") ? q.getRestriction("format") : null, q.restrictedBy("content") ? q.getRestriction("content") : null, q.restrictedBy("content/*") ? q.getRestriction("content/*") : null);	
	}

	@Override
	protected List<Record> lookup(List<Record> record, Query q)
			throws AppException {		
		return next.lookup(record, q);
	}

}
