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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;


import scala.NotImplementedError;
import utils.DateTimeUtils;
import utils.access.op.Condition;
import utils.access.op.EqualsSingleValueCondition;
import utils.access.op.ObjectCondition;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import models.ContentInfo;
import models.Record;
import models.RecordsInfo;
import models.enums.APSSecurityLevel;
import models.enums.AggregationType;

/**
 * query engine for records. Is called by RecordManager.
 *
 */
class QueryEngine {

	public static List<Record> list(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return RecordConversion.instance.currentVersionFromDB(fullQuery(new Query(properties, fields, cache, aps), aps));
	}
	
	public static List<DBRecord> listInternal(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return fullQuery(new Query(properties, fields, cache, aps, true), aps);
	}
	
	public static Collection<RecordsInfo> info(APSCache cache, ObjectId aps, Map<String, Object> properties, AggregationType aggrType) throws AppException {
		return infoQuery(new Query(properties, Sets.create("created", "group", "content", "format", "owner"), cache, aps, true), aps, false, aggrType, null);
	}
	
	public static List<DBRecord> isContainedInAps(APSCache cache, ObjectId aps, List<DBRecord> candidates) throws AppException {
		return onlyWithKey((new Feature_QueryRedirect(cache.getAPS(aps)).lookup(candidates, new Query(CMaps.map(RecordManager.FULLAPS_WITHSTREAMS).map("strict", true), Sets.create("_id"), cache, aps))));
	}
		
	public static boolean isInQuery(Map<String, Object> properties, DBRecord record) throws AppException {
		List<DBRecord> results = new ArrayList<DBRecord>(1);
		results.add(record);
		return listFromMemory(properties, results).size() > 0;		
	}
	
	public static List<DBRecord> listFromMemory(Map<String, Object> properties, List<DBRecord> records) throws AppException {
		Feature qm = new Feature_FormatGroups(new Feature_ContentFilter(new Feature_InMemoryQuery(records)));
		Query query = new Query(properties, Sets.create(), null, null, true);
		return postProcessRecords(qm, query, qm.query(query));		
	}
	
	private static String getInfoKey(AggregationType aggrType, String group, String content, String format, ObjectId owner) {
		switch (aggrType) {
		case ALL: return "";
		case GROUP: return group;
		case CONTENT: return content;
		case CONTENT_PER_OWNER : return content+"/"+(owner != null ? owner.toString() : "?");
		case FORMAT: return format;
		default: return content+"/"+format+"/"+(owner != null ? owner.toString() : "?");
		}
	}
	
