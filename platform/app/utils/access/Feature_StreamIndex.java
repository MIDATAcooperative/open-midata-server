/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.MidataId;
import utils.AccessLog;
import utils.access.Feature_AccountQuery.IdAndConsentFieldIterator;
import utils.access.index.StreamIndexKey;
import utils.access.index.StreamIndexLookup;
import utils.access.index.StreamIndexRoot;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class Feature_StreamIndex extends Feature {

	private Feature next;

	public Feature_StreamIndex(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.getApsId().equals(q.getCache().getAccountOwner()) &&
			(!q.isRestrictedOnTime() &&
			Feature_AccountQuery.allApsIncluded(q)) || (!q.deepQuery() && q.restrictedBy("_id"))) {	
		
			AccessLog.logBeginPath("stream-index", null);
			long startTime = System.currentTimeMillis();
			
			//Set<MidataId> targetAps = Feature_Indexes.determineTargetAps(q);
				
			List<DBRecord> result = Collections.emptyList();
	
			/*if (targetAps != null && targetAps.isEmpty()) {
				AccessLog.logEnd("end stream index query no target APS");
				return ProcessingTools.empty();
			}*/
	       
			StreamIndexRoot index = q.getCache().getStreamIndexRoot(); 
					
			StreamIndexLookup lookup = new StreamIndexLookup();			
			if (q.restrictedBy("app")) lookup.setApp(q.getMidataIdRestriction("app"));
			if (q.restrictedBy("owner")) lookup.setOwner(q.getMidataIdRestriction("owner")); 
			if (q.restrictedBy("content")) lookup.setContent(q.getRestriction("content"));
			if (q.restrictedBy("format")) lookup.setFormat(q.getRestriction("format"));
			
			boolean allfound = false;
			Collection<DBRecord> matches = null;
			if (q.restrictedBy("_id")) {
				allfound = true;
				/*List<DBRecord> prefetched = null;
				if (q.restrictedBy("quick")) {					
					DBRecord record = (DBRecord) q.getProperties().get("quick");
					if (record.stream != null) prefetched = Collections.singletonList(record);
				}
				if (prefetched == null) prefetched = QueryEngine.lookupRecordsById(q);
				matches = new ArrayList<DBRecord>(prefetched.size());*/
				for (MidataId id : q.getMidataIdRestriction("_id")) {
					lookup.setId(id);
					Collection<DBRecord> match = index.lookup(lookup);
					if (match==null || match.isEmpty()) allfound = false;
					matches = QueryEngine.combine(matches,  match);					
				}
				
			} else {
				matches = index.lookup(lookup);
			}								           
			
	        //AccessLog.log("index matches: "+matches.size());
			Set<MidataId> allAps = new HashSet<MidataId>();
	
			Map<MidataId, Set<DBRecord>> filterMatches = new HashMap<MidataId, Set<DBRecord>>();
			for (DBRecord match : matches) {
				//if (targetAps == null || targetAps.contains(match.getAps())) {
					Set<DBRecord> ids = filterMatches.get(match.consentAps);
					if (ids == null) {
						ids = new HashSet<DBRecord>();
						filterMatches.put(match.consentAps, ids);
						allAps.add(match.consentAps);
					}
					ids.add(match);
				//}
			}
			
			Map<MidataId, List<DBRecord>> newRecords = new HashMap<MidataId, List<DBRecord>>();
	        if (!allfound) {
			    AccessLog.logBeginPath("new-entries", null);
				Feature nextWithProcessing = new Feature_ProcessFilters(next);
				
				/*if (targetAps != null) {
					for (MidataId id : targetAps) {
						long v = index.getVersion(id);
						result = null;
						AccessContext context = Feature_Indexes.getContextForAps(q, id);
						if (context != null) {
							if (context instanceof ConsentAccessContext && ((ConsentAccessContext) context).getConsent().dataupdate <= v) {
								continue;
							} 
		
							List<DBRecord> add;
							Query updQuery = new Query(q, CMaps.mapPositive("shared-after", v).map("owner", "self").map("streams","true").map("flat","true"), id, context);
							add = nextWithProcessing.query(updQuery);
							AccessLog.log("found new updated entries aps=" + id + ": " + add.size());
							result = QueryEngine.combine(result, add);						
							if (result != null) {
								newRecords.put(id, result);
								allAps.add(id);
							}
						}
					}
				} else {*/
					long v = index.getAllVersion();
					// AccessLog.log("vx="+v);
					List<DBRecord> add;
					add = nextWithProcessing.query(new Query(q, "streamindex-shared-after", CMaps.mapPositive("shared-after", v).map("consent-limit",1000).map("streams","true").map("flat","true")));
					//AccessLog.log("found new updated entries: " + add.size());
					for (DBRecord r : add) {
						AccessLog.log(" id="+r._id+" aps="+r.consentAps+" ow="+r.owner+" str="+r.isStream);
						AccessLog.log(r.context.toString());
					}
					if (add.size()>0) {
						IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getAccessor(), q.getApsId(), true);
						IndexManager.instance.triggerUpdate(pseudo, q.getCache(), q.getCache().getAccessor(), index.getModel(), null);		        
					}
					
					result = QueryEngine.combine(result, add);				
					if (result != null && !result.isEmpty()) {
						for (DBRecord record : result) {
							MidataId id = record.context.getTargetAps();
							List<DBRecord> recs = newRecords.get(id);
							if (recs == null) {
								recs = new ArrayList<DBRecord>();
								newRecords.put(id, recs);
								allAps.add(id);
							}
							recs.add(record);
						}
					}
				//}
				AccessLog.logEndPath("new="+add.size());
	        }
			long endTime2 = System.currentTimeMillis();
	
			if (allAps.size() > Feature_AccountQuery.MIN_FOR_ACCELERATION) {
			  List<Consent> prefetched = new ArrayList<Consent>(Consent.getAllByAuthorized(q.getCache().getAccountOwner(), CMaps.map("_id", allAps), Consent.SMALL));
			  q.getCache().cache(prefetched);
			  FasterDecryptTool.accelerate(q.getCache(), prefetched);
			}
			List<AccessContext> contexts = new ArrayList<AccessContext>(allAps.size());
			for (MidataId aps : allAps) {
				AccessContext context = Feature_Indexes.getContextForAps(q, aps);
				if (context != null)
					contexts.add(context);
			}
			Collections.sort(contexts, new Feature_Indexes.ContextComparator());
			//AccessLog.log("index matches "+contexts.size()+" contexts");
	
			//Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			//queryFields.addAll(q.getFieldsFromDB());
	
			AccessLog.logEndPath("matches="+matches.size()+" #contexts="+contexts.size());
			if (contexts.isEmpty()) return ProcessingTools.empty();
			return ProcessingTools.noDuplicates(new StreamIndexIterator(q, lookup, contexts, newRecords, filterMatches));
		} else return next.iterator(q);
	}
	
	public class StreamIndexIterator extends Feature.MultiSource<AccessContext> {

		private Query q;
		private Set<String> queryFields;
		private Map<MidataId, List<DBRecord>> newRecords;
		private Map<MidataId, Set<DBRecord>> matches;
		private StreamIndexLookup lookup;
		private AccessContext currentContext;
		private int size;
		private String path;

		StreamIndexIterator(Query q, StreamIndexLookup lookup, List<AccessContext> contexts, Map<MidataId, List<DBRecord>> newRecords, Map<MidataId, Set<DBRecord>> matches) throws AppException {
			this.q = q;
			this.path = AccessLog.lp()+"/stream-index";
			this.query = q;
			this.lookup = lookup;
			queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			queryFields.addAll(q.getFieldsFromDB());
			this.newRecords = newRecords;
			this.matches = matches;
			
			if (q.getFromRecord() != null) {
				DBRecord from = q.getFromRecord();
				if (from.owner != null) {
					Iterator<AccessContext> it = contexts.iterator();
					while (it.hasNext()) {					  
						AccessContext c = it.next();
						  if (c.getOwner().equals(from.owner)) {
							  init(c, it);
							  return;
						  }
					  }
					  init(it);
					  return;
				}
			}
			
			this.init(contexts.iterator());
		}

		@Override
		public DBIterator<DBRecord> advance(AccessContext context) throws AppException {
			MidataId aps = context.getTargetAps();
			MidataId owner = context.getCache().getAPS(aps).getStoredOwner();			
			List<DBRecord> result = newRecords.get(aps);
			if (result == null)
				result = new ArrayList<DBRecord>();
			AccessLog.log("now "+path+": aps=" + aps.toString());

			Set<DBRecord> ids = matches.get(aps);
			if (ids != null) result.addAll(ids);            							           
			Collections.sort(result);
			size = result.size();
			currentContext = context;
			
			
			DBIterator res = new Feature_ConsentRestrictions(new Feature_Streams(new Feature_InMemoryQuery(owner, result))).iterator(query);
			
			return new IdAndConsentFieldIterator(res, context, aps, query.returns("id"));
			
			//return ProcessingTools.dbiterator("stream-index-use()", result.iterator());
		}

		@Override
		public String toString() {
			return "stream-index-access(["+passed+"] { ow:"+currentContext.getOwner().toString()+", id:"+currentContext.getTargetAps().toString()+", size:"+size+" })";
		}
		
		

	}
	
	protected static List<DBRecord> lookup(Query q, List<DBRecord> prefetched, Feature next) throws AppException {
		
		//AccessLog.logBegin("start lookup #recs="+prefetched.size());
		AccessLog.logBeginPath("lookup("+prefetched.size()+")", null);
		List<DBRecord> results = null;
		for (DBRecord record : prefetched) {
			List<DBRecord> partResult = null;
			if (record.stream != null) {
				//AccessLog.log("STREAMS");
				List<DBRecord> streamRec = next.query(new Query(q,"streamindex-lookup",CMaps.map("_id", record.stream).map("streams","only").map("flat",true)));
				if (streamRec != null && !streamRec.isEmpty()) {
					//AccessLog.log("STREAMS V1");
					DBRecord stream = streamRec.get(0);
					partResult = QueryEngine.combine(q, "si-lookup", CMaps.map("_id", record._id).map("stream", record.stream).mapNotEmpty("owner", stream.owner).map("quick", record), next);
				} else {
					AccessLog.logPath("no stream found");
					partResult = QueryEngine.combine(q, "si-lookup-nostream", CMaps.map("_id", record._id).map("flat", true).map("quick", record), next);
				}
				if (partResult.isEmpty()) partResult = null;
				if (partResult == null) {
					AccessLog.logPath("trying public branch");
					streamRec = next.query(new Query(q,"si-public-lookup-stream",CMaps.map("_id", record.stream).map("streams","only").map("flat",true).map("public","only")));
					if (streamRec != null && !streamRec.isEmpty()) {
						DBRecord stream = streamRec.get(0);
						partResult = QueryEngine.combine(q, "si-public-lookup",CMaps.map("_id", record._id).map("stream", record.stream).mapNotEmpty("owner", stream.owner).map("public","only").map("quick", record), next);
						if (partResult.isEmpty()) partResult = null;
					} 
				}
			}
			if (partResult == null) {
				AccessLog.logPath("have nothing");
			  partResult = QueryEngine.combine(q, "si-lookup-bad", CMaps.map("_id", record._id).mapNotEmpty("stream", record.stream).map("quick",  record), next);
			}
			  // Keep sharedAt from input if we got one
			if (record.sharedAt != null && partResult != null) {
				  for (DBRecord r : partResult) r.sharedAt = record.sharedAt;
			}
			  
			results = QueryEngine.combine(results,  partResult);						
		}
		if (results==null) results = Collections.emptyList();
		AccessLog.logEndPath("found="+results.size());
		//AccessLog.logEnd("end lookup");
		return results;
	}
}
