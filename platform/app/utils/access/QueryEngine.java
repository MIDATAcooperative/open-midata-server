package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import utils.RuntimeConstants;
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

	public final static Map<String, Object> NOTNULL = Collections.unmodifiableMap(Collections.singletonMap("$ne", null));

	
	public static List<Record> list(APSCache cache, MidataId aps, AccessContext context, Map<String, Object> properties, Set<String> fields) throws AppException {		
 		return RecordConversion.instance.currentVersionFromDB(ProcessingTools.collect(fullQuery(properties, fields, aps, context, cache)));		
	}
	
	public static DBIterator<Record> listIterator(APSCache cache, MidataId aps, AccessContext context, Map<String, Object> properties, Set<String> fields) throws AppException {		
 		return new ProcessingTools.ConvertIterator(fullQuery(properties, fields, aps, context, cache));		
	}
	
	public static List<DBRecord> listInternal(APSCache cache, MidataId aps, AccessContext context, Map<String, Object> properties, Set<String> fields) throws AppException {		
		return ProcessingTools.collect(fullQuery(properties, fields, aps, context, cache));		
	}
	
	public static DBIterator<DBRecord> listInternalIterator(APSCache cache, MidataId aps, AccessContext context, Map<String, Object> properties, Set<String> fields) throws AppException {
		return fullQuery(properties, fields, aps, context, cache);
	}
	
	public static Collection<RecordsInfo> info(APSCache cache, MidataId aps, AccessContext context, Map<String, Object> properties, AggregationType aggrType) throws AppException {		
		return infoQuery(new Query(properties, Sets.create("group", "content", "format", "owner", "app"), Feature_UserGroups.findApsCacheToUse(cache,aps), aps, context != null ? context : new DummyAccessContext(cache)), aps, false, aggrType, null);
	}
	
	public static List<DBRecord> isContainedInAps(APSCache cache, MidataId aps, List<DBRecord> candidates) throws AppException {
		
		if (!cache.getAPS(aps).isAccessible()) return new ArrayList<DBRecord>();

		if (AccessLog.detailedLog) AccessLog.logBegin("Begin check contained in aps #recs="+candidates.size());
		List<DBRecord> result = Feature_Prefetch.lookup(new Query(CMaps.map(RecordManager.FULLAPS_WITHSTREAMS).map("strict", true), Sets.create("_id"), cache, aps, null), candidates, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_AccountQuery(new Feature_Streams()))), false);
		if (AccessLog.detailedLog) AccessLog.logEnd("End check contained in aps #recs="+result.size());
		
		return result;						
	}
		
	public static boolean isInQuery(AccessContext context, Map<String, Object> properties, DBRecord record) throws AppException {
		List<DBRecord> results = new ArrayList<DBRecord>(1);
		results.add(record);
		return listFromMemory(context, properties, results).size() > 0;		
	}
	
	public static List<DBRecord> listFromMemory(AccessContext context, Map<String, Object> properties, List<DBRecord> records) throws AppException {
		if (AccessLog.detailedLog) AccessLog.logBegin("Begin list from memory #recs="+records.size());
		APS inMemory = new Feature_InMemoryQuery(records);
		context.getCache().addAPS(inMemory);
		Feature qm = new Feature_Or(new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_ContentFilter(inMemory))));		
		DBIterator<DBRecord> recs = qm.iterator(new Query(properties, Sets.create("_id"), context.getCache(), inMemory.getId(),context));
		//AccessLog.log("list from memory pre postprocess size = "+recs.size());
		List<DBRecord> result = ProcessingTools.collect(recs);		
		if (AccessLog.detailedLog) AccessLog.logEnd("End list from memory #recs="+result.size());
		return result;
	}
	
	private static String getInfoKey(AggregationType aggrType, String group, String content, String format, MidataId owner, String app) {
		switch (aggrType) {
		case ALL: return "";
		case GROUP: return group;
		case CONTENT: return content;
		case CONTENT_PER_OWNER : return content+"/"+(owner != null ? owner.toString() : "?");
		case FORMAT: return format;
		case CONTENT_PER_APP : return content+"/"+(app != null ? app.toString() : "?");
		default: return content+"/"+format+"/"+(owner != null ? owner.toString() : "?");
		}
	}
	
	public static Collection<RecordsInfo> infoQuery(Query q, MidataId aps, boolean cached, AggregationType aggrType, MidataId owner) throws AppException {
		long t = System.currentTimeMillis();
		AccessLog.logBegin("begin infoQuery aps="+aps+" cached="+cached);
		Map<String, RecordsInfo> result = new HashMap<String, RecordsInfo>();
		
		APS myaps = q.getCache().getAPS(aps);
		boolean doNotCacheInStreams = myaps.getMeta("_exclude") != null;
		boolean doNotQueryPerStream = myaps.getMeta("_exclude") != null;
		
		if (!doNotCacheInStreams) {
		  BasicBSONObject query = myaps.getMeta(APS.QUERY);
		  if (query != null) {
			  Map<String, Object> queryMap = query.toMap();
			  /*if (queryMap.containsKey("app")) {
				  doNotCacheInStreams = true;
				  doNotQueryPerStream = true;
			  }*/
		  }
		}
		
		if (doNotQueryPerStream) {			
			q.getProperties().remove("streams");
			q.getProperties().remove("flat");
		}
		
		if (cached) {
			String groupSystem = q.getStringRestriction("group-system");			
			BasicBSONObject obj = myaps.getMeta("_info");
			if (obj != null && obj.containsField("apps")) { // Check for apps for compatibility with old versions 
				
				RecordsInfo inf = new RecordsInfo();
				inf.count = obj.getInt("count");				
				inf.newest = obj.getDate("newest");
				inf.oldest = obj.getDate("oldest");
				inf.newestRecord = new MidataId(obj.getString("newestRecord"));								
				inf.formats.add(obj.getString("formats"));
				inf.contents.add(obj.getString("contents"));
				inf.apps.add(MidataId.from(obj.getString("apps")));
				for (String content : inf.contents) inf.groups.add(RecordGroup.getGroupForSystemAndContent(groupSystem, content));
				if (owner != null) inf.owners.add(owner.toString());
				inf.calculated = obj.getDate("calculated");
				String k = getInfoKey(aggrType, obj.getString("groups"), obj.getString("contents"), obj.getString("formats"), owner, obj.getString("apps"));
				
				result.put(k, inf);
				Date from = inf.calculated != null && (inf.calculated.getTime() - 1000 > inf.newest.getTime() + 1) ? new Date(inf.calculated.getTime() - 1000) : new Date(inf.newest.getTime() + 1);
				q = new Query(q, CMaps.map("created-after", from));
			
				long diff = myaps.getLastChanged() - from.getTime();
				AccessLog.log("DIFF:"+diff);
				
				if (diff < 1200) {
					AccessLog.logEnd("end infoQuery from cache");
					return result.values();
				}
			}
		}
		
		
		Feature qm = new Feature_Prefetch(false, new Feature_BlackList(myaps, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_Pseudonymization(new Feature_UserGroups(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Streams())))))))));
						 
		List<DBRecord> recs = ProcessingTools.collect(ProcessingTools.noDuplicates(qm.iterator(q)));
		
		if (!cached) q.getCache().prefetch(recs);
		
		
		for (DBRecord record : recs) {
			if (record.isStream) {				
				q.getCache().getAPS(record._id, record.key, record.owner); // Called to make sure stream is accessible
				
				Collection<RecordsInfo> streaminfo = infoQuery(new Query(q, CMaps.map("stream", record._id).map("owner", record.owner)), record._id, !doNotCacheInStreams, aggrType, record.owner);
				
				for (RecordsInfo inf : streaminfo) {
					if (record.owner != null) inf.owners.add(record.owner.toString());
					String k = getInfoKey(aggrType, inf.groups.iterator().next(), inf.contents.iterator().next(), inf.formats.iterator().next(), record.owner, inf.apps.isEmpty() ? "empty" : inf.apps.iterator().next().toString());
					RecordsInfo here = result.get(k);					
					if (here == null) {
						result.put(k, inf);
					} else {
						here.merge(inf);						
					}
				}
			} else {	
				Object app = record.meta.get("app");
				String appStr = app != null ? MidataId.from(app).toString() : RuntimeConstants.instance.portalPlugin.toString();
				String k = getInfoKey(aggrType, record.group, (String) record.meta.get("content"), (String) record.meta.get("format"), record.owner, appStr);
				RecordsInfo existing = result.get(k);
				RecordsInfo newentry = new RecordsInfo(record);					
				if (existing == null) {
					result.put(k, newentry);
				} else {
					existing.merge(newentry);
				} 				
			}
						
		}
		
		AccessLog.logEnd("end infoQuery result: cached="+cached+" records="+recs.size()+" result="+result.size()+" time="+(System.currentTimeMillis() - t));
		if (cached && recs.size()>0 && result.size() == 1) {
			RecordsInfo inf = result.values().iterator().next();
			if (inf.apps.size() == 1) {
				BasicBSONObject r = new BasicBSONObject();			
				r.put("formats", inf.formats.iterator().next());
				r.put("contents", inf.contents.iterator().next());
				r.put("apps", inf.apps.iterator().next().toString());
				r.put("count", inf.count);
				r.put("newest", inf.newest);
				r.put("oldest", inf.oldest);
				r.put("newestRecord", inf.newestRecord.toString());
				r.put("calculated", new Date());
				q.getCache().getAPS(aps).setMeta("_info", r);
			}
		}
		return result.values();
	}
	
    public static DBIterator<DBRecord> fullQuery(Map<String, Object> properties, Set<String> fields, MidataId aps, AccessContext context, APSCache cache) throws AppException {
    	AccessLog.logBegin("begin full query on aps="+aps.toString());
    	long queryStart = System.currentTimeMillis();
    	if (context == null) context = new DummyAccessContext(cache);
    	Feature qm = null;
    	MidataId userGroup = Feature_UserGroups.identifyUserGroup(cache, aps);
    	if (userGroup != null) {
    		AccessLog.log("with usergroup");
    		properties = new HashMap<String, Object>(properties);
    		properties.put("usergroup", userGroup);
    		qm = new Feature_Pagination(new Feature_Sort(new Feature_Or(new Feature_ProcessFilters(new Feature_Pseudonymization(new Feature_Versioning(new Feature_UserGroups(new Feature_Prefetch(false, new Feature_Indexes(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Consents(new Feature_Streams()))))))))))));
    	} else {    	
    	   APS target = cache.getAPS(aps);    	
    	   qm = new Feature_Pagination(new Feature_Sort(new Feature_Or(new Feature_BlackList(target, new Feature_QueryRedirect(new Feature_FormatGroups(new Feature_ProcessFilters(new Feature_Pseudonymization(new Feature_Versioning(new Feature_Prefetch(true, new Feature_UserGroups(new Feature_Indexes(new Feature_AccountQuery(new Feature_ConsentRestrictions(new Feature_Consents(new Feature_Streams())))))))))))))));
    	}
    	Query q = new Query(properties, fields, cache, aps, context);
    	AccessLog.logQuery(q.getApsId(), q.getProperties(), q.getFields());
    	DBIterator<DBRecord> result = qm.iterator(q);
    	    	    	
		if (result == null) {
			AccessLog.log("NULL result");
		}
		
		AccessLog.log("fullQuery="+result.toString());
							
		AccessLog.logEnd("end full query time= "+(System.currentTimeMillis() - queryStart)+" ms");
		
		return result;
	}
         
    /*
    protected static List<DBRecord> query(Map<String, Object> properties, Set<String> fields, MidataId apsId, AccessContext context, APSCache cache, Feature qm) throws AppException {
      if (properties.containsKey("$or")) {
    	  qm = new Feature_SortAndLimit(qm);
    	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) properties.get("$or");
    	  List<DBRecord> result = Collections.emptyList();
    	  for (Map<String, Object> prop : col) {
    		  prop = Feature_QueryRedirect.combineQuery(prop, properties);
    		  AccessLog.logQuery(apsId, prop, fields);
    		  result = QueryEngine.combine(result,  qm.query(new Query(prop, fields, cache, apsId, context)));
    	  }
    	  return result;
      } else {
    	  AccessLog.logQuery(apsId, properties, fields);
    	  return qm.query(new Query(properties, fields, cache, apsId, context));
      }
    }
    */
    /*
    protected static Iterator<DBRecord> queryIterator(Map<String, Object> properties, Set<String> fields, MidataId apsId, AccessContext context, APSCache cache, Feature qm) throws AppException {
        if (properties.containsKey("$or")) {
      	  qm = new Feature_SortAndLimit(qm);
      	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) properties.get("$or");      	  
      	  return ProcessingTools.multiQuery(qm, new Query(properties, fields, cache, apsId, context, true), col.iterator());      	  
        } else {
      	  AccessLog.logQuery(apsId, properties, fields);
      	  return qm.iterator(new Query(properties, fields, cache, apsId, context));
        }
      }
      */  
    
    protected static List<DBRecord> combine(Query query, Map<String, Object> properties, Feature qm) throws AppException {
    	if (properties.containsKey("$or")) {
      	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) properties.get("$or");
      	  List<DBRecord> result = Collections.emptyList();
      	  for (Map<String, Object> prop : col) {
      		  Map<String, Object> comb = Feature_QueryRedirect.combineQuery(prop, query.getProperties());
      		  if (comb != null) {
      			result = QueryEngine.combine(result, qm.query(new Query(comb, query.getFields(), query.getCache(), query.getApsId(), query.getContext())));
      		  }
      	  }
      	  return result;
        } else {
          Map<String, Object> comb = Feature_QueryRedirect.combineQuery(properties, query.getProperties());
    	  if (comb != null) {
    		  return qm.query(new Query(comb, query.getFields(), query.getCache(), query.getApsId(), query.getContext()));
    	  } else {
    		  AccessLog.log("empty combine");
    	  }
      	  return Collections.emptyList();
        }
    }
    
    protected static DBIterator<DBRecord> combineIterator(Query query, Map<String, Object> properties, Feature qm) throws AppException {
    	if (properties.containsKey("$or")) {
      	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) properties.get("$or");      	  
      	  return ProcessingTools.multiQuery(qm, query, ProcessingTools.dbiterator("", col.iterator()));
      	        	
        } else {
          Map<String, Object> comb = Feature_QueryRedirect.combineQuery(properties, query.getProperties());
    	  if (comb != null) {
    		  return qm.iterator(new Query(comb, query.getFields(), query.getCache(), query.getApsId(), query.getContext()).setFromRecord(query.getFromRecord()));
    	  } else {
    		  AccessLog.log("empty combine");
    	  }
      	  return ProcessingTools.empty();
        }
    }
    
    protected static void addFullIdField(Query q, APS source, List<DBRecord> result) {
    	if (q.returns("id")) {
			for (DBRecord record : result) record.id = record._id.toString()+"."+source.getId().toString();
		}
    }
    
    protected static List<DBRecord> scanForRecordsInMultipleAPS(Query q, Set<APS> apses, List<DBRecord> result) throws AppException {
    	for (APS aps : apses) {
    		result = QueryEngine.combine(result, aps.query(q));
    	}
    	return result;
    }       
    
    protected static void fetchFromDB(Query q, DBRecord record) throws InternalServerException {
    	if (record.encrypted == null) {
			DBRecord r2 = DBRecord.getById(record._id, q.getFieldsFromDB());
			if (r2 == null) throw new InternalServerException("error.internal", "Record with id "+record._id.toString()+" not found in database. Account or consent needs repair?");
			fetchFromDB(record, r2);			
		}
    }
    
    protected static void fetchFromDB(DBRecord record, DBRecord r2) throws InternalServerException {
    	if (record.encrypted == null) {			
			record.encrypted = r2.encrypted;
			record.encryptedData = r2.encryptedData;	
			record.encWatches = r2.encWatches;
			if (record.stream == null) record.stream = r2.stream;
			if (record.time == 0) record.time = r2.time;			
		}
    }
    
    private static final Set<String> DATA_ONLY = Sets.create("_id", "encryptedData");
    private static final Set<String> DATA_AND_WATCHES = Sets.create("_id", "encryptedData", "encWatches");
    protected static final Set<String> META_AND_DATA = Sets.create("_id", "encryptedData", "encrypted");
    
    protected static DBRecord loadData(DBRecord input) throws AppException {
    	if (input.data == null && input.encryptedData == null) {
    	   DBRecord r2 = DBRecord.getById(input._id, DATA_ONLY);
    	   if (r2 == null) throw new InternalServerException("error.internal", "Record with id "+input._id.toString()+" not found in database. Account or consent needs repair?");
		   // r2=null should not happen if (r2 != null) 
    	   input.encryptedData = r2.encryptedData;
    	}
		RecordEncryption.decryptRecord(input);
		return input;
    }
    
    protected static void loadData(Collection<DBRecord> records) throws AppException {
    	Map<MidataId, DBRecord> idMap = new HashMap<MidataId,DBRecord>(records.size());    	
    	for (DBRecord rec : records) {    		    	
	    	if (rec.data == null && rec.encryptedData == null) {
	    	   idMap.put(rec._id, rec);
	    	}
	    	
    	}
    	if (!idMap.isEmpty()) {
	    	Collection<DBRecord> fromDB = DBRecord.getAllList(CMaps.map("_id", idMap.keySet()).map("encryptedData", NOTNULL), META_AND_DATA);
	    	for (DBRecord r2 : fromDB) {
	    	   DBRecord r = idMap.get(r2._id);
	    	   r.encryptedData = r2.encryptedData;
	    	   r.encrypted = r2.encrypted;
	    	}
    	}
    	for (DBRecord rec : records) {    		
		   RecordEncryption.decryptRecord(rec);
    	}		
    }
    
    protected static void loadDataAndWatches(Collection<DBRecord> records) throws AppException {
    	Map<MidataId, DBRecord> idMap = new HashMap<MidataId,DBRecord>(records.size());
    	for (DBRecord rec : records) {    		    	
	    	if ((rec.data == null && rec.encryptedData == null) || (rec.watches == null && rec.encWatches == null)) {
	    	   idMap.put(rec._id, rec);
	    	}
	    	
    	}
    	if (!idMap.isEmpty()) {
	    	Collection<DBRecord> fromDB = DBRecord.getAllList(CMaps.map("_id", idMap.keySet()), DATA_AND_WATCHES);	    	
	    	for (DBRecord r2 : fromDB) {	    	
	    	   idMap.get(r2._id).encryptedData = r2.encryptedData;	    	   
	    	   idMap.get(r2._id).encWatches = r2.encWatches;
	    	}
    	}
    	for (DBRecord rec : records) {      		
		   RecordEncryption.decryptRecord(rec);		   
    	}		
    }
    
    /*
    protected static List<DBRecord> duplicateElimination(List<DBRecord> input) {
    	int size = input.size();
    	if (size<2) return input;
        int out = 0;
        {
            final Set<DBRecord> encountered = new HashSet<DBRecord>();
            input = QueryEngine.modifyable(input);
            for (int in = 0; in < size; in++) {
                final DBRecord t = input.get(in);
                final boolean first = encountered.add(t);
                if (first) {
                	input.set(out++, t);
                }
            }
        }
        while (out < size) {
        	input.remove(--size);
        }
    	return input;
    }
    */
    
    protected static List<DBRecord> onlyWithKey(List<DBRecord> input) {    	
    	List<DBRecord> filteredresult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord r : input) {
    		if (r.security != null) filteredresult.add(r);
    	}
    	return filteredresult;
    }
    
    protected static DBIterator<DBRecord> limitAndSortRecords(Map<String, Object> properties, DBIterator<DBRecord> input) throws AppException {
    	input = ProcessingTools.limit(properties, ProcessingTools.sort(properties, ProcessingTools.noDuplicates(input)));	    	    
		return input;
    }
    
    /*        
    protected static List<DBRecord> postProcessRecordsFilter(Query q, List<DBRecord> result) throws AppException {
    	if (result.size() > 0) {
    	AccessLog.logBegin("begin process filters size="+result.size());    	
			    	
    	int minTime = q.getMinTime();
    	int compress = 0;    
    	Map<MidataId, DBRecord> fetchIds = new HashMap<MidataId, DBRecord>();
    	
    	if (q.getFetchFromDB()) {	
    		//result = duplicateElimination(result);    		
			for (DBRecord record : result) {
				if (record.encrypted == null && record.data == null) {
					DBRecord old = fetchIds.put(record._id, record);
					if (old != null) old.meta = null;
				}
			}
    	} else {
    		Set<String> check = q.mayNeedFromDB();
    		
    		if (check.contains("created")) {
    			for (DBRecord record : result) {
    				record.meta.put("created", record._id.getCreationDate());
    			}
    		}
    		
    		if (!check.isEmpty()) {
    			for (DBRecord record : result) {
    				boolean fetch = false;
    				if (record.meta == null) continue;
    				for (String k : check) if (!record.meta.containsField(k)) {
    					AccessLog.log("need: "+k);
    					fetch = true; 
    				}
    				if (fetch) { 
    					DBRecord old = fetchIds.put(record._id, record);
    					if (old != null) old.meta = null;
    				}
    				
    			}
    		}
    	}
    	if (!fetchIds.isEmpty()) {	
    		
			List<DBRecord> read = lookupRecordsById(q, fetchIds.keySet(), q.restrictedBy("deleted"));			
			for (DBRecord record : read) {
				DBRecord old = fetchIds.get(record._id);
				fetchFromDB(old, record);
			}
			if (read.size() < fetchIds.size()) {
				for (DBRecord record : fetchIds.values()) {
					if (record.encrypted == null) record.meta = null;
				}
			}
    	}
    	
    	boolean checkDelete = !q.restrictedBy("deleted");
    	
		for (DBRecord record : result) {
			if (record.meta != null && (minTime == 0 || record.time ==0 || record.time >= minTime)) {
			  RecordEncryption.decryptRecord(record);
			  if (!record.meta.containsField("creator") && record.owner != null) record.meta.put("creator", record.owner.toDb());
			  if (checkDelete && record.meta.containsField("deleted")) { record.meta = null;compress++; }
			} else {compress++;record.meta=null;}			
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
    	if (q.restrictedBy("history-date")) result = Feature_Versioning.historyDate(q, result);
    	
		if (q.restrictedBy("creator")) result = filterByMetaSet(result, "creator", q.getIdRestrictionDB("creator"));
		if (q.restrictedBy("app")) result = filterByMetaSet(result, "app", q.getIdRestrictionDB("app"), q.restrictedBy("no-postfilter-streams"));
		if (q.restrictedBy("name")) result = filterByMetaSet(result, "name", q.getRestriction("name"));
		if (q.restrictedBy("code")) result = filterSetByMetaSet(result, "code", q.getRestriction("code"));
		
		if (q.restrictedBy("index") && !q.getApsId().equals(q.getCache().getAccountOwner())) {
			AccessLog.log("Manually applying index query aps="+q.getApsId().toString());
			result = QueryEngine.filterByDataQuery(result, q.getProperties().get("index"), null);
		}
		
		if (q.restrictedBy("data"))	result = filterByDataQuery(result, q.getProperties().get("data"), null);
		
		result = filterByDateRange(result, "created", q.getMinDateCreated(), q.getMaxDateCreated());			
		result = filterByDateRange(result, "lastUpdated", q.getMinDateUpdated(), q.getMaxDateUpdated());
		
		
		AccessLog.logEnd("end process filters size="+result.size());
	    
    	} 
	    	    	    
		return result;
    }
    */
    
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
    		if (!values.contains(record.meta.get(property))) {    			
    			continue;    		    		
    		}
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
    
    protected static List<DBRecord> filterByMetaSet(List<DBRecord> input, String property, Set values, boolean noPostfilterStreams) {
    	AccessLog.log("filter by meta-set: "+property);
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());
    	for (DBRecord record : input) {
    		if (!noPostfilterStreams || !record.isStream) {
	    		if (!values.contains(record.meta.get(property))) {    			
	    			continue;    		    		
	    		}
    		}
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
    
    protected static List<DBRecord> filterByDataQuery(List<DBRecord> input, Object query, List<DBRecord> nomatch) throws AppException {
    	if (input.size() == 0) return input;
    	loadData(input);
    	List<DBRecord> filteredResult = new ArrayList<DBRecord>(input.size());    	
    	Condition condition = null;
    	if (query instanceof Map<?, ?>) condition = new AndCondition((Map<String, Object>) query).optimize();
    	else if (query instanceof Condition) condition = ((Condition) query).optimize();
    	else throw new InternalServerException("error.internal", "Query type not implemented");
    	AccessLog.log("validate condition:"+condition.toString());
    	for (DBRecord record : input) {
            Object accessVal = record.data;                        
            if (condition.satisfiedBy(accessVal)) {
            	filteredResult.add(record);    		
            } else if (nomatch != null) nomatch.add(record);
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
    		if (cmp == null) cmp = record._id.getCreationDate(); //Fallback for lastUpdated
    		if (cmp == null) {
    			AccessLog.log("Record with _id "+record._id.toString()+" has not created date!");
    			continue;
    		}
    		if (minDate != null && cmp.before(minDate)) continue;
			if (maxDate != null && cmp.after(maxDate)) continue;    		    		    	
    		filteredResult.add(record);
    	}    	
    	return filteredResult;
    }
               
    
    protected static List<DBRecord> lookupRecordsById(Query q) throws AppException {
    	return lookupRecordsById(q, q.getMidataIdRestriction("_id"), true);
    }
    	
    protected static List<DBRecord> lookupRecordsById(Query q, Set<MidataId> ids, boolean alsoDeleted) throws AppException {  
			Map<String, Object> query = new HashMap<String, Object>();
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
			queryFields.addAll(q.getFieldsFromDB());
			query.put("_id", ids);
			q.addMongoTimeRestriction(query, true);
			if (!alsoDeleted) {
				query.put("encryptedData", NOTNULL);
			}
			return DBRecord.getAllList(query, queryFields);		
    }
        
    public final static List<DBRecord> combine(List<DBRecord> list1, List<DBRecord> list2) {
    	if (list1 == null || list1.isEmpty()) return list2;
    	if (list2 == null || list2.isEmpty()) return list1;
    	if (list1 instanceof ArrayList) {
    		list1.addAll(list2);
    		return list1;
    	}
    	ArrayList<DBRecord> result = new ArrayList<DBRecord>();
    	result.addAll(list1);
    	result.addAll(list2);
    	return result;
    }
    
    public final static List<DBRecord> modifyable(List<DBRecord> list) {
    	if (list == null || list.isEmpty()) return new ArrayList<DBRecord>();
    	if (list instanceof ArrayList) return list;
    	return new ArrayList<DBRecord>(list);
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
