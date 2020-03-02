package utils.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BasicBSONObject;

import models.MidataId;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

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
			Query qnt = q.withoutTime();
			
			List<DBRecord> recs = q.getCache().getAPS(q.getApsId()).historyQuery(after.getTime(), false);			
			
			if (!recs.isEmpty()) recs = Feature_Prefetch.lookup(qnt, recs, next, false);
			List<DBRecord> result = Collections.emptyList();			
			if (recs.size() > 0) {				
			
				boolean onlyStreams = qnt.isStreamOnlyQuery();
				for (DBRecord r : recs) {
					if (r.isStream!=null) {
						Query q2 = new Query(q, CMaps.map("stream", r._id));
						List<DBRecord> subresult = QueryEngine.onlyWithKey(next.query(q2));
						for (DBRecord r2 : subresult) {
							r2.sharedAt = r.sharedAt; 							
						}
	                    result = QueryEngine.combine(result, subresult);
					} else if (!onlyStreams) QueryEngine.combine(result, Collections.singletonList(r));
				}
			}
			
			AccessLog.logEnd("ended history query entries="+recs.size()+" results="+result.size());
			if (result.isEmpty()) return next.iterator(q);
			return ProcessingTools.noDuplicates(new SharedThenNormal(result, next, q));
		} 
		return next.iterator(q);	
	}

	static class SharedThenNormal extends Feature.MultiSource<Integer> {
		
		private Feature next;	
		private List<DBRecord> shared;
		private boolean sharedFlag;
		
		SharedThenNormal(List<DBRecord> shared, Feature next, Query q) throws AppException {			
		
			this.next = next;
			this.query = q;
			this.sharedFlag = true;
			this.shared = shared;
			
			Integer[] steps = {1,2};
			init(Arrays.asList(steps).iterator());
		}
		
		@Override
		public DBIterator<DBRecord> advance(Integer step) throws AppException {
            if (step == 1) {
            	sharedFlag = true;
            	return ProcessingTools.dbiterator("history()", shared.iterator());
            } else if (step == 2) {
            	sharedFlag = false;
            	return next.iterator(query);
            }
			return null;
		}

		@Override
		public String toString() {			
			return (!sharedFlag ? "shared-after(" : "shared-after-passed(")+"["+passed+"] "+current.toString()+")";
		}
		
		
		
	}

	
	
}
