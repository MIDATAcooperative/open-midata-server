/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import utils.RuntimeConstants;
import utils.access.Feature_Indexes.IndexUse;
import utils.collections.CMaps;
import utils.collections.NChainedMap;
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
			
			IndexUse iuse = (IndexUse) q.getProperties().get("index-ts-provider");
			if (iuse != null) {
				long v = iuse.version(q.getApsId());
				if (v<=0) {
					AccessLog.log("shared-after: new aps: "+q.getApsId());
					return new SetSharedDateIterator(new Date(q.getCache().getAPS(q.getApsId()).getLastChanged()), next.iterator(q.withoutTime()));
				}
			} 
			Date after = q.getDateRestriction("shared-after");
			AccessLog.logBeginPath("history-after("+after.toString()+")", null);
			Query qnt = q.withoutTime();
			
			List<DBRecord> recs = q.getCache().getAPS(q.getApsId()).historyQuery(after.getTime(), false);			
			
			if (!recs.isEmpty()) recs = lookup(qnt, recs, next);
			List<DBRecord> result = Collections.emptyList();			
			if (recs.size() > 0) {				
			
				boolean onlyStreams = qnt.isStreamOnlyQuery();
				for (DBRecord r : recs) {
					if (r.isStream!=null) {
						Query q2 = new Query(q, "history-after", CMaps.map("stream", r._id));
						List<DBRecord> subresult = QueryEngine.onlyWithKey(next.query(q2));
						for (DBRecord r2 : subresult) {
							r2.sharedAt = r.sharedAt; 							
						}
	                    result = QueryEngine.combine(result, subresult);
					} else if (!onlyStreams) QueryEngine.combine(result, Collections.singletonList(r));
				}
			}
			
			AccessLog.logEndPath("entries="+recs.size()+" results="+result.size());
			if (result.isEmpty()) return next.iterator(q);
			return ProcessingTools.noDuplicates(new SharedThenNormal(result, next, q));
		} 
		return next.iterator(q);	
	}
	
	protected static List<DBRecord> lookup(Query q, List<DBRecord> prefetched, Feature next) throws AppException {
	
		AccessLog.logBeginPath("simple-lookup("+prefetched.size()+")", null);
		long time = System.currentTimeMillis();
		List<DBRecord> results = null;
	
		Query q2 = new Query(q, "simple-lookup", CMaps.map("_id", q.getApsId()).map("flat", "true").map("streams", "true"));
		for (DBRecord record : prefetched) {
			  q2.getProperties().put("_id", record._id);
		      List<DBRecord> partResult = next.query(q2);	
					 				 		  
		      // Keep sharedAt from input if we got one
		      if (record.sharedAt != null && partResult != null) {
			    for (DBRecord r : partResult) r.sharedAt = record.sharedAt;
		      }
		  
		      results = QueryEngine.combine(results,  partResult);
		}
		if (results==null) results = Collections.emptyList();
																			
		AccessLog.logEndPath("#found="+results.size()+" time="+(System.currentTimeMillis() - time));
		return results;
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

	static class SetSharedDateIterator implements DBIterator<DBRecord> {
		Date sharedAt;
		DBIterator<DBRecord> it;
		
		public SetSharedDateIterator(Date sharedAt, DBIterator<DBRecord> it) {
			this.sharedAt = sharedAt;
			this.it = it;
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord r = it.next();
			r.sharedAt = sharedAt;
			return r;
		}

		@Override
		public boolean hasNext() throws AppException {
			return it.hasNext();
		}
		
		
	}
	
}
