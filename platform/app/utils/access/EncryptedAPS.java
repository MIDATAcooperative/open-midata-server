package utils.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.Record;
import models.enums.APSSecurityLevel;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import models.MidataId;


import utils.AccessLog;
import utils.auth.EncryptionNotSupportedException;
import utils.auth.KeyManager;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

/**
 * wrapper around the raw data of an access permission set. Handles encryption. 
 * Class should not be public. Is only public for debugging.
 */
public class EncryptedAPS {
	private AccessPermissionSet aps;
	private MidataId apsId;
	private MidataId who;
	private MidataId owner;
	private byte[] encryptionKey;	
	private boolean isValidated = false;
	private boolean keyProvided = false;
	private boolean notStored = false;
	private List<EncryptedAPS> sublists;
	private AccessPermissionSet acc_aps;		
		
	public EncryptedAPS(MidataId apsId, MidataId who) throws InternalServerException {
		this(apsId, who, null);
	}
	
	public EncryptedAPS(MidataId apsId, MidataId who, MidataId owner) throws InternalServerException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = owner;
	}
	
	public EncryptedAPS(MidataId apsId, MidataId who, byte[] enckey, MidataId owner) throws InternalServerException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = owner;
		this.encryptionKey = enckey;
		keyProvided = enckey != null;
	}
	
	public EncryptedAPS(MidataId apsId, MidataId who, byte[] enckey, MidataId owner, AccessPermissionSet set) throws InternalServerException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = owner;
		this.encryptionKey = enckey;
		this.aps = set;
		this.acc_aps = this.aps;
		isValidated = false;
		keyProvided = true;
	}

	public EncryptedAPS(MidataId apsId, MidataId who, MidataId owner, APSSecurityLevel lvl) throws InternalServerException {
	  this(apsId, who, owner, lvl, null);
	}
	
	public EncryptedAPS(MidataId apsId, MidataId who, MidataId owner, APSSecurityLevel lvl, byte[] encKey) throws InternalServerException {
		this.apsId = apsId;
		this.acc_aps = this.aps = new AccessPermissionSet();
		this.who = who;
		this.owner = owner;
						
		aps._id = apsId;
		aps.security = lvl;
		aps.permissions = new HashMap<String, Object>();
		aps.permissions.put("p", new BasicBSONList());
		aps.permissions.put("owner", owner.toString());
		aps.keys = new HashMap<String, byte[]>();		
		
		if (! lvl.equals(APSSecurityLevel.NONE)) {
		  encryptionKey = (encKey != null) ? encKey : EncryptionUtils.generateKey();		  
		}
		
		keyProvided = true;
		notStored = true;
		isValidated = true;				
	}
	
	protected EncryptedAPS(AccessPermissionSet subset, MidataId who) {
		this.acc_aps = this.aps = subset;
		this.apsId = subset._id;
		this.who = who;
	}
	
	protected EncryptedAPS createChild() throws AppException {
		EncryptedAPS result = new EncryptedAPS(new MidataId(), getAccessor(), getOwner(), getSecurityLevel(), EncryptionUtils.generateKey());
		if (this.aps.unmerged == null) this.aps.unmerged = new ArrayList<AccessPermissionSet>(); 
		aps.unmerged.add(result.aps);
		return result;
	}
	
	public boolean isLoaded() {
		return this.aps != null;
	}
	
	public boolean isOwner() throws AppException {		
		if (apsId.equals(who)) return true;	
		if (!isValidated) validate();
		if (who.equals(owner)) return true;
		return false;
	}
	
	public MidataId getOwner() throws AppException {
		if (apsId.equals(who)) return who;
		if (owner != null) return owner;
		if (!isValidated) validate();
		return owner;
	}
	
	public MidataId getAccessor() {
		return who;
	}
	
	public boolean isDirect() throws InternalServerException {
		if (!isLoaded()) load();
		return aps.security.equals(APSSecurityLevel.MEDIUM);
	}
	
	public MidataId getId() {
		return apsId;
	}
	
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {
		if (!isLoaded()) load();
		return aps.security;
	}
	
	public void setSecurityLevel(APSSecurityLevel lvl) throws InternalServerException {
		if (!notStored) throw new InternalServerException("error.internal", "APS already stored. Cannot change security.");
		aps.security = lvl;		
	}
	
	protected byte[] getAPSKey() throws AppException {
		if (!keyProvided && !isValidated) validate();
		return encryptionKey;
	}
	
	public long getVersion() throws InternalServerException {
		if (!isLoaded()) load();
		return aps.version;
	}
	
	public void setKey(String name, byte[] key) throws InternalServerException {
		if (!isLoaded()) load();
		aps.keys.put(name,  key);
	}
	
	public void removeKey(String name) throws InternalServerException {
		if (!isLoaded()) load();
		aps.keys.remove(name);
	}
	
	public void updateKeys() throws InternalServerException, LostUpdateException {
		if (!isLoaded()) return;
		if (notStored) {
			create(); 
		} else {
		    aps.updateKeys();
		}
	}
	
	public Set<String> keyNames() throws InternalServerException {
		if (!isLoaded()) load();
		return aps.keys.keySet();
	}
	public byte[] getKey(String name) throws InternalServerException {
		if (!isLoaded()) load();
		return aps.keys.get(name);
	}
	
	public boolean hasKey(String name) throws InternalServerException {
		if (!isLoaded()) load();
		return aps.keys.containsKey(name);
	}
	
	public Map<String, Object> getPermissions() throws AppException {
		if (acc_aps != aps) return acc_aps.permissions;
		//if (owner!=null && !isAccessable()) return getPermissions(owner);
		if (!isValidated) validate();		
		return acc_aps.permissions;
	}
	
	protected void useAccessibleSubset(EncryptedAPS subeaps) {
		if (sublists == null) sublists = new ArrayList<EncryptedAPS>();
		sublists.add(subeaps);
		acc_aps = subeaps.aps;
	}
	
	protected boolean findAndselectAccessibleSubset() throws AppException {
		if (isAccessable()) {			
			return true;
		}
		AccessLog.log("notAccessable");
		if (aps.unmerged == null) return false;
		for (AccessPermissionSet a : aps.unmerged) {
			if (a.keys.get(who.toString()) != null) {
				AccessLog.log("using accessible subset for user: "+who.toString()+" aps: "+apsId.toString());
				EncryptedAPS wrapper = new EncryptedAPS(a, who);
				wrapper.validate();
				useAccessibleSubset(wrapper);
				return true;
			}
	    }
		return false;
	}
	
	protected List<EncryptedAPS> getAllUnmerged() throws AppException {
		List<EncryptedAPS> result = new ArrayList<EncryptedAPS>();
		for (AccessPermissionSet subaps : aps.unmerged) {				
		   EncryptedAPS encsubaps = new EncryptedAPS(subaps, who);
		   encsubaps.validate();
		   result.add(encsubaps);
		}	
		return result;
	}
	
	protected void clearUnmerged() {
		aps.unmerged = null;
		sublists = null;
	}		
	
	public void create() throws InternalServerException {
		if (!notStored) throw new InternalServerException("error.internal", "APS is already created");
		if (!aps.security.equals(APSSecurityLevel.NONE)) {
			encodeAPS();
			aps.permissions = null;
		}
		AccessPermissionSet.add(this.aps);
		notStored = false;
	}
	
	public void savePermissions() throws InternalServerException, LostUpdateException {
		if (!isLoaded()) return;
		
		if (!aps.security.equals(APSSecurityLevel.NONE)) {
			if (sublists != null) {
				for (EncryptedAPS subeaps : sublists) {
					subeaps.encodeAPS();
					subeaps.aps.permissions = null;
				}		
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
	
	public void touch() throws InternalServerException, LostUpdateException {
		if (!isLoaded()) load();
		aps.updateVersionOnly();
	}
	
	public void reload() throws InternalServerException {
		load();
	}
		
	private void load() throws InternalServerException {
		this.aps = AccessPermissionSet.getById(this.apsId);		
		if (this.aps == null) {
			AccessLog.log("APS does not exist: aps="+this.apsId.toString());
			throw new APSNotExistingException(this.apsId, "APS does not exist:"+this.apsId.toString());						
		}
		this.acc_aps = this.aps;
		isValidated = false;
	}
				
	private void validate() throws AppException {
		if (!isLoaded()) load();		
		AccessLog.apsAccess(aps._id, who, aps.security);
		if (aps.keys == null) { isValidated = true; return;} // Old version support
		try {
		if (!keyProvided) 
		{
			if (aps.security.equals(APSSecurityLevel.NONE)) {			
				encryptionKey = null;
				if (aps.keys.containsKey(who.toString())) return;						
				if (aps.keys.get("owner") instanceof byte[]) {
					if (Arrays.equals(who.toByteArray(), aps.keys.get("owner"))) { this.owner = who; return; }
					throw new InternalServerException("error.internal", "APS not readable by user");
				} else this.owner = who; // Old version support			
			} else {		
				byte[] key = aps.keys.get(who.toString());
				if (key==null) { key = aps.keys.get("owner"); this.owner = who; } 
			    if (key==null /*|| ! key.startsWith("key"+who.toString())*/) throw new InternalServerException("error.internal", "APS not readable by user");
			    		 			    
			    encryptionKey = KeyManager.instance.decryptKey(who, key);
			    
			    decodeAPS();
			}
		} else {
			if (!aps.security.equals(APSSecurityLevel.NONE)) { decodeAPS(); }
		}
		isValidated = true;
		
		// Lazily patch old version APS
		if (!aps.permissions.containsKey("owner") && owner != null) {
			aps.permissions.put("owner", owner.toString());
			try {
 			  savePermissions();
			} catch (LostUpdateException e) {}
		}
		
		} catch (AuthException e) {
			AccessLog.decryptFailure(e);
			throw e;
		}
	}
	
	public boolean isAccessable() throws InternalServerException {
		if (who.equals(owner)) return true;
		if (apsId.equals(who)) return true;
		if (!isLoaded()) load();	
		if (aps.keys.containsKey(who.toString())) return true;
		if (owner == null) {
			if (!isValidated && !keyProvided) {
				try {
				  validate();
				} catch (AppException e) {
					return false;
				}
			}
			if (owner!= null && owner.equals(who)) return true;			
		}
		return false;
	}
	
	public boolean needsMerge() throws InternalServerException{
		if (!isLoaded()) load();		
		return isAccessable() && (aps.unmerged != null && aps.unmerged.size() > 0);
	}
		 		
	private void decodeAPS() throws InternalServerException  {
		if (aps.permissions == null && aps.encrypted != null) {
			try {
			    BSONObject decrypted = EncryptionUtils.decryptBSON(encryptionKey, aps.encrypted);
		    	aps.permissions = decrypted.toMap();
		    	//AccessLog.debug("decoded:"+decrypted.toString());
		    	if (aps.permissions == null) throw new NullPointerException();
		    	aps.encrypted = null;
			} catch (InternalServerException e) {
				AccessLog.log("Error decoding APS="+apsId.toString()+" user="+who.toString());
				throw e;
			}
	    }		
	}
	
	
	
	private void encodeAPS() throws InternalServerException {
		if (aps.permissions != null && !aps.security.equals(APSSecurityLevel.NONE)) {											
		   aps.encrypted = EncryptionUtils.encryptBSON(encryptionKey, new BasicBSONObject(aps.permissions));				
	    }
	}
}
