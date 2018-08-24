package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import models.Record;
import utils.AccessLog;

public class RecordConversion {
	
	public static RecordConversion instance = new RecordConversion();

	public Record currentVersionFromDB(DBRecord dbrecord) {
		Record record = new Record();
		
		record._id = dbrecord._id;
		record.format = (String) dbrecord.meta.get("format");		
		if (record.format != null) {
		  record.app = MidataId.from(dbrecord.meta.get("app"));
		  Object creator = dbrecord.meta.get("creator");
		  if (creator != null) record.creator = MidataId.from(creator); else record.creator = dbrecord.owner;
		  record.name = (String) dbrecord.meta.get("name");				
		  record.created = (Date) dbrecord.meta.get("created");
		  record.lastUpdated = (Date) dbrecord.meta.get("lastUpdated");
		  if (record.lastUpdated == null) record.lastUpdated = record.created;
		  record.description = (String) dbrecord.meta.get("description");
		  record.version = (String) dbrecord.meta.get("version");
		  if (record.version == null) record.version = VersionedDBRecord.INITIAL_VERSION;
						
		  record.content = (String) dbrecord.meta.get("content");
		  Object code = dbrecord.meta.get("code");
		  if (code != null) {
		    record.code = (code instanceof String) ? Collections.singleton((String) code) : new HashSet((Collection) code);
		  }
		  Object tags = dbrecord.meta.get("tags");
		  if (tags != null) {
			  record.tags = new HashSet<String>();
			  for (Object o : ((BasicBSONList) tags)) {
				  record.tags.add(o.toString());				  
			  }
		  }			
		}
		
		record.owner = dbrecord.owner;
		record.ownerName = (String) dbrecord.meta.get("ownerName");						
		record.group = dbrecord.group;
		record.id = dbrecord.id;
		record.isStream = dbrecord.isStream;
		record.stream = dbrecord.stream;
		
		if (dbrecord.data != null) record.data = dbrecord.data;
		
		return record;		
	}
	
	public List<Record> currentVersionFromDB(List<DBRecord> dbrecords) {
		long now = System.currentTimeMillis();
		AccessLog.log("start convert");
		List<Record> result = new ArrayList<Record>(dbrecords.size());
		for (DBRecord dbrecord : dbrecords) result.add(currentVersionFromDB(dbrecord));
		AccessLog.log("end convert time="+(System.currentTimeMillis() - now));
		return result;
	}
	

	
	public DBRecord toDB(Record record) {
		DBRecord dbrecord = new DBRecord();
		dbrecord._id = record._id;
		dbrecord.data = record.data;
		dbrecord.owner = record.owner;
		BSONObject meta = dbrecord.meta;		
		meta.put("app", record.app !=null ? record.app.toDb() : null);
		meta.put("creator", (record.creator != null && !record.creator.equals(record.owner)) ? record.creator.toDb() : null);
		meta.put("name", record.name);
		meta.put("created", record.created);
		if (record.lastUpdated != null) meta.put("lastUpdated", record.lastUpdated);
		meta.put("description", record.description);
		meta.put("version", record.version);
		meta.put("tags", record.tags);
		meta.put("format", record.format);		
		meta.put("content", record.content);
		if (record.code != null && record.code.size() == 1) {
		   meta.put("code", record.code.iterator().next());
		} else {
		   meta.put("code", record.code);
		}
		dbrecord.data = record.data;
		return dbrecord;
	}
	
	public List<DBRecord> toDB(List<Record> records) {
		List<DBRecord> result = new ArrayList<DBRecord>(records.size());
		for (Record record : records) result.add(toDB(record));
		return result;
	}
	
}
