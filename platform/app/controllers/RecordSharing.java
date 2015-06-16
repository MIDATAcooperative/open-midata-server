package controllers;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map; 
import java.util.Random;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import utils.DateTimeUtils;
import utils.access.AccessLog;
import utils.access.EncryptedAPS;
import utils.access.EncryptionUtils;
import utils.auth.CodeGenerator;
import utils.auth.EncryptionNotSupportedException;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.LostUpdateException;
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
import models.enums.APSSecurityLevel;

public class RecordSharing {
	
	public static RecordSharing instance = new RecordSharing();		
	
	public final static Map<String, Object> FULLAPS = new HashMap<String, Object>();
	public final static Set<String> INTERNALIDONLY = Sets.create("_id");
	public final static Set<String> COMPLETE_META = Sets.create("id", "owner", "app", "creator", "created", "name", "format", "description");
	public final static Set<String> COMPLETE_DATA = Sets.create("id", "owner", "app", "creator", "created", "name", "format", "description", "data");
	public final static String STREAM_TYPE = "Stream";
	public final static String QUERY = "_query";
	
	public final static String KEY_ALGORITHM = "AES";
	public final static String CIPHER_ALGORITHM = "AES";
	
	public Random rand = new Random(System.currentTimeMillis());
	
	public ObjectId createPrivateAPS(ObjectId who, ObjectId proposedId) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
				
		newset._id = proposedId;		
		newset.permissions = new HashMap<String, BasicBSONObject>();
		newset.keys = new HashMap<String, byte[]>();
			
		SecretKey encryptionKey = EncryptionUtils.generateKey(KEY_ALGORITHM);	
		try {
		  newset.keys.put("owner", KeyManager.instance.encryptKey(who, encryptionKey.getEncoded()));
		  newset.security = APSSecurityLevel.HIGH;
		} catch (EncryptionNotSupportedException e) {
		  newset.security = APSSecurityLevel.NONE;
		  newset.keys.put("owner", who.toByteArray());
		}
			
