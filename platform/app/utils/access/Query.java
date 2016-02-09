package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * parameters for a single query for records
 *
 */
public class Query {

	private Map<String, Object> properties;
	private Set<String> fields;		
	private Set<String> fieldsFromDB;
	private Set<String> mayNeedFromDB;
	private int minTime;
	private int maxTime;
	private Date minDateCreated;
	private Date maxDateCreated;
	private Date minDateUpdated;
	private Date maxDateUpdated;
	private boolean fetchFromDB;
	private boolean restrictedOnTime;
	private APSCache cache;
	private ObjectId apsId;
	private boolean giveKey;	
		
	public Query(Map<String, Object> properties, Set<String> fields, APSCache cache, ObjectId apsId) {
		this.properties = properties;
		this.fields = fields;
		this.cache = cache;
		this.apsId = apsId;
		process();
		AccessLog.logQuery(properties, fields);
	}
	
	public Query(Query q, Map<String, Object> properties) {
		this.properties = new HashMap<String, Object>(q.getProperties());
		this.properties.putAll(properties);
		this.fields = q.getFields();
		this.cache = q.getCache();
		this.apsId = q.getApsId();
		this.giveKey = q.giveKey;
		process();
		AccessLog.logQuery(properties, fields);
	}
	
	protected Query(Map<String, Object> properties, Set<String> fields, APSCache cache,  ObjectId apsId, boolean giveKey) {
		this.properties = properties;
		this.fields = fields;
		this.cache = cache;
		this.apsId = apsId;
		this.giveKey = giveKey;
		process();
		AccessLog.logQuery(properties, fields);
	}
	
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	public Set<String> getFields() {
		return fields;
	}
	
	public APSCache getCache() {
		return cache;
	}
	
	public ObjectId getApsId() {
		return apsId;
	}
	
	public Set<String> getFieldsFromDB() {
		return fieldsFromDB;
	}
	
	public Set<String> mayNeedFromDB() {
		return mayNeedFromDB;
	}
	
	public boolean getFetchFromDB() {
		return fetchFromDB;
	}
	
	public boolean getGiveKey() {
		return giveKey;
	}
	
	public boolean isRestrictedOnTime() {
		return restrictedOnTime;
	}
	
	public Set<String> getRestriction(String name) throws BadRequestException {
		Object v = properties.get(name);
		if (v instanceof String) {
			return Collections.singleton((String) v);
		} else if (v instanceof ObjectId) {
			return Collections.singleton(v.toString());
		} else if (v instanceof Set) {
			return (Set) v;
		} else if (v instanceof Collection) {
			Set<String> results = new HashSet<String>();
			results.addAll((Collection<String>) v);
			return results;											
		} else throw new BadRequestException("error.badquery","Bad Restriction 1: "+name);
	}
	
	public Set<ObjectId> getObjectIdRestriction(String name) throws BadRequestException {
		Object v = properties.get(name);
		if (v instanceof ObjectId) {
			return Collections.singleton((ObjectId) v);
		/*} else if (v instanceof Set) {
			return (Set) v;*/
		} else if (v instanceof Collection) {
			Set<ObjectId> results = new HashSet<ObjectId>();
			for (Object obj : (Collection<?>) v) { results.add(new ObjectId(obj.toString())); }			
			return results;		
		} else if (v instanceof String && ObjectId.isValid((String) v)) {
			return Collections.singleton( new ObjectId((String) v));
		} else throw new BadRequestException("error.badquery", "Bad Restriction 2: "+name);
	}
	
	public boolean restrictedBy(String field) {
		return properties.containsKey(field);
	}
	
	public boolean returns(String field) {
		return fields.contains(field);
	}
	
	public boolean uses(String field) {
		return restrictedBy(field) || returns(field);
	}
	
	public boolean isStreamOnlyQuery() {
		return (restrictedBy("streams") && properties.get("streams").equals("only"));
	}
	
	public boolean includeStreams() {
		return isStreamOnlyQuery() || (restrictedBy("streams") && properties.get("streams").equals("true"));
	}
	
	public boolean deepQuery() {
		return !restrictedBy("flat");
	}
	
	public int getMinTime() {
		return minTime;
	}
	
	public int getMaxTime() {
		return maxTime;
	}
	
