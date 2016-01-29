package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import models.Record;
import models.enums.APSSecurityLevel;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.auth.EncryptionNotSupportedException;
import utils.auth.KeyManager;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import com.mongodb.BasicDBObject;

/**
 * implementation of an access permission set.
 *
 */
class APSImplementation extends APS {

	public EncryptedAPS eaps;

	public final static String QUERY = "_query";
	public Random rand = new Random(System.currentTimeMillis());

	public APSImplementation(EncryptedAPS eaps) {
		this.eaps = eaps;
	}

	public ObjectId getId() {
		return eaps.getId();
	}
	
	public boolean isAccessible() throws AppException {
		return eaps.isAccessable();
	}
	
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {
		return eaps.getSecurityLevel();
	}
	
	public void provideRecordKey(DBRecord record) throws AppException {
		record.direct = eaps.isDirect();
		
		if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || eaps.getSecurityLevel().equals(APSSecurityLevel.LOW)) {
			record.key = null;			
		} else if (record.direct) {
			record.key = eaps.getAPSKey() != null ? eaps.getAPSKey().getEncoded() : null;		
		} else {
			record.key = EncryptionUtils.generateKey(EncryptionUtils.KEY_ALGORITHM).getEncoded();
		}
		
	}
	
	public void touch() throws AppException {
		try {
		   eaps.touch();
		} catch (LostUpdateException e) {
			try {
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			touch();
		}
	}
	
	public long getLastChanged() throws AppException {
		return eaps.getVersion();
	}

	public void addAccess(Set<ObjectId> targets) throws AppException, EncryptionNotSupportedException {
		merge();
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
			if (changed)
				eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			addAccess(targets);
		}
	}

	public void addAccess(ObjectId target, byte[] publickey) throws AppException, EncryptionNotSupportedException {
		merge();
		try {
			boolean changed = false;
			if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
				if (!eaps.hasKey(target.toString())) {
					eaps.setKey(target.toString(), null);
					changed = true;
				}
			} else {
				if (eaps.getKey(target.toString()) == null) {
					eaps.setKey(target.toString(), KeyManager.instance.encryptKey(publickey, eaps.getAPSKey().getEncoded()));
					changed = true;
				}
			}
			if (changed)
				eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			addAccess(target, publickey);
		}
	}		

	public void removeAccess(Set<ObjectId> targets) throws InternalServerException {
		try {
			boolean changed = false;
			for (ObjectId target : targets)
				if (eaps.hasKey(target.toString())) {
					eaps.removeKey(target.toString());
					changed = true;
				}
			if (changed)
				eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			removeAccess(targets);
		}
	}

	public void setMeta(String key, Map<String, Object> data) throws AppException {
		try {
			eaps.getPermissions().put(key, new BasicDBObject(data));
			eaps.savePermissions();
		} catch (LostUpdateException e) {
			eaps.reload();
			setMeta(key, data);
		}
	}

	public void removeMeta(String key) throws AppException {
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

	public BasicBSONObject getMeta(String key) throws AppException {
		return (BasicBSONObject) eaps.getPermissions().get(key);
	}

	public List<DBRecord> query(Query q) throws AppException {
		merge();
		// AccessLog.logLocalQuery(eaps.getId(), q.getProperties(),
		// q.getFields() );
		List<DBRecord> result = new ArrayList<DBRecord>();
		boolean withOwner = q.returns("owner");

		if (eaps.isDirect()) {
			if (q.isStreamOnlyQuery())
				return result;

			
			Map<String, Object> query = new HashMap<String, Object>();
			query.put("stream", eaps.getId());
			query.put("direct", Boolean.TRUE);
			q.addMongoTimeRestriction(query);
			List<DBRecord> directResult = new ArrayList<DBRecord>(DBRecord.getAll(query, q.getFieldsFromDB()));
			for (DBRecord record : directResult) {
				record.key = eaps.getAPSKey() != null ? eaps.getAPSKey().getEncoded() : null;
				if (withOwner)
					record.owner = eaps.getOwner();
			}
			AccessLog.debug("direct query stream=" + eaps.getId()+" #size="+directResult.size());
			result.addAll(directResult);
			return result;
		}

		// 4 restricted by time? has APS time restriction? load other APS -> APS
		// (4,5,6) APS LIST -> Records

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
					if (target != null && satisfies(target, q)) {
						result.add(createRecordFromAPSEntry(id.toString(), row, target, withOwner));
					}
				}
			}
		} else {
			for (BasicBSONObject row : rows) {
				BasicBSONObject map = APSEntry.getEntries(row);
				// AccessLog.debug("format:" + format+" map="+map.toString());
				for (String id : map.keySet()) {
					BasicBSONObject target = (BasicBSONObject) map.get(id);
					if (satisfies(target, q)) {
						result.add(createRecordFromAPSEntry(id, row, target, withOwner));
					}
				}
			}
		}
		return result;
	}

	protected boolean satisfies(BasicBSONObject entry, Query q) {
		if (q.getMinDate() != null) {
			Date created = entry.getDate("created");
			if (created != null && created.before(q.getMinDate()))
				return false;
		}
		if (q.getMaxDate() != null) {
			Date created = entry.getDate("created");
			if (created != null && created.after(q.getMinDate()))
				return false;
		}
		return true;
	}

	protected boolean lookupSingle(DBRecord input, Query q) throws AppException {
		// AccessLog.lookupSingle(eaps.getId(), input._id, q.getProperties());
		if (eaps.isDirect()) {
			input.key = eaps.getAPSKey() != null ? eaps.getAPSKey().getEncoded() : null;
			input.owner = eaps.getOwner();
			return true;
		}		
		
		Map<String, Object> permissions = eaps.getPermissions();

		List<BasicBSONObject> rows = APSEntry.findMatchingRowsForQuery(permissions, q);

		// AccessLog.logMap(formats);

		for (BasicBSONObject row : rows) {
			BasicBSONObject map = APSEntry.getEntries(row);
			//AccessLog.debug(getId()+":"+row.toString());
			BasicBSONObject target = (BasicBSONObject) map.get(input._id.toString());
			if (target == null && input.document != null)
				target = (BasicBSONObject) map.get(input.document.toString());
			if (target != null) {
				Object k = target.get("key");
				input.key = (k instanceof String) ? null : (byte[]) k; // Old
																		// version
																		// support
				APSEntry.populateRecord(row, input);
				input.isStream = target.getBoolean("s");
				if (input.owner == null) {
					String owner = target.getString("owner");
					if (owner != null)
						input.owner = new ObjectId(owner);
					else
						input.owner = eaps.getOwner();
				}
				// AccessLog.identified(eaps.getId(), input._id);
				return true;
			}
		}

		return false;
	}

	private DBRecord createRecordFromAPSEntry(String id, BasicBSONObject row, BasicBSONObject entry, boolean withOwner) throws AppException {
		DBRecord record = new DBRecord();		
		
		record._id = new ObjectId(id);
		APSEntry.populateRecord(row, record);
		record.isStream = entry.getBoolean("s");
		record.isReadOnly = entry.getBoolean("ro");

		if (entry.get("key") instanceof String)
			record.key = null; // For old version support
		else
			record.key = (byte[]) entry.get("key");

		if (withOwner || record.isStream) {
			String owner = entry.getString("owner");
			if (owner != null)
				record.owner = new ObjectId(owner);
			else
				record.owner = eaps.getOwner();
		}

		record.meta.put("created", entry.getDate("created"));

		return record;
	}

	private void addPermissionInternal(DBRecord record, boolean withOwner) throws AppException {

		if (record.key == null)
			throw new InternalServerException("error.internal", "Record with NULL key: Record:" + record._id.toString() + "/" + record.isStream);
		// resolve Format
		BasicBSONObject obj = APSEntry.findMatchingRowForRecord(eaps.getPermissions(), record, true);
		obj = APSEntry.getEntries(obj);
		// add entry
		BasicBSONObject entry = new BasicDBObject();
		entry.put("key", record.key);
		if (record.isStream) {
			entry.put("s", true);
		}
		if (record.isReadOnly && !record.isStream) throw new InternalServerException("error.internal", "readonly only supported for streams!!");
		if (record.isReadOnly) { 
			entry.put("ro", true);
		}
		if (record.owner != null && withOwner) {
			entry.put("owner", record.owner.toString());
		}
		if (!record.isStream) {
			entry.put("created", record.meta.get("created"));
		}
		// if (record.format.equals(Query.STREAM_TYPE)) entry.put("name",
		// record.name);
		obj.put(record._id.toString(), entry);

	}

	public void addPermission(DBRecord record, boolean withOwner) throws AppException {
		try {
			forceAccessibleSubset();
			addPermissionInternal(record, withOwner);
			eaps.savePermissions();
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			addPermission(record, withOwner);
		}
	}

	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		try {
			forceAccessibleSubset();
			for (DBRecord record : records)
				addPermissionInternal(record, withOwner);
			eaps.savePermissions();
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			addPermission(records, withOwner);
		}
	}

	private void recoverFromLostUpdate() throws InternalServerException {
		try {
			Thread.sleep(rand.nextInt(1000));
		} catch (InterruptedException e) {
		}
		;

		eaps.reload();
	}

	private boolean removePermissionInternal(DBRecord record) throws AppException {

		// resolve Format
		BasicBSONObject obj = APSEntry.findMatchingRowForRecord(eaps.getPermissions(), record, false);
		if (obj == null)
			return false;
		obj = APSEntry.getEntries(obj);
		// remove entry
		boolean result = obj.containsField(record._id.toString());
		obj.remove(record._id.toString());

		return result;
	}

	public boolean removePermission(DBRecord record) throws AppException {
		try {
			boolean success = removePermissionInternal(record);

			// Store
			if (success)
				eaps.savePermissions();
			return success;
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			return removePermission(record);
		}
	}

	public void removePermission(Collection<DBRecord> records) throws AppException {
		try {
			for (DBRecord record : records)
				removePermissionInternal(record);

			// Store
			eaps.savePermissions();
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			removePermission(records);
		}
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q) throws InternalServerException {
		return records;
	}

	@Override
	protected List<DBRecord> lookup(List<DBRecord> input, Query q) throws AppException {
		merge();
		List<DBRecord> filtered = new ArrayList<DBRecord>(input.size());
		for (DBRecord record : input) {
			if (lookupSingle(record, q)) {
				filtered.add(record);
			}
		}
		return filtered;
	}
	
	//-----------
	
	protected void forceAccessibleSubset() throws AppException {
		if (eaps.findAndselectAccessibleSubset()) return; 

		AccessLog.debug("creating accessible subset for user: "+eaps.getAccessor().toString()+" aps: "+eaps.getId().toString());		
		
		EncryptedAPS wrapper = eaps.createChild();		
	   for (String ckey : eaps.keyNames()) {
		   try {					 
			 if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
				if (ckey.equals("owner")) wrapper.setKey("owner", eaps.getOwner().toByteArray());
				else wrapper.setKey(ckey, null);
			 } else {
				ObjectId person = ckey.equals("owner") ? eaps.getOwner() : new ObjectId(ckey);
		        wrapper.setKey(ckey, KeyManager.instance.encryptKey(person, wrapper.getAPSKey().getEncoded()));
			 }
		   } catch (EncryptionNotSupportedException e) {}
		   
	   }
	   try {
		 if (wrapper.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
			wrapper.setKey(eaps.getAccessor().toString(), null);
		 } else wrapper.setKey(eaps.getAccessor().toString(), KeyManager.instance.encryptKey(eaps.getAccessor(), wrapper.getAPSKey().getEncoded()));
	   } catch (EncryptionNotSupportedException e) {}
								
		eaps.useAccessibleSubset(wrapper);        
	}
	
	protected void merge() throws AppException {
		try {
		if (eaps.needsMerge()) {
			AccessLog.debug("merge:" + eaps.getId().toString());
			
			for (EncryptedAPS encsubaps : eaps.getAllUnmerged()) {													
				APSEntry.mergeAllInto(encsubaps.getPermissions(), eaps.getPermissions());								
			}
			
			eaps.clearUnmerged();
			eaps.savePermissions();
		}
		} catch (LostUpdateException e) {
			eaps.reload();
			merge();		
		}
	}



}