	public static Collection<RecordsInfo> infoQuery(Query q, ObjectId aps, boolean cached, AggregationType aggrType, ObjectId owner) throws AppException {
		AccessLog.debug("infoQuery aps="+aps+" cached="+cached);
		Map<String, RecordsInfo> result = new HashMap<String, RecordsInfo>();
		
		if (cached) {
			APS myaps = q.getCache().getAPS(aps);
			BasicBSONObject obj = myaps.getMeta("_info");
			if (obj != null && obj.containsField("formats")) { // check for formats to remove date from older software version
				
				RecordsInfo inf = new RecordsInfo();
				inf.count = obj.getInt("count");				
				inf.newest = obj.getDate("newest");
				inf.oldest = obj.getDate("oldest");
				inf.newestRecord = new ObjectId(obj.getString("newestRecord"));				
				inf.groups.add(obj.getString("groups"));
				inf.formats.add(obj.getString("formats"));
				inf.contents.add(obj.getString("contents"));
				if (owner != null) inf.owners.add(owner.toString());
				inf.calculated = obj.getDate("calculated");
				String k = getInfoKey(aggrType, obj.getString("groups"), obj.getString("contents"), obj.getString("formats"), owner);
				
				result.put(k, inf);
				Date from = inf.calculated != null ? new Date(inf.calculated.getTime() - 1000) : new Date(inf.newest.getTime() + 1);
				q = new Query(q, CMaps.map("created-after", from));
			
				long diff = myaps.getLastChanged() - from.getTime();
				AccessLog.debug("DIFF:"+diff);
				
				if (diff < 1200) return result.values();
			}
		}
		
		
		Feature qm = new Feature_BlackList(q, new Feature_QueryRedirect(new Feature_AccountQuery(new Feature_FormatGroups(new Feature_Streams()))));
				
		List<DBRecord> recs = qm.query(q);
		recs = postProcessRecords(qm, q, recs);
		
		for (DBRecord record : recs) {
			if (record.isStream) {
				q.getCache().getAPS(record._id, record.key, record.owner); // Called to make sure stream is accessible
				
				Collection<RecordsInfo> streaminfo = infoQuery(new Query(q, CMaps.map("stream", record._id)), record._id, true, aggrType, record.owner);
				
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
		
		AccessLog.debug("infoQuery result: cached="+cached+" records="+recs.size()+" result="+result.size());
		if (cached && recs.size()>0 && result.size() == 1) {
			RecordsInfo inf = result.values().iterator().next();
			BasicBSONObject r = new BasicBSONObject();
			r.put("groups", inf.groups.iterator().next());
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
	
    public static List<DBRecord> fullQuery(Query q, ObjectId aps) throws AppException {
    	List<DBRecord> result;
    	
    	
    	Feature qm = new Feature_BlackList(q, new Feature_QueryRedirect(new Feature_AccountQuery(new Feature_FormatGroups(new Feature_Documents(new Feature_Streams())))));
    									
		result = findRecordsDirectlyInDB(q);
    	
		if (result != null) {												
		   result = qm.lookup(result, q);															
		} else {												
		   result = qm.query(q);			
		}
		if (result == null) {
			AccessLog.debug("NULL result");
		}
		AccessLog.debug("Pre Postprocess result size:"+result.size());
				
		return postProcessRecords(qm, q, result);
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
    
    protected static List<DBRecord> findRecordsDirectlyInDB(Query q) throws InternalServerException {
    	List<DBRecord> result = null;
    	
    	if (q.restrictedBy("_id")) result = lookupRecordsById(q);			
		else if (q.restrictedBy("document")) result = lookupRecordsByDocument(q);
	    
    	if (result != null) AccessLog.debug("found directly :"+result.size());
    	return result;
    }
    
    protected static void fetchFromDB(Query q, DBRecord record) throws InternalServerException {
    	if (record.encrypted == null) {
			DBRecord r2 = DBRecord.getById(record._id, q.getFieldsFromDB());			
			record.encrypted = r2.encrypted;
			record.encryptedData = r2.encryptedData;							
		}
    }
    
    protected static List<DBRecord> duplicateElimination(List<DBRecord> input) {
    	Set<ObjectId> used = new HashSet<ObjectId>(input.size());
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
    		if (r.key != null) filteredresult.add(r);
    	}
    	return filteredresult;
    }
    
    protected static List<DBRecord> postProcessRecords(Feature qm, Query q, List<DBRecord> result) throws AppException {    	
    	result = duplicateElimination(result); 
			
    	boolean postFilter = q.getMinDate() != null || q.getMaxDate() != null;
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
		   if (!q.getGiveKey()) for (DBRecord record : result) record.clearSecrets();
		}
    	AccessLog.debug("compress: "+compress+ "minTime="+minTime);
    	if (compress > 0) {
    		List<DBRecord> result_new = new ArrayList<DBRecord>(result.size() - compress);
    		for (DBRecord r : result) {
    			if (r.meta != null) result_new.add(r);
    		}
    		result = result_new;    		
    	}
    	
		if (qm!=null) result = qm.postProcess(result, q);     	
								
		// 8 Post filter records if necessary		
						
		if (q.restrictedBy("created")) result = filterByMetaSet(result, "created", q.getObjectIdRestriction("created"));
		if (q.restrictedBy("app")) result = filterByMetaSet(result, "app", q.getObjectIdRestriction("app"));
		if (q.restrictedBy("name")) result = filterByMetaSet(result, "name", q.getRestriction("name"));
		if (q.restrictedBy("data"))	result = filterByDataQuery(result, (Map<String,Object>) q.getProperties().get("data"));
		
		if (postFilter) {
			Date minDate = q.getMinDate();
			Date maxDate = q.getMaxDate();
			result = filterByDateRange(result, "created", minDate, maxDate);			
		}
		// 9 Order records
	    Collections.sort(result);
	    
	    result = limitResultSize(q, result);
	    
	    AccessLog.debug("END Full Query, result size="+result.size());
	    
		return result;
    }
    
    
    
    
    protected static List<DBRecord> filterByWCFormat(List<DBRecord> input, Set<String> contentsWC) {
    	if (contentsWC == null) return input;    	
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {    	    		
    		if (!contentsWC.contains(ContentInfo.getWildcardName((String) record.meta.get("content")))) continue;    		
    		filteredResult.add(record);
    	}
    	
    	return filteredResult;
    }
    
    protected static List<DBRecord> filterByMetaSet(List<DBRecord> input, String property, Set values) {    	
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {
    		if (!values.contains(record.meta.get(property))) continue;    		    		
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
    
    protected static List<DBRecord> filterByDataQuery(List<DBRecord> input, Map<String, Object> query) {    	
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());    	
    	Condition condition = new ObjectCondition(query);
    	
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
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {
    		Date cmp = (Date) record.meta.get(property);
    		if (minDate != null && cmp.before(minDate)) continue;
			if (maxDate != null && cmp.after(maxDate)) continue;    		    		    	
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
    
    
    
    protected static List<DBRecord> limitResultSize(Query q, List<DBRecord> result) {
    	if (q.restrictedBy("limit")) {
	    	Object limitObj = q.getProperties().get("limit");
	    	int limit = Integer.parseInt(limitObj.toString());
	    	if (result.size() > limit) result = result.subList(0, limit);
	    }
    	return result;
    }
    
    protected static List<DBRecord> lookupRecordsById(Query q) throws InternalServerException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("_id", q.getProperties().get("_id"));
			//q.addMongoTimeRestriction(query);			
			return new ArrayList<DBRecord>(DBRecord.getAll(query, queryFields));		
    }
    
    protected static List<DBRecord> lookupRecordsByDocument(Query q) throws InternalServerException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("document", q.getProperties().get("document") );
			//q.addMongoTimeRestriction(query);
			if (q.restrictedBy("part"))	query.put("part", q.getProperties().get("part"));		
			return new ArrayList<DBRecord>(DBRecord.getAll(query, queryFields));						
    }
   
 
}