package utils.access;

import java.util.Iterator;
import java.util.List;

import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_Sort extends Feature {
	
    private Feature next;
	
	public Feature_Sort(Feature next) {
		this.next = next;
	}
		

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("sort")) {
			q.setFromRecord(null);
			return ProcessingTools.sort(q.getProperties(), next.iterator(q));
			
		} else return next.iterator(q);		
	}

}
