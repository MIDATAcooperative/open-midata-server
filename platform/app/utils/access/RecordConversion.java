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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import models.Record;
import utils.AccessLog;

public class RecordConversion {
	
	public static RecordConversion instance = new RecordConversion();

	public Record currentVersionFromDB(DBRecord dbrecord) {
		Record record = new Record();
		
		record._id = dbrecord._id;
		record.context = dbrecord.context;
		record.format = (String) dbrecord.meta.get("format");		
		if (record.format != null) {
		  record.app = MidataId.from(dbrecord.meta.get("app"));
		  record.version = (String) dbrecord.meta.get("version");
		  
		  Object creator = dbrecord.meta.get("creator");
		  if (creator != null) record.creator = MidataId.from(creator); else record.creator = dbrecord.owner;
		  
		  Object creatorOrg = dbrecord.meta.get("creatorOrg");
          if (creatorOrg != null) record.creatorOrg = MidataId.from(creatorOrg);
          
		  
		  Object modifiedBy = dbrecord.meta.get("modifiedBy");
		  if (modifiedBy != null) {
			  if ("O".equals(modifiedBy)) record.modifiedBy = dbrecord.owner;
			  else record.modifiedBy = MidataId.from(modifiedBy); 
		  } else record.modifiedBy = record.creator;
		  
		  Object modifiedByOrg = dbrecord.meta.get("modifiedByOrg");
          if (modifiedByOrg != null) record.modifiedByOrg = MidataId.from(modifiedByOrg);
          
		  
		  record.name = (String) dbrecord.meta.get("name");				
		  record.created = (Date) dbrecord.meta.get("created");
		  record.lastUpdated = (Date) dbrecord.meta.get("lastUpdated");
		  if (record.lastUpdated == null) record.lastUpdated = record.created;
		  record.description = (String) dbrecord.meta.get("description");
		  
		  if (record.version == null) record.version = VersionedDBRecord.INITIAL_VERSION;
						
		  record.content = (String) dbrecord.meta.get("content");
		  Object code = dbrecord.meta.get("code");
		  if (code != null) {
		    record.code = (code instanceof String) ? Collections.singleton((String) code) : new HashSet((Collection) code);
		  }
		  record.tags = getTags(dbrecord);		  	
		}
		
		record.owner = dbrecord.owner;
		record.ownerName = (String) dbrecord.meta.get("ownerName");
		record.ownerType = dbrecord.ownerType;
		record.group = dbrecord.group;
		record.id = dbrecord.id;
		record.isStream = dbrecord.isStream!=null;
		record.stream = dbrecord.stream;
		
		if (dbrecord.data != null) record.data = dbrecord.data;
		
		return record;		
	}
	
	public List<Record> currentVersionFromDB(List<DBRecord> dbrecords) {
		long now = System.currentTimeMillis();
		AccessLog.log("start convert");
		List<Record> result = new ArrayList<Record>(dbrecords.size());
		for (DBRecord dbrecord : dbrecords) result.add(currentVersionFromDB(dbrecord));
		AccessLog.log("end convert time=", Long.toString(System.currentTimeMillis() - now));
		return result;
	}
	
	public Set<String> getTags(DBRecord dbrecord) {
		 Object tags = dbrecord.meta.get("tags");
		 if (tags != null) {
			  Set<String> tagSet = new HashSet<String>();
			  for (Object o : ((BasicBSONList) tags)) {
				  tagSet.add(o.toString());					  
			  }			  
			  return tagSet;
		 } else return null;		 
	}
	
	
	public DBRecord toDB(Record record) {
		DBRecord dbrecord = new DBRecord();
		dbrecord._id = record._id;		
		dbrecord.owner = record.owner;
		BSONObject meta = dbrecord.meta;		
		meta.put("app", record.app !=null ? record.app.toDb() : null);
		meta.put("creator", (record.creator != null && !record.creator.equals(record.owner)) ? record.creator.toDb() : null);
		if (record.creatorOrg != null) {
		    meta.put("creatorOrg", record.creatorOrg.toDb());
		}
		if (record.modifiedBy != null && !record.modifiedBy.equals(record.creator)) {
		  meta.put("modifiedBy", !record.modifiedBy.equals(record.owner) ? record.modifiedBy.toDb() : "O");
		}
		if (record.modifiedByOrg != null && !record.modifiedByOrg.equals(record.creatorOrg)) {
            meta.put("modifiedByOrg", record.modifiedByOrg.toDb());
        }
						
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
		if (record.data==null) dbrecord.data = new BasicBSONObject();
		return dbrecord;
	}
	
	public List<DBRecord> toDB(List<Record> records) {
		List<DBRecord> result = new ArrayList<DBRecord>(records.size());
		for (Record record : records) result.add(toDB(record));
		return result;
	}
	
}
