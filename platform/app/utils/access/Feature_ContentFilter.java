package utils.access;

import utils.exceptions.AppException;

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
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		DBIterator<DBRecord> chain = next.iterator(q);
		
		if (q.restrictedBy("format")) chain = new ProcessingTools.FilterByMetaSet(chain, "format", q.getRestrictionOrNull("format"), false);
		if (q.restrictedBy("content")) chain = new ProcessingTools.FilterByMetaSet(chain, "content", q.getRestrictionOrNull("content"), false);
		if (q.restrictedBy("app")) chain = new ProcessingTools.FilterByMetaSet(chain, "app", q.getIdRestrictionDB("app"), false);	
		
		return chain;
	}
		
}
