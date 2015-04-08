package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map; 
import java.util.Set;

import org.bson.types.ObjectId;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;

import utils.DateTimeUtils;
import utils.auth.CodeGenerator;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.db.ObjectIdConversion;
import utils.search.Search;
import utils.search.SearchException;

import models.AccessPermissionSet;
import models.LargeRecord;
import models.Member;
import models.ModelException;
import models.Record;
import models.User;

public class RecordSharing {
	
	public static RecordSharing instance = new RecordSharing();		
	
	public final static Map<String, Object> FULLAPS = new HashMap<String, Object>();
	public final static Set<String> INTERNALIDONLY = Sets.create("_id");
	public final static Set<String> COMPLETE_META = Sets.create("id", "owner", "app", "creator", "created", "name", "format", "description");
	public final static Set<String> COMPLETE_DATA = Sets.create("id", "owner", "app", "creator", "created", "name", "format", "description", "data");
	public final static String STREAM_TYPE = "Stream";
	
	public ObjectId createPrivateAPS(ObjectId who, ObjectId proposedId) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		String encryptionKey = generateKey();
		
		newset._id = proposedId;
		newset.permissions = new HashMap<String, BasicDBObject>();
		newset.keys = new HashMap<String, String>();
		newset.keys.put("owner", "key"+who.toString()+":"+encryptionKey);
		AccessPermissionSet.add(newset);
		return newset._id;
	}
	
	public ObjectId createAnonymizedAPS(ObjectId owner, ObjectId other, ObjectId proposedId) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		String encryptionKey = generateKey();
		
		newset._id = proposedId;
		newset.permissions = new HashMap<String, BasicDBObject>();
		newset.keys = new HashMap<String, String>();
		newset.keys.put("owner", "key"+owner.toString()+":"+encryptionKey);
		newset.keys.put(other.toString(), "key"+other.toString()+":"+encryptionKey);
		AccessPermissionSet.add(newset);
		return newset._id;
	}
	
	public ObjectId createAPSForRecord(ObjectId owner, ObjectId recordId, String key, boolean direct) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		String encryptionKey = generateKey();
		
		newset._id = recordId;
		newset.direct = direct;
		newset.permissions = new HashMap<String, BasicDBObject>();
		newset.keys = new HashMap<String, String>();
		newset.keys.put("owner", "key"+owner.toString()+":"+encryptionKey);
		
		AccessPermissionSet.add(newset);
		return newset._id;
	}
	
	public void shareAPS(ObjectId apsId, ObjectId ownerId, Set<ObjectId> targetUsers) throws ModelException {
		APSWrapper apswrapper = new APSWrapper(apsId, ownerId);
		
		for (ObjectId targetUser : targetUsers) {
			apswrapper.aps.keys.put(targetUser.toString(), "key"+targetUser.toString()+":"+apswrapper.encryptionKey);
		}
		apswrapper.aps.setKeys(apswrapper.aps.keys);
	}
	
	public void unshareAPS(ObjectId apsId, ObjectId ownerId, Set<ObjectId> targetUsers) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		if (aps==null) throw new ModelException("APS not found.");
		validateReadable(aps, ownerId);
		
		for (ObjectId targetUser : targetUsers) {
		  aps.keys.remove(targetUser.toString());
		}
		aps.setKeys(aps.keys);
	}

	public void share(ObjectId who, ObjectId fromAPS, ObjectId toAPS, Set<ObjectId> records, boolean withOwnerInformation) throws ModelException {
		
		APSWrapper source = new APSWrapper(fromAPS, who);
		APSWrapper target = new APSWrapper(toAPS, who);
		
		List<Record> recordEntries = source.lookup(CMaps.map("_id", records), Sets.create("_id", "key", "owner", "format"));
		
		target.addPermission(recordEntries, withOwnerInformation);
				
		// target.setPermissions(target.permissions);		
	}
	
	public void unshare(ObjectId who, ObjectId apsId, Set<ObjectId> records) throws ModelException {
		
		APSWrapper source = new APSWrapper(apsId, who);		
		List<Record> recordEntries = source.lookup(CMaps.map("_id", records), Sets.create("_id", "format"));		
		source.removePermission(recordEntries);		
	}
	
	public Record createStream(Member owner, ObjectId targetAPS, String name, boolean direct) throws ModelException {
		Record result = new Record();
		result._id = new ObjectId();
		result.name = name;
		result.direct = direct;
		result.format = STREAM_TYPE;
		result.created = DateTimeUtils.now();
		result.data = new BasicDBObject();
		
		addRecord(owner, result);
		return result;
	}
	
	private long getTimeFromDate(Date dt) {
		return dt.getTime() / 1000 / 60 / 60 / 24 / 7;
	}
	
	private String generateKey() {
		return CodeGenerator.nextCode();
	}
	
	public void addRecord(Member owner, Record record) throws ModelException {
		record.time = System.currentTimeMillis() / 1000 / 60 / 60 / 24 / 7;
		
		record = record.clone();
		APSWrapper apswrapper;
		boolean apsDirect = false;
		
		if (record.stream != null) {
		  apswrapper = new APSWrapper(record.stream, owner._id);
		  
		} else {		
		  apswrapper = new APSWrapper(owner.myaps, owner._id);
		}
	
		if (record.format.equals(STREAM_TYPE)) {
			apsDirect = record.direct;
			record.stream = null;			
			record.direct = false;
		}
		
		if (record.direct) {
			record.key = apswrapper.encryptionKey;
		} else {
		    record.key = generateKey();
		}
		try {
		    Search.add(record.owner, "record", record._id, record.name, record.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
		apswrapper.encryptRecord(record);		
	    Record.add(record);	  
	    
		if (!record.direct) apswrapper.addPermission(record, false);
		if (record.format.equals(STREAM_TYPE)) {
			RecordSharing.instance.createAPSForRecord(owner._id, record._id, record.key, apsDirect);
		}
		
		record.key = null;
	}
		
	
	public void addRecord2(Member owner, Record record) throws ModelException {
        record.time = System.currentTimeMillis() / 1000 / 60 / 60 / 24 / 7;
		
		record = record.clone();
		APSWrapper apswrapper;
		
		/*if (record.series != null) {
		  apswrapper = new APSWrapper(record.series, owner._id);
		  
		} else {*/		
		  apswrapper = new APSWrapper(owner.myaps, owner._id);
		//}
	
		/*if (record.format.equals("stream")) {
			record.series = null;
		}
		
		if (record.direct) {
			record.key = apswrapper.encryptionKey;
		} else { */
		record.key = generateKey();
		//}
		apswrapper.encryptRecord(record);		
	    Record.set(record._id, "encrypted", record.encrypted);	  
	    
		apswrapper.addPermission(record, false);
		/*
		if (record.format.equals("stream")) {
			RecordSharing.instance.createAPSForRecord(owner._id, record._id, record.key);
		}*/
		
		record.key = null;    	   
	}
	
	public void deleteAPS(ObjectId apsId, ObjectId ownerId) throws ModelException {
		//AccessPermissionSet aps = AccessPermissionSet.getById(apsId);	
		AccessPermissionSet.delete(apsId);
	}
					
	public List<Record> list(ObjectId who, ObjectId apsId, Map<String, Object> properties, Set<String> fields) throws ModelException {
		
		APSWrapper apswrapper = new APSWrapper(apsId, who);
		
		return apswrapper.lookup(properties, fields);		
	}
	
	public Record fetch(ObjectId who, RecordToken token) throws ModelException {
		List<Record> result = list(who, new ObjectId(token.apsId), CMaps.map("_id", new ObjectId(token.recordId)), RecordSharing.COMPLETE_DATA );
		if (result.isEmpty()) return null; else return result.get(0);				
	}
	
	public Set<String> listRecordIds(ObjectId who, ObjectId apsId) throws ModelException {
		List<Record> result = list(who, apsId, RecordSharing.FULLAPS, RecordSharing.INTERNALIDONLY);
		Set<String> ids = new HashSet<String>();
		for (Record record : result) ids.add(record._id.toString());
		return ids;
	}
		
	
	void validateReadable(AccessPermissionSet aps, ObjectId who) throws ModelException {
		if (aps.keys == null) return; // Old version support
		String key = aps.keys.get(who.toString());
		if (key==null) key = aps.keys.get("owner"); 
	    if (key==null || ! key.equals("key"+who.toString())) throw new ModelException("APS not readable by user");		
	}
	
	class APSWrapper {
		
		private AccessPermissionSet aps;
		private ObjectId who;
		private ObjectId owner;
		private String encryptionKey;		
		
		//private String myFormat;
		//private int myMinTime;
		//private int myMaxTime;				
		
		APSWrapper(ObjectId apsId, ObjectId who) throws ModelException {
			this.aps = AccessPermissionSet.getById(apsId);
			this.who = who;
			this.owner = null;
			validateReadable(this.aps, this.who);
		}
		
		private void validateReadable(AccessPermissionSet aps, ObjectId who) throws ModelException {
			if (aps.keys == null) return; // Old version support
			String key = aps.keys.get(who.toString());
			if (key==null) { key = aps.keys.get("owner"); this.owner = who; } 
		    if (key==null || ! key.startsWith("key"+who.toString())) throw new ModelException("APS not readable by user");
		    encryptionKey = key.substring(key.indexOf(':'));
		}
		
		protected boolean lookupSingle(Record input, Map<String, Object> properties) {
			if (aps.direct) {
				input.key = encryptionKey;
				input.owner = owner;
				return true;
			}
			
            Map<String, BasicDBObject> formats;			
			
			if (properties.containsKey("format")) {
				Object formatRestriction = properties.get("format");
				if (formatRestriction instanceof String) {
				  formats = new HashMap<String, BasicDBObject>();
				  formats.put((String) formatRestriction, aps.permissions.get((String) formatRestriction));
				} else {
					formats = new HashMap<String, BasicDBObject>();
					for (String format : (Iterable<String>) formatRestriction) {
						formats.put(format, aps.permissions.get(format));
					}
				}
			} else {
				formats = aps.permissions;
			}
			
			
			for (String format : formats.keySet()) {
				   BasicDBObject map = formats.get(format);
				   BasicDBObject target = (BasicDBObject) map.get(input._id.toString());
				   if (target!=null) {
					   input.key = target.getString("key");
					   input.format = format;
					   if (input.owner == null) {
						   String owner = target.getString("owner");
						   if (owner!=null) input.owner = new ObjectId(owner); else input.owner = this.owner;
					   }					   
					   return true;
				   }
			}
			
			return false;
		}
		
		protected void scanForStreams(Map<String, APSWrapper> apsToScan) throws ModelException {
		  	BasicDBObject streams = aps.permissions.get(STREAM_TYPE);
		  	if (streams != null) {
		  		for (String key : streams.keySet()) {
		  			if (!apsToScan.containsKey(key)) {
		  				APSWrapper streamWrapper = new APSWrapper(new ObjectId(key), who);
		  				apsToScan.put(key, streamWrapper);
		  			}
		  		}
		  	}
		}
		
		protected Record createRecordFromAPSEntry(String id, String format, BasicDBObject entry, boolean withOwner) {
			Record record = new Record();
			record._id = new ObjectId(id);
			record.format = format;
			record.key = entry.getString("key");			
		
			if (withOwner) {
				String owner = entry.getString("owner");
			    if (owner!=null) record.owner = new ObjectId(owner); else record.owner = this.owner;
			}
							
			return record;
		}
		
		protected void encryptRecord(Record record) throws ModelException {
			if (record.key == null) throw new ModelException("Cannot encrypt");
			record.encrypted = "enc"+record.key;
			//record.clearEncryptedFields();
		}
		
		protected void decryptRecord(Record record) throws ModelException {
			if (record.created != null) return;
			if (!record.encrypted.equals("enc"+record.key)) throw new ModelException("Cannot decrypt");
		}
		
		protected void localQuery(List<Record> result, Map<String, Object> properties, Set<String> fields, Set<String> fieldsFromDB, int minTime, int maxTime) throws ModelException {
			boolean withOwner = fields.contains("owner");
			
			if (aps.direct) {
				Map<String, Object> query = new HashMap<String, Object>();
				query.put("stream", aps._id);
				query.put("direct", Boolean.TRUE);				
				List<Record> directResult = new ArrayList<Record>(Record.getAll(query, fieldsFromDB));
				for (Record record : directResult) {
					record.key = encryptionKey;
					if (withOwner) record.owner = this.owner;					
				}
				result.addAll(directResult);
				return;
			}
			
			
			// 4 restricted by time? has APS time restriction? load other APS -> APS (4,5,6) APS LIST -> Records			
			
			// 5 Create list format -> Permission List (maybe load other APS)
			Map<String, BasicDBObject> formats;			
			
			if (properties.containsKey("format")) {
				Object formatRestriction = properties.get("format");
				if (formatRestriction instanceof String) {
				  formats = new HashMap<String, BasicDBObject>();
				  formats.put((String) formatRestriction, aps.permissions.get((String) formatRestriction));
				} else {
					formats = new HashMap<String, BasicDBObject>();
					for (String format : (Iterable<String>) formatRestriction) {
						formats.put(format, aps.permissions.get(format));
					}
				}
			} else {
				formats = aps.permissions;
			}
			
			
			// 6 Each permission list : apply filters -> Records
			boolean restrictedById = properties.containsKey("_id");
			boolean restrictedByOwner = properties.containsKey("owner");
			if (restrictedById) {
				Object ids = properties.get("_id");
				if (ids instanceof ObjectId) {
					for (String format : formats.keySet()) {
					   BasicDBObject map = formats.get(format);
					   BasicDBObject target = (BasicDBObject) map.get(ids.toString());
					   if (target!=null) result.add(createRecordFromAPSEntry(ids.toString(), format, target, withOwner));
					}
				} else {
					for (ObjectId id : (Iterable<ObjectId>) ids) {
						for (String format : formats.keySet()) {
							   BasicDBObject map = formats.get(format);
							   BasicDBObject target = (BasicDBObject) map.get(id.toString());
							   if (target!=null)
							   result.add(createRecordFromAPSEntry(id.toString(), format, target, withOwner));
							}
					}
				}
			} else {
				for (String format : formats.keySet()) {
				    BasicDBObject map = formats.get(format);
				    for (String id : map.keySet()) {
				    	BasicDBObject target = (BasicDBObject) map.get(id);
				    	result.add(createRecordFromAPSEntry(id , format, target, withOwner));
				    }
				}
			}			
		}
						
		List<Record> lookup(Map<String, Object> properties, Set<String> fields) throws ModelException {
						
			ArrayList<Record> result = new ArrayList<Record>();
			Map<String, APSWrapper> apsToScan = new HashMap<String, APSWrapper>();			
			
			// 1 If APS is from other server execute there (if APS redirection) (DB/API)
			
			// Prepare
            boolean fetchFromDB = fields.contains("data") ||
            		              fields.contains("app") || 
            		              fields.contains("creator") || 
            		              fields.contains("created") || 
            		              fields.contains("name") || 
            		              fields.contains("description") || 
            		              fields.contains("tags") ||
            		              properties.containsKey("app") ||
            		              properties.containsKey("creator") ||
            		              properties.containsKey("created") ||
            		              properties.containsKey("name")
            		              ;
            Set<String> fieldsFromDB = Sets.create();
            if (fetchFromDB) fieldsFromDB.add("encrypted");
            if (fields.contains("data")) fieldsFromDB.add("encryptedData");
            
            // TODO Remove later
            if (fields.contains("data")) fieldsFromDB.add("data");
            if (fields.contains("app")) fieldsFromDB.add("app");
            if (fields.contains("format")) fieldsFromDB.add("format");
            if (fields.contains("creator")) fieldsFromDB.add("creator");
            if (fields.contains("created")) fieldsFromDB.add("created");
            if (fields.contains("name")) fieldsFromDB.add("name");
            if (fields.contains("description")) fieldsFromDB.add("description");
            if (fields.contains("tags")) fieldsFromDB.add("tags");
            
            
			// 2 Load Records from DB (if ID given) -> Records (possibly, encrypted) (if properties _id set )
			boolean restrictedOnTime = false;
			boolean giveKey = fields.contains("key");
			int minTime = 0;
			int maxTime = 0;
			
			boolean restrictedById = properties.containsKey("_id");			
			if (restrictedById) {
				Map<String, Object> query = new HashMap<String, Object>();
				Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
				queryFields.addAll(fieldsFromDB);
				query.put("_id", properties.get("_id"));
				result = new ArrayList<Record>(Record.getAll(query, queryFields));
			}
		
			// Load Records from DB if document is given
			boolean restrictedByDocument = properties.containsKey("document");
			boolean restrictedByPart = properties.containsKey("part");
			if (restrictedByDocument && !restrictedById) {
				Map<String, Object> query = new HashMap<String, Object>();
				Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
				queryFields.addAll(fieldsFromDB);
				query.put("document", properties.get("document"));
				if (restrictedByPart) {
					query.put("part", properties.get("part"));
				}
				result.addAll(Record.getAll(query, queryFields));
			}
			
			// try single lookup in given APS with (time, _id, format? (4,5,6) (use 3 only if fails) (Records)
			for (Record record : result) {
				lookupSingle(record, properties);
			}
									
			// 3 Create list of used APS from Records -> (where key is missing) APS (if 2)
			if (restrictedById) {
				for (Record record : result) {
					if (record.stream != null && record.key == null) {
						APSWrapper target = apsToScan.get(record.stream.toString());
						if (target == null) {
							target = new APSWrapper(record.stream, this.who);
							apsToScan.put(record.stream.toString(), target);
						}
						target.lookupSingle(record, properties);
					}
				}
			}			
			
			// Create list of used APS from "stream" type entries if no restriction on stream,id,document
			boolean restrictedByStream = properties.containsKey("stream");
			if (!restrictedByStream && !restrictedByDocument && !restrictedById) {
				scanForStreams(apsToScan);
			}
						
			
			// Query Records for non APS streams.  stream in [], time [] -> Records
			if (!restrictedById && !restrictedByDocument) {
				localQuery(result, properties, fields, fieldsFromDB, minTime, maxTime);			
			
				for (APSWrapper wrap : apsToScan.values()) {
					wrap.localQuery(result, properties, fields, fieldsFromDB, minTime, maxTime);
				}
			}
						
			
			// 7 Load Records from DB (in not all info present)
			if (fetchFromDB) {				
				for (Record record : result) {
					if (record.encrypted == null) {
						Record r2 = Record.getById(record._id, fieldsFromDB);
						record.encrypted = r2.encrypted;
						record.encryptedData = r2.encryptedData;	
						
						// TODO may be removed if encryption is working
						record.app = r2.app;		
						record.creator = r2.creator;	
						record.created = r2.created;
						record.name = r2.name;						
						record.description = r2.description;
						record.data = r2.data;
					}
					decryptRecord(record);
					if (!giveKey) record.clearSecrets();
				}
			} else {
			   if (!giveKey) for (Record record : result) record.clearSecrets();
			}
			
			if (fields.contains("id")) {
				for (Record record : result) record.id = record._id.toString()+"."+this.aps._id.toString();
			}
									
			// 8 Post filter records if necessary
			// 9 Order records
		
			return result;
		}
		
		private void addPermissionInternal(Record record, boolean withOwner) throws ModelException {
			// resolve time
			AccessPermissionSet withTime = aps;
			
			// resolve Format
			BasicDBObject obj = withTime.permissions.get(record.format);
			if (obj == null) {
				obj = new BasicDBObject();
				withTime.permissions.put(record.format, obj);
			}
			
			// add entry
			BasicDBObject entry = new BasicDBObject();
			entry.put("key", record.key);
			if (record.owner!=null && withOwner) entry.put("owner", record.owner);
			obj.put(record._id.toString(), entry);
						
		}
		
		public void addPermission(Record record, boolean withOwner) throws ModelException {
			addPermissionInternal(record, withOwner);
			
			// Store
			aps.setPermissions(aps.permissions);
		}
		
		public void addPermission(Collection<Record> records, boolean withOwner) throws ModelException {
			for (Record record : records) addPermissionInternal(record, withOwner);
			
			// Store
			aps.setPermissions(aps.permissions);
		}
		
		private void removePermissionInternal(Record record) throws ModelException {
			// resolve time
			AccessPermissionSet withTime = aps;
			
			// resolve Format
			BasicDBObject obj = withTime.permissions.get(record.format);
			if (obj == null) return;
						
			// remove entry			
			obj.remove(record._id.toString());
						
		}
		
		public void removePermission(Record record) throws ModelException {
			removePermissionInternal(record);
			
			// Store
			aps.setPermissions(aps.permissions);
		}
		
		public void removePermission(Collection<Record> records) throws ModelException {
			for (Record record : records) removePermissionInternal(record);
			
			// Store
			aps.setPermissions(aps.permissions);
		}
		
		
	}
	
	
	
	
}
