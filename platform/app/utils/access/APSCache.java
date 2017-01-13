package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.AccessPermissionSet;
import models.MidataId;
import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * caches access permission sets during a request. No inter-request caching. 
 *
 */
class APSCache {

	private Map<String, APS> cache;
	private Map<String, APSCache> subcache;
	private MidataId ownerId;
	
	public APSCache(MidataId who) {
		this.ownerId = who;
		this.cache = new HashMap<String, APS>();
	}
	
	public MidataId getOwner() {
		return ownerId;
	}
	
	public boolean hasAPS(MidataId apsId) throws AppException {
		APS result = cache.get(apsId.toString());
		if (result == null) return false;
		return result.isReady();
	}
	
	public APS getAPS(MidataId apsId) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public APS getAPS(MidataId apsId, MidataId owner) throws InternalServerException {
		APS result = cache.get(apsId.toString());
		if (result == null) {
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId, owner));
			cache.put(apsId.toString(), result);
		}	
		return result;
	}
	
	public APS getAPS(MidataId apsId, byte[] unlockKey, MidataId owner) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId, unlockKey, owner));
			if (!result.isAccessible()) {
				AccessLog.log("Adding missing access for "+ownerId.toString()+" APS:"+apsId.toString());
				result.addAccess(Collections.<MidataId>singleton(ownerId));
			}
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public APS getAPS(MidataId apsId, byte[] unlockKey, MidataId owner, AccessPermissionSet set) throws AppException, EncryptionNotSupportedException {
		APS result = cache.get(apsId.toString());
		if (result == null) { 
			result = new APSImplementation(new EncryptedAPS(apsId, ownerId, unlockKey, owner, set));
			if (!result.isAccessible()) result.addAccess(Collections.<MidataId>singleton(ownerId));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public void addAPS(APS aps) {		
		cache.put(aps.getId().toString(), aps);
	}

	public APSCache getSubCache(MidataId group) {
		if (subcache == null) {
			subcache = new HashMap<String, APSCache>();
		}
		APSCache result = subcache.get(group.toString());
		if (result == null) {
			result = new APSCache(group);
			subcache.put(group.toString(), result);
		}
		return result;
	}
	
	
}
