package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import org.bson.types.ObjectId;

import utils.auth.EncryptionNotSupportedException;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class APSCache {

	private Map<String, SingleAPSManager> cache;
	private ObjectId who;
	
	public APSCache(ObjectId who) {
		this.who = who;
		this.cache = new HashMap<String, SingleAPSManager>();
	}
	
	public ObjectId getOwner() {
		return who;
	}
	
	public SingleAPSManager getAPS(ObjectId apsId) throws InternalServerException {
		SingleAPSManager result = cache.get(apsId.toString());
		if (result == null) {
			result = new SingleAPSManager(new EncryptedAPS(apsId, who));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	public SingleAPSManager getAPS(ObjectId apsId, ObjectId owner) throws InternalServerException {
		SingleAPSManager result = cache.get(apsId.toString());
		if (result == null) {
			result = new SingleAPSManager(new EncryptedAPS(apsId, who, owner));
			cache.put(apsId.toString(), result);
		}	
		return result;
	}
	
	public SingleAPSManager getAPS(ObjectId apsId, byte[] unlockKey, ObjectId owner) throws AppException, EncryptionNotSupportedException {
		SingleAPSManager result = cache.get(apsId.toString());
		if (result == null) { 
			result = new SingleAPSManager(new EncryptedAPS(apsId, who, unlockKey, owner));
			if (!result.eaps.isOwner()) result.addAccess(Collections.<ObjectId>singleton(who));
			cache.put(apsId.toString(), result);
		}
		return result;
	}
	
	
}
