package utils.access;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.exceptions.AppException;

public class Feature_Consents extends Feature {

	private Feature next;
	
	public Feature_Consents(Feature next) {
		this.next = next;
	}
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q) throws AppException {
		return next.lookup(record, q);
	}

	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.restrictedBy("shared-after")) {			
			Date after = q.getDateRestriction("shared-after");
			AccessLog.logBegin("start history query after="+after.toString());
			List<DBRecord> recs = q.getCache().getAPS(q.getApsId()).historyQuery(after.getTime(), false);			
			
			recs = next.lookup(recs, q);
			List<DBRecord> result = new ArrayList<DBRecord>(recs.size());
			AccessLog.logDB("found "+recs.size()+" history entries");
			if (recs.size() > 0) {
			/*  Set<ObjectId> ids = new HashSet<ObjectId>();
			  for (DBRecord r : recs) ids.add(r._id);
			  Query q2 = new Query(q, CMaps.map(q.getProperties()).map("_id", ids));
			  result = next.query(q2); //next.lookup(recs, q);
			}*/
			
			
				boolean onlyStreams = q.isStreamOnlyQuery();
				for (DBRecord r : recs) {
					if (r.isStream) {
						Query q2 = new Query(q, CMaps.map("stream", r._id));
	                    result.addAll(QueryEngine.onlyWithKey(next.query(q2)));
					} else if (!onlyStreams) result.add(r);
				}
			}
			
			AccessLog.logEnd("ended history query with size="+result.size());
			
			return result;
		} 
		return next.query(q);
		/*
		if (q.restrictedBy("removed-after")) {
			Date after = q.getDateRestriction("removed-after");
			List<DBRecord> recs = q.getCache().getAPS(q.getApsId()).historyQuery(after.getTime(), false);
			if (recs.size() == 0) return new ArrayList<DBRecord>();
			
			List<DBRecord> result = new ArrayList<DBRecord>(recs.size());
			
			
		}*/
		
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q) throws AppException {
		return next.postProcess(records, q);
	}

}