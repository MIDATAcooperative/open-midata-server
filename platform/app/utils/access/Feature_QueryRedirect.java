package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.collections.Sets;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import com.mongodb.BasicDBObject;

import controllers.RuleApplication;

import models.FilterRule;
import models.Record;

/**
 * allows access permission sets not only to have a list of records but also a link to another
 * APS with a filter-query 
 *
 */
public class Feature_QueryRedirect extends Feature {

    private Feature next;
	
	public Feature_QueryRedirect(Feature next) {
		this.next = next;
	}
	
	@Override
	protected List<DBRecord> lookup(List<DBRecord> record, Query q)
			throws AppException {		
		List<DBRecord> result = next.lookup(record, q);
		
		// Add Filter
				
		BasicBSONObject query = q.getCache().getAPS(q.getApsId()).getMeta(APS.QUERY);    	
    	// Ignores queries in main APS 
		if (query != null && !q.getApsId().equals(q.getCache().getOwner())) {			
						
			if (query.containsField("$or")) {
				Collection queryparts = (Collection) query.get("$or");
				List<DBRecord> filteredResult = new ArrayList<DBRecord>(result.size());
				for (Object part : queryparts) {
					filteredResult.addAll(memoryQuery(q, (BasicBSONObject) part, result));
				}
				return filteredResult;
			} else {
				return memoryQuery(q, query, result);
			}															
		}
					
		return result;
	}
		

	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		
		BasicBSONObject query = q.getCache().getAPS(q.getApsId()).getMeta(APS.QUERY);    	
    	// Ignores queries in main APS 
		if (query != null && !q.getApsId().equals(q.getCache().getOwner())) {			
			List<DBRecord> result = next.query(q);
			
			if (query.containsField("$or")) {
				Collection queryparts = (Collection) query.get("$or");
				for (Object part : queryparts) {
					query(q, (BasicBSONObject) part, result);
				}
			} else {
				query(q, query, result);
			}
						
			return result;
						
		}
		
		return next.query(q);		
	}
	
	private void query(Query q, BasicBSONObject query, List<DBRecord> results) throws AppException {
		Map<String, Object> combined = combineQuery(q.getProperties(), query);
		if (combined == null) {
			AccessLog.debug("combine empty:");			
			return;
		}
		Object targetAPSId = query.get("aps");
		AccessLog.debug("Redirect to Query:");
		List<DBRecord> result = next.query(new Query(combined, q.getFields(), q.getCache(), new ObjectId(targetAPSId.toString())));
		
		if (query.containsField("_exclude") && result.size() > 0) {			
			List<DBRecord> excluded = QueryEngine.listFromMemory((Map<String, Object>) query.get("_exclude"), result);
            result.removeAll(excluded);						
		} 
		
		results.addAll(result);
	}
	
	private List<DBRecord> memoryQuery(Query q, BasicBSONObject query, List<DBRecord> results) throws AppException {
		Map<String, Object> combined = combineQuery(q.getProperties(), query);
		if (combined == null) {
			AccessLog.debug("combine empty:");			
			return Collections.emptyList();
		}
		
		List<DBRecord> result = QueryEngine.listFromMemory(combined, results); 
									
		if (query.containsField("_exclude") && result.size() > 0) {			
			List<DBRecord> excluded = QueryEngine.listFromMemory((Map<String, Object>) query.get("_exclude"), result);
            result.removeAll(excluded);						
		} 
		
		return result;
	}
	
	public static Map<String, Object> combineQuery(Map<String,Object> properties, Map<String,Object> query) throws InternalServerException {
		
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
						return null;
					}
				 } else {
					 if ( ((Collection<?>) val1).contains(val2)) {
						 combined.put(key, val2);
					 } else {						 
						 return null;
					 }
				 }
				} else {
					if (val2 instanceof Collection<?>) {
						if ( ((Collection<?>) val2).contains(val1)) continue;
						else {						
							return null;
						}
					} else {					
						return null;
					}
				}
				
			} else combined.put(key, query.get(key));
		}
				
		return combined;
	}
	
	public static void setQuery(APSCache cache, ObjectId apsId, Map<String, Object> query) throws AppException {		
		APS aps = cache.getAPS(apsId);
		aps.setMeta(APS.QUERY, query);
		
		//List<DBRecord> r = FormatHandling.findStreams(new Query(query, Sets.create("_id","key"),  cache, true);
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws AppException {
		return next.postProcess(records, q);
		
	}

	

}