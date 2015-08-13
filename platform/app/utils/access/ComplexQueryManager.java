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
		
	public static boolean isInQuery(Map<String, Object> properties, Record record) throws ModelException {
		List<Record> results = new ArrayList<Record>(1);
		results.add(record);
		return listFromMemory(properties, results).size() > 0;		
	}
	
	public static List<Record> listFromMemory(Map<String, Object> properties, List<Record> records) throws ModelException {
		QueryManager qm = new FormatGroupHandling(new ContentFilterQM(new InMemoryQM(records)));
		Query query = new Query(properties, Sets.create(), null, null);
		return postProcessRecords(qm, query, qm.query(query));		
	}
	
    public static List<Record> fullQuery(Query q, ObjectId aps) throws ModelException {
    	List<Record> result;
    	
    	
    	QueryManager qm = new APSQSupportingQM(new AccountLevelQueryManager(new FormatGroupHandling(new StreamQueryManager())));
    									
		result = findRecordsDirectlyInDB(q);
    	
		if (result != null) {									
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
				
		return postProcessRecords(qm, q, result);
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
    
    protected static void fetchFromDB(Query q, Record record) throws ModelException {
    	if (record.encrypted == null) {
			Record r2 = Record.getById(record._id, q.getFieldsFromDB());			
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
    
    protected static List<Record> postProcessRecords(QueryManager qm, Query q, List<Record> result) throws ModelException {    	
    	result = duplicateElimination(result); 
			
    	boolean postFilter = q.getMinDate() != null || q.getMaxDate() != null || q.restrictedBy("creator") || (q.restrictedBy("format") && q.restrictedBy("document"));
    	if (q.getFetchFromDB()) {				
			for (Record record : result) {
				fetchFromDB(q, record);
				SingleAPSManager.decryptRecord(record);
				if (record.creator == null) record.creator = record.owner;
				if (!q.getGiveKey()) record.clearSecrets();
			}
		} else {
		   if (!q.getGiveKey()) for (Record record : result) record.clearSecrets();
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
    
    
    
    
    protected static List<Record> filterByFormat(List<Record> input, Set<String> formats, Set<String> contents) {
    	if (formats == null && contents == null) return input;
    	AccessLog.debug("filterByFormat:" + formats);
    	AccessLog.debug("filterByContents:" + contents);
    	List<Record> filteredResult = new ArrayList<Record>(input.size());
    	for (Record record : input) {
    		AccessLog.debug("test:"+record.content);
    		if (formats!= null && !formats.contains(record.format)) continue;
    		if (contents!= null && !contents.contains(record.content)) continue;
    		AccessLog.debug("add:"+record.content);
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
