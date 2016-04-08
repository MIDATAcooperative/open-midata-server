package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.ContentCode;
import models.Plugin;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.format.ISODateTimeFormat;

import utils.AccessLog;
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
		
	public Query(Map<String, Object> properties, Set<String> fields, APSCache cache, ObjectId apsId) throws BadRequestException, InternalServerException {
		this.properties = properties;
		this.fields = fields;
		this.cache = cache;
		this.apsId = apsId;
		process();
		AccessLog.logQuery(properties, fields);
	}
	
	public Query(Query q, Map<String, Object> properties) throws BadRequestException, InternalServerException {
		this.properties = new HashMap<String, Object>(q.getProperties());
		this.properties.putAll(properties);
		this.fields = q.getFields();
		this.cache = q.getCache();
		this.apsId = q.getApsId();
		this.giveKey = q.giveKey;
		process();
		AccessLog.logQuery(properties, fields);
	}
	
	public Query(Query q, Map<String, Object> properties, ObjectId aps) throws BadRequestException, InternalServerException {
		this.properties = new HashMap<String, Object>(q.getProperties());
		this.properties.putAll(properties);
		this.fields = q.getFields();
		this.cache = q.getCache();
		this.apsId = aps;
		this.giveKey = q.giveKey;
		process();
		AccessLog.logQuery(properties, fields);
	}
	
	protected Query(Map<String, Object> properties, Set<String> fields, APSCache cache,  ObjectId apsId, boolean giveKey) throws BadRequestException, InternalServerException {
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
	
	public boolean isRestrictedToSelf() throws BadRequestException {
		if (!restrictedBy("owner")) return false;
		Set<String> owner = getRestriction("owner");
		if (owner.size() == 1 && (owner.contains("self") || owner.contains(cache.getOwner().toString()))) return true;
		return false;
	}
	
	public Set<String> getRestriction(String name) throws BadRequestException {
		Object v = properties.get(name);
		return getRestriction(v, name);		
	}
	
	public static Set<String> getRestriction(Object v, String name) throws BadRequestException {		
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
		return properties.get(field) != null;
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
	
	public Date getDateRestriction(String name) throws BadRequestException {
		Object restriction = properties.get(name);
		if (restriction == null) return null;
		if (restriction instanceof Date) return (Date) restriction;
		if (restriction instanceof Long || restriction instanceof Integer) {
			return new Date((long) restriction);
		}
		String resStr = restriction.toString();
		Date date;
		if (resStr.length() == 0) throw new BadRequestException("error.date", "Cannot restrict date field '"+name+"' to empty string.");
		if (StringUtils.isNumeric(resStr)) {
			date = new Date(Long.parseLong(resStr));
		} else {
			try {
		    date = ISODateTimeFormat.dateTimeParser().parseDateTime(restriction.toString()).toDate();
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("error.date", "Bad date restriction on field '"+name+"': "+e.getMessage());
			}
		}
		properties.put(name, date);
		return date;
	}
	
	public String getStringRestriction(String name) throws BadRequestException {
		Object restriction = properties.get(name);
		if (restriction == null) return null;
		if (restriction instanceof String) return (String) restriction;
		throw new BadRequestException("error.string", "Restriction on field '"+name+"' must be string.");		
	}
	
	private void process() throws BadRequestException, InternalServerException {
		 if (fields.contains("group")) fields.add("content");
		
		 fetchFromDB = fields.contains("data") ||
	              fields.contains("app") || 
	              fields.contains("creator") || 	             
	              fields.contains("name") ||
	              fields.contains("code") || 
	              fields.contains("description") || 
	              fields.contains("tags") ||	              
	              fields.contains("watches") ||
	              properties.containsKey("app") ||
	              properties.containsKey("creator") ||
	              properties.containsKey("code") ||
	              properties.containsKey("name");
		 
		 restrictedOnTime = properties.containsKey("created") || properties.containsKey("max-age") || properties.containsKey("created-after") || properties.containsKey("updated-after") || properties.containsKey("updated-before");
		 
         fieldsFromDB = Sets.create("createdOld", "encrypted");
         mayNeedFromDB = new HashSet<String>();
         if (fields.contains("format") || properties.containsKey("format") || properties.containsKey("group")) mayNeedFromDB.add("format");
         if (fields.contains("content") || properties.containsKey("content") || properties.containsKey("group")) mayNeedFromDB.add("content");
         if (fields.contains("subformat") || properties.containsKey("subformat") || properties.containsKey("group")) mayNeedFromDB.add("subformat");
         if (fields.contains("created")) mayNeedFromDB.add("created");
         
         if (restrictedOnTime) fieldsFromDB.add("time");
		 if (fetchFromDB) fieldsFromDB.add("encrypted");
		 if (fields.contains("data")) fieldsFromDB.add("encryptedData");
		 if (fields.contains("watches")) fieldsFromDB.add("encWatches");
				
		 // TODO Remove later
		 /*if (fields.contains("data")) fieldsFromDB.add("data");
		 if (fields.contains("app")) fieldsFromDB.add("app");
		 if (fields.contains("format")) fieldsFromDB.add("format");
		 if (fields.contains("content")) fieldsFromDB.add("content");
		 if (uses("creator")) fieldsFromDB.add("creator");	
		 if (fields.contains("name")) fieldsFromDB.add("name");
		 if (fields.contains("description")) fieldsFromDB.add("description");
		 if (fields.contains("tags")) fieldsFromDB.add("tags");
		 */
		 if (properties.containsKey("max-age")) {
			Number maxAge = Long.parseLong(properties.get("max-age").toString());
			minDateCreated = new Date(System.currentTimeMillis() - 1000 * maxAge.longValue());
			minTime = getTimeFromDate(minDateCreated);
		 }
		 
		 if (properties.containsKey("created-after")) {				
				minDateCreated = getDateRestriction("created-after");
				minTime = getTimeFromDate(minDateCreated);
		 }
		 
		 if (properties.containsKey("updated-after")) {				
				minDateUpdated = getDateRestriction("updated-after");
				minTime = getTimeFromDate(minDateUpdated); 
				fetchFromDB = true;
		 }
		 
		 if (properties.containsKey("created-before")) {				
				maxDateCreated = getDateRestriction("created-before");
				// maxTime = getTimeFromDate(maxDate); NO lastUpdated also changes maxTime entry
		 }
		 
		 if (properties.containsKey("updated-before")) {				
				maxDateUpdated = getDateRestriction("updated-before");
				maxTime = getTimeFromDate(maxDateUpdated);
				fetchFromDB = true;
		 }
		 
		 if (properties.containsKey("app")) {
			 Set<String> apps = getRestriction("app");
			 Set<ObjectId> resolved = new HashSet<ObjectId>();
			 for (Object app : apps) {
				 if (!ObjectId.isValid(app.toString())) {
					 Plugin p = Plugin.getByFilename(app.toString(), Sets.create("_id"));	
					 if (p!=null) resolved.add(p._id);
				 } else resolved.add(new ObjectId(app.toString()));
			 }
			 properties.put("app", resolved);
		 }
		 
		 if (properties.containsKey("code") && !properties.containsKey("content")) {
			 Set<String> codes = getRestriction("code");
			 Set<String> contents = new HashSet<String>();
			 for (String code : codes) {
				 String content = ContentCode.getContentForSystemCode(code);
				 if (content == null) throw new BadRequestException("error.code", "Unknown code '"+code+"' in restriction.");
				 contents.add(content);
			 }
			 properties.put("content", contents);
		 }
	}
	
	public static int getTimeFromDate(Date dt) {
		return (int) (dt.getTime() / 1000 / 60 / 60 / 24 / 7);
	}
}
