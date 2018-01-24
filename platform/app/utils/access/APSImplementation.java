package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import models.APSNotExistingException;
import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.auth.KeyManager;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.stats.Stats;

/**
 * implementation of an access permission set.
 *
 */
class APSImplementation extends APS {

	public EncryptedAPS eaps;

	public final static String QUERY = "_query";
	public Random rand = new Random(System.currentTimeMillis());
	
	private List<DBRecord> cachedRecords;
	private final static Map<String, Object> NOTNULL = Collections.unmodifiableMap(Collections.singletonMap("$ne", null));

	public APSImplementation(EncryptedAPS eaps) {
		this.eaps = eaps;
	}

	public MidataId getId() {
		return eaps.getId();
	}
	
	public boolean isAccessible() throws AppException {
		return eaps.isAccessable();
	}
	
	public boolean isUsable() throws AppException {
		try {
		    return eaps.isAccessable();
		} catch (APSNotExistingException e) {
			return false;
		}
	}
	
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {
		return eaps.getSecurityLevel();
	}
	
	public void provideRecordKey(DBRecord record) throws AppException {
		record.direct = eaps.isDirect();
		
		if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || eaps.getSecurityLevel().equals(APSSecurityLevel.LOW)) {
			record.key = null;	
			record.security = APSSecurityLevel.NONE;
		} else if (record.direct) {
			record.key = eaps.getAPSKey() != null ? eaps.getAPSKey() : null;
			record.security = eaps.getSecurityLevel();
		} else {
			record.key = EncryptionUtils.generateKey();
			record.security = APSSecurityLevel.HIGH;
		}
		
	}
	
	public void provideAPSKeyAndOwner(byte[] unlock, MidataId owner) {
		eaps.provideAPSKeyAndOwner(unlock, owner);
	}
	
	public void touch() throws AppException {
		try {
		   eaps.touch();
		} catch (LostUpdateException e) {
			try {
				Stats.reportConflict();
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

	public void addAccess(Set<MidataId> targets) throws AppException, EncryptionNotSupportedException {
		merge();
		try {
			boolean changed = false;
			if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
				for (MidataId target : targets)
					if (!eaps.hasKey(target.toString())) {
						eaps.setKey(target.toString(), null);
						changed = true;
					}
			} else {
				for (MidataId target : targets)
					if (eaps.getKey(target.toString()) == null) {
						eaps.setKey(target.toString(), KeyManager.instance.encryptKey(target, eaps.getAPSKey()));
						changed = true;
					}
			}
			if (changed)
				eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				Stats.reportConflict();
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			addAccess(targets);
		}
	}

	public void addAccess(MidataId target, byte[] publickey) throws AppException, EncryptionNotSupportedException {
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
					eaps.setKey(target.toString(), KeyManager.instance.encryptKey(publickey, eaps.getAPSKey()));
					changed = true;
				}
			}
			if (changed)
				eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				Stats.reportConflict();
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			addAccess(target, publickey);
		}
	}		

	public void removeAccess(Set<MidataId> targets) throws InternalServerException {
		try {
			boolean changed = false;
			for (MidataId target : targets)
				if (eaps.hasKey(target.toString())) {
					eaps.removeKey(target.toString());
					changed = true;
				}
			if (changed)
				eaps.updateKeys();
		} catch (LostUpdateException e) {
			try {
				Stats.reportConflict();
				Thread.sleep(rand.nextInt(1000));
			} catch (InterruptedException e2) {
			}
			eaps.reload();
			removeAccess(targets);
		}
	}
	
	public boolean hasAccess(MidataId target) throws InternalServerException {		
		return eaps.hasKey(target.toString());
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
	
	public MidataId getStoredOwner() throws AppException {
		String ownerStr = (String) eaps.getPermissions().get("owner");
		if (ownerStr != null) return new MidataId(ownerStr); else return null;
	}

	@Override
	public List<DBRecord> query(Query q) throws AppException {		
		merge();		 
		// AccessLog.logLocalQuery(eaps.getId(), q.getProperties(),
		// q.getFields() );
		List<DBRecord> result = null;
		boolean withOwner = q.returns("owner");

		if (eaps.isDirect()) {
			if (q.isStreamOnlyQuery())
				return Collections.emptyList();
			
			if (q.restrictedBy("quick")) {					
				DBRecord record = (DBRecord) q.getProperties().get("quick");															
				record.key = eaps.getAPSKey() != null ? eaps.getAPSKey() : null;
				record.security = eaps.getSecurityLevel();
				if (withOwner)
					record.owner = eaps.getOwner(); 
							
				return Collections.singletonList(record);
			}
			
			Map<String, Object> query = new HashMap<String, Object>();
			query.put("stream", eaps.getId());
			if (!q.restrictedBy("deleted")) {
				query.put("encryptedData", NOTNULL);
			}
			boolean useCache = true;
			if (q.restrictedBy("_id")) {
				                				
				Set<MidataId> idRestriction = q.getMidataIdRestriction("_id");
								query.put("_id", idRestriction);
				useCache = false;
			}
			//query.put("direct", Boolean.TRUE);
			useCache = !q.addMongoTimeRestriction(query, false) && useCache;
			List<DBRecord> directResult;
			
			if (useCache && cachedRecords != null) {
				//result.addAll(cachedRecords);
				return Collections.unmodifiableList(cachedRecords);
			}
			
			directResult = DBRecord.getAllList(query, q.getFieldsFromDB());
			for (DBRecord record : directResult) {
				record.key = eaps.getAPSKey() != null ? eaps.getAPSKey() : null;
				record.security = eaps.getSecurityLevel();
				if (withOwner)
					record.owner = eaps.getOwner();
			}
			AccessLog.log("direct query stream=" + eaps.getId()+" #size="+directResult.size());
			if (useCache && withOwner) cachedRecords = directResult;
			//result.addAll(directResult);
			return Collections.unmodifiableList(directResult);
		}

		// 4 restricted by time? has APS time restriction? load other APS -> APS
		// (4,5,6) APS LIST -> Records

		// 5 Create list format -> Permission List (maybe load other APS)
		Map<String, Object> permissions = eaps.getPermissions();
		List<BasicBSONObject> rows = APSEntry.findMatchingRowsForQuery(permissions, q);
        if (rows == null) return Collections.emptyList();
        result = new ArrayList<DBRecord>();
		// 6 Each permission list : apply filters -> Records
		boolean restrictedById = q.restrictedBy("_id");
		if (restrictedById) {
			for (MidataId id : q.getMidataIdRestriction("_id")) {
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
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					BasicBSONObject target = (BasicBSONObject) entry.getValue();
					if (satisfies(target, q)) {
						result.add(createRecordFromAPSEntry(entry.getKey(), row, target, withOwner));
					}
				}
			}
		}
		//AccessLog.log("query APS=" + eaps.getId()+" #size="+result.size());
		return result;
	}
	
	
	
	@Override
	protected Iterator<DBRecord> iterator(Query q) throws AppException {
		List<DBRecord> result = query(q);
		return new APSIterator(result.iterator(), result.size(), getId());
	}

	static class APSIterator implements Iterator<DBRecord> {
		private Iterator<DBRecord> data;
		private int size;
		private MidataId aps;
		
		APSIterator(Iterator<DBRecord> data, int size, MidataId aps) {
			this.data = data;
			this.aps = aps;
			this.size = size;
		}

		@Override
		public boolean hasNext() {
			return data.hasNext();
		}

		@Override
		public DBRecord next() {
			return data.next();
		}

		@Override
		public String toString() {
			return "aps("+size+"@"+aps.toString()+")";
		}
		
		
		
	}

	protected boolean satisfies(BasicBSONObject entry, Query q) {
		if (q.getMinDateCreated() != null) {
			Date created = entry.getDate("created");
			if (created != null && created.before(q.getMinDateCreated()))
				return false;
		}
		if (q.getMaxDateCreated() != null) {
			Date created = entry.getDate("created");
			if (created != null && created.after(q.getMaxDateCreated()))
				return false;
		}
		return true;
	}

	private DBRecord createRecordFromAPSEntry(String id, BasicBSONObject row, BasicBSONObject entry, boolean withOwner) throws AppException {
		DBRecord record = new DBRecord();		
		
		record._id = new MidataId(id);		
		APSEntry.populateRecord(row, record);
		record.isStream = entry.getBoolean("s");
		record.isReadOnly = entry.getBoolean("ro");

		if (entry.get("key") instanceof String) {
			record.key = null; // For old version support
			record.security = APSSecurityLevel.NONE;
		} else {
			record.key = (byte[]) entry.get("key");
			record.security = record.key != null ? getSecurityLevel() : APSSecurityLevel.NONE;
		}

		if (withOwner || record.isStream) {
			String owner = entry.getString("owner");
			if (owner != null)
				record.owner = new MidataId(owner);
			else
				record.owner = eaps.getOwner();
		}

		record.meta.put("created", entry.getDate("created"));

		return record;
	}

	private void addPermissionInternal(DBRecord record, boolean withOwner) throws AppException {

		if (record.key == null && !record.security.equals(APSSecurityLevel.NONE))
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
		addHistory(record._id, record.isStream, false);

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
			Stats.reportConflict();
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
		if (result) {
			obj.remove(record._id.toString());				
		    if (obj.isEmpty()) APSEntry.cleanupRows(eaps.getPermissions());
		    addHistory(record._id, record.isStream, true);
		}
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
			boolean updated = false;
			for (DBRecord record : records)
				updated = removePermissionInternal(record) || updated;

			// Store
			if (updated) eaps.savePermissions();
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			removePermission(records);
		}
	}
		
	public void clearPermissions() throws AppException {
		try {
			eaps.getPermissions().put("p", new BasicDBList());		
			eaps.savePermissions();
		} catch (LostUpdateException e) {
			recoverFromLostUpdate();
			clearPermissions();
		}

		
	}
		
	
	//-----------
	
	protected void forceAccessibleSubset() throws AppException {
		if (eaps.findAndselectAccessibleSubset()) return; 

		AccessLog.logBegin("start creating accessible subset for user: "+eaps.getAccessor().toString()+" aps: "+eaps.getId().toString());		
		
		EncryptedAPS wrapper = eaps.createChild();		
	   for (String ckey : eaps.keyNames()) {
		   try {					 
			 if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
				if (ckey.equals("owner")) wrapper.setKey("owner", eaps.getOwner().toByteArray());
				else wrapper.setKey(ckey, null);
			 } else {
				MidataId person = ckey.equals("owner") ? eaps.getOwner() : new MidataId(ckey);
		        wrapper.setKey(ckey, KeyManager.instance.encryptKey(person, wrapper.getAPSKey()));
			 }
		   } catch (EncryptionNotSupportedException e) {}
		   
	   }
	   try {
		 if (wrapper.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
			wrapper.setKey(eaps.getAccessor().toString(), null);
		 } else wrapper.setKey(eaps.getAccessor().toString(), KeyManager.instance.encryptKey(eaps.getAccessor(), wrapper.getAPSKey()));
	   } catch (EncryptionNotSupportedException e) {}
								
		eaps.useAccessibleSubset(wrapper);
		
		AccessLog.logEnd("end creating accessible subset");
	}
	
	protected void merge() throws AppException {
		try {
		if (eaps.needsMerge()) {
			AccessLog.logBegin("begin merge:" + eaps.getId().toString());
			
			for (EncryptedAPS encsubaps : eaps.getAllUnmerged()) {													
				APSEntry.mergeAllInto(encsubaps.getPermissions(), eaps.getPermissions());
				mergeHistory(encsubaps);
			}
			
			eaps.clearUnmerged();
			eaps.savePermissions();
			AccessLog.logEnd("end merge");
		}
		} catch (LostUpdateException e) {
			eaps.reload();
			merge();		
		}
	}

	@Override
	public boolean isReady() throws AppException {
		return eaps.isLoaded();
	}
	
	private void addHistory(MidataId recordId, boolean isStream, boolean isRemove) throws AppException {
		BasicBSONList history = (BasicBSONList) eaps.getPermissions().get("_history");
		if (history != null) {
			BasicBSONObject newEntry = new BasicBSONObject();
			newEntry.put("r", recordId.toString());
			if (isStream) newEntry.put("s", isStream);
			newEntry.put("ts", System.currentTimeMillis());
			if (isRemove) newEntry.put("d", true);
			history.add(newEntry);
		}
	}
	
	private void mergeHistory(EncryptedAPS subaps) throws AppException {
		BasicBSONList history = (BasicBSONList) eaps.getPermissions().get("_history");
		BasicBSONList history2 = (BasicBSONList) subaps.getPermissions().get("_history");
		if (history != null && history2 != null) {
			history.addAll(history2);
		}
	}
    
	public List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException {
		BasicBSONList history = (BasicBSONList) eaps.getPermissions().get("_history");
		if (history == null) return Collections.emptyList();
		List<DBRecord> result = new ArrayList<DBRecord>();		
		for (Object entry1 : history) {
			BasicBSONObject entry = (BasicBSONObject) entry1;
			if (entry.getLong("ts") >= minUpd && (entry.containsField("d") == removes)) {
				DBRecord r = new DBRecord();
				r._id = new MidataId(entry.getString("r"));
				r.isStream = entry.containsField("s");				
				result.add(r);
			}
		}
		return result;
	}
	
	public boolean hasNoDirectEntries() throws AppException {
	   merge();
	   return ((BasicBSONList) eaps.getPermissions().get("p")).isEmpty();
	}


}
