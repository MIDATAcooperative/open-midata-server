package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.access.index.IndexDefinition;
import utils.access.index.IndexMatch;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class Feature_Indexes extends Feature {

private Feature next;
	
	public Feature_Indexes(Feature next) {
		this.next = next;
	}
	
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q) throws AppException {
		return next.lookup(record, q);
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
			
			if (index == null) { // TODO Restriction to self or not
				AccessLog.logBegin("start index creation");
				index = IndexManager.instance.createIndex(q.getCache(), q.getCache().getOwner(), q.getRestriction("format"), true, pathes);
				AccessLog.logEnd("end index creation");
			}
			
			List<DBRecord> result = new ArrayList<DBRecord>();
			Collection<IndexMatch> matches = IndexManager.instance.queryIndex(q.getCache(), q.getCache().getOwner(), index, condition);
			
			Map<String, Object> readRecs = new HashMap<String, Object>();
			
			if (matches.size() > 5) {
				Map<String, Object> props = new HashMap<String, Object>();
				props.putAll(q.getProperties());
				props.put("streams", "only");
				List<DBRecord> matchStreams = next.query(new Query(props, Sets.create("_id"), q.getCache(), q.getApsId()));
				AccessLog.debug("index query streams "+matchStreams.size()+" matches.");
				Set<ObjectId> streams = new HashSet<ObjectId>();
				for (DBRecord r : matchStreams) streams.add(r._id);
				readRecs.put("stream", streams);
			}
			
			AccessLog.debug("index query found "+matches.size()+" matches.");
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
			queryFields.addAll(q.getFieldsFromDB());
			Set<ObjectId> recIds = new HashSet<ObjectId>();
			
			for (IndexMatch match : matches) {
				recIds.add(match.recordId);				
			}
			readRecs.put("_id", recIds);
						
			result = new ArrayList(DBRecord.getAll(readRecs, queryFields));
			
			result = next.lookup(result, q);			
			AccessLog.logEnd("end index query "+result.size()+" matches.");
			return result;
			
		} else return next.query(q);
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q) throws AppException {		
		return next.postProcess(records, q);
	}

}
