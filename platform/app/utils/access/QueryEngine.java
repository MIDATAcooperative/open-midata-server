package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import models.MidataId;
import models.Record;
import models.RecordGroup;
import models.RecordsInfo;
import models.enums.AggregationType;
import utils.AccessLog;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * query engine for records. Is called by RecordManager.
 *
 */
class QueryEngine {

	public static List<Record> list(APSCache cache, MidataId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return RecordConversion.instance.currentVersionFromDB(fullQuery(properties, fields, aps, cache));
	}
	
	public static List<DBRecord> listInternal(APSCache cache, MidataId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return fullQuery(properties, fields, aps, cache);
	}
	
	public static Collection<RecordsInfo> info(APSCache cache, MidataId aps, Map<String, Object> properties, AggregationType aggrType) throws AppException {
		return infoQuery(new Query(properties, Sets.create("created", "group", "content", "format", "owner"), cache, aps), aps, false, aggrType, null);
	}
	
	public static List<DBRecord> isContainedInAps(APSCache cache, MidataId aps, List<DBRecord> candidates) throws AppException {
		
		if (!cache.getAPS(aps).isAccessible()) return new ArrayList<DBRecord>();

		if (AccessLog.detailedLog) AccessLog.logBegin("Begin check contained in aps #recs="+candidates.size());
		List<DBRecord> result = Feature_Prefetch.lookup(new Query(CMaps.map(RecordManager.FULLAPS_WITHSTREAMS).map("strict", true), Sets.create("_id"), cache, aps), candidates, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_AccountQuery(new Feature_Documents(new Feature_Streams())))));
		if (AccessLog.detailedLog) AccessLog.logEnd("End check contained in aps #recs="+result.size());
		
		return result;						
	}
		
	public static boolean isInQuery(APSCache cache, Map<String, Object> properties, DBRecord record) throws AppException {
		List<DBRecord> results = new ArrayList<DBRecord>(1);
		results.add(record);
		return listFromMemory(cache, properties, results).size() > 0;		
	}
	
	public static List<DBRecord> listFromMemory(APSCache cache, Map<String, Object> properties, List<DBRecord> records) throws AppException {
		if (AccessLog.detailedLog) AccessLog.logBegin("Begin list from memory #recs="+records.size());
		APS inMemory = new Feature_InMemoryQuery(records);
		cache.addAPS(inMemory);
		Feature qm = new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_ContentFilter(inMemory)));
		Query query = new Query(properties, Sets.create("_id"), cache, inMemory.getId());
		List<DBRecord> recs = qm.query(query);
		AccessLog.log("list from memory pre postprocess size = "+recs.size());
		List<DBRecord> result = postProcessRecords(qm, query.getProperties(), recs);		
		if (AccessLog.detailedLog) AccessLog.logEnd("End list from memory #recs="+result.size());
		return result;
	}
	
	private static String getInfoKey(AggregationType aggrType, String group, String content, String format, MidataId owner) {
		switch (aggrType) {
		case ALL: return "";
		case GROUP: return group;
		case CONTENT: return content;
		case CONTENT_PER_OWNER : return content+"/"+(owner != null ? owner.toString() : "?");
		case FORMAT: return format;
		default: return content+"/"+format+"/"+(owner != null ? owner.toString() : "?");
		}
	}
	
	public static Collection<RecordsInfo> infoQuery(Query q, MidataId aps, boolean cached, AggregationType aggrType, MidataId owner) throws AppException {
		AccessLog.logBegin("begin infoQuery aps="+aps+" cached="+cached);
		Map<String, RecordsInfo> result = new HashMap<String, RecordsInfo>();
		
		APS myaps = q.getCache().getAPS(aps);
		boolean doNotCacheInStreams = myaps.getMeta("_exclude") != null;
		boolean doNotQueryPerStream = myaps.getMeta("_exclude") != null;
		
		if (!doNotCacheInStreams) {
		  BasicBSONObject query = myaps.getMeta(APS.QUERY);
		  if (query != null) {
			  Map<String, Object> queryMap = query.toMap();
			  if (queryMap.containsKey("app")) {
				  doNotCacheInStreams = true;
				  doNotQueryPerStream = true;
			  }
		  }
		}
		
		if (doNotQueryPerStream) {			
			q.getProperties().remove("streams");
			q.getProperties().remove("flat");
		}
		
		if (cached) {
			String groupSystem = q.getStringRestriction("group-system");			
			BasicBSONObject obj = myaps.getMeta("_info");
			if (obj != null) { 
				
				RecordsInfo inf = new RecordsInfo();
				inf.count = obj.getInt("count");				
				inf.newest = obj.getDate("newest");
				inf.oldest = obj.getDate("oldest");
				inf.newestRecord = new MidataId(obj.getString("newestRecord"));								
				inf.formats.add(obj.getString("formats"));
				inf.contents.add(obj.getString("contents"));
				for (String content : inf.contents) inf.groups.add(RecordGroup.getGroupForSystemAndContent(groupSystem, content));
				if (owner != null) inf.owners.add(owner.toString());
				inf.calculated = obj.getDate("calculated");
				String k = getInfoKey(aggrType, obj.getString("groups"), obj.getString("contents"), obj.getString("formats"), owner);
				
				result.put(k, inf);
				Date from = inf.calculated != null ? new Date(inf.calculated.getTime() - 1000) : new Date(inf.newest.getTime() + 1);
				q = new Query(q, CMaps.map("created-after", from));
			
				long diff = myaps.getLastChanged() - from.getTime();
				AccessLog.log("DIFF:"+diff);
				
				if (diff < 1200) {
					AccessLog.logEnd("end infoQuery from cache");
					return result.values();
				}
			}
		}
		
		
		Feature qm = new Feature_Prefetch(new Feature_BlackList(myaps, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Streams())))))));
				
		List<DBRecord> recs = qm.query(q);
		recs = postProcessRecords(qm, q.getProperties(), recs);
		
		for (DBRecord record : recs) {
			if (record.isStream) {				
				q.getCache().getAPS(record._id, record.key, record.owner); // Called to make sure stream is accessible
				
				Collection<RecordsInfo> streaminfo = infoQuery(new Query(q, CMaps.map("stream", record._id)), record._id, !doNotCacheInStreams, aggrType, record.owner);
				
				for (RecordsInfo inf : streaminfo) {
					if (record.owner != null) inf.owners.add(record.owner.toString());
					String k = getInfoKey(aggrType, inf.groups.iterator().next(), inf.contents.iterator().next(), inf.formats.iterator().next(), record.owner);
					RecordsInfo here = result.get(k);					
					if (here == null) {
						result.put(k, inf);
					} else {
						here.merge(inf);						
					}
				}
			} else {			
				String k = getInfoKey(aggrType, record.group, (String) record.meta.get("content"), (String) record.meta.get("format"), record.owner);
				RecordsInfo existing = result.get(k);
				RecordsInfo newentry = new RecordsInfo(record);					
				if (existing == null) {
					result.put(k, newentry);
				} else {
					existing.merge(newentry);
				} 				
			}
						
		}
		
		AccessLog.logEnd("end infoQuery result: cached="+cached+" records="+recs.size()+" result="+result.size());
		if (cached && recs.size()>0 && result.size() == 1) {
			RecordsInfo inf = result.values().iterator().next();
			BasicBSONObject r = new BasicBSONObject();			
			r.put("formats", inf.formats.iterator().next());
			r.put("contents", inf.contents.iterator().next());			
			r.put("count", inf.count);
			r.put("newest", inf.newest);
			r.put("oldest", inf.oldest);
			r.put("newestRecord", inf.newestRecord.toString());
			r.put("calculated", new Date());
			q.getCache().getAPS(aps).setMeta("_info", r);
		}
		return result.values();
	}
	
    public static List<DBRecord> fullQuery(Map<String, Object> properties, Set<String> fields, MidataId apsId, APSCache cache) throws AppException {
    	AccessLog.logBegin("begin full query");
    	    	
    	APS target = cache.getAPS(apsId);
    	Feature qm = new Feature_BlackList(target, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_Prefetch(new Feature_Indexes(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Consents(new Feature_Documents(new Feature_Streams()))))))))));
    	
    	List<DBRecord> result = query(properties, fields, apsId, cache, qm);
    	
		if (result == null) {
			AccessLog.log("NULL result");
		}
					
		result = postProcessRecords(qm, properties, result);
		AccessLog.logEnd("end full query");
		
		return result;
	}
             
    protected static List<DBRecord> query(Map<String, Object> properties, Set<String> fields, MidataId apsId, APSCache cache, Feature qm) throws AppException {
      if (properties.containsKey("$or")) {
    	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) properties.get("$or");
    	  List<DBRecord> result = new ArrayList<DBRecord>();
    	  for (Map<String, Object> prop : col) {
    		  result.addAll(qm.query(new Query(prop, fields, cache, apsId)));
    	  }
    	  return result;
      } else {
    	  return qm.query(new Query(properties, fields, cache, apsId));
      }
    }
    
    protected static List<DBRecord> combine(Query query, Map<String, Object> properties, Feature qm) throws AppException {
    	if (properties.containsKey("$or")) {
      	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) properties.get("$or");
      	  List<DBRecord> result = new ArrayList<DBRecord>();
      	  for (Map<String, Object> prop : col) {
      		  Map<String, Object> comb = Feature_QueryRedirect.combineQuery(prop, query.getProperties());
      		  if (comb != null) {
      		    result.addAll(qm.query(new Query(comb, query.getFields(), query.getCache(), query.getApsId())));
      		  }
      	  }
      	  return result;
        } else {
          Map<String, Object> comb = Feature_QueryRedirect.combineQuery(properties, query.getProperties());
    	  if (comb != null) {
    		  return qm.query(new Query(comb, query.getFields(), query.getCache(), query.getApsId()));
    	  } else {
    		  AccessLog.log("empty combine");
    	  }
      	  return new ArrayList<DBRecord>();
        }
    }
    
    protected static void addFullIdField(Query q, APS source, List<DBRecord> result) {
    	if (q.returns("id")) {
			for (DBRecord record : result) record.id = record._id.toString()+"."+source.getId().toString();
		}
    }
    
    protected static List<DBRecord> scanForRecordsInMultipleAPS(Query q, Set<APS> apses, List<DBRecord> result) throws AppException {
    	for (APS aps : apses) {
    		result.addAll(aps.query(q));
    	}
    	return result;
    }       
    
    protected static void fetchFromDB(Query q, DBRecord record) throws InternalServerException {
    	if (record.encrypted == null) {
			DBRecord r2 = DBRecord.getById(record._id, q.getFieldsFromDB());
			if (r2 == null) throw new InternalServerException("error.internal", "Record with id "+record._id.toString()+" not found in database. Account needs repair?");			
			record.encrypted = r2.encrypted;
			record.encryptedData = r2.encryptedData;	
			record.encWatches = r2.encWatches;
		}
    }
    
    private static final Set<String> DATA_ONLY = Sets.create("_id", "encryptedData");
    protected static DBRecord loadData(DBRecord input) throws AppException {
    	if (input.data == null && input.encryptedData == null) {
    	   DBRecord r2 = DBRecord.getById(input._id, DATA_ONLY);					
		   input.encryptedData = r2.encryptedData;
    	}
		RecordEncryption.decryptRecord(input);
		return input;
    }
    
    protected static List<DBRecord> duplicateElimination(List<DBRecord> input) {
    	Set<MidataId> used = new HashSet<MidataId>(input.size());
    	List<DBRecord> filteredresult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord r : input) {
    		if (!used.contains(r._id)) {
    			used.add(r._id);
    			filteredresult.add(r);
    		}
    	}
    	return filteredresult;
    }
    
    protected static List<DBRecord> onlyWithKey(List<DBRecord> input) {    	
    	List<DBRecord> filteredresult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord r : input) {
    		if (r.security != null) filteredresult.add(r);
    	}
    	return filteredresult;
    }
    
    protected static List<DBRecord> postProcessRecords(Feature qm, Map<String, Object> properties, List<DBRecord> result) throws AppException {
    	if (result.size() > 0) {
    	   result = duplicateElimination(result); 
	       Collections.sort(result);	    
	       result = limitResultSize(properties, result);	    
    	}
	    
	    AccessLog.log("END Full Query, result size="+result.size());
	    
		return result;
    }
    
    protected static List<DBRecord> postProcessRecordsFilter(Query q, List<DBRecord> result) throws AppException {
    	if (result.size() > 0) {
    	AccessLog.logBegin("begin process filters size="+result.size());    	
			    	
    	int minTime = q.getMinTime();
    	int compress = 0;    	    	
    	if (q.getFetchFromDB()) {				
			for (DBRecord record : result) {
				fetchFromDB(q, record);
				if (minTime == 0 || record.time ==0 || record.time >= minTime) {
				  RecordEncryption.decryptRecord(record);
				  if (!record.meta.containsField("creator")) record.meta.put("creator", record.owner);
				} else {compress++;record.meta=null;}				
				//if (!q.getGiveKey()) record.clearSecrets();
			}
    	} else {
    		Set<String> check = q.mayNeedFromDB(); 
    		if (!check.isEmpty()) {
    			for (DBRecord record : result) {
    				boolean fetch = false;
    				for (String k : check) if (!record.meta.containsField(k)) {
    					AccessLog.log("need: "+k);
    					fetch = true; 
    				}
    				if (fetch) {
    					fetchFromDB(q, record);
    					if (minTime == 0 || record.time ==0 || record.time >= minTime) {
    					  RecordEncryption.decryptRecord(record);
    					  if (!record.meta.containsField("creator")) record.meta.put("creator", record.owner);
    					} else {compress++;record.meta=null;}
    				}
    			}
    	    }
    					   
		}
    	
    	if (compress > 0) {
    		List<DBRecord> result_new = new ArrayList<DBRecord>(result.size() - compress);
    		for (DBRecord r : result) {
    			if (r.meta != null) result_new.add(r);
    		}
    		result = result_new;    		
    	}
    	
		//if (qm!=null) result = qm.postProcess(result, q);     	
								
		// 8 Post filter records if necessary		
						
		if (q.restrictedBy("creator")) result = filterByMetaSet(result, "creator", q.getIdRestrictionDB("creator"));
		if (q.restrictedBy("app")) result = filterByMetaSet(result, "app", q.getIdRestrictionDB("app"));
		if (q.restrictedBy("name")) result = filterByMetaSet(result, "name", q.getRestriction("name"));
		if (q.restrictedBy("code")) result = filterByMetaSet(result, "code", q.getRestriction("code"));
		
		if (q.restrictedBy("index") && !q.getApsId().equals(q.getCache().getOwner())) {
			AccessLog.log("Manually applying index query aps="+q.getApsId().toString());
			result = QueryEngine.filterByDataQuery(result, q.getProperties().get("index"));
		}
		
		if (q.restrictedBy("data"))	result = filterByDataQuery(result, q.getProperties().get("data"));
		
		result = filterByDateRange(result, "created", q.getMinDateCreated(), q.getMaxDateCreated());			
		result = filterByDateRange(result, "lastUpdated", q.getMinDateUpdated(), q.getMaxDateUpdated());
		AccessLog.logEnd("end process filters size="+result.size());
	    
    	}
	    	    	    
		return result;
    }
    
    
    /*
    protected static List<DBRecord> filterByWCFormat(List<DBRecord> input, String name, Set<String> contentsWC) {
    	if (contentsWC == null) return input;    	
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {    	    		
    		if (!contentsWC.contains(ContentInfo.getWildcardName((String) record.meta.get(name)))) continue;    		
    		filteredResult.add(record);
    	}
    	
    	return filteredResult;
    }*/
    
    protected static List<DBRecord> filterByMetaSet(List<DBRecord> input, String property, Set values) {
    	AccessLog.log("filter by meta-set: "+property);
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {
    		if (!values.contains(record.meta.get(property))) continue;    		    		
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
    
    protected static List<DBRecord> filterSetByMetaSet(List<DBRecord> input, String property, Set values) {
    	AccessLog.log("filter set by meta-set: "+property);
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {
    		Object v = record.meta.get(property);
    		if (v == null) continue;
    		if (v instanceof String) {
    		   if (!values.contains((String) v)) continue;	
    		} else {
    		  boolean any = false;
    		  for (String vi : (Collection<String>) v) {
    			  if (values.contains(vi)) any = true;
    		  }
    		  if (!any) continue;
    		}
    		    		    		
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
    
    protected static List<DBRecord> filterByDataQuery(List<DBRecord> input, Object query) throws InternalServerException {    	
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());    	
    	Condition condition = null;
    	if (query instanceof Map<?, ?>) condition = new AndCondition((Map<String, Object>) query).optimize();
    	else if (query instanceof Condition) condition = ((Condition) query).optimize();
    	else throw new InternalServerException("error.internal", "Query type not implemented");
    	
    	for (DBRecord record : input) {
            Object accessVal = record.data;                        
            if (condition.satisfiedBy(accessVal)) filteredResult.add(record);    		
    	}    	
    	return filteredResult;
    }
    
    protected static Object access(Object obj, String path) {
    	if (obj == null) return null;
    	if (obj instanceof BSONObject) {
    		return ((BSONObject) obj).get(path);
    	}
    	return null;
    }
            
    protected static List<DBRecord> filterByDateRange(List<DBRecord> input, String property, Date minDate, Date maxDate) {
    	if (minDate == null && maxDate == null) return input;
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {
    		Date cmp = (Date) record.meta.get(property);
    		if (cmp == null) cmp = (Date) record.meta.get("created"); //Fallback for lastUpdated
    		if (minDate != null && cmp.before(minDate)) continue;
			if (maxDate != null && cmp.after(maxDate)) continue;    		    		    	
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
    
    
    
    protected static List<DBRecord> limitResultSize(Map<String, Object> properties, List<DBRecord> result) {
    	if (properties.containsKey("limit")) {
	    	Object limitObj = properties.get("limit");
	    	int limit = Integer.parseInt(limitObj.toString());
	    	if (result.size() > limit) result = result.subList(0, limit);
	    }
    	return result;
    }
    
    protected static List<DBRecord> lookupRecordsById(Query q) throws AppException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("_id", q.getMidataIdRestriction("_id"));
			//q.addMongoTimeRestriction(query);			
			return new ArrayList<DBRecord>(DBRecord.getAll(query, queryFields));		
    }
    
    /*protected static List<DBRecord> lookupRecordsByDocument(Query q) throws InternalServerException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("document", q.getProperties().get("document") );
			//q.addMongoTimeRestriction(query);
			if (q.restrictedBy("part"))	query.put("part", q.getProperties().get("part"));		
			return new ArrayList<DBRecord>(DBRecord.getAll(query, queryFields));						
    }*/
   
 
}
