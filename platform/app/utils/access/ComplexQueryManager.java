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

public class ComplexQueryManager {

	public static List<Record> list(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return fullQuery(new Query(properties, fields, cache, aps), aps);
	}
	
	public static List<Record> listInternal(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		return fullQuery(new Query(properties, fields, cache, aps, true), aps);
	}
	
	public static Collection<RecordsInfo> info(APSCache cache, ObjectId aps, Map<String, Object> properties) throws AppException {
		return infoQuery(new Query(properties, Sets.create("created", "group"), cache, aps), aps, false);
	}
	
	public static List<Record> isContainedInAps(APSCache cache, ObjectId aps, List<Record> candidates) throws AppException {
		return onlyWithKey((new APSQSupportingQM(cache.getAPS(aps)).lookup(candidates, new Query(CMaps.map(RecordSharing.FULLAPS_WITHSTREAMS).map("strict", true), Sets.create("_id"), cache, aps))));
	}
		
	public static boolean isInQuery(Map<String, Object> properties, Record record) throws AppException {
		List<Record> results = new ArrayList<Record>(1);
		results.add(record);
		return listFromMemory(properties, results).size() > 0;		
	}
	
	public static List<Record> listFromMemory(Map<String, Object> properties, List<Record> records) throws AppException {
		QueryManager qm = new FormatGroupHandling(new ContentFilterQM(new InMemoryQM(records)));
		Query query = new Query(properties, Sets.create(), null, null, true);
		return postProcessRecords(qm, query, qm.query(query));		
	}
	
	public static Collection<RecordsInfo> infoQuery(Query q, ObjectId aps, boolean cached) throws AppException {
		AccessLog.debug("infoQuery aps="+aps+" cached="+cached);
		Map<String, RecordsInfo> result = new HashMap<String, RecordsInfo>();
		
		if (cached) {
			BasicBSONObject obj = q.getCache().getAPS(aps).getMeta("_info");
			if (obj != null) {
				
				RecordsInfo inf = new RecordsInfo();
				inf.count = obj.getInt("count");
				inf.group = obj.getString("group");
				inf.newest = obj.getDate("newest");
				inf.oldest = obj.getDate("oldest");
				inf.newestRecord = obj.getObjectId("newestRecord");
				result.put(inf.group, inf);
				q = new Query(q, CMaps.map("created-after", new Date(inf.newest.getTime() + 1 /*+ 1000 * 60 * 60* 24*60 */)));
			
			}
		}
		
		
		QueryManager qm = new BlackListQM(q, new APSQSupportingQM(new AccountLevelQueryManager(new FormatGroupHandling(new StreamQueryManager()))));
		
		boolean checkDates = q.uses("created");
		List<Record> recs = qm.query(q);
		recs = postProcessRecords(qm, q, recs);
		
		for (Record record : recs) {
			if (record.isStream) {
				Collection<RecordsInfo> streaminfo = infoQuery(new Query(q, CMaps.map("stream", record._id)), record._id, true);
				
				for (RecordsInfo inf : streaminfo) {
					RecordsInfo here = result.get(inf.group);
					if (here == null) {
						result.put(inf.group, inf);
					} else {
						here.count += inf.count;
						if (inf.newest.after(here.newest)) {
							here.newest = inf.newest;
							here.newestRecord = inf.newestRecord;
						}
						if (inf.oldest.before(here.oldest)) {
							here.oldest = inf.oldest;
						}
					}
				}
			} else {
			
				RecordsInfo ri = result.get(record.group);
				if (ri == null) {
					ri = new RecordsInfo();
					ri.group = record.group;
					if (checkDates) {
					ri.newest = record.created;
					ri.oldest = record.created;
					ri.newestRecord = record._id;
					}
					result.put(record.group, ri);				
				}
				ri.count++;
				if (checkDates) {
					if (record.created.after(ri.newest)) {
						ri.newest = record.created;
						ri.newestRecord = record._id;
					}
					if (record.created.before(ri.oldest)) ri.oldest = record.created;
				}
			}
						
		}
		
		AccessLog.debug("infoQuery result: cached="+cached+" records="+recs.size()+" result="+result.size()+" checkDates="+checkDates);
		if (cached && recs.size()>0 && result.size() == 1 && checkDates) {
			RecordsInfo inf = result.get(recs.get(0).group);
			BasicBSONObject r = new BasicBSONObject();
			r.put("group", inf.group);
			r.put("count", inf.count);
			r.put("newest", inf.newest);
			r.put("oldest", inf.oldest);
			r.put("newestRecord", inf.newestRecord);
			q.getCache().getAPS(aps).setMeta("_info", r);
		}
		return result.values();
	}
	
