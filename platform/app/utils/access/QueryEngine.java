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


import utils.DateTimeUtils;
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
		return fullQuery(new Query(properties, fields, cache, aps), aps);
	}
	
	public static List<Record> listInternal(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return fullQuery(new Query(properties, fields, cache, aps, true), aps);
	}
	
	public static Collection<RecordsInfo> info(APSCache cache, ObjectId aps, Map<String, Object> properties, AggregationType aggrType) throws AppException {
		return infoQuery(new Query(properties, Sets.create("created", "group", "content", "format", "owner"), cache, aps, true), aps, false, aggrType);
	}
	
	public static List<Record> isContainedInAps(APSCache cache, ObjectId aps, List<Record> candidates) throws AppException {
		return onlyWithKey((new Feature_QueryRedirect(cache.getAPS(aps)).lookup(candidates, new Query(CMaps.map(RecordManager.FULLAPS_WITHSTREAMS).map("strict", true), Sets.create("_id"), cache, aps))));
	}
		
	public static boolean isInQuery(Map<String, Object> properties, Record record) throws AppException {
		List<Record> results = new ArrayList<Record>(1);
		results.add(record);
		return listFromMemory(properties, results).size() > 0;		
	}
	
	public static List<Record> listFromMemory(Map<String, Object> properties, List<Record> records) throws AppException {
		Feature qm = new Feature_FormatGroups(new Feature_ContentFilter(new Feature_InMemoryQuery(records)));
		Query query = new Query(properties, Sets.create(), null, null, true);
		return postProcessRecords(qm, query, qm.query(query));		
	}
	
	private static String getInfoKey(AggregationType aggrType, String group, String content, String format) {
		switch (aggrType) {
		case ALL: return "";
		case GROUP: return group;
		case CONTENT: return content;
		case FORMAT: return format;
		default: return content+"/"+format;
		}
	}
	
	public static Collection<RecordsInfo> infoQuery(Query q, ObjectId aps, boolean cached, AggregationType aggrType) throws AppException {
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
				inf.calculated = obj.getDate("calculated");
				String k = getInfoKey(aggrType, obj.getString("groups"), obj.getString("contents"), obj.getString("formats"));
				
				result.put(k, inf);
				Date from = inf.calculated != null ? new Date(inf.calculated.getTime() - 1000) : new Date(inf.newest.getTime() + 1);
				q = new Query(q, CMaps.map("created-after", from));
			
				long diff = myaps.getLastChanged() - from.getTime();
				AccessLog.debug("DIFF:"+diff);
				
				if (diff < 1200) return result.values();
			}
		}
		
		
		Feature qm = new Feature_BlackList(q, new Feature_QueryRedirect(new Feature_AccountQuery(new Feature_FormatGroups(new Feature_Streams()))));
				
		List<Record> recs = qm.query(q);
		recs = postProcessRecords(qm, q, recs);
		
		for (Record record : recs) {
			if (record.isStream) {
				q.getCache().getAPS(record._id, record.key, record.owner); // Called to make sure stream is accessible
				
				Collection<RecordsInfo> streaminfo = infoQuery(new Query(q, CMaps.map("stream", record._id)), record._id, true, aggrType);
				
				for (RecordsInfo inf : streaminfo) {
					String k = getInfoKey(aggrType, inf.groups.iterator().next(), inf.contents.iterator().next(), inf.formats.iterator().next());
					RecordsInfo here = result.get(k);					
					if (here == null) {
						result.put(k, inf);
					} else {
						here.merge(inf);						
					}
				}
			} else {			
				String k = getInfoKey(aggrType, record.group, record.content, record.format);
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
			r.put("calculated", inf.calculated);
			q.getCache().getAPS(aps).setMeta("_info", r);
		}
		return result.values();
	}
	
    public static List<Record> fullQuery(Query q, ObjectId aps) throws AppException {
    	List<Record> result;
    	
    	
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
    
    protected static void addFullIdField(Query q, APS source, List<Record> result) {
    	if (q.returns("id")) {
			for (Record record : result) record.id = record._id.toString()+"."+source.getId().toString();
		}
    }
    
    protected static List<Record> scanForRecordsInMultipleAPS(Query q, Set<APS> apses, List<Record> result) throws AppException {
    	for (APS aps : apses) {
    		result.addAll(aps.query(q));
    	}
    	return result;
    }
    
    protected static List<Record> findRecordsDirectlyInDB(Query q) throws InternalServerException {
    	List<Record> result = null;
    	
    	if (q.restrictedBy("_id")) result = lookupRecordsById(q);			
		else if (q.restrictedBy("document")) result = lookupRecordsByDocument(q);
	    
    	if (result != null) AccessLog.debug("found directly :"+result.size());
    	return result;
    }
    
    protected static void fetchFromDB(Query q, Record record) throws InternalServerException {
    	if (record.encrypted == null) {
			Record r2 = Record.getById(record._id, q.getFieldsFromDB());			
			record.encrypted = r2.encrypted;
			record.encryptedData = r2.encryptedData;	
						
		}
    }
    
    protected static List<Record> duplicateElimination(List<Record> input) {
    	Set<ObjectId> used = new HashSet<ObjectId>(input.size());
    	List<Record> filteredresult = new ArrayList<Record>(input.size());
    	for (Record r : input) {
    		if (!used.contains(r._id)) {
    			used.add(r._id);
    			filteredresult.add(r);
    		}
    	}
    	return filteredresult;
    }
    
    protected static List<Record> onlyWithKey(List<Record> input) {    	
    	List<Record> filteredresult = new ArrayList<Record>(input.size());
    	for (Record r : input) {
    		if (r.key != null) filteredresult.add(r);
    	}
    	return filteredresult;
    }
    
    protected static List<Record> postProcessRecords(Feature qm, Query q, List<Record> result) throws AppException {    	
    	result = duplicateElimination(result); 
			
    	boolean postFilter = q.getMinDate() != null || q.getMaxDate() != null || q.restrictedBy("creator") || (q.restrictedBy("format") && q.restrictedBy("document"));
    	int minTime = q.getMinTime();
    	int compress = 0;
    	if (q.getFetchFromDB()) {				
			for (Record record : result) {
				fetchFromDB(q, record);
				if (minTime == 0 || record.time ==0 || record.time >= minTime) {
				  RecordEncryption.decryptRecord(record);
				  if (record.creator == null) record.creator = record.owner;
				} else compress++;				
				if (!q.getGiveKey()) record.clearSecrets();
			}
		} else {
		   if (!q.getGiveKey()) for (Record record : result) record.clearSecrets();
		}
    	AccessLog.debug("compress: "+compress+ "minTime="+minTime);
    	if (compress > 0) {
    		List<Record> result_new = new ArrayList<Record>(result.size() - compress);
    		for (Record r : result) {
    			if (r.created != null) result_new.add(r);
    		}
    		result = result_new;    		
    	}
    	
		if (qm!=null) result = qm.postProcess(result, q);     	
								
		// 8 Post filter records if necessary		
		Set<ObjectId> creators = null;		
		if (q.restrictedBy("creator")) {
			creators = q.getObjectIdRestriction("creator");			
		}
		
					
		if (postFilter) {
			Date minDate = q.getMinDate();
			Date maxDate = q.getMaxDate();
			List<Record> filteredResult = new ArrayList<Record>(result.size());
			for (Record record : result) {
				if (record.name == null) continue;
				//if (minDate != null) AccessLog.debug("minDate="+minDate.toString()+" vs "+record.createdOld.toString()+" skip="+record.created)
				if (minDate != null && record.created.before(minDate)) continue;
				if (maxDate != null && record.created.after(maxDate)) continue;
				if (creators != null && !creators.contains(record.creator)) continue;	
				
				filteredResult.add(record);
			}
			result = filteredResult;
		}
		// 9 Order records
	    Collections.sort(result);
	    
	    result = limitResultSize(q, result);
	    
	    AccessLog.debug("END Full Query, result size="+result.size());
	    
		return result;
    }
    
    
    
    
    protected static List<Record> filterByFormat(List<Record> input, Set<String> formats, Set<String> contents, Set<String> contentsWC) {
    	if (formats == null && contents == null && contentsWC == null) return input;
    	/*AccessLog.debug("filterByFormat:" + formats);
    	AccessLog.debug("filterByContents:" + contents);
    	AccessLog.debug("filterByContentsWC:" + contentsWC);*/
    	List<Record> filteredResult = new ArrayList<Record>(input.size());
    	for (Record record : input) {    	
    		if (formats!= null && !formats.contains(record.format)) continue;
    		if (contents!= null && !contents.contains(record.content)) continue;
    		if (contentsWC!=null && !contentsWC.contains(ContentInfo.getWildcardName(record.content))) continue;    		
    		filteredResult.add(record);
    	}
    	
    	return filteredResult;
    }
    
    protected static List<Record> limitResultSize(Query q, List<Record> result) {
    	if (q.restrictedBy("limit")) {
	    	Object limitObj = q.getProperties().get("limit");
	    	int limit = Integer.parseInt(limitObj.toString());
	    	if (result.size() > limit) result = result.subList(0, limit);
	    }
    	return result;
    }
    
    protected static List<Record> lookupRecordsById(Query q) throws InternalServerException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("_id", q.getProperties().get("_id"));
			//q.addMongoTimeRestriction(query);			
			return new ArrayList<Record>(Record.getAll(query, queryFields));		
    }
    
    protected static List<Record> lookupRecordsByDocument(Query q) throws InternalServerException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("document", q.getProperties().get("document") );
			//q.addMongoTimeRestriction(query);
			if (q.restrictedBy("part"))	query.put("part", q.getProperties().get("part"));		
			return new ArrayList<Record>(Record.getAll(query, queryFields));						
    }
   
 
}
