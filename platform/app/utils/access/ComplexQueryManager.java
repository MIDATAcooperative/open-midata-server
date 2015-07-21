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
import utils.collections.Sets;
import models.ModelException;
import models.Record;
import models.enums.APSSecurityLevel;

public class ComplexQueryManager {

	public static List<Record> list(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws ModelException {
		return fullQuery(new Query(properties, fields, cache, aps), aps);
	}
	
	public static List<Record> listInternal(APSCache cache, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws ModelException {
		return fullQuery(new Query(properties, fields, cache, aps, true), aps);
	}
		
	
	
    public static List<Record> fullQuery(Query q, ObjectId aps) throws ModelException {
    	List<Record> result;
    	
    	
    	QueryManager qm = new APSQSupportingQM(new AccountLevelQueryManager(new FormatGroupHandling(new StreamQueryManager())));
    									
		result = findRecordsDirectlyInDB(q);
    	
		if (result != null) {
						
			// try single lookup in given APS with (time, _id, format? (4,5,6) (use 3 only if fails) (Records)
			for (Record record : result) {
				qm.lookupSingle(record, q);
			}
														
		} else {												
			result = qm.query(q);			
		}
		if (result == null) {
			AccessLog.debug("NULL result");
		}
		AccessLog.debug("Pre Postprocess result size:"+result.size());

		// 7 Load Records from DB (in not all info present)
		/*
		if (q.returns("id")) {
			for (Record record : result) record.id = record._id.toString()+"."+eaps.getId().toString();
		}
		*/
		
		return postProcessRecords(q, result);
	}
    
    protected static void addFullIdField(Query q, SingleAPSManager source, List<Record> result) {
    	if (q.returns("id")) {
			for (Record record : result) record.id = record._id.toString()+"."+source.getId().toString();
		}
    }
    
    protected static List<Record> scanForRecordsInMultipleAPS(Query q, Set<SingleAPSManager> apses, List<Record> result) throws ModelException {
    	for (SingleAPSManager aps : apses) {
    		result.addAll(aps.query(q));
    	}
    	return result;
    }
    
    protected static List<Record> findRecordsDirectlyInDB(Query q) throws ModelException {
    	List<Record> result = null;
    	
    	if (q.restrictedBy("_id")) result = lookupRecordsById(q);			
		else if (q.restrictedBy("document")) result = lookupRecordsByDocument(q);
	    
    	return result;
    }
    
    protected static List<Record> postProcessRecords(Query q, List<Record> result) throws ModelException {
    	// Duplicate Elimination
    	Set<ObjectId> used = new HashSet<ObjectId>(result.size());
    	List<Record> filteredresult = new ArrayList<Record>(result.size());
    	for (Record r : result) {
    		if (!used.contains(r._id)) {
    			used.add(r._id);
    			filteredresult.add(r);
    		}
    	}
    	result = filteredresult; 
		
    	
    	boolean postFilter = q.getMinDate() != null || q.getMaxDate() != null || q.restrictedBy("creator") || (q.restrictedBy("format") && q.restrictedBy("document"));
    	if (q.getFetchFromDB()) {	
			int minTime = q.getMinTime();
			int maxTime = q.getMaxTime();
			for (Record record : result) {
				if (record.encrypted == null) {
					Record r2 = Record.getById(record._id, q.getFieldsFromDB());
					if (minTime != 0 && r2.time < minTime) continue;
					if (maxTime != 0 && r2.time > maxTime) continue;
					record.encrypted = r2.encrypted;
					record.encryptedData = r2.encryptedData;	
					
					// TODO may be removed if encryption is working
					record.app = r2.app;		
					record.creator = r2.creator;	
					record.created = r2.created;
					record.name = r2.name;						
					record.description = r2.description;
					record.data = r2.data;
					
					record.createdOld = r2.createdOld;
				}
				SingleAPSManager.decryptRecord(record);
				if (record.creator == null) record.creator = record.owner;
				if (!q.getGiveKey()) record.clearSecrets();
			}
		} else {
		   if (!q.getGiveKey()) for (Record record : result) record.clearSecrets();
		}
		
    	
								
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
    
    protected static List<Record> limitResultSize(Query q, List<Record> result) {
    	if (q.restrictedBy("limit")) {
	    	Object limitObj = q.getProperties().get("limit");
	    	int limit = Integer.parseInt(limitObj.toString());
	    	if (result.size() > limit) result = result.subList(0, limit);
	    }
    	return result;
    }
    
    protected static List<Record> lookupRecordsById(Query q) throws ModelException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("_id", q.getProperties().get("_id"));
			q.addMongoTimeRestriction(query);			
			return new ArrayList<Record>(Record.getAll(query, queryFields));		
    }
    
    protected static List<Record> lookupRecordsByDocument(Query q) throws ModelException {    	
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("document", /*new ObjectId("55686c8be4b08b543c12b847")*/ q.getProperties().get("document") );
			q.addMongoTimeRestriction(query);
			if (q.restrictedBy("part"))	query.put("part", q.getProperties().get("part"));		
			return new ArrayList<Record>(Record.getAll(query, queryFields));						
    }
   
 
}
