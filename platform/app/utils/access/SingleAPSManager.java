package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;

import controllers.KeyManager;

import utils.DateTimeUtils;
import utils.auth.EncryptionNotSupportedException;
import utils.db.LostUpdateException;

import models.ModelException;
import models.Record;
import models.enums.APSSecurityLevel;

public class SingleAPSManager extends QueryManager {

	public EncryptedAPS eaps;
	
	public final static String QUERY = "_query";
	public Random rand = new Random(System.currentTimeMillis());
	
	public SingleAPSManager(EncryptedAPS eaps) {
		this.eaps = eaps;
	}
	
	public ObjectId getId() {
		return eaps.getId();
	}
	
	public void addAccess(Set<ObjectId> targets) throws ModelException,EncryptionNotSupportedException {
		try {
		  boolean changed = false;
		  if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
			  for (ObjectId target : targets)
		      if (!eaps.hasKey(target.toString())) {
		    	  eaps.setKey(target.toString(), null);
		    	  changed = true;
		      }
		  } else {
			  for (ObjectId target : targets)
			  if (eaps.getKey(target.toString()) == null) {			 
				 eaps.setKey(target.toString(), KeyManager.instance.encryptKey(target, eaps.getAPSKey().getEncoded()));
				 changed = true;
			  }
		  }
		  if (changed) eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				  Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {}
			eaps.reload();
			addAccess(targets);
		}
	}
	
	public void removeAccess(Set<ObjectId> targets) throws ModelException {
		try {
			  boolean changed = false;
			  for (ObjectId target : targets)
			  if (eaps.hasKey(target.toString())) {
				 eaps.removeKey(target.toString());
				 changed = true;
			  }
			  if (changed) eaps.updateKeys();
			} catch (LostUpdateException e) {
				try {
					  Thread.sleep(rand.nextInt(1000));
				} catch (InterruptedException e2) {}
				eaps.reload();
				removeAccess(targets);
			}
	}
	
	public void setMeta(String key, Map<String, Object> data) throws ModelException {
		try {			
			eaps.getPermissions().put(key, new BasicDBObject(data));
			eaps.savePermissions();
		} catch (LostUpdateException e) {
			eaps.reload();
			setMeta(key, data);
		}
	} 
	
	public void removeMeta(String key) throws ModelException {
		try {			
			if (eaps.getPermissions().containsKey(key)) {
				eaps.getPermissions().remove(key);
				eaps.savePermissions();
			}
		} catch (LostUpdateException e) {
			eaps.reload();
			removeMeta(key);
		}
	}
	
	

	public BasicBSONObject getMeta(String key) throws ModelException {
		return (BasicBSONObject) eaps.getPermissions().get(key);
	}
	
	public List<Record> query(Query q) throws ModelException {		
		//AccessLog.logLocalQuery(eaps.getId(), q.getProperties(), q.getFields() );
		List<Record> result = new ArrayList<Record>();
		boolean withOwner = q.returns("owner");	
		
		if (eaps.isDirect()) {
			if (q.isStreamOnlyQuery()) return result;
			
			AccessLog.debug("direct query stream="+eaps.getId());
			Map<String, Object> query = new HashMap<String, Object>();
			query.put("stream", eaps.getId());
			query.put("direct", Boolean.TRUE);
			q.addMongoTimeRestriction(query);
			List<Record> directResult = new ArrayList<Record>(Record.getAll(query, q.getFieldsFromDB()));
			for (Record record : directResult) {
				record.key = eaps.getAPSKey() != null ? eaps.getAPSKey().getEncoded() : null;
				if (withOwner) record.owner = eaps.getOwner();					
			}
			result.addAll(directResult);
			return result;
		}
								
		// 4 restricted by time? has APS time restriction? load other APS -> APS (4,5,6) APS LIST -> Records			
		
		// 5 Create list format -> Permission List (maybe load other APS)
		Map<String, Object> permissions = eaps.getPermissions();
		List<BasicBSONObject> rows = APSEntry.findMatchingRowsForQuery(permissions, q);
		        			
		// 6 Each permission list : apply filters -> Records
		boolean restrictedById = q.restrictedBy("_id");			
		if (restrictedById) {
			for (ObjectId id : q.getObjectIdRestriction("_id")) {
				for (BasicBSONObject row : rows) {
					   BasicBSONObject map = APSEntry.getEntries(row);					   
					   BasicBSONObject target = (BasicBSONObject) map.get(id.toString());
					   if (target!=null) {
						  result.add(createRecordFromAPSEntry(id.toString(), row, target, withOwner));
					   }					   
				}
			}			
		}  else {
			for (BasicBSONObject row : rows) {								
				BasicBSONObject map =APSEntry.getEntries(row);
				//AccessLog.debug("format:" + format+" map="+map.toString());			    
				for (String id : map.keySet()) {
				    	BasicBSONObject target = (BasicBSONObject) map.get(id);
				    	result.add(createRecordFromAPSEntry(id , row, target, withOwner));
				}			    
			}
		}		
		return result;
	}
	
	protected boolean lookupSingle(Record input, Query q) throws ModelException {
		//AccessLog.lookupSingle(eaps.getId(), input._id, q.getProperties());
		if (eaps.isDirect()) {
			input.key = eaps.getAPSKey() != null ? eaps.getAPSKey().getEncoded() : null;
			input.owner = eaps.getOwner();
			return true;
		}
		
		Map<String, Object> permissions = eaps.getPermissions();
		
		List<BasicBSONObject> rows = APSEntry.findMatchingRowsForQuery(permissions, q);
		        
		//AccessLog.logMap(formats);
		
		for (BasicBSONObject row : rows) {
			   BasicBSONObject map = APSEntry.getEntries(row);			   
			   BasicBSONObject target = (BasicBSONObject) map.get(input._id.toString());
			   if (target==null && input.document!=null) target = (BasicBSONObject) map.get(input.document.toString());
			   if (target!=null) {
				   Object k = target.get("key");
				   input.key = (k instanceof String) ? null : (byte[]) k; // Old version support
				   APSEntry.populateRecord(row, input);
				   input.isStream = target.getBoolean("s");
				   if (input.owner == null) {
					   String owner = target.getString("owner");
					   if (owner!=null) input.owner = new ObjectId(owner); else input.owner = eaps.getOwner();
				   }	
				   //AccessLog.identified(eaps.getId(), input._id);
				   return true;
			   }
		}
		
		return false;
	}
	
	private Record createRecordFromAPSEntry(String id, BasicBSONObject row, BasicBSONObject entry, boolean withOwner) throws ModelException {
		Record record = new Record();
		record._id = new ObjectId(id);
		APSEntry.populateRecord(row, record);		
		record.isStream = entry.getBoolean("s");
		
		if (entry.get("key") instanceof String) record.key = null; // For old version support
		else record.key = (byte[]) entry.get("key");			
				
		if (withOwner || record.isStream) {
			String owner = entry.getString("owner");
		    if (owner!=null) record.owner = new ObjectId(owner); else record.owner = eaps.getOwner();
		}
						
		return record;
	}
	
	protected void encryptRecord(Record record) throws ModelException {
		encryptRecord(record, eaps.getSecurityLevel());
	}
		
	public static void encryptRecord(Record record, APSSecurityLevel lvl) throws ModelException {
			if (lvl.equals(APSSecurityLevel.NONE) || lvl.equals(APSSecurityLevel.LOW)) {
				record.clearSecrets();
				return;
			}
			
			if (record.key == null) throw new ModelException("Cannot encrypt");
			
			SecretKey encKey = new SecretKeySpec(record.key, EncryptedAPS.KEY_ALGORITHM);
			
			Map<String, Object> meta = new HashMap<String, Object>();
			meta.put("app", record.app);
			meta.put("creator", record.creator);
			meta.put("name", record.name);
			meta.put("created", record.created);
			meta.put("description", record.description);
			meta.put("tags", record.tags);
			meta.put("format", record.format);
			meta.put("content", record.content);
			
			record.encrypted = EncryptionUtils.encryptBSON(encKey, new BasicBSONObject(meta));
			record.encryptedData = EncryptionUtils.encryptBSON(encKey, record.data);
														
			record.clearEncryptedFields();
		}
		
		protected static void decryptRecord(Record record) throws ModelException {
			if (record.created != null) return;
			
			// Convert old format into new
			if (record.createdOld != null) {
				record.created = DateTimeUtils.toDate(record.createdOld);
				record.time = Query.getTimeFromDate(record.created);
				Record.set(record._id, "created", record.created);
				Record.set(record._id, "time", record.time);
				//Record.set(record._id, "createdOld", null);
				return;
			}
			
			if (record.encrypted == null && record.encryptedData == null) return;
			if (record.key == null) AccessLog.decryptFailure(record._id);
			
			SecretKey encKey = new SecretKeySpec(record.key, EncryptedAPS.KEY_ALGORITHM);
			if (record.encrypted != null) {
				try {
			    BSONObject meta = EncryptionUtils.decryptBSON(encKey, record.encrypted);
			    
			    record.app = (ObjectId) meta.get("app");
				record.creator = (ObjectId) meta.get("creator");
				record.name = (String) meta.get("name");				
				record.created = (Date) meta.get("created");
				record.description = (String) meta.get("description");
				String format = (String) meta.get("format");
				if (format!=null) record.format = format;
				String content = (String) meta.get("content");
				if (content!=null) record.content = content;
				record.tags = (Set<String>) meta.get("tags");
				} catch (ModelException e) {
					AccessLog.debug("Error decrypting record: id="+record._id.toString());
					//throw e;
				}
			}
			
			if (record.encryptedData != null) {
				try {
				record.data = EncryptionUtils.decryptBSON(encKey, record.encryptedData);
				} catch (ModelException e) {
					AccessLog.debug("Error decrypting data of record: id="+record._id.toString());
					//throw e;
				}
			}
			
			//if (!record.encrypted.equals("enc"+record.key)) throw new ModelException("Cannot decrypt");
		}
						
		private void addPermissionInternal(Record record, boolean withOwner) throws ModelException {
						
			// resolve Format
			BasicBSONObject obj = APSEntry.findMatchingRowForRecord(eaps.getPermissions(), record, true);
			obj = APSEntry.getEntries(obj);	
			// add entry
			BasicBSONObject entry = new BasicDBObject();
			entry.put("key", record.key);
			if (record.isStream) entry.put("s", true);
			if (record.owner!=null && withOwner) entry.put("owner", record.owner);
			//if (record.format.equals(Query.STREAM_TYPE)) entry.put("name", record.name);
			obj.put(record._id.toString(), entry);
						
		}
						
		public void addPermission(Record record, boolean withOwner) throws ModelException {
			try {
				addPermissionInternal(record, withOwner);								
				eaps.savePermissions();
			} catch (LostUpdateException e) {
				recoverFromLostUpdate();
				addPermission(record,  withOwner);
			}
		}
		
		public void addPermission(Collection<Record> records, boolean withOwner) throws ModelException {
			try {
			   for (Record record : records) addPermissionInternal(record, withOwner);					
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
		
		private boolean removePermissionInternal(Record record) throws ModelException {
						
			// resolve Format
			BasicBSONObject obj = APSEntry.findMatchingRowForRecord(eaps.getPermissions(), record, false);
			if (obj == null) return false;
			obj = APSEntry.getEntries(obj);	
			// remove entry			
			boolean result = obj.containsField(record._id.toString());
			obj.remove(record._id.toString());

			return result;
		}
		
		public void removePermission(Record record) throws ModelException {
			try {
			  boolean success = removePermissionInternal(record);
			
			  // Store
			  if (success) eaps.savePermissions();
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
}
