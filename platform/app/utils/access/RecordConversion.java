package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;

import models.MidataId;
import models.Record;

public class RecordConversion {
	
	public static RecordConversion instance = new RecordConversion();

	public Record currentVersionFromDB(DBRecord dbrecord) {
		Record record = new Record();
		
		record._id = dbrecord._id;
		record.app = MidataId.from(dbrecord.meta.get("app"));
		record.creator = MidataId.from(dbrecord.meta.get("creator"));
		record.name = (String) dbrecord.meta.get("name");				
		record.created = (Date) dbrecord.meta.get("created");
		record.description = (String) dbrecord.meta.get("description");
		record.version = (String) dbrecord.meta.get("version");
		if (record.version == null) record.version = VersionedDBRecord.INITIAL_VERSION;
		record.format = (String) dbrecord.meta.get("format");		
		record.subformat = (String) dbrecord.meta.get("subformat");
		record.content = (String) dbrecord.meta.get("content");
		Object code = dbrecord.meta.get("code");
		if (code != null) {
		  record.code = (code instanceof String) ? Collections.singleton((String) code) : new HashSet((Collection) code);
		}
		record.owner = dbrecord.owner;
		record.ownerName = (String) dbrecord.meta.get("ownerName");		
		record.tags = (Set<String>) dbrecord.meta.get("tags");
		record.document = dbrecord.document;
		record.group = dbrecord.group;
		record.id = dbrecord.id;
		record.isStream = dbrecord.isStream;
		record.stream = dbrecord.stream;
		
		if (dbrecord.data != null) record.data = dbrecord.data;
		
		return record;		
	}
	
	public List<Record> currentVersionFromDB(List<DBRecord> dbrecords) {
		List<Record> result = new ArrayList<Record>(dbrecords.size());
		for (DBRecord dbrecord : dbrecords) result.add(currentVersionFromDB(dbrecord));
		return result;
	}
	

	
	public DBRecord toDB(Record record) {
		DBRecord dbrecord = new DBRecord();
		dbrecord._id = record._id;
		dbrecord.data = record.data;
		dbrecord.owner = record.owner;
		BSONObject meta = dbrecord.meta;		
		meta.put("app", record.app !=null ? record.app.toDb() : null);
		meta.put("creator", record.creator.toDb());
		meta.put("name", record.name);
		meta.put("created", record.created);
		meta.put("description", record.description);
		meta.put("version", record.version);
		meta.put("tags", record.tags);
		meta.put("format", record.format);
		if (record.subformat != null) meta.put("subformat", record.subformat);
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
