package utils.access;

import java.util.List;

import utils.exceptions.AppException;

public class Feature_Prefetch extends Feature {

    private Feature next;  
	
	public Feature_Prefetch(Feature next) throws AppException {
		this.next = next;		
	}
		
	@Override
	protected List<DBRecord> query(Query q) throws AppException {						
		if (q.restrictedBy("_id")) {
			return next.lookup(QueryEngine.lookupRecordsById(q), q);		
		} else return next.query(q);
	}
	
	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws AppException {
		return next.postProcess(records, q);		
	}

	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q) throws AppException {
		return next.lookup(record, q);
	}

	
	

}