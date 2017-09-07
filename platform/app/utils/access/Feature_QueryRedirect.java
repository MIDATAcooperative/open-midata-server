package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BasicBSONObject;

import models.MidataId;
import utils.AccessLog;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

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
	protected List<DBRecord> query(Query q) throws AppException {
		
		APS target = q.getCache().getAPS(q.getApsId());
		BasicBSONObject query = target.getMeta(APS.QUERY);    	
    	// Ignores queries in main APS 
		if (query != null && !q.getApsId().equals(q.getCache().getAccountOwner())) {			
			List<DBRecord> result;
			
			if (q.restrictedBy("redirect-only") || target.hasNoDirectEntries()) {
			  result = new ArrayList<DBRecord>();	
			} else {
			  result = next.query(q);
			}
			
			if (!q.restrictedBy("ignore-redirect")) {
				if (query.containsField("$or")) {
					Collection queryparts = (Collection) query.get("$or");
					for (Object part : queryparts) {
						query(q, (BasicBSONObject) part, result);
					}
				} else {
					query(q, query, result);
				}
			}
						
			return result;
						
		}
		
		return next.query(q);		
	}
	
	private void query(Query q, BasicBSONObject query, List<DBRecord> results) throws AppException {
		Map<String, Object> combined = combineQuery(q.getProperties(), query);
		if (combined == null) {
			AccessLog.log("combine empty:");			
			return;
		}
		Object targetAPSId = query.get("aps");
		AccessLog.logBegin("begin redirect to Query:");
		List<DBRecord> result = next.query(new Query(combined, q.getFields(), q.getCache(), new MidataId(targetAPSId.toString()), new AccountAccessContext(q.getCache(), q.getContext())));
		
		/*if (query.containsField("_exclude") && result.size() > 0) {			
			List<DBRecord> excluded = QueryEngine.listFromMemory(q.getCache(), (Map<String, Object>) query.get("_exclude"), result);
            result.removeAll(excluded);						
		}*/
		
		results.addAll(result);
		AccessLog.logEnd("end redirect");
	}
	
	private List<DBRecord> memoryQuery(Query q, BasicBSONObject query, List<DBRecord> results) throws AppException {
		Map<String, Object> combined = combineQuery(q.getProperties(), query);
		if (combined == null) {
			AccessLog.log("combine empty:");			
			return Collections.emptyList();
		}
		
		List<DBRecord> result = QueryEngine.listFromMemory(q.getCache(), combined, results); 
									
		/*if (query.containsField("_exclude") && result.size() > 0) {			
			List<DBRecord> excluded = QueryEngine.listFromMemory(q.getCache(), (Map<String, Object>) query.get("_exclude"), result);
            result.removeAll(excluded);						
		}*/ 
		
		return result;
	}
	
	public static Map<String, Object> combineQuery(Map<String,Object> properties, Map<String,Object> query) throws InternalServerException {
		
		Map<String, Object> combined = new HashMap<String,Object>();
		combined.putAll(properties);
		for (String key : query.keySet()) {
			if (combined.containsKey(key)) {
				Object val1 = combined.get(key);
				Object val2 = query.get(key);
				//if (val1 instanceof MidataId) val1 = val1.toString();
				//if (val2 instanceof MidataId) val2 = val2.toString();
				if (val1.equals(val2) || val1.toString().equals(val2.toString())) continue;
				if (val1 instanceof Collection<?>) {
				 if (val2 instanceof Collection<?>) {
					Collection<?> c1 = (Collection<?>) val1;
					Collection<?> c2 = (Collection<?>) val2;
					ArrayList<Object> comb = new ArrayList<Object>();
					for (Object o : c1) {
						if (c2.contains(o) || c2.contains(o.toString())) comb.add(o);
					}
					combined.put(key, comb);
					if (comb.isEmpty()) {		
						AccessLog.log("empty (col/col): "+key);
						return null;
					}
				 } else {
					 if ( ((Collection<?>) val1).contains(val2) || ((Collection<?>) val1).contains(val2.toString())) {
						 combined.put(key, val2);
					 } else {						 
						 AccessLog.log("empty (col/val): "+key);
						 return null;
					 }
				 }
				} else {
					if (val2 instanceof Collection<?>) {
						if ( ((Collection<?>) val2).contains(val1) || ((Collection<?>) val2).contains(val1.toString())) continue;
						else {						
							AccessLog.log("empty (val/col): "+key+" val1="+val1.toString()+" val2="+val2.toString());
							return null;
						}
					} else {					
						AccessLog.log("empty (val/val): "+key);
						return null;
					}
				}
				
			} else combined.put(key, query.get(key));
		}
				
		return combined;
	}
	
	public static void setQuery(APSCache cache, MidataId apsId, Map<String, Object> query) throws AppException {		
		APS aps = cache.getAPS(apsId);
		aps.setMeta(APS.QUERY, query);
		
		//List<DBRecord> r = FormatHandling.findStreams(new Query(query, Sets.create("_id","key"),  cache, true);
	}


	

}
