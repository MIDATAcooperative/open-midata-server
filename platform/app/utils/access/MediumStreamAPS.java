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
import utils.access.APSImplementation.APSIterator;
import utils.auth.EncryptionNotSupportedException;
import utils.auth.KeyManager;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.stats.Stats;

public class MediumStreamAPS extends APS {
	
	private MidataId apsId;	
	private MidataId owner;
	private byte[] encryptionKey;		
	private List<DBRecord> cachedRecords;
	private final static Map<String, Object> NOTNULL = Collections.unmodifiableMap(Collections.singletonMap("$ne", null));	

	public MediumStreamAPS(MidataId apsId, MidataId owner, byte[] encryptionKey) {
		if (owner == null) throw new NullPointerException();
		this.apsId = apsId;
		this.owner = owner;
		this.encryptionKey = encryptionKey;
	}

	public MidataId getId() {
		return apsId;
	}
	
	public boolean isAccessible() throws AppException {
		return true;
	}
	
	public boolean isUsable() throws AppException {
		return true;
	}
	
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {
		return APSSecurityLevel.MEDIUM;
	}
	
	public void provideRecordKey(DBRecord record) throws AppException {
		record.direct = true;
		record.security = APSSecurityLevel.MEDIUM;
		record.key = encryptionKey;				
	}
	
	public void provideAPSKeyAndOwner(byte[] unlock, MidataId owner) {
		this.owner = owner;
	}
	
	public void touch() throws AppException {
		
	}
	
	public long getLastChanged() throws AppException {
		return 0;
	}

	public void addAccess(Set<MidataId> targets) throws AppException, EncryptionNotSupportedException {
	}

	public void addAccess(MidataId target, byte[] publickey) throws AppException, EncryptionNotSupportedException {
	}		

	public void removeAccess(Set<MidataId> targets) throws InternalServerException {	
	}
	
	public boolean hasAccess(MidataId target) throws InternalServerException {		
		return true;
	}

	public void setMeta(String key, Map<String, Object> data) throws AppException {
		throw new NullPointerException();
	}

	public void removeMeta(String key) throws AppException {
		throw new NullPointerException();
	}

	public BasicBSONObject getMeta(String key) throws AppException {
		throw new NullPointerException();
	}
	
	public MidataId getStoredOwner() throws AppException {
		return owner;		
	}

	@Override
	public List<DBRecord> query(Query q) throws AppException {		
		
		List<DBRecord> result = null;
		boolean withOwner = q.returns("owner");
		
		if (q.isStreamOnlyQuery()) return Collections.emptyList();
			
		if (q.restrictedBy("quick")) {					
			DBRecord record = (DBRecord) q.getProperties().get("quick");															
			record.key = encryptionKey;
			record.security = APSSecurityLevel.MEDIUM;
			if (withOwner) record.owner = owner; 
							
			return Collections.singletonList(record);
		}
			
		Map<String, Object> query = new HashMap<String, Object>();
		query.put("stream", apsId);
		if (!q.restrictedBy("deleted")) {
			query.put("encryptedData", NOTNULL);
		}
		boolean useCache = true;
		if (q.restrictedBy("_id")) {
				                				
			Set<MidataId> idRestriction = q.getMidataIdRestriction("_id");
							query.put("_id", idRestriction);
			useCache = false;
		}
			
		useCache = !q.addMongoTimeRestriction(query, false) && useCache;
		List<DBRecord> directResult;
			
		if (useCache && cachedRecords != null) {
				return Collections.unmodifiableList(cachedRecords);
		}
			
		directResult = DBRecord.getAllList(query, q.getFieldsFromDB());
		for (DBRecord record : directResult) {
			record.key = encryptionKey;
			record.security = APSSecurityLevel.MEDIUM;
			if (withOwner) record.owner = owner;
		}
		AccessLog.log("direct query stream=" + apsId+" #size="+directResult.size());
			
		// Disabled: Produces wrong results first call to Observation/$lastn after first record inserted
		// if (useCache && withOwner) cachedRecords = directResult;
			
		return Collections.unmodifiableList(directResult);
				//AccessLog.log("query APS=" + eaps.getId()+" #size="+result.size());		
	}
	
		
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		List<DBRecord> result = query(q);
		return ProcessingTools.dbiterator("aps({id:"+getId()+", size:"+result.size()+"})", result.iterator());
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
	

	public void addPermission(DBRecord record, boolean withOwner) throws AppException {
		
	}

	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		
	}

		
	public boolean removePermission(DBRecord record) throws AppException {
		return false;
	}

	public void removePermission(Collection<DBRecord> records) throws AppException {
		
	}
		
	public void clearPermissions() throws AppException {		
		
	}
			

	@Override
	public boolean isReady() throws AppException {
		return true;
	}
	    
	public List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException {
		return null;
	}
	
	public boolean hasNoDirectEntries() throws AppException {
	   return true;
	}


}
