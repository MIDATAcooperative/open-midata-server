/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.format.ISODateTimeFormat;

import models.ContentCode;
import models.ContentInfo;
import models.FormatInfo;
import models.MidataId;
import models.Plugin;
import models.RecordGroup;
import models.Study;
import utils.AccessLog;
import utils.PluginLoginCache;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;

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
	private Date minDateShared;
	//private Date maxDateShared;
	private boolean fetchFromDB;
	private boolean restrictedOnTime;
	private APSCache cache;
	private MidataId apsId;		
	private AccessContext context;
	private String path;
	private Query prev;

	public Query(String path, Map<String, Object> properties, Set<String> fields, APSCache cache, MidataId apsId, AccessContext context, Query prev) throws AppException {
		this(path, null, properties,fields,cache,apsId,context,prev);
	}
	public Query(String path, String extra, Map<String, Object> properties, Set<String> fields, APSCache cache, MidataId apsId, AccessContext context, Query prev) throws AppException {
		this.path = path;
		this.prev = prev;
		this.properties = new HashMap<String, Object>(properties);
		this.fields = fields;
		this.cache = cache;
		this.apsId = apsId;
		this.context = context;
		if (extra != null) {
		  AccessLog.log(path," : ",extra);
		} else {
		  AccessLog.log(path);
		}
		//AccessLog.logQuery(apsId, properties, fields);
		process();
		
	}
	
	// Internal use withoutTime
	public Query(Map<String, Object> properties, Set<String> fields, APSCache cache, MidataId apsId, AccessContext context, boolean noinit)  {
		this.properties = new HashMap<String, Object>(properties);
		this.fields = fields;
		this.cache = cache;
		this.apsId = apsId;
		this.context = context;			
	}
	
	public Query(Query q, String path, Map<String, Object> properties) throws AppException {
		this(q, path, properties, q.getApsId(), q.getContext());
	}
	
	public Query(Query q, String path, Map<String, Object> properties, MidataId aps, AccessContext context) throws AppException {
		this.properties = new HashMap<String, Object>(q.getProperties());
		this.properties.putAll(properties);
		this.fields = q.getFields();
		this.cache = q.getCache();
		this.apsId = aps;			
		this.context = context;
		this.prev = q;
		this.path = path;
		process();
		StringBuilder sb = new StringBuilder();
		getPath(sb);
		sb.append(" : ");
		sb.append(properties.toString());
		AccessLog.log(sb.toString());
		//AccessLog.logQuery(apsId, properties, fields);
	}
	
	public Query(Query q, String path, Map<String, Object> properties, Set<String> fields) throws AppException {
		this.properties = new HashMap<String, Object>(q.getProperties());
		this.properties.putAll(properties);
		this.fields = fields;
		this.cache = q.getCache();
		this.apsId = q.getApsId();			
		this.context = q.getContext();
		this.prev = q;
		this.path = path;
		process();
		StringBuilder sb = new StringBuilder();
		getPath(sb);
		sb.append(" : ");
		sb.append(properties.toString());
		AccessLog.log(sb.toString());
		//AccessLog.logQuery(apsId, properties, fields);
	}	
	
	public Query() throws AppException {
		this.properties = new HashMap<String, Object>();		
		this.fields = Sets.create(APSEntry.groupingFields,"_id","created","owner");
		this.cache = null;
		this.apsId = null;			
		this.context = null;
		this.prev = null;
		this.path = null;
		process();		
	}
	
	public Query withoutTime() throws AppException {
		Query r = new Query(properties, fields, cache, apsId, context, true);
		r.path = this.path;
		r.properties.remove("shared-after");
		r.properties.remove("index-ts-provider");
		r.process();
		return r;
	}
	
	public void getPath(StringBuilder str) {
		if (prev != null) {
			prev.getPath(str);
			str.append('/');
		}
		str.append(path);	
		if (fromRecord!=null) str.append("*");
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
	
	public AccessContext getContext() {
		return context;
	}
	
	public MidataId getApsId() {
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
		
	
	public boolean isRestrictedOnTime() {
		return restrictedOnTime;
	}
	
	public boolean isRestrictedToSelf() throws AppException {
		if (!restrictedBy("owner")) return false;
		Set<String> owner = getRestrictionOrNull("owner");
		if (owner!=null && owner.size() == 1 && (owner.contains("self") || owner.contains(cache.getAccountOwner().toString()))) return true;
		return false;
	}
	
	public Set<String> getRestriction(String name) throws AppException {
		Object v = properties.get(name);
		return getRestriction(v, name);		
	}
	
	public Set<String> getRestrictionOrNull(String name) throws AppException {
		Object v = properties.get(name);
		if (v == null) return null;
		if (v instanceof Set) return (Set) v;
		Set<String> result = getRestriction(v, name);
		properties.put(name, result);
		return result;
	}
	
	public static Set<String> getRestriction(Object v, String name) throws AppException {		
		if (v instanceof String) {
			return Collections.singleton((String) v);
		} else if (v instanceof MidataId) {
			return Collections.singleton(v.toString());
		} else if (v instanceof ObjectId) {
			return Collections.singleton(v.toString());
		} else if (v instanceof Set) {
			return (Set) v;
		} else if (v instanceof Collection) {
			Set<String> results = new HashSet<String>();
			for (Object val : (Collection) v) { results.add(val.toString()); }			
			return results;											
		} else {
			AccessLog.log("Restriction on "+name+" : "+(v!=null ? (v.toString()+" of class "+v.getClass().getName()) : "null"));
			throw new InternalServerException("error.internal","Bad Restriction 1: "+name);
		}
	}
	
	public static Set<String> getAnyRestrictionFromQuery(Map<String, Object> properties, String name) throws AppException {
		if (properties.containsKey("$or")) {
			Set<String> result = new HashSet<String>();
			for (Map<String, Object> part : ((Collection<Map<String, Object>>) properties.get("$or"))){
				result.addAll(getAnyRestrictionFromQuery(part, name));
			}
			return result;
		}
		if (properties.containsKey(name)) {
			return getRestriction(properties.get(name), name);
		}
		return Collections.emptySet();		
	}
	
	public Set<MidataId> getMidataIdRestriction(String name) throws BadRequestException {
		Object v = properties.get(name);
		if (v instanceof MidataId) {
			return Collections.singleton((MidataId) v);
		/*} else if (v instanceof Set) {
			return (Set) v;*/
		} else if (v instanceof Collection) {
			Set<MidataId> results = new HashSet<MidataId>();
			for (Object obj : (Collection<?>) v) { results.add(new MidataId(obj.toString())); }			
			return results;		
		} else if (v instanceof String && MidataId.isValid((String) v)) {
			return Collections.singleton( new MidataId((String) v));
		} else throw new BadRequestException("error.internal", "Bad Restriction 2: "+name);
	}
	
	public Set<Object> getIdRestrictionDB(String name) throws BadRequestException {
		Object v = properties.get(name);
		if (v instanceof MidataId) {
			return Collections.singleton(((MidataId) v).toDb());
		/*} else if (v instanceof Set) {
			return (Set) v;*/
		} else if (v instanceof Collection) {
			Set<Object> results = new HashSet<Object>();
			for (Object obj : (Collection<?>) v) { results.add(new MidataId(obj.toString()).toDb()); }			
			return results;		
		} else if (v instanceof String && MidataId.isValid((String) v)) {
			return Collections.singleton( new MidataId((String) v).toDb());
		} else throw new BadRequestException("error.internal", "Bad Restriction 2: "+name);
	}
	
	public Set<String> getIdRestrictionAsString(String name) throws BadRequestException {
		Object v = properties.get(name);
		if (v == null) return null;
		if (v instanceof MidataId) {
			return Collections.singleton(((MidataId) v).toString());
		/*} else if (v instanceof Set) {
			return (Set) v;*/
		} else if (v instanceof Collection) {
			Set<String> results = new HashSet<String>();
			for (Object obj : (Collection<?>) v) { results.add(obj.toString()); }			
			return results;		
		} else if (v instanceof String && MidataId.isValid((String) v)) {
			return Collections.singleton((String) v);
		} else throw new BadRequestException("error.internal", "Bad Restriction 2: "+name);
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
	
	public Date getMinDateShared() {
		return minDateShared;
	}
	
	public long getMinCreatedTimestamp() {
		if (minDateCreated != null) return minDateCreated.getTime();
		return 0;
	}
	
	public long getMaxCreatedTimestamp() {
		if (maxDateCreated != null) return maxDateCreated.getTime();
		return 0;
	}
	
	public long getMinUpdatedTimestamp() {
		if (minDateUpdated != null) return minDateUpdated.getTime();
		return 0;
	}
	
	public long getMinSharedTimestamp() throws BadRequestException {
		if (minDateShared != null) return minDateShared.getTime();
		return 0;
	}
	
	public Date getMaxDateCreated() {
		return maxDateCreated;
	}
	
	public Date getMaxDateUpdated() {
		return maxDateUpdated;
	}
	
	/*public Date getMaxDateShared() {
		return maxDateShared;
	}*/
	
	public MidataId getFrom() {
		String fr = properties.get("from").toString();
		int p = fr.indexOf('.');
		if (p <=0) p = fr.length(); 
		return MidataId.from(fr.substring(0, p));		
	}
	
	public MidataId getFromOwner() {
		String fr = properties.get("from").toString();
		int p = fr.indexOf('.');
		if (p <=0) return null; 
		return MidataId.from(fr.substring(p+1));			
	}
	
	private DBRecord fromRecord;
	public DBRecord getFromRecord() {
		return fromRecord;		
	}
	public Query setFromRecord(DBRecord record) {
		this.fromRecord = record;
		return this;
	}
	
	public boolean addMongoTimeRestriction(Map<String, Object> properties1, boolean allowZero) {
		Map<String, Object> properties = properties1;		
		if (minTime != 0 || maxTime != 0) {
			if (allowZero) {
				Map<String, Object> prop2 = CMaps.map();
				properties.putAll(CMaps.or(CMaps.map("time", 0), prop2) );
				properties = prop2;
			}
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
			return true;
		}
		return false;
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
		if (resStr.length() == 0) throw new BadRequestException("error.invalid.date", "Cannot restrict date field '"+name+"' to empty string.");
		if (StringUtils.isNumeric(resStr)) {
			date = new Date(Long.parseLong(resStr));
		} else {
			try {
		    date = ISODateTimeFormat.dateTimeParser().parseDateTime(restriction.toString()).toDate();
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("error.invalid.date", "Bad date restriction on field '"+name+"': "+e.getMessage());
			}
		}
		properties.put(name, date);
		return date;
	}
	
	public String getStringRestriction(String name) throws BadRequestException {
		Object restriction = properties.get(name);
		if (restriction == null) return null;
		if (restriction instanceof String) return (String) restriction;
		throw new BadRequestException("error.invalid.string", "Restriction on field '"+name+"' must be string.");		
	}
	
	private void process() throws AppException {
		 if (properties.containsKey("$or")) return; //throw new InternalServerException("error.internal", "Query may not contain or");
		 if (fields.contains("group") && !fields.contains("content")) fields.add("content");
		 if (fields.contains("ownerName") && !fields.contains("owner")) fields.add("owner");
		
		 if (properties.containsKey("history-date") || properties.containsKey("version") || properties.containsKey("history") || isStreamOnlyQuery()) properties.put("deleted", true);
		
		 fetchFromDB = fields.contains("data") ||
	              //fields.contains("app") || 
	              fields.contains("creator") || 	             
	              fields.contains("name") ||
	              fields.contains("code") || 
	              fields.contains("description") ||
	              fields.contains("lastUpdated") || 
	              fields.contains("tags") ||	              
	              fields.contains("watches") ||
	              (!properties.containsKey("deleted")) ||
	              //properties.containsKey("app") ||
	              properties.containsKey("creator") ||
	              properties.containsKey("data") ||
	              properties.containsKey("code") ||
	              properties.containsKey("tag") ||
	              properties.containsKey("name");
		 
		 restrictedOnTime = properties.containsKey("created") || properties.containsKey("max-age") || properties.containsKey("created-after") || properties.containsKey("created-before") || properties.containsKey("updated-after") || properties.containsKey("shared-after") || properties.containsKey("updated-before") || properties.containsKey("history-date");
		 
         fieldsFromDB = Sets.create();
         mayNeedFromDB = new HashSet<String>();
         if (fields.contains("stream")) { 
        	 fieldsFromDB.add("stream");
        	 fieldsFromDB.add("time");
        	 fieldsFromDB.add("document");
        	 fieldsFromDB.add("part");
         }
         if (fields.contains("format") || properties.containsKey("format") || properties.containsKey("group")) mayNeedFromDB.add("format");
         if (fields.contains("content") || properties.containsKey("content") || properties.containsKey("group")) mayNeedFromDB.add("content");
         if (fields.contains("app") || properties.containsKey("app") || properties.containsKey("group")) mayNeedFromDB.add("app");
         if (fields.contains("created") || restrictedOnTime) mayNeedFromDB.add("created");
         
         if (properties.containsKey("sort")) {
			 String sort = properties.get("sort").toString();
			 if (sort.startsWith("data.")) { fieldsFromDB.add("encryptedData"); }
			 fetchFromDB = true;
		 }
         
         if (restrictedOnTime) fieldsFromDB.add("time");
		 //if (fetchFromDB) fieldsFromDB.add("encrypted");
		 if (fields.contains("data") || properties.containsKey("data")) fieldsFromDB.add("encryptedData");
		 if (fields.contains("watches")) fieldsFromDB.add("encWatches");
		 
		 					
		 if (properties.containsKey("max-age")) {
			Number maxAge = (long) Double.parseDouble(properties.get("max-age").toString());
			minDateCreated = new Date(System.currentTimeMillis() - 1000 * maxAge.longValue());
			minTime = Math.max(minTime, getTimeFromDate(minDateCreated));
		 }
		 
		 if (properties.containsKey("created-after")) {				
				minDateCreated = getDateRestriction("created-after");
				minTime = Math.max(minTime, getTimeFromDate(minDateCreated));
		 }
		 
		 if (properties.containsKey("updated-after")) {				
				minDateUpdated = getDateRestriction("updated-after");
				minTime = Math.max(minTime, getTimeFromDate(minDateUpdated)); 
				fetchFromDB = true;
		 }
		 
		 if (properties.containsKey("shared-after")) {				
				minDateShared = getDateRestriction("shared-after");
				minTime = Math.max(minTime, getTimeFromDate(minDateShared)); 
				fetchFromDB = true;
		 }
		 
		 if (properties.containsKey("history-date")) {				
				maxDateCreated = getDateRestriction("history-date");
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
		 
		 /*
		 if (properties.containsKey("shared-before")) {				
				maxDateShared = getDateRestriction("shared-before");
				int maxTime1 = getTimeFromDate(maxDateShared);
				if (maxTime1 < maxTime || maxTime == 0) maxTime = maxTime1;
				fetchFromDB = true;
		 }
		 */
		 
		 resolveConstants(properties, context);		 	
		 
		 if (fetchFromDB) fieldsFromDB.add("encrypted");
		 //AccessLog.log("query fetch="+fetchFromDB+" fields="+fields.toString()+" fdb="+fieldsFromDB.toString()+" props="+properties.toString());
	}
	
	public static void resolveConstants(Map<String, Object> properties, AccessContext context) throws AppException {
		 if (properties.containsKey("app")) {
			 Set<String> apps = getRestriction(properties.get("app"), "app");
			 Set<String> resolved = new HashSet<String>();
			 for (Object app : apps) {
				 if (!MidataId.isValid(app.toString())) {
					 Plugin p = PluginLoginCache.getByFilename(app.toString());					 
					 if (p!=null) resolved.add(p._id.toString());
					 else throw new PluginException(context.getUsedPlugin(), "error.internal", "Queried for unknown app in access filter with name '"+app.toString()+"'.");
				 } else resolved.add(app.toString());
			 }
			 properties.put("app", resolved);
		 }
		 if (properties.containsKey("observer")) {
			 Set<String> apps = getRestriction(properties.get("observer"), "observer");
			 Set<String> resolved = new HashSet<String>();
			 for (Object app : apps) {
				 if (!MidataId.isValid(app.toString())) {
					 Plugin p = PluginLoginCache.getByFilename(app.toString());					 
					 if (p!=null) resolved.add(p._id.toString());
					 else throw new PluginException(context.getUsedPlugin(), "error.internal", "Queried for unknown app as observer with name '"+app.toString()+"'.");
				 } else resolved.add(app.toString());
			 }
			 properties.put("observer", resolved);
		 }
		 
		 if (properties.containsKey("study")) {
			 Set<String> studies = getRestriction(properties.get("study"), "study");
			 Set<String> resolved = new HashSet<String>();
			 for (Object study : studies) {
				 if (!MidataId.isValid(study.toString())) {
					 Study s = Study.getByCodeFromMember(study.toString(), Sets.create("_id"));					 
					 if (s!=null) resolved.add(s._id.toString());
					 else throw new BadRequestException("error.internal", "Queried for unknown study in access filter.");
				 } else resolved.add(study.toString());
			 }
			 properties.put("study", resolved);
		 }
		 
		 if (properties.get("owner")!=null) {
			 Set<String> owners = getRestriction(properties.get("owner"), "owner");
			 Set<Object> resolved = new HashSet<Object>();
			 for (Object owner : owners) {
				 //AccessLog.log("check owner:"+owner.toString());
				 if (MidataId.isValid(owner.toString())) {
					 resolved.add(owner);
				 } else {
					 if (owner.equals("self")) {
						 resolved.add(context.getSelf().toString());
					 } else if (owner.equals("all")) {
						 properties.remove("owner");
						 resolved = null;break;
					 } else resolved.add(owner);
				 }
			 }
			 if (resolved != null) properties.put("owner", resolved);
		 }
		 
		 if (properties.containsKey("code") && !properties.containsKey("content")) {
			 Set<String> codes = getRestriction(properties.get("code"), "code");
			 Set<String> contents = new HashSet<String>();
			 for (String code : codes) {
				 String content = ContentCode.getContentForSystemCode(code);
				 if (content == null) throw new BadRequestException("error.unknown.code", "Unknown code '"+code+"' in restriction.");
				 contents.add(content);
			 }
			 properties.put("content", contents);
		 }
	}
	
	public static int getTimeFromDate(Date dt) {
		return (int) (dt.getTime() / 1000 / 60 / 60 / 24 / 7);
	}
	
	public static void validate(Map<String, Object> query, boolean requiresContent) throws AppException {
		boolean contentSet = false;
		if (query.containsKey("$or")) {
			if (query.size() > 1) throw new BadRequestException("error.invalid.access_query", "Access query with $or is not allowed to have other restriction.");
			Object test = query.get("$or");
			if (!(test instanceof Collection)) throw new BadRequestException("error.invalid.access_query", "Access query with $or needs array.");
			Collection<Object> parts = (Collection<Object>) test;
			for (Object part : parts) {
				if (!(part instanceof Map)) throw new BadRequestException("error.invalid.access_query", "Access query with $or needs array with objects.");
				Map<String, Object> part2 = (Map<String, Object>) part;
				validate(part2, requiresContent);
			}
			return;
		}
		if (query.containsKey("group")) {
			Object system = query.get("group-system");
			if (system == null || ! (system instanceof String)) throw new BadRequestException("error.missing.groupsystem", "Missing group-system for query");
			Set<String> groups = Query.getRestriction(query.get("group"), "group"); 
			query.put("group", groups);
			for (String group : groups) if (RecordGroup.getBySystemPlusName(system.toString(), group) == null) throw new BadRequestException("error.unknown.group",  "Unknown group'"+group+"' for system '"+system.toString()+"'.");
			contentSet = true;
		}
		if (query.containsKey("group-strict")) {
			Object system = query.get("group-system");
			if (system == null || ! (system instanceof String)) throw new BadRequestException("error.missing.groupsystem", "Missing group-system for query");
			Set<String> groups = Query.getRestriction(query.get("group-strict"), "group");
			query.put("group-strict", groups);
			for (String group : groups) if (RecordGroup.getBySystemPlusName(system.toString(), group) == null) throw new BadRequestException("error.unknown.group",  "Unknown group'"+group+"' for system '"+system.toString()+"'.");
			contentSet = true;
		}
		if (query.containsKey("content")) {
			Set<String> contents = Query.getRestriction(query.get("content"), "content");
			for (String content : contents) ContentInfo.getByName(content);
			query.put("content", contents);
			contentSet = true;
		}
		if (query.containsKey("code")) {
			Set<String> codes = Query.getRestriction(query.get("code"), "code");
			for (String code : codes) if (ContentCode.getBySystemCode(code) == null) throw new BadRequestException("error.unknown.code","Unknown code '"+code+"' in query.");
			query.put("code", codes);
			contentSet = true;
		}
		if (query.containsKey("format")) {
			Set<String> formats = Query.getRestriction(query.get("format"), "format");
			for (String format : formats) FormatInfo.getByName(format);
			query.put("format", formats);
		}
		if (query.containsKey("app")) {
			query.put("app", Query.getRestriction(query.get("app"), "app"));
		}
		
		if (query.containsKey("owner")) {
			query.put("owner", Query.getRestriction(query.get("owner"), "owner"));
		}
		if (requiresContent && !contentSet) {
			throw new BadRequestException("error.invalid.access_query", "Access query must restrict by 'code', 'content' or 'group'!");
		}
	}
}
