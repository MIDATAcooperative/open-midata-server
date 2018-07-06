package utils.access;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;

public class Feature_Consents extends Feature {

	private Feature next;
	
	public Feature_Consents(Feature next) {
		this.next = next;
	}
	
	
	
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("shared-after")) {			
			Date after = q.getDateRestriction("shared-after");
			AccessLog.logBegin("start history query after="+after.toString());
			List<DBRecord> recs = q.getCache().getAPS(q.getApsId()).historyQuery(after.getTime(), false);			
			
			recs = Feature_Prefetch.lookup(q, recs, next, false);
			List<DBRecord> result = Collections.emptyList();
			AccessLog.log("found "+recs.size()+" history entries");
			if (recs.size() > 0) {				
			
				boolean onlyStreams = q.isStreamOnlyQuery();
				for (DBRecord r : recs) {
					if (r.isStream) {
						Query q2 = new Query(q, CMaps.map("stream", r._id));
	                    result = QueryEngine.combine(result ,QueryEngine.onlyWithKey(next.query(q2)));
					} else if (!onlyStreams) QueryEngine.combine(result, Collections.singletonList(r));
				}
			}
			
			AccessLog.logEnd("ended history query with size="+result.size());
			
			return ProcessingTools.dbiterator("history()", result.iterator());
		} 
		return next.iterator(q);	
	}


	
}
