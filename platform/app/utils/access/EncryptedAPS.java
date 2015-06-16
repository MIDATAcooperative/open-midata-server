package utils.access;

import java.util.Arrays;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import models.AccessPermissionSet;
import models.ModelException;
import models.enums.APSSecurityLevel;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.db.LostUpdateException;

import controllers.KeyManager;

public class EncryptedAPS {
	private AccessPermissionSet aps;
	private ObjectId apsId;
	private ObjectId who;
	private ObjectId owner;
	private SecretKey encryptionKey;
	private boolean isEncrypted = false;
	private boolean isValidated = false;
	
	public final static String KEY_ALGORITHM = "AES";
		
	public EncryptedAPS(ObjectId apsId, ObjectId who) throws ModelException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = null;
	}
	
	public EncryptedAPS(AccessPermissionSet aps, ObjectId who) throws ModelException {
		this.apsId = aps._id;
		this.aps = aps;
		this.who = who;
		this.owner = null;
		validate();
		isEncrypted = true;
		encodeAPS();
		if (!aps.security.equals(APSSecurityLevel.NONE)) this.aps.permissions = null;
		AccessPermissionSet.add(this.aps);
	}
	
	public boolean isLoaded() {
		return this.aps != null;
	}
	
	public boolean isOwner() throws ModelException {		
		if (apsId.equals(who)) return true;	
		if (!isValidated) validate();
		if (who.equals(owner)) return true;
		return false;
	}
	
	public ObjectId getOwner() throws ModelException {
		if (apsId.equals(who)) return who;
		if (!isValidated) validate();
		return owner;
	}
	
	public ObjectId getAccessor() {
		return who;
	}
	
	public boolean isDirect() throws ModelException {
		if (!isLoaded()) load();
		return aps.direct;
	}
	
	public ObjectId getId() {
		return apsId;
	}
	
	public APSSecurityLevel getSecurityLevel() throws ModelException {
		if (!isLoaded()) load();
		return aps.security;
	}
	
	public SecretKey getAPSKey() throws ModelException {
		if (!isValidated) validate();
		return encryptionKey;
	}
	
	public long getVersion() throws ModelException {
		if (!isLoaded()) load();
		return aps.version;
	}
	
	public void setKey(String name, byte[] key) throws ModelException {
		if (!isLoaded()) load();
		aps.keys.put(name,  key);
	}
	
	public void removeKey(String name) throws ModelException {
		if (!isLoaded()) load();
		aps.keys.remove(name);
	}
	
	public void updateKeys() throws ModelException, LostUpdateException {
		if (!isLoaded()) return;
		aps.updateKeys();
	}
	
	public byte[] getKey(String name) throws ModelException {
		if (!isLoaded()) load();
		return aps.keys.get(name);
	}
	
	public Map<String, BasicBSONObject> getPermissions() throws ModelException {
		if (!isValidated) validate();		
		return aps.permissions;
	}
	
	public void savePermissions() throws ModelException, LostUpdateException {
		if (!isLoaded() || !isValidated) return;
		if (isEncrypted) {
			encodeAPS();
			aps.updateEncrypted();
		} else {
			aps.updatePermissions();
		}
	}
	
	public void reload() throws ModelException {
		load();
	}
	
	private void load() throws ModelException {
		this.aps = AccessPermissionSet.getById(this.apsId);
		if (this.aps == null) throw new ModelException("APS does not exist.");
		isValidated = false;
	}
	
		
	private void validate() throws ModelException {
		if (!isLoaded()) load();
		AccessLog.apsAccess(aps._id, who);
		if (aps.keys == null) { isValidated = true; return;} // Old version support
		
		if (aps.security.equals(APSSecurityLevel.NONE)) {			
			encryptionKey = null;
			if (aps.keys.containsKey(who.toString())) return;
			if (aps.keys.get("owner") instanceof byte[]) {
				if (Arrays.equals(who.toByteArray(), aps.keys.get("owner"))) { this.owner = who; return; }
				throw new ModelException("APS not readable by user");
			} else this.owner = who; // Old version support			
		} else {		
			byte[] key = aps.keys.get(who.toString());
			if (key==null) { key = aps.keys.get("owner"); this.owner = who; } 
		    if (key==null /*|| ! key.startsWith("key"+who.toString())*/) throw new ModelException("APS not readable by user");
		    		 
		    byte[] decryptedKey = KeyManager.instance.decryptKey(who, key);
		    encryptionKey = new SecretKeySpec(decryptedKey, KEY_ALGORITHM);// SecretKeyFactory.getInstance(KEY_ALGORITHM).key.substring(key.indexOf(':'));
		    
		    decodeAPS();
		}
		isValidated = true;
	}
			
	private void decodeAPS() throws ModelException  {
		if (aps.permissions == null && aps.encrypted != null) {										
		    	aps.permissions = EncryptionUtils.decryptBSON(encryptionKey, aps.encrypted).toMap();
		    	
		    	if (aps.permissions == null) throw new NullPointerException();
		    	aps.encrypted = null;
		    	isEncrypted = true;				
	    }
	}
			
	private void encodeAPS() throws ModelException {
		if (aps.permissions != null && !aps.security.equals(APSSecurityLevel.NONE)) {											
		   aps.encrypted = EncryptionUtils.encryptBSON(encryptionKey, new BasicBSONObject(aps.permissions));				
	    }
	}
}
