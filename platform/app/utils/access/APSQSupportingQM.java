package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.collections.Sets;
import utils.db.LostUpdateException;

import com.mongodb.BasicDBObject;

import models.ModelException;
import models.Record;

public class APSQSupportingQM extends QueryManager {

    private QueryManager next;
	
	public APSQSupportingQM(QueryManager next) {
		this.next = next;
	}
	
	@Override
	protected boolean lookupSingle(Record record, Query q) throws ModelException {
		
		
        // TODO Correct
		return next.lookupSingle(record, q);
	}

	@Override
	protected List<Record> query(Query q) throws ModelException {
		
		BasicBSONObject query = q.getCache().getAPS(q.getApsId()).getMeta(SingleAPSManager.QUERY);
    	SingleAPSManager queryAPS;
    	// Ignores queries in main APS 
		if (query != null && !q.getApsId().equals(q.getCache().getOwner())) {			
			Map<String, Object> combined = combineQuery(q.getProperties(), query);
			if (combined == null) {
				AccessLog.debug("combine empty:");
				//AccessLog.logMap(q.getProperties());
				//AccessLog.logMap(query);
				return new ArrayList<Record>();
			}
			Object targetAPSId = query.get("aps");
			AccessLog.debug("Redirect to Query:");
			List<Record> result = next.query(new Query(combined, q.getFields(), q.getCache(), new ObjectId(targetAPSId.toString())));
			List<Record> result2 = next.query(q);
			result.addAll(result2);
			return result;
			
			/*queryAPS = q.getCache().getAPS(new ObjectId(targetAPSId.toString()));
			
			boolean ok = StreamLayouter.instance.adjustQuery(eaps.getAccessor(), queryAPS.getId(), combined);
			AccessLog.logQuery(q.getBaseAPS().getId(), q.getProperties() ,q.getFields());
			AccessLog.debug("is okay?"+ok);
			return ok ? queryAPS.lookup(combined, fields) : Collections.<Record>emptyList();*/
		}
		
		return next.query(q);		
	}
	
	public static Map<String, Object> combineQuery(Map<String,Object> properties, Map<String,Object> query) throws ModelException {
		//Object fq = properties.get("format");
		//if (fq != null && fq.equals(Query.STREAM_TYPE)) return properties;
		Map<String, Object> combined = new HashMap<String,Object>();
		combined.putAll(properties);
		for (String key : query.keySet()) {
			if (combined.containsKey(key)) {
				Object val1 = combined.get(key);
				Object val2 = query.get(key);
				if (val1.equals(val2)) continue;
				if (val1 instanceof Collection<?>) {
				 if (val2 instanceof Collection<?>) {
					((Collection<?>) val1).retainAll((Collection<?>) val2);
					if (((Collection<?>) val1).isEmpty()) {
						//AccessLog.debug("A");
						return null;
					}
				 } else {
					 if ( ((Collection<?>) val1).contains(val2)) {
						 combined.put(key, val2);
					 } else {
						 //AccessLog.debug("B");
						 return null;
					 }
				 }
				} else {
					if (val2 instanceof Collection<?>) {
						if ( ((Collection<?>) val2).contains(val1)) continue;
						else {
							//AccessLog.debug("C: "+val2.toString()+" v:"+val1.toString());
							return null;
						}
					} else {
						//AccessLog.debug("D");
						return null;
					}
				}
				
			} else combined.put(key, query.get(key));
		}
				
		return combined;
	}
	
	public static void setQuery(APSCache cache, ObjectId apsId, Map<String, Object> query) throws ModelException {		
		SingleAPSManager aps = cache.getAPS(apsId);
		aps.setMeta(SingleAPSManager.QUERY, query);
		
		//List<Record> r = FormatHandling.findStreams(new Query(query, Sets.create("_id","key"),  cache, true);
	}

	@Override
	protected void postProcess(List<Record> records, Query q)
			throws ModelException {
		next.postProcess(records, q);
		
	}

}
