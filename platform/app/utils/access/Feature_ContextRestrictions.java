package utils.access;

import java.util.Collection;
import java.util.Map;

import utils.exceptions.AppException;

public class Feature_ContextRestrictions extends Feature {

    private Feature next;
	
	public Feature_ContextRestrictions(Feature next) {
		this.next = next;
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {		
		Map<String, Object> restrictions = q.getContext().getQueryRestrictions();
		if (restrictions != null) {
			return QueryEngine.combineIterator(q, "context-restrictions", restrictions, next);		
		} else return next.iterator(q);
	}

}