    public static List<Record> fullQuery(Query q, ObjectId aps) throws AppException {
    	List<Record> result;
    	
    	
    	QueryManager qm = new BlackListQM(q, new APSQSupportingQM(new AccountLevelQueryManager(new FormatGroupHandling(new StreamQueryManager()))));
    									
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
    
    protected static void addFullIdField(Query q, SingleAPSManager source, List<Record> result) {
    	if (q.returns("id")) {
			for (Record record : result) record.id = record._id.toString()+"."+source.getId().toString();
		}
    }
    
    protected static List<Record> scanForRecordsInMultipleAPS(Query q, Set<SingleAPSManager> apses, List<Record> result) throws AppException {
    	for (SingleAPSManager aps : apses) {
    		result.addAll(aps.query(q));
    	}
    	return result;
    }
    
    protected static List<Record> findRecordsDirectlyInDB(Query q) throws InternalServerException {
    	List<Record> result = null;
    	
    	if (q.restrictedBy("_id")) result = lookupRecordsById(q);			
		else if (q.restrictedBy("document")) result = lookupRecordsByDocument(q);
	    
    	return result;
    }
    
    protected static void fetchFromDB(Query q, Record record) throws InternalServerException {
    	if (record.encrypted == null) {
			Record r2 = Record.getById(record._id, q.getFieldsFromDB());			
			record.encrypted = r2.encrypted;
			record.encryptedData = r2.encryptedData;	
			
			// may be removed if encryption is working
			/*
			record.app = r2.app;		
			record.creator = r2.creator;	
			record.created = r2.created;
			record.name = r2.name;						
			record.description = r2.description;
			record.data = r2.data;
			record.time = r2.time;
			
			record.createdOld = r2.createdOld;
			*/
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
    
    protected static List<Record> postProcessRecords(QueryManager qm, Query q, List<Record> result) throws AppException {    	
    	result = duplicateElimination(result); 
			
    	boolean postFilter = q.getMinDate() != null || q.getMaxDate() != null || q.restrictedBy("creator") || (q.restrictedBy("format") && q.restrictedBy("document"));
    	int minTime = q.getMinTime();
    	int compress = 0;
    	if (q.getFetchFromDB()) {				
			for (Record record : result) {
				fetchFromDB(q, record);
				if (minTime == 0 || record.time ==0 || record.time >= minTime) {
				  SingleAPSManager.decryptRecord(record);
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
		FormatMatching formats = null;
		if (q.restrictedBy("creator")) {
			creators = q.getObjectIdRestriction("creator");			
		}
		if (q.restrictedBy("document") && q.restrictedBy("format")) {
			formats = new FormatMatching(q.getRestriction("format"));				
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
				if (formats != null && !formats.matches(record.format)) continue;
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
    		//AccessLog.debug("test:"+record.content);
    		if (formats!= null && !formats.contains(record.format)) continue;
    		if (contents!= null && !contents.contains(record.content)) continue;
    		if (contentsWC!=null && !contentsWC.contains(ContentInfo.getWildcardName(record.content))) continue;
    		//AccessLog.debug("add:"+record.content);
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
			q.addMongoTimeRestriction(query);			
			return new ArrayList<Record>(Record.getAll(query, queryFields));		
    }
    
    protected static List<Record> lookupRecordsByDocument(Query q) throws InternalServerException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("document", /*new ObjectId("55686c8be4b08b543c12b847")*/ q.getProperties().get("document") );
			q.addMongoTimeRestriction(query);
			if (q.restrictedBy("part"))	query.put("part", q.getProperties().get("part"));		
			return new ArrayList<Record>(Record.getAll(query, queryFields));						
    }
   
 
}
