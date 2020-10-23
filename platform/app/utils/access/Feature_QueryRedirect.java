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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.MidataId;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;

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
	
	/*
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		
		APS target = q.getCache().getAPS(q.getApsId());
		BasicBSONObject query = target.getMeta(APS.QUERY);    	
    	// Ignores queries in main APS 
		if (query != null && !q.getApsId().equals(q.getCache().getAccountOwner())) {			
			List<DBRecord> result;
			
			if (q.restrictedBy("redirect-only") || target.hasNoDirectEntries()) {
			  result = Collections.emptyList();	
			} else {
			  result = next.query(q);
			}
			
			if (!q.restrictedBy("ignore-redirect")) {				
				if (query.containsField("$or")) {
					Collection queryparts = (Collection) query.get("$or");
					for (Object part : queryparts) {
						result = query(q, (BasicBSONObject) part, result);
					}
				} else {
					result = query(q, query, result);
				}
			}
						
			return result;
						
		}
		
		return next.query(q);		
	}
	*/
	
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		APS target = q.getCache().getAPS(q.getApsId());
		BasicBSONObject query = target.getMeta(APS.QUERY);    	
    	// Ignores queries in main APS 
		if (query != null && !q.getApsId().equals(q.getCache().getAccountOwner())) {			
			return new RedirectIterator(target, query, next, q);						
		}
		
		return next.iterator(q);	
	}
	
	static class RedirectIterator extends Feature.MultiSource<Integer> {

		private APS target;
		private BasicBSONObject requery;
		private Feature next;	
		private boolean redirect;
		
		RedirectIterator(APS target, BasicBSONObject query, Feature next, Query q) throws AppException {			
			this.target = target;
			this.requery = query;
			this.next = next;
			this.query = q;
			
			Integer[] steps = {1,2};
			init(Arrays.asList(steps).iterator());
		}
		
		@Override
		public DBIterator<DBRecord> advance(Integer step) throws AppException {
            if (step == 1) {
            	redirect = false;
            	if (query.restrictedBy("redirect-only") || target.hasNoDirectEntries()) {
      			  return ProcessingTools.empty();
      			} else {
      				query.setFromRecord(null);
      			  return next.iterator(query);
      			}	
            } else if (step == 2) {
            	redirect = true;
            	if (!query.restrictedBy("ignore-redirect")) {
            		Object targetAPSId = requery.get("aps");
            		return QueryEngine.combineIterator(new Query(query, "redirect-base", CMaps.map(), MidataId.from(targetAPSId), new AccountAccessContext(query.getCache(), query.getContext())).setFromRecord(query.getFromRecord()), "redirect", requery.toMap(), next);            				    			
    			}
            	return ProcessingTools.empty();
            }
			return null;
		}

		@Override
		public String toString() {			
			return (!redirect ? "noredirect(" : "redirect(")+"["+passed+"] "+current.toString()+")";
		}
		
		
		
	}

	/*
	private List<DBRecord> query(Query q, BasicBSONObject query, List<DBRecord> results) throws AppException {
		Map<String, Object> combined = combineQuery(q.getProperties(), query);
		if (combined == null) {
			AccessLog.log("combine empty:");			
			return results;
		}
		Object targetAPSId = query.get("aps");
		AccessLog.logBegin("begin redirect to Query:");
		List<DBRecord> result = next.query(new Query(combined, q.getFields(), q.getCache(), new MidataId(targetAPSId.toString()), new AccountAccessContext(q.getCache(), q.getContext())));			
		
		results = QueryEngine.combine(results, result);
		
		AccessLog.logEnd("end redirect");
		return results;
	}*/
	/*
	private List<DBRecord> memoryQuery(Query q, BasicBSONObject query, List<DBRecord> results) throws AppException {
		Map<String, Object> combined = combineQuery(q.getProperties(), query);
		if (combined == null) {
			AccessLog.log("combine empty:");			
			return Collections.emptyList();
		}
		
		List<DBRecord> result = QueryEngine.listFromMemory(q.getCache(), combined, results); 
									
		
		
		return result;
	}*/
	
	public static Map<String, Object> combineQuery(Map<String,Object> properties, Map<String,Object> query, AccessContext context) throws AppException {
		//AccessLog.log("COMBINE WITH:"+properties.toString());
		Map<String, Object> combined = new HashMap<String,Object>();
		combined.putAll(properties);
		Query.resolveConstants(combined, context);
		for (String key : query.keySet()) {
			if (key.equals("$or")) continue;
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

	/**
	 * create a unique key(string) for an access filter
	 * @param in
	 * @param ignore
	 * @return
	 */
	private static String createKey(Map<String, Object> in, Set<String> ignore) {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, Object> entry : in.entrySet()) {
			if (ignore.contains(entry.getKey())) continue;
			result.append(entry.getKey());
			if (entry.getValue() == null) {
				result.append(":null");
			} else if (entry.getValue() instanceof Set) {
				result.append("::");
				result.append(entry.getValue().toString());
			} else {
				result.append("::");
				result.append(entry.getValue().toString());
			}
		}
		return result.toString();
	}
	
	/**
	 * or-combine two values of an access filter
	 * @param in1
	 * @param in2
	 * @return
	 */
	private static Collection combineParam(Object in1, Object in2) {
		if (in1==null || in2==null) return null;
		Set result = new HashSet();
		if (in1 instanceof Collection) result.addAll((Collection) in1);
		else result.add(in1);
		if (in2 instanceof Collection) result.addAll((Collection) in2);
		else result.add(in2);
		return result;
	}
	
	/**
	 * set field of access filter or remove it if value is null
	 * @param target
	 * @param name
	 * @param value
	 */
	private static void setOrRemove(Map<String, Object> target, String name, Object value) {		
		if (value == null) target.remove(name);
		else target.put(name, value);
	}
	
	/**
	 * simplify access filters with $or by combining all parts that only differ in content and code into one part.
	 * @param in
	 * @return
	 */
    public static Map<String, Object> simplifyAccessFilter(MidataId pluginId, Map<String, Object> in) throws AppException {
    	//AccessLog.log("simplifyAccessFilter");
    	if (in == null) throw new PluginException(pluginId, "error.plugin", "No access filter defined for plugin.");
    	if (!in.containsKey("$or")) return in;
    	Map<String, Object> out = new HashMap<String, Object>();
    	Set<String> ignore = Sets.create("code","content");
    	out.putAll(in);
    	List<Map<String, Object>> newOr = new ArrayList<Map<String,Object>>();
    	Map<String, Map<String, Object>> summarize = new HashMap<String, Map<String, Object>>();
    	
    	Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) in.get("$or");
		for (Map<String, Object> part : parts) {
			String key = createKey(part, ignore);
			if (summarize.containsKey(key)) {				
				Map<String, Object> existing = summarize.get(key);
				setOrRemove(existing, "code", combineParam(existing.get("code"), part.get("code")));
				setOrRemove(existing, "content", combineParam(existing.get("content"), part.get("content")));				
			} else {				
				Map<String, Object> copy = new HashMap<String,Object>(part); 
				summarize.put(key, copy);
				newOr.add(copy);
			}						
		}
		out.put("$or", newOr);
		//AccessLog.log("out="+out.toString());
		return out;
    }
	

}
