package utils.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import models.APSNotExistingException;
import models.AccessPermissionSet;
import models.Record;
import models.enums.APSSecurityLevel;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import utils.auth.EncryptionNotSupportedException;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.ModelException;

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
	
	public EncryptedAPS(ObjectId apsId, ObjectId who, ObjectId owner) throws ModelException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = owner;
	}
	
	public EncryptedAPS(ObjectId apsId, ObjectId who, byte[] enckey, ObjectId owner) throws ModelException {
		this.apsId = apsId;		
		this.who = who;
		this.owner = owner;
		this.encryptionKey = (enckey != null) ? new SecretKeySpec(enckey, KEY_ALGORITHM) : null;
		keyProvided = true;
	}
			
	public EncryptedAPS(ObjectId apsId, ObjectId who, ObjectId owner, APSSecurityLevel lvl, byte[] encKey) throws ModelException {
		this.apsId = apsId;
		this.aps = new AccessPermissionSet();
		this.who = who;
		this.owner = owner;
						
		aps._id = apsId;
		aps.security = lvl;
		aps.permissions = new HashMap<String, Object>();
		aps.permissions.put("p", new BasicBSONList());
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
		this.apsId = subset._id;
		this.who = who;
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
	
	public ObjectId getOwner() throws AppException {
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
		if (!notStored) throw new ModelException("error.internal", "APS already stored. Cannot change security.");
		aps.security = lvl;		
	}
	
	public SecretKey getAPSKey() throws AppException {
		if (!keyProvided && !isValidated) validate();
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
	
	public boolean hasKey(String name) throws ModelException {
		if (!isLoaded()) load();
		return aps.keys.containsKey(name);
	}
	
	public Map<String, Object> getPermissions() throws AppException {
		if (owner!=null && !isAccessable()) return getPermissions(owner);
		if (!isValidated) validate();		
		return aps.permissions;
	}
	
	public Map<String, Object> getPermissions(ObjectId owner) throws AppException {
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
			   AccessLog.debug("split:" + apsId.toString());
			 	   
			   wrapper = new EncryptedAPS(new ObjectId(), who, owner, aps.security, EncryptionUtils.generateKey(KEY_ALGORITHM).getEncoded());
			   aps.unmerged.add(wrapper.aps);
			   for (String ckey : aps.keys.keySet()) {
				   try {					 
					 if (aps.security.equals(APSSecurityLevel.NONE)) {
						if (ckey.equals("owner")) wrapper.setKey("owner", owner.toByteArray());
						else wrapper.setKey(ckey, null);
					 } else {
						ObjectId person = ckey.equals("owner") ? owner : new ObjectId(ckey);
				        wrapper.setKey(ckey, KeyManager.instance.encryptKey(person, wrapper.getAPSKey().getEncoded()));
					 }
				   } catch (EncryptionNotSupportedException e) {}
				   
			   }
			   try {
				 if (wrapper.getSecurityLevel().equals(APSSecurityLevel.NONE)) {
					wrapper.setKey(who.toString(), null);
				 } else wrapper.setKey(who.toString(), KeyManager.instance.encryptKey(who, wrapper.getAPSKey().getEncoded()));
			   } catch (EncryptionNotSupportedException e) {}
			} else { AccessLog.debug("use existing split:" + apsId.toString()); };
			
			if (sublists == null) sublists = new ArrayList<EncryptedAPS>();
			sublists.add(wrapper);
			return wrapper.aps.permissions;
		} 
        return aps.permissions;
	}
	
	public void create() throws ModelException {
		if (!notStored) throw new ModelException("error.internal", "APS is already created");
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
	
	public void merge() throws AppException {
		try {
		if (aps.unmerged != null) {
			AccessLog.debug("merge:" + apsId.toString());
			Record dummy = new Record();
			for (AccessPermissionSet subaps : aps.unmerged) {
				EncryptedAPS encsubaps = new EncryptedAPS(subaps, who);
				encsubaps.validate();
				Map<String, Object> props = encsubaps.getPermissions();												
				BasicBSONList lst = (BasicBSONList) props.get("p");
				
				for (Object row : lst) {
					BasicBSONObject crit = (BasicBSONObject) row;
					BasicBSONObject entries = APSEntry.getEntries(crit);
                    APSEntry.populateRecord(crit, dummy);										
				    for (String key : entries.keySet()) {
					   BasicBSONObject copyVal = (BasicBSONObject) entries.get(key);
					   
					   BasicBSONObject targetRow = APSEntry.findMatchingRowForRecord(aps.permissions, dummy, true);
					   BasicBSONObject targetEntries = APSEntry.getEntries(targetRow);
					   
					   if (!targetEntries.containsField(key)) {
						  targetEntries.put(key, new BasicBSONObject());
					   }
					   BasicBSONObject targetVals = (BasicBSONObject) targetEntries.get(key);
					
					   for (String v : copyVal.keySet()) {
						  targetVals.put(v, copyVal.get(v));
					   }
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
		if (this.aps == null) {
			AccessLog.debug("APS does not exist: aps="+this.apsId.toString());
			throw new APSNotExistingException(this.apsId, "APS does not exist:"+this.apsId.toString());
			/*this.aps = new AccessPermissionSet();
			aps.permissions = new HashMap<String, BasicBSONObject>();
			aps.keys = new HashMap<String, byte[]>();*/
			
		}
		isValidated = false;
	}
				
	private void validate() throws AppException {
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
					throw new ModelException("error.internal", "APS not readable by user");
				} else this.owner = who; // Old version support			
			} else {		
				byte[] key = aps.keys.get(who.toString());
				if (key==null) { key = aps.keys.get("owner"); this.owner = who; } 
			    if (key==null /*|| ! key.startsWith("key"+who.toString())*/) throw new ModelException("error.internal", "APS not readable by user");
			    		 
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
	
	public boolean isAccessable() throws ModelException {
		if (who.equals(owner)) return true;
		if (apsId.equals(who)) return true;
		if (!isLoaded()) load();	
		if (aps.keys.containsKey(who.toString())) return true;
		return false;
	}
	
	 
			
	private void decodeAPS() throws ModelException  {
		if (aps.permissions == null && aps.encrypted != null) {
			try {
			    BSONObject decrypted = EncryptionUtils.decryptBSON(encryptionKey, aps.encrypted);
		    	aps.permissions = decrypted.toMap();
		    	//AccessLog.debug("decoded:"+decrypted.toString());
		    	if (aps.permissions == null) throw new NullPointerException();
		    	aps.encrypted = null;
			} catch (ModelException e) {
				AccessLog.debug("Error decoding APS="+apsId.toString()+" user="+who.toString());
				throw e;
			}
	    }
		if (!aps.permissions.containsKey("p")) {
			try {
			patchOldFormat();
			} catch (LostUpdateException e) {
				throw new NullPointerException("Lost Update!!");
			}
		}
	}
	
	private void patchOldFormat() throws ModelException, LostUpdateException {
		aps.permissions.put("p", new BasicDBList());
		for (String key : aps.permissions.keySet()) {
			if (key.equals("p") || (key.startsWith("_") && !key.toLowerCase().startsWith("stream"))) continue;
			
			BasicBSONObject entries = (BasicBSONObject) aps.permissions.get(key);
			for (String id : entries.keySet()) {
				BasicBSONObject obj = (BasicBSONObject) entries.get(id);
				Record record = new Record();
				record._id = new ObjectId(id);
				
				if (obj.get("key") instanceof String) record.key = null; // For old version support
				else record.key = (byte[]) obj.get("key");	
				
				record.format = key;
				if (key.toLowerCase().startsWith("stream")) {
					record.isStream = true;
					record.format = obj.getString("name");
				}
				record.content = "other";
				
				String owner = obj.getString("owner");
			    if (owner!=null) record.owner = new ObjectId(owner); 
			    
			    BasicBSONObject obj2 = APSEntry.findMatchingRowForRecord(aps.permissions, record, true);
				obj2 = APSEntry.getEntries(obj2);	
				// add entry
				BasicBSONObject entry = new BasicDBObject();
				entry.put("key", record.key);
				if (record.isStream) entry.put("s", true);
				if (record.owner!=null) entry.put("owner", record.owner);				
				obj2.put(record._id.toString(), entry);
			}
			
			
		}
		savePermissions();
	}
			
	private void encodeAPS() throws ModelException {
		if (aps.permissions != null && !aps.security.equals(APSSecurityLevel.NONE)) {											
		   aps.encrypted = EncryptionUtils.encryptBSON(encryptionKey, new BasicBSONObject(aps.permissions));				
	    }
	}
}