	public Date getMinDateCreated() {
		return minDateCreated;
	}
	
	public Date getMinDateUpdated() {
		return minDateUpdated;
	}
	
	public long getMinCreatedTimestamp() {
		if (minDateCreated != null) return minDateCreated.getTime();
		return 0;
	}
	
	public long getMinUpdatedTimestamp() {
		if (minDateUpdated != null) return minDateUpdated.getTime();
		return 0;
	}
	
	public Date getMaxDateCreated() {
		return maxDateCreated;
	}
	
	public Date getMaxDateUpdated() {
		return maxDateUpdated;
	}
	
	public void addMongoTimeRestriction(Map<String, Object> properties) {
		if (minTime != 0 || maxTime != 0) {
			if (minTime == maxTime) {
				properties.put("time", minTime);
			} else {
			    Map<String, Integer> restriction = new HashMap<String, Integer>();
			    if (minTime!=0) {
			    	restriction.put("$gte", minTime);
			    	//AccessLog.debug("$gte:"+minTime);
			    }
			    if (maxTime!=0) restriction.put("$lte", maxTime);
			    properties.put("time", restriction);
			}
		}
	}
	
	private void process() {
		 if (fields.contains("group")) fields.add("content");
		
		 fetchFromDB = fields.contains("data") ||
	              fields.contains("app") || 
	              fields.contains("creator") || 	             
	              fields.contains("name") || 
	              fields.contains("description") || 
	              fields.contains("tags") ||	              
	              fields.contains("watches") ||
	              properties.containsKey("app") ||
	              properties.containsKey("creator") ||
	              
	              properties.containsKey("name");
		 
		 restrictedOnTime = properties.containsKey("created") || properties.containsKey("max-age") || properties.containsKey("created-after");
		 
         fieldsFromDB = Sets.create("createdOld");
         mayNeedFromDB = new HashSet<String>();
         if (fields.contains("format")) mayNeedFromDB.add("format");
         if (fields.contains("content")) mayNeedFromDB.add("content");
         if (fields.contains("created")) mayNeedFromDB.add("created");
         
         if (restrictedOnTime) fieldsFromDB.add("time");
		 if (fetchFromDB) fieldsFromDB.add("encrypted");
		 if (fields.contains("data")) fieldsFromDB.add("encryptedData");
		 if (fields.contains("watches")) fieldsFromDB.add("encWatches");
				
		 // TODO Remove later
		 if (fields.contains("data")) fieldsFromDB.add("data");
		 if (fields.contains("app")) fieldsFromDB.add("app");
		 if (fields.contains("format")) fieldsFromDB.add("format");
		 if (fields.contains("content")) fieldsFromDB.add("content");
		 if (uses("creator")) fieldsFromDB.add("creator");
		 //if (fields.contains("created") || restrictedOnTime) fieldsFromDB.add("created");
		 if (fields.contains("name")) fieldsFromDB.add("name");
		 if (fields.contains("description")) fieldsFromDB.add("description");
		 if (fields.contains("tags")) fieldsFromDB.add("tags");
		 
		 if (properties.containsKey("max-age")) {
			Number maxAge = Long.parseLong(properties.get("max-age").toString());
			minDateCreated = new Date(System.currentTimeMillis() - 1000 * maxAge.longValue());
			minTime = getTimeFromDate(minDateCreated);
		 }
		 
		 if (properties.containsKey("created-after")) {				
				minDateCreated = (Date) properties.get("created-after");
				minTime = getTimeFromDate(minDateCreated);
		 }
		 
		 if (properties.containsKey("updated-after")) {				
				minDateUpdated = (Date) properties.get("updated-after");
				minTime = getTimeFromDate(minDateUpdated); 
		 }
		 
		 if (properties.containsKey("created-before")) {				
				maxDateCreated = (Date) properties.get("created-before");
				// maxTime = getTimeFromDate(maxDate); NO lastUpdated also changes maxTime entry
		 }
		 
		 if (properties.containsKey("updated-before")) {				
				maxDateUpdated = (Date) properties.get("updated-before");
				maxTime = getTimeFromDate(maxDateUpdated);
		 }
	}
	
	public static int getTimeFromDate(Date dt) {
		return (int) (dt.getTime() / 1000 / 60 / 60 / 24 / 7);
	}
}
