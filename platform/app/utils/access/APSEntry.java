package utils.access;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * functions for reading and manipulation of entries of an access permission set.
 *
 */
class APSEntry {

	public static final Set<String> groupingFields = Sets.create("format", "content", "app");
	
	public static List<BasicBSONObject> findMatchingRowsForQuery(Map<String, Object> permissions, Query q) throws AppException {
		List<BasicBSONObject> result = null;// = new ArrayList<BasicBSONObject>();
		
		BasicBSONList lst = (BasicBSONList) permissions.get("p");
		//AccessLog.debug("ALL:"+lst.toString());
		
		Set<String> formats = q.getRestrictionOrNull("format");	
		Set<String> contents = q.getRestrictionOrNull("content");	
		Set<String> apps = q.getIdRestrictionAsString("app");
		
		for (Object row : lst) {
			BasicBSONObject crit = (BasicBSONObject) row;
			
			if (formats != null) {
			  String fmt = crit.getString("format");
			  if (fmt != null && !formats.contains(fmt)) continue; 
			}
			if (contents != null) {
			  String fmt = crit.getString("content");
			  if (fmt != null && !contents.contains(fmt)) continue;  
			}
			if (apps != null) {
				String fmt = crit.getString("app");
				if (fmt != null && !apps.contains(fmt)) continue;
			}	
			if (result == null) result = new ArrayList<BasicBSONObject>();
			result.add(crit);			
		}
		
		return result;
	}
	
	public static BasicBSONObject findMatchingRowForRecord(Map<String, Object> permissions, DBRecord record, boolean create) throws InternalServerException {
				
		BasicBSONList lst = (BasicBSONList) permissions.get("p");		
		
		for (Object row : lst) {
			BasicBSONObject crit = (BasicBSONObject) row;
			boolean match = true;
			
			for (String field : groupingFields) {			
			  String val = crit.getString(field);
			  String oval = record.meta.getString(field);
			  if (oval !=null && (val == null || !oval.equals(val))) match = false;
			}
												
			if (match) return crit;			
		}
        if (!create) return null;		
		
		BasicBSONObject newentry = new BasicBSONObject();
		for (String field : groupingFields) {
			Object o = record.meta.get(field);
		  newentry.put(field, o != null ? o.toString() : null);
		}
		newentry.put("r", new BasicBSONObject());
		lst.add(newentry);
		return newentry;
	}
	
	public static void cleanupRows(Map<String, Object> permissions) {
        BasicBSONList lst = (BasicBSONList) permissions.get("p");		
		
        ListIterator<Object> it = lst.listIterator();
        while (it.hasNext()) {
		   Object row = it.next();
		   if (row instanceof BSONObject) {
			   BasicBSONObject r = (BasicBSONObject) ((BSONObject) row).get("r");
			   if (r.isEmpty()) it.remove();
		   } else it.remove();			
		}
	}
	
	public static BasicBSONObject getEntries(BasicBSONObject row) {
		return (BasicBSONObject) row.get("r");
	}
	
	public static void populateRecord(BasicBSONObject row, DBRecord record) {
		for (String field : groupingFields) {		
		  String val = row.getString(field);
		  if (val != null) record.meta.put(field, val);
		}
		Object app = record.meta.get("app");
		if (app != null) record.meta.put("app", new ObjectId(app.toString()));
	}
	 
	public static void mergeAllInto(Map<String, Object> props, Map<String, Object> targetPermissions) throws InternalServerException {
		BasicBSONList lst = (BasicBSONList) props.get("p");
		DBRecord dummy = new DBRecord();		
		
		for (Object row : lst) {
			BasicBSONObject crit = (BasicBSONObject) row;
			BasicBSONObject entries = APSEntry.getEntries(crit);
            APSEntry.populateRecord(crit, dummy);										
		    for (String key : entries.keySet()) {
			   BasicBSONObject copyVal = (BasicBSONObject) entries.get(key);
			   
			   BasicBSONObject targetRow = APSEntry.findMatchingRowForRecord(targetPermissions, dummy, true);
			   BasicBSONObject targetEntries = APSEntry.getEntries(targetRow);
			   
			   if (!targetEntries.containsField(key)) {
				  targetEntries.put(key, new BasicBSONObject());
			   }
			   BasicBSONObject targetVals = (BasicBSONObject) targetEntries.get(key);
			
			   for (String v : copyVal.keySet()) {
				  targetVals.put(v, copyVal.get(v));
			   }
		    }
		}
	}
		
}
