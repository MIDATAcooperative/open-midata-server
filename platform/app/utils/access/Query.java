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

import models.ModelException;

public class Query {

	private Map<String, Object> properties;
	private Set<String> fields;		
	private Set<String> fieldsFromDB;
	private int minTime;
	private int maxTime;
	private Date minDate;
	private Date maxDate;
	private boolean fetchFromDB;
	private boolean restrictedOnTime;
	private APSCache cache;
	private ObjectId apsId;
	private boolean giveKey;	
	
	
	public final static String STREAM_TYPE = "Stream";
		
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
	
	public boolean getFetchFromDB() {
		return fetchFromDB;
	}
	
	public boolean getGiveKey() {
		return giveKey;
	}
	
	public Set<String> getRestriction(String name) throws ModelException {
		Object v = properties.get(name);
		if (v instanceof String) {
			return Collections.singleton((String) v);
		} else if (v instanceof Set) {
			return (Set) v;
		} else if (v instanceof Collection) {
			Set<String> results = new HashSet<String>();
			results.addAll((Collection<String>) v);
			return results;											
		} else throw new ModelException("Bad Restriction: "+name);
	}
	
	public Set<ObjectId> getObjectIdRestriction(String name) throws ModelException {
		Object v = properties.get(name);
		if (v instanceof ObjectId) {
			return Collections.singleton((ObjectId) v);
		/*} else if (v instanceof Set) {
			return (Set) v;*/
		} else if (v instanceof Collection) {
			Set<ObjectId> results = new HashSet<ObjectId>();
			for (Object obj : (Collection<?>) v) { results.add(new ObjectId(obj.toString())); }			
			return results;											
		} else throw new ModelException("Bad Restriction: "+name);
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
		return restrictedBy("format") && properties.get("format").equals(STREAM_TYPE);
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
	
	public Date getMinDate() {
		return minDate;
	}
	
	public Date getMaxDate() {
		return maxDate;
	}
	
	public void addMongoTimeRestriction(Map<String, Object> properties) {
		if (minTime != 0 || maxTime != 0) {
			if (minTime == maxTime) {
				properties.put("time", minTime);
			} else {
			    Map<String, Integer> restriction = new HashMap<String, Integer>();
			    if (minTime!=0) restriction.put("$gte", minTime);
			    if (maxTime!=0) restriction.put("$lte", maxTime);
			    properties.put("time", restriction);
			}
		}
	}
	
	private void process() {
		 fetchFromDB = fields.contains("data") ||
	              fields.contains("app") || 
	              fields.contains("creator") || 
	              fields.contains("created") || 
	              fields.contains("name") || 
	              fields.contains("description") || 
	              fields.contains("tags") ||
	              properties.containsKey("app") ||
	              properties.containsKey("creator") ||
	              properties.containsKey("created") ||
	              properties.containsKey("name");
		 
		 restrictedOnTime = properties.containsKey("created") || properties.containsKey("max-age");
		 
         fieldsFromDB = Sets.create("createdOld");
         if (restrictedOnTime) fieldsFromDB.add("time");
		 if (fetchFromDB) fieldsFromDB.add("encrypted");
		 if (fields.contains("data")) fieldsFromDB.add("encryptedData");
				
		 // TODO Remove later
		 if (fields.contains("data")) fieldsFromDB.add("data");
		 if (fields.contains("app")) fieldsFromDB.add("app");
		 if (fields.contains("format")) fieldsFromDB.add("format");
		 if (uses("creator")) fieldsFromDB.add("creator");
		 if (fields.contains("created") || restrictedOnTime) fieldsFromDB.add("created");
		 if (fields.contains("name")) fieldsFromDB.add("name");
		 if (fields.contains("description")) fieldsFromDB.add("description");
		 if (fields.contains("tags")) fieldsFromDB.add("tags");
		 
		 if (properties.containsKey("max-age")) {
			Number maxAge = Long.parseLong(properties.get("max-age").toString());
			minDate = new Date(System.currentTimeMillis() - 1000 * maxAge.longValue());
			minTime = getTimeFromDate(minDate);
		 }
	}
	
	public static int getTimeFromDate(Date dt) {
		return (int) (dt.getTime() / 1000 / 60 / 60 / 24 / 7);
	}
}
