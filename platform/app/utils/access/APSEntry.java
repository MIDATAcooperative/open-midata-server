package utils.access;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import models.ContentInfo;
import models.Record;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import com.mongodb.BasicDBList;

/**
 * functions for reading and manipulation of entries of an access permission set.
 *
 */
class APSEntry {

	public static final Set<String> groupingFields = Sets.create("format", "content", "subformat");
	
	public static List<BasicBSONObject> findMatchingRowsForQuery(Map<String, Object> permissions, Query q) throws AppException {
		List<BasicBSONObject> result = new ArrayList<BasicBSONObject>();
		
		BasicBSONList lst = (BasicBSONList) permissions.get("p");
		//AccessLog.debug("ALL:"+lst.toString());
		
		Set<String> formats = q.restrictedBy("format") ? q.getRestriction("format") : null;
		//Set<String> formatsWC = q.restrictedBy("format/*") ? q.getRestriction("format/*") : null;
		Set<String> contents = q.restrictedBy("content") ? q.getRestriction("content") : null;
		//Set<String> contentsWC = q.restrictedBy("content/*") ? q.getRestriction("content/*") : null;
		Set<String> subformats = q.restrictedBy("subformat") ? q.getRestriction("subformat") : null;
		
		for (Object row : lst) {
			BasicBSONObject crit = (BasicBSONObject) row;
			boolean match = true;
			if (formats != null) {
			  String fmt = crit.getString("format");
			  if (fmt != null && !formats.contains(fmt)) match = false; 
			}
			if (contents != null) {
			  String fmt = crit.getString("content");
			  if (fmt != null && !contents.contains(fmt)) match = false;  
			}
			if (subformats != null) {
				String fmt = crit.getString("subformat");
				if (fmt != null && !subformats.contains(fmt)) match = false;  
			}
			/*if (contentsWC != null) {
  			  String fmt = crit.getString("content");
			  if (fmt != null && !contentsWC.contains(ContentInfo.getWildcardName(fmt))) match = false;
			}
			if (formatsWC != null) {
	  			  String fmt = crit.getString("format");
				  if (fmt != null && !formatsWC.contains(ContentInfo.getWildcardName(fmt))) match = false;
		    }*/
			if (match) result.add(crit);			
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
			  if (val != null && oval !=null && !oval.equals(val)) match = false;
			}
												
			if (match) return crit;			
		}
        if (!create) return null;		
		
		BasicBSONObject newentry = new BasicBSONObject();
		for (String field : groupingFields) {
		  newentry.put(field, record.meta.get(field));
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
