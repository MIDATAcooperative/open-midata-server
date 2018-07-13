package utils.access;

import java.util.Collection;
import java.util.Map;

import utils.exceptions.AppException;

public class Feature_Or extends Feature {

	private Feature next;
	
	public Feature_Or(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.getProperties().containsKey("$or")) {
			  
			  // TODO overlapping ORs
			  q.setFromRecord(null);
			
	      	  Feature qm = new Feature_Sort(next);
	      	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) q.getProperties().get("$or");      	  
	      	  return ProcessingTools.noDuplicates(ProcessingTools.multiQuery(qm, q, ProcessingTools.dbiterator("", col.iterator())));      	  
	    } return next.iterator(q);
	}
	
	
}
