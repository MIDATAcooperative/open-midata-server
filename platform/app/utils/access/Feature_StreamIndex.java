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
			!q.isRestrictedOnTime() &&
			(Feature_AccountQuery.allApsIncluded(q) || q.restrictedBy("_id"))) {	
		
			AccessLog.logBegin("start stream index query");
			long startTime = System.currentTimeMillis();
			
			//Set<MidataId> targetAps = Feature_Indexes.determineTargetAps(q);
				
			List<DBRecord> result = Collections.emptyList();
	
			/*if (targetAps != null && targetAps.isEmpty()) {
				AccessLog.logEnd("end stream index query no target APS");
				return ProcessingTools.empty();
			}*/
	
			StreamIndexRoot index = IndexManager.instance.getStreamIndex(q.getCache(), q.getCache().getAccountOwner());
					
			StreamIndexLookup lookup = new StreamIndexLookup();			
			if (q.restrictedBy("app")) lookup.setApp(q.getMidataIdRestriction("app"));
			if (q.restrictedBy("owner")) lookup.setOwner(q.getMidataIdRestriction("owner")); 
			if (q.restrictedBy("content")) lookup.setContent(q.getRestriction("content"));
			if (q.restrictedBy("format")) lookup.setFormat(q.getRestriction("format"));
			AccessLog.log(lookup.toString());
			
			Collection<DBRecord> matches = null;
			if (q.restrictedBy("_id")) {
				List<DBRecord> prefetched = QueryEngine.lookupRecordsById(q);
				matches = new ArrayList<DBRecord>(prefetched.size());
				for (DBRecord r : prefetched) {
					lookup.setId(r._id);
					Collection<DBRecord> match = index.lookup(lookup);
					if (match.isEmpty() && r.stream != null) {
						lookup.setId(r.stream);
						match = index.lookup(lookup);
					}
					if (!match.isEmpty()) {
						matches.addAll(match);
					}
				}
				
			} else {
				matches = index.lookup(lookup);
			}			
					
				
			IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
			IndexManager.instance.triggerUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index.getModel(), null);
			
	        AccessLog.log("index matches: "+matches.size());
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
	
			AccessLog.logBegin("start to look for new entries");
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
				add = nextWithProcessing.query(new Query(q, CMaps.mapPositive("shared-after", v).map("consent-limit",1000).map("streams","true").map("flat","true")));
				AccessLog.log("found new updated entries: " + add.size());
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
			AccessLog.logEnd("end to look for new entries");
			long endTime2 = System.currentTimeMillis();
	
			if (allAps.size() > Feature_AccountQuery.MIN_FOR_ACCELERATION) {
			  List<Consent> prefetched = new ArrayList<Consent>(Consent.getAllByAuthorized(q.getCache().getAccountOwner(), CMaps.map("_id", allAps), Consent.SMALL));
			  q.getCache().cache(prefetched);
			  FasterDecryptTool.accelerate(q, prefetched);
			}
			List<AccessContext> contexts = new ArrayList<AccessContext>(allAps.size());
			for (MidataId aps : allAps) {
				AccessContext context = Feature_Indexes.getContextForAps(q, aps);
				if (context != null)
					contexts.add(context);
			}
			Collections.sort(contexts, new Feature_Indexes.ContextComparator());
			AccessLog.log("index matches "+contexts.size()+" contexts");
	
			//Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			//queryFields.addAll(q.getFieldsFromDB());
	
			AccessLog.logEnd("end index query");
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

		StreamIndexIterator(Query q, StreamIndexLookup lookup, List<AccessContext> contexts, Map<MidataId, List<DBRecord>> newRecords, Map<MidataId, Set<DBRecord>> matches) throws AppException {
			this.q = q;
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
			AccessLog.log("Now processing aps:" + aps.toString());

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
	
}
