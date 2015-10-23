package utils.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import models.ContentInfo;
import models.Record;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import com.mongodb.BasicDBList;

public class APSEntry {

	public static List<BasicBSONObject> findMatchingRowsForQuery(Map<String, Object> permissions, Query q) throws AppException {
		List<BasicBSONObject> result = new ArrayList<BasicBSONObject>();
		
		BasicBSONList lst = (BasicBSONList) permissions.get("p");
		Set<String> formats = q.restrictedBy("format") ? q.getRestriction("format") : null;
		Set<String> contents = q.restrictedBy("content") ? q.getRestriction("content") : null;
		Set<String> contentsWC = q.restrictedBy("content/*") ? q.getRestriction("content/*") : null;
		
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
			if (contentsWC != null) {
  			  String fmt = crit.getString("content");
			  if (fmt != null && !contentsWC.contains(ContentInfo.getWildcardName(fmt))) match = false;
			}
			if (match) result.add(crit);			
		}
		
		return result;
	}
	
	public static BasicBSONObject findMatchingRowForRecord(Map<String, Object> permissions, Record record, boolean create) throws InternalServerException {
				
		BasicBSONList lst = (BasicBSONList) permissions.get("p");		
		
		for (Object row : lst) {
			BasicBSONObject crit = (BasicBSONObject) row;
			boolean match = true;
			
			String fmt = crit.getString("format");
			if (fmt != null && !record.format.equals(fmt)) match = false; 
						
			fmt = crit.getString("content");
			if (fmt != null && !record.content.equals(fmt)) match = false; 
			
			if (match) return crit;			
		}
        if (!create) return null;		
		
		BasicBSONObject newentry = new BasicBSONObject();
		newentry.put("format", record.format);
		newentry.put("content", record.content);
		newentry.put("r", new BasicBSONObject());
		lst.add(newentry);
		return newentry;
	}
	
	public static BasicBSONObject getEntries(BasicBSONObject row) {
		return (BasicBSONObject) row.get("r");
	}
	
	public static void populateRecord(BasicBSONObject row, Record record) {
		String fmt = row.getString("format");
		if (fmt != null) record.format = fmt;
		
		fmt = row.getString("content");
		if (fmt != null) record.content = fmt;
		//AccessLog.debug("populate:"+record.format+"/"+record.content);
	}
		
}
