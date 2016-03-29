package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import models.AccessPermissionSet;

import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * caches access permission sets during a request. No inter-request caching. 
 *
 */
class APSCache {

	private Map<String, APS> cache;
	private ObjectId ownerId;
	
	public APSCache(ObjectId who) {
		this.ownerId = who;
		this.cache = new HashMap<String, APS>();
	}
	
	public ObjectId getOwner() {
		return ownerId;
	}
	
	public boolean hasAPS(ObjectId apsId) throws AppException {
		APS result = cache.get(apsId.toString());
		if (result == null) return false;
		return result.isReady();
	}
	
	public APS getAPS(ObjectId apsId) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public APS getAPS(ObjectId apsId, ObjectId owner) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId, owner));
			cache.put(apsId.toString(), result);
		}	
		return result;
	}
	
	public APS getAPS(ObjectId apsId, byte[] unlockKey, ObjectId owner) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId, unlockKey, owner));
			if (!result.isAccessible()) {
				AccessLog.log("Adding missing access for "+ownerId.toString()+" APS:"+apsId.toString());
				result.addAccess(Collections.<ObjectId>singleton(ownerId));
			}
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public APS getAPS(ObjectId apsId, byte[] unlockKey, ObjectId owner, AccessPermissionSet set) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId, unlockKey, owner, set));
			if (!result.isAccessible()) result.addAccess(Collections.<ObjectId>singleton(ownerId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public void addAPS(APS aps) {		
		cache.put(aps.getId().toString(), aps);
	}
	
	
}
