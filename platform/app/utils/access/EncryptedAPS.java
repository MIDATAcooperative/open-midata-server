/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.Consent;
import models.MidataId;
import models.enums.APSSecurityLevel;
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
	private boolean unmergedSubAPS = false;
	private List<EncryptedAPS> sublists;
	private AccessPermissionSet acc_aps;
	private byte[] subEncryptionKey; //To be used with acc_aps
		
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
		keyProvided = enckey != null;
	}

	public EncryptedAPS(MidataId apsId, MidataId who, MidataId owner, APSSecurityLevel lvl, boolean consent) throws InternalServerException {
	  this(apsId, who, owner, lvl, null, consent);
	}
	
	public EncryptedAPS(MidataId apsId, MidataId who, MidataId owner, APSSecurityLevel lvl, byte[] encKey, boolean consent) throws InternalServerException {
		this.apsId = apsId;
		this.acc_aps = this.aps = new AccessPermissionSet();
		this.who = who;
		this.owner = owner;
						
		aps._id = apsId;
		aps.security = lvl;
		aps.consent = consent;
		aps.permissions = new HashMap<String, Object>();
		//aps.permissions.put("p", new BasicBSONList());
		aps.permissions.put("owner", owner.toString());
		aps.keys = new HashMap<String, byte[]>();		
		
		if (! lvl.equals(APSSecurityLevel.NONE)) {			
		  encryptionKey = (encKey != null) ? encKey : EncryptionUtils.generateKey();
		  if (encryptionKey[0] != 0) aps.format = 1;
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
		EncryptedAPS result = new EncryptedAPS(new MidataId(), getAccessor(), getOwner(), getSecurityLevel(), EncryptionUtils.generateKey(), this.aps.consent);
		if (this.aps.unmerged == null) this.aps.unmerged = new ArrayList<AccessPermissionSet>(); 
		aps.unmerged.add(result.aps);
		return result;
	}
	
	public boolean isLoaded() {
		return this.aps != null;
	}
	
	public boolean isForConsent() throws InternalServerException {
		if (!isLoaded()) load();
		return aps.consent;
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
	
	protected byte[] getLocalAPSKey() {		
		if (acc_aps != aps) return subEncryptionKey;
		return encryptionKey;
	}
	
	protected byte[] exportAPSKey() throws AppException {
		if (!keyProvided && !isValidated) validate();
		if (encryptionKey != null && !keyProvided && needsKeyUpgrade()) doKeyUpgrade();
		return encryptionKey;
	}
	
	protected void provideAPSKeyAndOwner(byte[] unlock, MidataId owner) {
		if (!keyProvided && !isValidated) {
			encryptionKey = unlock;
			keyProvided = true;
			if (this.owner == null) this.owner = owner;
		}
	}
	
	public long getVersion() throws InternalServerException {
		if (!isLoaded()) load();
		return aps.version;
	}
	
	public void setKey(String name, byte[] key) throws InternalServerException {
	    if (key==null) return;
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
		    if (aps.consent) Consent.touch(apsId, aps.version);
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
	
	public Map<String, Object> getPermissions() throws InternalServerException {
		if (acc_aps != aps) return acc_aps.permissions;
		//if (owner!=null && !isAccessable()) return getPermissions(owner);
		if (!isValidated) validate();	
		if (needsKeyUpgrade()) doKeyUpgrade();
		return acc_aps.permissions;
	}
	
	protected void useAccessibleSubset(EncryptedAPS subeaps) {
		if (sublists == null) sublists = new ArrayList<EncryptedAPS>();
		subeaps.unmergedSubAPS = true;
		sublists.add(subeaps);
		acc_aps = subeaps.aps;
		subEncryptionKey = subeaps.getLocalAPSKey();
	}
	
	protected boolean findAndselectAccessibleSubset() throws InternalServerException {
		if (isAccessable()) {			
			return true;
		}
		AccessLog.log("notAccessable");
		if (aps.unmerged == null) return false;
		for (AccessPermissionSet a : aps.unmerged) {
			if (a.keys.get(who.toString()) != null) {
				AccessLog.log("using accessible subset for user: ",who.toString()," aps: ",apsId.toString());
				EncryptedAPS wrapper = new EncryptedAPS(a, who);
				wrapper.validate();
				useAccessibleSubset(wrapper);
				return true;
			}
	    }
		return false;
	}
	
	protected List<EncryptedAPS> getAllUnmerged() throws InternalServerException {
		List<EncryptedAPS> result = new ArrayList<EncryptedAPS>();
		for (AccessPermissionSet subaps : aps.unmerged) {				
		   EncryptedAPS encsubaps = new EncryptedAPS(subaps, who);
		   encsubaps.unmergedSubAPS = true;
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
					subeaps.unmergedSubAPS = true;
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
			if (aps.consent) Consent.touch(apsId, aps.version);
			// If we are using an accessible sublist permissions are null now, we need to restore them
			if (acc_aps != aps) {
				acc_aps = aps;
				subEncryptionKey = null;
				findAndselectAccessibleSubset();
			}
		} else {
			aps.updatePermissions();
			if (aps.consent) Consent.touch(apsId, aps.version);
		}
	}
	
	public void touch() throws InternalServerException, LostUpdateException {
		if (!isLoaded()) load();
		aps.updateVersionOnly();
		if (aps.consent) Consent.touch(apsId, aps.version);
	}
	
	public void reload() throws InternalServerException {
		load();
	}
		
	private void load() throws InternalServerException {
		this.aps = AccessPermissionSet.getById(this.apsId);		
		if (this.aps == null) {
			AccessLog.log("APS does not exist: aps=",this.apsId.toString());
			throw new APSNotExistingException(this.apsId, "APS does not exist:"+this.apsId.toString());						
		}
		this.acc_aps = this.aps;
		this.subEncryptionKey = null;
		isValidated = false;
	}
				
	private void validate() throws InternalServerException {
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
				if (key==null) { 
					key = aps.keys.get("owner");
					encryptionKey = KeyManager.instance.decryptKey(who, key);			    
				    decodeAPS();
					this.owner = who; 
				} else {			    			    		 			   
			        encryptionKey = KeyManager.instance.decryptKey(who, key);			    
			        decodeAPS();
				}
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
			throw new InternalServerException("error.internal", e);
		}
	}
	
	public boolean isAccessable() throws InternalServerException {
		if (who.equals(owner)) return true;
		if (apsId.equals(who)) return true;		
		if (!isLoaded()) load();	
		if (keyProvided) return true;
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
	
	public boolean needsKeyUpgrade() throws InternalServerException {
		if (isLoaded() && isAccessable() && aps.security == APSSecurityLevel.HIGH && owner != null &&
				(apsId.equals(owner) || aps.consent) && !unmergedSubAPS && (acc_aps.permissions != null && !acc_aps.permissions.containsKey("idxEnc"))) {			
			return EncryptionUtils.isDeprecatedKey(encryptionKey);
		} else return false;
	}
	
	protected void doKeyUpgrade() throws InternalServerException  {
		
		AccessLog.logBegin("begin key upgrade:",getId().toString());
		if (!isValidated) validate();
		try {
			byte[] newKey = EncryptionUtils.generateKey();
		    for (String ckey : keyNames()) {
			   try {	
				   if (ckey.equals("owner")) {
					   setKey(ckey, KeyManager.instance.encryptKey(owner, newKey, true));
				   } else {
					   setKey(ckey, KeyManager.instance.encryptKey(new MidataId(ckey), newKey, false));  
				   }			      
			   } catch (EncryptionNotSupportedException e) {
				   if (! e.getMessage().equals("No public key")) throw new InternalServerException("error.internal", e);
			   }			   
			}
		    encryptionKey = newKey;
		    aps.format = 1;
		    
		    encodeAPS();
			aps.updateAll();
		} catch (LostUpdateException e) {
			reload();
			validate();
			if (needsKeyUpgrade()) doKeyUpgrade();
		} finally {
		   AccessLog.logEnd("end key upgrade");
		}
		
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
				AccessLog.log("Error decoding APS=",apsId.toString()," user=",who.toString());
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
