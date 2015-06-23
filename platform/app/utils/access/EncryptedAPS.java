package utils.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import models.AccessPermissionSet;
import models.ModelException;
import models.enums.APSSecurityLevel;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.auth.EncryptionNotSupportedException;
import utils.db.LostUpdateException;

import controllers.KeyManager;

public class EncryptedAPS {
	private AccessPermissionSet aps;
	private ObjectId apsId;
	private ObjectId who;
	private ObjectId owner;
	private SecretKey encryptionKey;	
	private boolean isValidated = false;
	private boolean keyProvided = false;
	private boolean notStored = false;
	private List<EncryptedAPS> sublists;
	
	public final static String KEY_ALGORITHM = "AES";
		
	public EncryptedAPS(ObjectId apsId, ObjectId who) throws ModelException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = null;
	}
	
	public EncryptedAPS(ObjectId apsId, ObjectId who, byte[] enckey, boolean isOwner) throws ModelException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = isOwner ? who : null;
		this.encryptionKey = (enckey != null) ? new SecretKeySpec(enckey, KEY_ALGORITHM) : null;
		keyProvided = true;
	}
			
	public EncryptedAPS(ObjectId apsId, ObjectId who, APSSecurityLevel lvl, byte[] encKey) throws ModelException {
		this.apsId = apsId;
		this.aps = new AccessPermissionSet();
		this.who = who;
		this.owner = who;
						
		aps._id = apsId;
		aps.security = lvl;
		aps.permissions = new HashMap<String, BasicBSONObject>();
		aps.keys = new HashMap<String, byte[]>();
		aps.direct = lvl.equals(APSSecurityLevel.MEDIUM);
		
		if (! lvl.equals(APSSecurityLevel.NONE)) {
		  encryptionKey = (encKey != null) ? new SecretKeySpec(encKey, KEY_ALGORITHM) : EncryptionUtils.generateKey(KEY_ALGORITHM);		  
		}
		
		keyProvided = true;
		notStored = true;
		isValidated = true;				
	}
	
	protected EncryptedAPS(AccessPermissionSet subset, ObjectId who) {
		this.aps = subset;
		this.who = who;
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
	
	public void setSecurityLevel(APSSecurityLevel lvl) throws ModelException {
		if (!notStored) throw new ModelException("APS already stored. Cannot change security.");
		aps.security = lvl;		
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
		if (notStored) {
			create(); 
		} else {
		    aps.updateKeys();
		}
	}
	
	public byte[] getKey(String name) throws ModelException {
		if (!isLoaded()) load();
		return aps.keys.get(name);
	}
	
	public Map<String, BasicBSONObject> getPermissions() throws ModelException {
		if (!isValidated) validate();		
		return aps.permissions;
	}
	
	public Map<String, BasicBSONObject> getPermissions(ObjectId owner) throws ModelException {
		if (!isLoaded()) load();	
		if (!isValidated && (getKey(who.toString()) != null || owner.equals(who) )) validate();
		if (!isValidated) {
			if (aps.unmerged == null) aps.unmerged = new ArrayList<AccessPermissionSet>();
			
			EncryptedAPS wrapper = null;
			
			for (AccessPermissionSet a : aps.unmerged) {
					if (a.keys.get(who.toString()) != null) {
						wrapper = new EncryptedAPS(a, who);
						wrapper.validate();
						break;
					}
			}
			
			if (wrapper == null) {							
			   AccessPermissionSet newaps = new AccessPermissionSet();
			   newaps.security = aps.security;			   
			   wrapper = new EncryptedAPS(null, who, null, false);
			   aps.unmerged.add(wrapper.aps);
			   for (String ckey : aps.keys.keySet()) {
				   try {
					 ObjectId person = ckey.equals("owner") ? owner : new ObjectId(ckey);
				     wrapper.setKey(ckey, KeyManager.instance.encryptKey(person, wrapper.getAPSKey().getEncoded()));
				   } catch (EncryptionNotSupportedException e) {}
			   }
			}
			
			if (sublists == null) sublists = new ArrayList<EncryptedAPS>();
			sublists.add(wrapper);
			return wrapper.aps.permissions;
		} 
        return aps.permissions;
	}
	
	public void create() throws ModelException {
		if (!notStored) throw new ModelException("APS is already created");
		if (!aps.security.equals(APSSecurityLevel.NONE)) {
			encodeAPS();
			aps.permissions = null;
		}
		AccessPermissionSet.add(this.aps);
		notStored = false;
	}
	
	public void savePermissions() throws ModelException, LostUpdateException {
		if (!isLoaded()) return;
		
		if (sublists != null) {
			for (EncryptedAPS subeaps : sublists) {
				subeaps.encodeAPS();
			}		
		}
		
		if (notStored) {
			create();
		} else if (!aps.security.equals(APSSecurityLevel.NONE)) {
			encodeAPS();
			aps.updateEncrypted();
		} else {
			aps.updatePermissions();
		}
	}
	
	public void reload() throws ModelException {
		load();
	}
	
	public void merge() throws ModelException {
		try {
		if (aps.unmerged != null) {
			for (AccessPermissionSet subaps : aps.unmerged) {
				EncryptedAPS encsubaps = new EncryptedAPS(subaps, who);
				encsubaps.validate();
				Map<String, BasicBSONObject> props = encsubaps.getPermissions();
				for (String key : props.keySet()) {
					BasicBSONObject copyVal = props.get(key);
					if (!aps.permissions.containsKey(key)) {
						aps.permissions.put(key, new BasicBSONObject());
					}
					BasicBSONObject sourceVal = aps.permissions.get(key);
					for (String v : copyVal.keySet()) {
						sourceVal.put(v, copyVal.get(v));
					}
				}
				
			}
			aps.unmerged = null;
			savePermissions();
		}
		} catch (LostUpdateException e) {
			reload();
			validate();
			//merge();
		}
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
		
		if (!keyProvided) 
		{
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
		} else {
			if (!aps.security.equals(APSSecurityLevel.NONE)) { decodeAPS(); }
		}
		isValidated = true;
		if (aps.unmerged != null) merge();
	}
	
	 
			
	private void decodeAPS() throws ModelException  {
		if (aps.permissions == null && aps.encrypted != null) {		
			    BSONObject decrypted = EncryptionUtils.decryptBSON(encryptionKey, aps.encrypted);
		    	aps.permissions = decrypted.toMap();
		    	AccessLog.debug("decoded:"+decrypted.toString());
		    	if (aps.permissions == null) throw new NullPointerException();
		    	aps.encrypted = null;		 				
	    }
	}
			
	private void encodeAPS() throws ModelException {
		if (aps.permissions != null && !aps.security.equals(APSSecurityLevel.NONE)) {											
		   aps.encrypted = EncryptionUtils.encryptBSON(encryptionKey, new BasicBSONObject(aps.permissions));				
	    }
	}
}