		return new APSWrapper(newset, who).getId();
		
	}
	
	public ObjectId createAnonymizedAPS(ObjectId owner, ObjectId other, ObjectId proposedId) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		SecretKey encryptionKey = EncryptionUtils.generateKey(KEY_ALGORITHM);
		
		newset._id = proposedId;

		newset.permissions = new HashMap<String, BasicBSONObject>();
		newset.keys = new HashMap<String, byte[]>();
		
		
			try {
			  newset.keys.put("owner", KeyManager.instance.encryptKey(owner, encryptionKey.getEncoded()));
			  try {
				  newset.keys.put(other.toString(), KeyManager.instance.encryptKey(other, encryptionKey.getEncoded()));
				  newset.security = APSSecurityLevel.HIGH;
			  } catch (EncryptionNotSupportedException e2) {
				  throw new ModelException("NOT POSSIBLE ENCRYPTION REQUIRED");
			  }
			} catch (EncryptionNotSupportedException e) {
				newset.keys.put("owner", owner.toByteArray());		
				newset.keys.put(other.toString(), null);
				newset.security = APSSecurityLevel.NONE;
			}
			
				
		
		return new APSWrapper(newset, owner).getId();		
	}
	
	public ObjectId createAPSForRecord(ObjectId owner, ObjectId recordId, byte[] key, boolean direct) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		byte[] encryptionKey = key!=null ? key : EncryptionUtils.generateKey(KEY_ALGORITHM).getEncoded();
				
		newset._id = recordId;		
		newset.direct = direct;
		newset.permissions = new HashMap<String, BasicBSONObject>();
		newset.keys = new HashMap<String, byte[]>();
		
		if (key == null) {
		  newset.keys.put("owner", owner.toByteArray());
		  newset.security = APSSecurityLevel.NONE;
		} else {
			try {
		        newset.keys.put("owner", KeyManager.instance.encryptKey(owner, encryptionKey));
		        newset.security = direct ? APSSecurityLevel.MEDIUM : APSSecurityLevel.HIGH;
			} catch (EncryptionNotSupportedException e) {
				newset.keys.put("owner", owner.toByteArray());
				newset.security = APSSecurityLevel.NONE;
			}
		}
		
		return new APSWrapper(newset, owner).getId();		
	}
	
	public void shareAPS(ObjectId apsId, ObjectId ownerId, Set<ObjectId> targetUsers) throws ModelException {
		try {
			APSWrapper apswrapper = new APSWrapper(apsId, ownerId);

			if (apswrapper.eaps.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
				for (ObjectId targetUser : targetUsers) {
					apswrapper.eaps.setKey(targetUser.toString(), null);
				}	
			} else
			for (ObjectId targetUser : targetUsers) {
				try {
				    apswrapper.eaps.setKey(targetUser.toString(), KeyManager.instance.encryptKey(targetUser, apswrapper.eaps.getAPSKey().getEncoded()));
				} catch (EncryptionNotSupportedException e) {
					throw new ModelException("ENCRYPTION NOT POSSIBLE");
				}
			}
			apswrapper.eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
			  Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {}
			shareAPS(apsId, ownerId, targetUsers);
		}
	}
	
	public void unshareAPS(ObjectId apsId, ObjectId ownerId, Set<ObjectId> targetUsers) throws ModelException {
		try {
			APSWrapper apswrapper = new APSWrapper(apsId, ownerId);
									
			for (ObjectId targetUser : targetUsers) {
				apswrapper.eaps.removeKey(targetUser.toString());
			}
			apswrapper.eaps.updateKeys();			
		} catch (LostUpdateException e) {
			try {
			  Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {}
			unshareAPS(apsId, ownerId, targetUsers);
		}
	}

	public void share(ObjectId who, ObjectId fromAPS, ObjectId toAPS, Set<ObjectId> records, boolean withOwnerInformation) throws ModelException {
		
		APSWrapper source = new APSWrapper(fromAPS, who);
		APSWrapper target = new APSWrapper(toAPS, who);
		
		List<Record> recordEntries = source.lookup(CMaps.map("_id", records), Sets.create("_id", "key", "owner", "format"));
		
		target.addPermission(recordEntries, withOwnerInformation);
				
		// target.setPermissions(target.permissions);		
	}
	
	public void shareByQuery(ObjectId who, ObjectId fromAPS, ObjectId toAPS, Map<String, Object> query) throws ModelException {
		
		APSWrapper target = new APSWrapper(toAPS, who);
		query.put("aps", fromAPS.toString());
		
		target.setQuery(query);
	}
	
	public void unshare(ObjectId who, ObjectId apsId, Set<ObjectId> records) throws ModelException {
		
		APSWrapper source = new APSWrapper(apsId, who);		
		List<Record> recordEntries = source.lookup(CMaps.map("_id", records), Sets.create("_id", "format"));		
		source.removePermission(recordEntries);		
	}
	
	public Record createStream(ObjectId owner, ObjectId targetAPS, String name, boolean direct) throws ModelException {
		Record result = new Record();
		result._id = new ObjectId();
		result.name = name;
		result.owner = owner;
		result.direct = direct;
		result.format = STREAM_TYPE;
		result.created = DateTimeUtils.now();
		result.data = new BasicDBObject();
		
		addRecord(owner, result);
		return result;
	}
	
	public ObjectId getStreamByName(ObjectId who, ObjectId apsId, String name) throws ModelException {
		List<Record> result = list(who, apsId, CMaps.map("format", RecordSharing.STREAM_TYPE).map("name", name), RecordSharing.INTERNALIDONLY);
		if (result.isEmpty()) return null;
		return result.get(0)._id;
	}
	
	public Set<String> getStreamsByName(ObjectId who, ObjectId apsId, Collection<String> name) throws ModelException {
		return listRecordIds(who, apsId, CMaps.map("format", RecordSharing.STREAM_TYPE).map("name", name));		
	}
	
	private int getTimeFromDate(Date dt) {
		return (int) (dt.getTime() / 1000 / 60 / 60 / 24 / 7);
	}
	
		
	public void addDocumentRecord(ObjectId owner, Record record, Collection<Record> parts) throws ModelException {
	    byte[] key = addRecordIntern(owner, record, false);
	    if (key == null) throw new NullPointerException("no key");
	    for (Record rec : parts) {
	    	rec.document = record._id;
	    	rec.key = key;
	    	addRecordIntern(owner, rec, true);
	    }
	}
	
	public void addRecord(ObjectId owner, Record record) throws ModelException {
				
		addRecordIntern(owner, record, false);
		record.key = null;
	}
	
	private byte[] addRecordIntern(ObjectId owner, Record record, boolean documentPart) throws ModelException {
		StreamLayouter.instance.placeRecord(owner, owner, record);
		
		byte[] usedKey = null;
		record.time = getTimeFromDate(record.created); //System.currentTimeMillis() / 1000 / 60 / 60 / 24 / 7;
		
		record = record.clone();
		APSWrapper apswrapper;
		boolean apsDirect = false;
		
		if (record.owner.equals(record.creator)) record.creator = null;
		
		if (record.stream != null) {
		  apswrapper = new APSWrapper(record.stream, owner);
		  record.direct = apswrapper.eaps.isDirect();
		  
		} else {		
		  apswrapper = new APSWrapper(owner, owner);
		}
	
		if (record.format.equals(STREAM_TYPE)) {
			apsDirect = record.direct;
			record.stream = null;			
			record.direct = false;
		}
		
		if (apswrapper.eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || apswrapper.eaps.getSecurityLevel().equals(APSSecurityLevel.LOW)) {
			record.key = null;
			if (record.document != null) documentPart = true;
		} else if (record.direct) {
			record.key = apswrapper.eaps.getAPSKey().getEncoded();		
		} else if (!documentPart) {
			if (record.document != null) {
				List<Record> doc = apswrapper.lookup(CMaps.map("_id", record.document.toString()), Sets.create("key"));
				if (doc.size() == 1) record.key = doc.get(0).key;
				else throw new ModelException("Document not identified");
				documentPart = true;
			} else  record.key = EncryptionUtils.generateKey(KEY_ALGORITHM).getEncoded();
		}
	    usedKey = record.key;
	    
		try {
		    if (!documentPart) Search.add(record.owner, "record", record._id, record.name, record.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
		
		if (!record.direct && !documentPart) apswrapper.addPermission(record, false);
		
		apswrapper.encryptRecord(record);		
	    Record.add(record);	  
	    		
		if (record.format.equals(STREAM_TYPE)) {
			RecordSharing.instance.createAPSForRecord(owner, record._id, record.key, apsDirect);
		}
		
		return usedKey;		
	}
		
	
	public void addRecord2(Member owner, Record record) throws ModelException {
        record.time = getTimeFromDate(record.created); //System.currentTimeMillis() / 1000 / 60 / 60 / 24 / 7;
		
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
		record.key = EncryptionUtils.generateKey(KEY_ALGORITHM).getEncoded();
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
		boolean ok = StreamLayouter.instance.adjustQuery(who, apsId, properties);
		return ok ? apswrapper.lookup(properties, fields) : Collections.<Record>emptyList();		
	}
	
	public Record fetch(ObjectId who, RecordToken token) throws ModelException {
		return fetch(who, token, RecordSharing.COMPLETE_DATA);			
	}
	
	public Record fetch(ObjectId who, RecordToken token, Set<String> fields) throws ModelException {
		List<Record> result = list(who, new ObjectId(token.apsId), CMaps.map("_id", new ObjectId(token.recordId)), fields );
		if (result.isEmpty()) return null; else return result.get(0);				
	}
	
	public Record fetch(ObjectId who, ObjectId aps, ObjectId recordId) throws ModelException {
		List<Record> result = list(who, aps, CMaps.map("_id", recordId), RecordSharing.COMPLETE_DATA );
		if (result.isEmpty()) return null; else return result.get(0);				
	}
	
	public Set<String> listRecordIds(ObjectId who, ObjectId apsId) throws ModelException {
		return listRecordIds(who, apsId, RecordSharing.FULLAPS);		
	}
	
	public Set<String> listRecordIds(ObjectId who, ObjectId apsId, Map<String,Object> properties) throws ModelException {
		List<Record> result = list(who, apsId, properties, RecordSharing.INTERNALIDONLY);
		Set<String> ids = new HashSet<String>();
		for (Record record : result) ids.add(record._id.toString());
		return ids;
	}
			
	class APSWrapper {
		
		private EncryptedAPS eaps;		
		private APSWrapper queryAPS;
		
		//private String myFormat;
		//private int myMinTime;
		//private int myMaxTime;				
		
		APSWrapper(ObjectId apsId, ObjectId who) throws ModelException {
			this.eaps = new EncryptedAPS(apsId, who);			
		}
		
		APSWrapper(AccessPermissionSet aps, ObjectId who) throws ModelException {
			this.eaps = new EncryptedAPS(aps, who);			
		}
		
		public ObjectId getId() {
			return eaps.getId();
		}
				
		protected boolean lookupSingle(Record input, Map<String, Object> properties) throws ModelException {
			AccessLog.lookupSingle(getId(), input._id, properties);
			if (eaps.isDirect()) {
				input.key = eaps.getAPSKey().getEncoded();
				input.owner = eaps.getOwner();
				return true;
			}
			
			Map<String, BasicBSONObject> permissions = eaps.getPermissions();
            Map<String, BasicBSONObject> formats = null;			
			
			if (properties.containsKey("format")) {
				Object formatRestriction = properties.get("format");
				if (formatRestriction instanceof String) {
				  formats = new HashMap<String, BasicBSONObject>();
				  BasicBSONObject fObj = permissions.get((String) formatRestriction);
				  if (fObj == null) return false;
				  formats.put((String) formatRestriction, fObj);
				} else {
					formats = new HashMap<String, BasicBSONObject>();
					for (String format : (Iterable<String>) formatRestriction) {
						BasicBSONObject fObj = permissions.get(format);
						if (fObj != null) formats.put(format, fObj);
					}
				}
			} else {
				formats = permissions; 
			}
			AccessLog.logMap(formats);
			
			for (String format : formats.keySet()) {
				   BasicBSONObject map = formats.get(format);
				   if (map == null) continue;
				   BasicBSONObject target = (BasicBSONObject) map.get(input._id.toString());
				   if (target==null && input.document!=null) target = (BasicBSONObject) map.get(input.document.toString());
				   if (target!=null) {
					   Object k = target.get("key");
					   input.key = (k instanceof String) ? null : (byte[]) k; // Old version support
					   input.format = format;
					   if (input.owner == null) {
						   String owner = target.getString("owner");
						   if (owner!=null) input.owner = new ObjectId(owner); else input.owner = eaps.getOwner();
					   }	
					   AccessLog.identified(getId(), input._id);
					   return true;
				   }
			}
			
			return false;
		}
		
		protected void scanForStreams(Map<String, APSWrapper> apsToScan) throws ModelException {
			BasicBSONObject streams = eaps.getPermissions().get(STREAM_TYPE);
		  	if (streams != null) {
		  		for (String key : streams.keySet()) {
		  			if (!apsToScan.containsKey(key)) {
		  				APSWrapper streamWrapper = new APSWrapper(new ObjectId(key), eaps.getAccessor());
		  				apsToScan.put(key, streamWrapper);
		  			}
		  		}
		  	}
		}
		
		protected void scanStream(Map<String, APSWrapper> apsToScan, String id) throws ModelException {
			if (! apsToScan.containsKey(id)) {
				APSWrapper streamWrapper = new APSWrapper(new ObjectId(id), eaps.getAccessor());
  				apsToScan.put(id, streamWrapper);
			}
		}
		
		protected Record createRecordFromAPSEntry(String id, String format, BasicBSONObject entry, boolean withOwner) throws ModelException {
			Record record = new Record();
			record._id = new ObjectId(id);
			record.format = format;
			
			if (entry.get("key") instanceof String) record.key = null; // For old version support
			else record.key = (byte[]) entry.get("key");			
					
			if (withOwner) {
				String owner = entry.getString("owner");
			    if (owner!=null) record.owner = new ObjectId(owner); else record.owner = eaps.getOwner();
			}
							
			return record;
		}
		
		protected void encryptRecord(Record record) throws ModelException {
			if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || eaps.getSecurityLevel().equals(APSSecurityLevel.LOW)) {
				record.clearSecrets();
				return;
			}
			
			if (record.key == null) throw new ModelException("Cannot encrypt");
			
			SecretKey encKey = new SecretKeySpec(record.key, KEY_ALGORITHM);
			
			Map<String, Object> meta = new HashMap<String, Object>();
			meta.put("app", record.app);
			meta.put("creator", record.creator);
			meta.put("name", record.name);
			meta.put("created", record.created);
			meta.put("description", record.description);
			meta.put("tags", record.tags);
			meta.put("format", record.format);
			
			record.encrypted = EncryptionUtils.encryptBSON(encKey, new BasicBSONObject(meta));
			record.encryptedData = EncryptionUtils.encryptBSON(encKey, record.data);
														
			record.clearEncryptedFields();
		}
		
		protected void decryptRecord(Record record) throws ModelException {
			if (record.created != null) return;
			
			// Convert old format into new
			if (record.createdOld != null) {
				record.created = DateTimeUtils.toDate(record.createdOld);
				record.time = getTimeFromDate(record.created);
				Record.set(record._id, "created", record.created);
				Record.set(record._id, "time", record.time);
				//Record.set(record._id, "createdOld", null);
				return;
			}
			
			if (record.encrypted == null && record.encryptedData == null) return;
			if (record.key == null) AccessLog.decryptFailure(record._id);
			
			SecretKey encKey = new SecretKeySpec(record.key, KEY_ALGORITHM);
			if (record.encrypted != null) {
			    BSONObject meta = EncryptionUtils.decryptBSON(encKey, record.encrypted);
			    
			    record.app = (ObjectId) meta.get("app");
				record.creator = (ObjectId) meta.get("creator");
				record.name = (String) meta.get("name");				
				record.created = (Date) meta.get("created");
				record.description = (String) meta.get("description");
				String format = (String) meta.get("format");
				if (format!=null) record.format = format;
				record.tags = (Set<String>) meta.get("tags");				
			}
			
			if (record.encryptedData != null) {
				record.data = EncryptionUtils.decryptBSON(encKey, record.encryptedData);
			}
			
			//if (!record.encrypted.equals("enc"+record.key)) throw new ModelException("Cannot decrypt");
		}
		
		protected void addTimeRestriction(Map<String, Object> properties, int minTime, int maxTime) {
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
		
		private Map<String, Object> combineQuery(Map<String,Object> properties, Map<String,Object> query) throws ModelException {
			Object fq = properties.get("format");
			if (fq != null && fq.equals(STREAM_TYPE)) return properties;
			Map<String, Object> combined = new HashMap<String,Object>();
			combined.putAll(properties);
			for (String key : query.keySet()) {
				if (combined.containsKey(key)) {
					Object val1 = combined.get(key);
					Object val2 = query.get(key);
					if (val1.equals(val2)) continue;
					if (val1 instanceof Collection<?>) {
					 if (val2 instanceof Collection<?>) {
						((Collection<?>) val1).retainAll((Collection<?>) val2);
						if (((Collection<?>) val1).isEmpty()) return null;
					 } else {
						 if ( ((Collection<?>) val1).contains(val2)) {
							 combined.put(key, val2);
						 } else return null;
					 }
					} else {
						if (val2 instanceof Collection<?>) {
							if ( ((Collection<?>) val2).contains(val1)) continue;
							else return null;
						} else return null;
					}
					
				} else combined.put(key, query.get(key));
			}
			
			AccessLog.logMap(combined);
			return combined;
		}
		
		protected void localQuery(List<Record> result, Map<String, Object> properties, Set<String> fields, Set<String> fieldsFromDB, int minTime, int maxTime) throws ModelException {
			AccessLog.logLocalQuery(getId(), properties, fields);
			boolean withOwner = fields.contains("owner");
			boolean restrictedOnFormat = properties.containsKey("format");
			boolean streamOnlySearch = restrictedOnFormat && properties.get("format").equals(STREAM_TYPE);
			
			if (eaps.isDirect()) {
				Map<String, Object> query = new HashMap<String, Object>();
				query.put("stream", eaps.getId());
				query.put("direct", Boolean.TRUE);
				addTimeRestriction(query, minTime, maxTime);
				List<Record> directResult = new ArrayList<Record>(Record.getAll(query, fieldsFromDB));
				for (Record record : directResult) {
					record.key = eaps.getAPSKey().getEncoded();
					if (withOwner) record.owner = eaps.getOwner();					
				}
				result.addAll(directResult);
				return;
			}
									
			// 4 restricted by time? has APS time restriction? load other APS -> APS (4,5,6) APS LIST -> Records			
			
			// 5 Create list format -> Permission List (maybe load other APS)
			Map<String, BasicBSONObject> permissions = eaps.getPermissions();
			Map<String, BasicBSONObject> formats;			
			
			if (restrictedOnFormat) {
				Object formatRestriction = properties.get("format");
				if (formatRestriction instanceof String) {
				  formats = new HashMap<String, BasicBSONObject>();
				  formats.put((String) formatRestriction, permissions.get((String) formatRestriction));
				} else {
					formats = new HashMap<String, BasicBSONObject>();
					for (String format : (Iterable<String>) formatRestriction) {
						formats.put(format, permissions.get(format));
					}
				}
			} else {
				formats = permissions;
			}
			
			
			// 6 Each permission list : apply filters -> Records
			boolean restrictedById = properties.containsKey("_id");
			boolean restrictedByName = properties.containsKey("name");
			boolean restrictedByOwner = properties.containsKey("owner");
			if (restrictedById) {
				Object ids = properties.get("_id");
				if (ids instanceof ObjectId) {
					for (String format : formats.keySet()) {
						BasicBSONObject map = formats.get(format);
					   if (map != null) {
						   BasicBSONObject target = (BasicBSONObject) map.get(ids.toString());
						   if (target!=null) result.add(createRecordFromAPSEntry(ids.toString(), format, target, withOwner));
					   }
					}
				} else {
					for (ObjectId id : (Iterable<ObjectId>) ids) {
						for (String format : formats.keySet()) {
							   BasicBSONObject map = formats.get(format);
							   BasicBSONObject target = (BasicBSONObject) map.get(id.toString());
							   if (target!=null)
							   result.add(createRecordFromAPSEntry(id.toString(), format, target, withOwner));
							}
					}
				}
			} else if (restrictedByName) {
				
				Set<String> names = restrictionStrings(properties.get("name"));
				for (String format : formats.keySet()) {
					BasicBSONObject map = formats.get(format);
				    if (map != null) {
					    for (String id : map.keySet()) {
					    	BasicBSONObject target = (BasicBSONObject) map.get(id);
					    	String name = target.getString("name");
					    	if (name == null || names.contains(name)) result.add(createRecordFromAPSEntry(id , format, target, withOwner));
					    }
				    }
				}
			} else {
				for (String format : formats.keySet()) {
					if (!streamOnlySearch && format.equals(STREAM_TYPE)) continue;
					BasicBSONObject map = formats.get(format);
				    if (map != null) {
					    for (String id : map.keySet()) {
					    	BasicBSONObject target = (BasicBSONObject) map.get(id);
					    	result.add(createRecordFromAPSEntry(id , format, target, withOwner));
					    }
				    }
				}
			}			
		}
		
		private Set<String> restrictionStrings(Object val) {			
			if (val instanceof Collection<?>) {
				Set<String> results = new HashSet<String>();
				results.addAll((Collection<String>) val);
				return results;					
			} else if (val instanceof String) {
				Set<String> results = new HashSet<String>();
				results.add((String) val);
				return results;
			} else if (val instanceof ObjectId) {
				Set<String> results = new HashSet<String>();
				results.add(((ObjectId) val).toString());
				return results;
			}
			return null;			
		}
						
		List<Record> lookup(Map<String, Object> properties, Set<String> fields) throws ModelException {
						
			List<Record> result = new ArrayList<Record>();
			Map<String, APSWrapper> apsToScan = new HashMap<String, APSWrapper>();			
			
			// 1 If APS is from other server execute there (if APS redirection) (DB/API)
			
			
			// If APS is a query redirect with query
			if (eaps.getPermissions().containsKey(QUERY)) {
				BasicBSONObject query = eaps.getPermissions().get(QUERY);
				Map<String, Object> combined = combineQuery(properties, query);
				if (combined == null) {
					AccessLog.debug("combine empty:");
					AccessLog.logMap(properties);
					AccessLog.logMap(query);
					return new ArrayList<Record>();
				}
				if (queryAPS == null) {
					Object targetAPSId = query.get("aps");
					queryAPS = new APSWrapper(new ObjectId(targetAPSId.toString()), eaps.getAccessor());					
				}
				boolean ok = StreamLayouter.instance.adjustQuery(eaps.getAccessor(), queryAPS.getId(), combined);
				AccessLog.logQuery(this.getId(), properties,fields);
				AccessLog.debug("is okay?"+ok);
				return ok ? queryAPS.lookup(combined, fields) : Collections.<Record>emptyList();
			}
			
			AccessLog.logQuery(this.getId(), properties,fields);
			
			// Prepare
			boolean restrictedOnTime = properties.containsKey("created") || properties.containsKey("max-age");
			boolean restrictedOnCreator = properties.containsKey("creator");
			boolean restrictedOnFormat = properties.containsKey("format");
			boolean restrictedByDocument = properties.containsKey("document");
			boolean restrictedByPart = properties.containsKey("part");
			boolean restrictedByName = properties.containsKey("name");
			boolean streamOnlySearch = restrictedOnFormat && properties.get("format").equals(STREAM_TYPE);
			
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
            Set<String> fieldsFromDB = Sets.create("createdOld");
            if (fetchFromDB) fieldsFromDB.add("encrypted");
            if (fields.contains("data")) fieldsFromDB.add("encryptedData");
            
            // TODO Remove later
            if (fields.contains("data")) fieldsFromDB.add("data");
            if (fields.contains("app")) fieldsFromDB.add("app");
            if (fields.contains("format")) fieldsFromDB.add("format");
            if (fields.contains("creator") || restrictedOnCreator) fieldsFromDB.add("creator");
            if (fields.contains("created") || restrictedOnTime) fieldsFromDB.add("created");
            if (fields.contains("name")) fieldsFromDB.add("name");
            if (fields.contains("description")) fieldsFromDB.add("description");
            if (fields.contains("tags")) fieldsFromDB.add("tags");
            
            
			
				
			boolean giveKey = fields.contains("key");
			int minTime = 0;
			int maxTime = 0;
			Date minDate = null;
			Date maxDate = null;
			if (restrictedOnTime) {
				fieldsFromDB.add("time");
				if (properties.containsKey("max-age")) {
					Number maxAge = Long.parseLong(properties.get("max-age").toString());
					minDate = new Date(System.currentTimeMillis() - 1000 * maxAge.longValue());
					minTime = getTimeFromDate(minDate);
				}
				
				/*Object timeRestriction = properties.get("created");
				if (timeRestriction instanceof Map) {
				  Map<String, Object> timeMap = (Map<String, Object>) timeRestriction;
				  Object min = timeMap.get("$gt");
				  Object max = timeMap.get("$lt");
				  if (min!=null && min instanceof Number) {
					  minTime = 0;
				  }
				}*/
			}
			boolean postFilter = minDate != null || maxDate != null || restrictedOnCreator || (restrictedOnFormat && restrictedByDocument);
			
			// 2 Load Records from DB (if ID given) -> Records (possibly, encrypted) (if properties _id set )
			boolean restrictedById = properties.containsKey("_id");			
			if (restrictedById) {
				Map<String, Object> query = new HashMap<String, Object>();
				Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct");
				queryFields.addAll(fieldsFromDB);
				query.put("_id", properties.get("_id"));
				addTimeRestriction(query, minTime, maxTime);
				result = new ArrayList<Record>(Record.getAll(query, queryFields));
			}
		
			// Load Records from DB if document is given
			
			if (restrictedByDocument && !restrictedById) {
				Map<String, Object> query = new HashMap<String, Object>();
				Set<String> queryFields = Sets.create("stream", "time", "document", "part", "encrypted");
				queryFields.addAll(fieldsFromDB);
				query.put("document", /*new ObjectId("55686c8be4b08b543c12b847")*/ properties.get("document") );
				addTimeRestriction(query, minTime, maxTime);
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
							target = new APSWrapper(record.stream, eaps.getAccessor());
							apsToScan.put(record.stream.toString(), target);
						}
						target.lookupSingle(record, properties);
					}
				}
			}			
			
			// Create list of used APS from "stream" type entries if no restriction on stream,id,document
			boolean restrictedByStream = properties.containsKey("stream");
			if (!restrictedByStream && !restrictedByDocument && !restrictedById && !streamOnlySearch) {
				scanForStreams(apsToScan);
			}
			
			if (restrictedByStream) {
			  Set<String> streams = restrictionStrings(properties.get("stream"));
			  for (String streamId : streams) {
				 scanStream(apsToScan, streamId);
			  }
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
						if (minTime != 0 && r2.time < minTime) continue;
						if (maxTime != 0 && r2.time > maxTime) continue;
						record.encrypted = r2.encrypted;
						record.encryptedData = r2.encryptedData;	
						
						// TODO may be removed if encryption is working
						record.app = r2.app;		
						record.creator = r2.creator;	
						record.created = r2.created;
						record.name = r2.name;						
						record.description = r2.description;
						record.data = r2.data;
						
						record.createdOld = r2.createdOld;
					}
					decryptRecord(record);
					if (record.creator == null) record.creator = record.owner;
					if (!giveKey) record.clearSecrets();
				}
			} else {
			   if (!giveKey) for (Record record : result) record.clearSecrets();
			}
			
			if (fields.contains("id")) {
				for (Record record : result) record.id = record._id.toString()+"."+eaps.getId().toString();
			}
									
			// 8 Post filter records if necessary		
			Set<ObjectId> creators = null;
			Set<String> formats = null;
			if (restrictedOnCreator) {
				Object val = properties.get("creator");
				if (val instanceof Collection<?>) {
					creators = new HashSet<ObjectId>();
					for (Object obj : (Collection<?>) val) { creators.add(new ObjectId(obj.toString())); }
				} else {
					creators = new HashSet<ObjectId>();
					creators.add(new ObjectId(val.toString()));
				}
			}
			if (restrictedByDocument && restrictedOnFormat) {
				formats = restrictionStrings(properties.get("format"));				
			}
						
			if (postFilter) {
				List<Record> filteredResult = new ArrayList<Record>(result.size());
				for (Record record : result) {
					if (record.name == null) continue;
					if (minDate != null && record.created.before(minDate)) continue;
					if (maxDate != null && record.created.after(maxDate)) continue;
					if (creators != null && !creators.contains(record.creator)) continue;	
					if (formats != null && !formats.contains(record.format)) continue;
					filteredResult.add(record);
				}
				result = filteredResult;
			}
			// 9 Order records
		    Collections.sort(result);
		    if (properties.containsKey("limit")) {
		    	Object limitObj = properties.get("limit");
		    	int limit = Integer.parseInt(limitObj.toString());
		    	if (result.size() > limit) result = result.subList(0, limit);
		    }
			
		    AccessLog.debug("END Full Query, result size="+result.size());
		    
			return result;
		}
		
		private void addPermissionInternal(Record record, boolean withOwner) throws ModelException {
			// resolve time
			EncryptedAPS withTime = eaps;
			
			// resolve Format
			BasicBSONObject obj = withTime.getPermissions().get(record.format);
			if (obj == null) {
				obj = new BasicDBObject();
				withTime.getPermissions().put(record.format, obj);
			}
			
			// add entry
			BasicBSONObject entry = new BasicDBObject();
			entry.put("key", record.key);
			if (record.owner!=null && withOwner) entry.put("owner", record.owner);
			if (record.format.equals(STREAM_TYPE)) entry.put("name", record.name);
			obj.put(record._id.toString(), entry);
						
		}
						
		public void addPermission(Record record, boolean withOwner) throws ModelException {
			try {
				addPermissionInternal(record, withOwner);
				
				// Store
				eaps.savePermissions();
			} catch (LostUpdateException e) {
				recoverFromLostUpdate();
				addPermission(record,  withOwner);
			}
		}
		
		public void addPermission(Collection<Record> records, boolean withOwner) throws ModelException {
			try {
			for (Record record : records) addPermissionInternal(record, withOwner);
			
			// Store
			  eaps.savePermissions();
			} catch (LostUpdateException e) {
				recoverFromLostUpdate();
				addPermission(records,  withOwner);
			}
		}
		
		private void recoverFromLostUpdate() throws ModelException {
			try {
			   Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e) {};
			
			eaps.reload();			
		}
		
		private void removePermissionInternal(Record record) throws ModelException {
			// resolve time
			EncryptedAPS withTime = eaps;
			
			// resolve Format
			BasicBSONObject obj = withTime.getPermissions().get(record.format);
			if (obj == null) return;
						
			// remove entry			
			obj.remove(record._id.toString());
						
		}
		
		public void removePermission(Record record) throws ModelException {
			try {
			  removePermissionInternal(record);
			
			  // Store
			  eaps.savePermissions();
			} catch (LostUpdateException e) {
				recoverFromLostUpdate();
				removePermission(record);
			}
		}
		
		public void removePermission(Collection<Record> records) throws ModelException {
			try {
			  for (Record record : records) removePermissionInternal(record);
			
			  // Store
			  eaps.savePermissions();
			} catch (LostUpdateException e) {
				recoverFromLostUpdate();
				removePermission(records);
			}
		}
		
		public void setQuery(Map<String, Object> query) throws ModelException {
			try {
				eaps.getPermissions().put(QUERY, new BasicDBObject(query));
				eaps.savePermissions();
			} catch (LostUpdateException e) {
				recoverFromLostUpdate();
				setQuery(query);
			}
		}
		
		
	}
		
	
}
