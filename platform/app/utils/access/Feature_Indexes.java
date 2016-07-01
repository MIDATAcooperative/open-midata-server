package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;

import org.bson.types.ObjectId;

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
	
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.restrictedBy("index") && q.getApsId().equals(q.getCache().getOwner())) {
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
												
			IndexDefinition index = IndexManager.instance.findIndex(q.getCache(),  q.getCache().getOwner(), q.getRestriction("format"), pathes);
			
			if (index == null) { 
				AccessLog.logBegin("start index creation");
				index = IndexManager.instance.createIndex(q.getCache(), q.getCache().getOwner(), q.getRestriction("format"), q.isRestrictedToSelf(), pathes);
				AccessLog.logEnd("end index creation");
			}
			
			Set<Consent> consents = Feature_AccountQuery.getConsentsForQuery(q);			
			Set<ObjectId> targetAps = new HashSet<ObjectId>();
			if (Feature_AccountQuery.mainApsIncluded(q)) targetAps.add(q.getApsId());
			for (Consent consent : consents) targetAps.add(consent._id);
						
			List<DBRecord> result = new ArrayList<DBRecord>();
			IndexRoot root = IndexManager.instance.getIndexRootAndUpdate(q.getCache(), q.getCache().getOwner(), index, targetAps);
			Collection<IndexMatch> matches = IndexManager.instance.queryIndex(root, condition);
			
			Map<ObjectId, Set<ObjectId>> filterMatches = new HashMap<ObjectId, Set<ObjectId>>();
			for (IndexMatch match : matches) {
				if (targetAps.contains(match.apsId)) {
					Set<ObjectId> ids = filterMatches.get(match.apsId);
					if (ids == null) {
						ids = new HashSet<ObjectId>();
						filterMatches.put(match.apsId, ids);
					}
					ids.add(match.recordId);				
				}
			}
			
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			queryFields.addAll(q.getFieldsFromDB());
			
			for (Map.Entry<ObjectId, Set<ObjectId>> entry : filterMatches.entrySet()) {				
			   ObjectId aps = entry.getKey();
			   AccessLog.log("Now processing aps:"+aps.toString());
			   Set<ObjectId> ids = entry.getValue();
			   Map<String, Object> readRecs = new HashMap<String, Object>();
			   if (ids.size() > 5) {
				    Map<String, Object> props = new HashMap<String, Object>();
					props.putAll(q.getProperties());
					props.put("streams", "only");
					List<DBRecord> matchStreams = next.query(new Query(props, Sets.create("_id"), q.getCache(), aps));
					AccessLog.log("index query streams "+matchStreams.size()+" matches.");
					Set<ObjectId> streams = new HashSet<ObjectId>();
					for (DBRecord r : matchStreams) streams.add(r._id);
					readRecs.put("stream", streams);
			   }
			   readRecs.put("_id", ids);
				
			   List<DBRecord> partresult = new ArrayList(DBRecord.getAll(readRecs, queryFields));
				
				Query q3 = new Query(q, CMaps.map("strict", "true"), aps);
				partresult = Feature_Prefetch.lookup(q3, partresult, next);
				
	            Query q2 = new Query(q, CMaps.map("_id", ids));
	            List<DBRecord> additional = next.query(q2);
	            AccessLog.log("looked up directly="+partresult.size()+" additionally="+additional.size());
	            result.addAll(partresult);
				result.addAll(additional);
			}

			AccessLog.log("index query found "+matches.size()+" matches, "+result.size()+" in correct aps.");
																													
			IndexManager.instance.revalidate(result, root, indexQuery, condition);
			
			AccessLog.logEnd("end index query "+result.size()+" matches.");
			return result;
			
		} else return next.query(q);
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q) throws AppException {		
		return next.postProcess(records, q);
	}

	
}
