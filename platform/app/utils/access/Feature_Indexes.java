package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.MidataId;
import utils.AccessLog;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMatch;
import utils.access.index.IndexRoot;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class Feature_Indexes extends Feature {

private Feature next;
	
	public Feature_Indexes(Feature next) {
		this.next = next;
	}
	
	public final static int INDEX_REVERSE_USE = 300;
	public final static int AUTOCREATE_INDEX_COUNT = 30;
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.restrictedBy("index") && !q.restrictedBy("_id")) {
			
			IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), false);
			
		    if (pseudo == null) {
				List<DBRecord> recs = next.query(q);
				if (recs.size() > AUTOCREATE_INDEX_COUNT) {
					pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
				} else { return recs; }
			}			
			
			AccessLog.logBegin("start index query");
			Map<String,Object> indexQuery = (Map<String,Object>) q.getProperties().get("index");
			
			List<String> pathes = new ArrayList<String>();
			Condition[] condition = new Condition[indexQuery.size()];

			int idx = 0;
			for (Map.Entry<String, Object> entry : indexQuery.entrySet()) {
				pathes.add(entry.getKey());
				condition[idx] = AndCondition.parseRemaining(entry.getValue()).optimize();
				idx++;
			}
												
			IndexDefinition index = IndexManager.instance.findIndex(pseudo, q.getRestriction("format"), pathes);
			
			if (index == null) { 
				AccessLog.logBegin("start index creation");
				index = IndexManager.instance.createIndex(pseudo, q.getRestriction("format"), pathes);
				AccessLog.logEnd("end index creation");
			}
			
			Set<MidataId> targetAps;
			
			if (!q.getApsId().equals(q.getCache().getAccountOwner())) {
				targetAps = Collections.singleton(q.getApsId());
			} else {
				boolean allTarget = Feature_AccountQuery.allApsIncluded(q);
													
				if (allTarget) {
				   targetAps = null;			   			  
				} else {
				   Set<Consent> consents = Feature_AccountQuery.getConsentsForQuery(q);			
				   targetAps =  new HashSet<MidataId>();
				   if (Feature_AccountQuery.mainApsIncluded(q)) targetAps.add(q.getApsId());
				   for (Consent consent : consents) targetAps.add(consent._id);
				}
			}
						
			List<DBRecord> result = new ArrayList<DBRecord>();
			IndexRoot root = IndexManager.instance.getIndexRootAndUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index, targetAps);
			Collection<IndexMatch> matches = IndexManager.instance.queryIndex(root, condition);
			
			Map<MidataId, Set<MidataId>> filterMatches = new HashMap<MidataId, Set<MidataId>>();
			for (IndexMatch match : matches) {
				if (targetAps == null || targetAps.contains(match.apsId)) {
					Set<MidataId> ids = filterMatches.get(match.apsId);
					if (ids == null) {
						ids = new HashSet<MidataId>();
						filterMatches.put(match.apsId, ids);
					}
					ids.add(match.recordId);				
				}
			}
			
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			queryFields.addAll(q.getFieldsFromDB());
			
			for (Map.Entry<MidataId, Set<MidataId>> entry : filterMatches.entrySet()) {				
			   MidataId aps = entry.getKey();
			   AccessLog.log("Now processing aps:"+aps.toString());
			   Set<MidataId> ids = entry.getValue();
			   
			   if (ids.size() > INDEX_REVERSE_USE) {
				   Query q4 = new Query(q, CMaps.map(), aps);
				   List<DBRecord> unindexed = next.query(q4);
				   for (DBRecord candidate : unindexed) {
					   if (ids.contains(candidate._id)) result.add(candidate);
				   }
				   AccessLog.log("add unindexed ="+unindexed.size());
				   //result.addAll(unindexed);
			   } else {
				   Map<String, Object> readRecs = new HashMap<String, Object>();
				   boolean add = false;
				   boolean directQuery = true;
				   if (ids.size() > 5) {
					    Map<String, Object> props = new HashMap<String, Object>();
						props.putAll(q.getProperties());
						props.put("streams", "only");
						List<DBRecord> matchStreams = next.query(new Query(props, Sets.create("_id"), q.getCache(), aps));
						AccessLog.log("index query streams "+matchStreams.size()+" matches.");
						if (matchStreams.isEmpty()) directQuery = false;
						else {
							Set<MidataId> streams = new HashSet<MidataId>();
							for (DBRecord r : matchStreams) streams.add(r._id);
							readRecs.put("stream", streams);
						}
						add = true;
				   }
				   readRecs.put("_id", ids);
				
				   int directSize = 0;
				   if (directQuery) {
					   long time = System.currentTimeMillis();
					   List<DBRecord> partresult = new ArrayList(DBRecord.getAll(readRecs, queryFields));
					   AccessLog.log("db time:"+(System.currentTimeMillis() - time));
					   
					   Query q3 = new Query(q, CMaps.map("strict", "true"), aps);
					   partresult = Feature_Prefetch.lookup(q3, partresult, next);
					   result.addAll(partresult);
					   directSize = partresult.size();
				   }
					
					if (add) {
		              Query q2 = new Query(q, CMaps.map(q.getProperties()).map("_id", ids), aps);
		              List<DBRecord> additional = next.query(q2);
		              result.addAll(additional);
		              AccessLog.log("looked up directly="+directSize+" additionally="+additional.size());
					} else {
		              AccessLog.log("looked up directly="+directSize);
					}		            
					
			   }
			}

			AccessLog.log("index query found "+matches.size()+" matches, "+result.size()+" in correct aps.");
																													
			IndexManager.instance.revalidate(result, root, indexQuery, condition);
			
			AccessLog.logEnd("end index query "+result.size()+" matches.");
			return result;
						
			
		} else return next.query(q);
	}
	

	
}
