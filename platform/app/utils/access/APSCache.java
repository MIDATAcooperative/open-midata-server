package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import org.bson.types.ObjectId;

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
			if (!result.isAccessible()) result.addAccess(Collections.<ObjectId>singleton(ownerId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	
}
