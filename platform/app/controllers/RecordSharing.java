package controllers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;

import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;

import models.AccessPermissionSet;
import models.LargeRecord;
import models.Member;
import models.ModelException;
import models.Record;
import models.User;

public class RecordSharing {
	
	public static RecordSharing instance = new RecordSharing();
	
	public ObjectId createPrivateAPS(ObjectId who) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		newset._id = new ObjectId();
		newset.permissions = new HashMap<String, BasicDBObject>();
		newset.keys = new HashMap<String, String>();
		newset.keys.put("owner", "key"+who.toString());
		AccessPermissionSet.add(newset);
		return newset._id;
	}
	
	public ObjectId createAnonymizedAPS(ObjectId owner, ObjectId other) throws ModelException {
		AccessPermissionSet newset = new AccessPermissionSet();
		newset._id = new ObjectId();
		newset.permissions = new HashMap<String, BasicDBObject>();
		newset.keys = new HashMap<String, String>();
		newset.keys.put("owner", "key"+owner.toString());
		newset.keys.put(other.toString(), "key"+other.toString());
		AccessPermissionSet.add(newset);
		return newset._id;
	}
	
	public void shareAPS(ObjectId apsId, ObjectId ownerId, Set<ObjectId> targetUsers) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		if (aps==null) throw new ModelException("APS not found.");
		validateReadable(aps, ownerId);
		for (ObjectId targetUser : targetUsers) {
		  aps.keys.put(targetUser.toString(), "key"+targetUser.toString());
		}
		aps.setKeys(aps.keys);
	}
	
	public void unshareAPS(ObjectId apsId, ObjectId ownerId, Set<ObjectId> targetUsers) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		if (aps==null) throw new ModelException("APS not found.");
		validateReadable(aps, ownerId);
		
		for (ObjectId targetUser : targetUsers) {
		  aps.keys.remove(targetUser.toString());
		}
		aps.setKeys(aps.keys);
	}

	public void share(ObjectId who, ObjectId fromAPS, ObjectId toAPS, Set<ObjectId> records, boolean withOwnerInformation) throws ModelException {
		
		AccessPermissionSet source = AccessPermissionSet.getById(fromAPS);
		if (source==null) throw new ModelException("source APS not found.");
		AccessPermissionSet target = AccessPermissionSet.getById(toAPS);
		if (target==null) throw new ModelException("target APS not found.");
				
		validateReadable(source, who);
		validateReadable(target, who);
		
		for (ObjectId record : records) {
			String key = record.toString();
			BasicDBObject entry = source.permissions.get(key);
			if (entry!=null) {
				if (withOwnerInformation && entry.get("owner") == null) {
					entry = (BasicDBObject) entry.copy();
					entry.put("owner", who);					
				}
				
				target.permissions.put(key, entry);
			} else throw new ModelException("source APS does not contain record to share.");
		}
		
		target.setPermissions(target.permissions);		
	}
	
	public void unshare(ObjectId who, ObjectId apsId, Set<ObjectId> records) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		validateReadable(aps, who);
		
		for (ObjectId rec : records) {
			aps.permissions.remove(rec.toString());
		}
		
		aps.setPermissions(aps.permissions);
	}
	
	public void addRecord(Member owner, Record record) throws ModelException {
	    AccessPermissionSet aps = AccessPermissionSet.getById(owner.myaps);	
	    validateReadable(aps, owner._id);
	    
	    Record.add(record);
	    
	    BasicDBObject entry = new BasicDBObject();	    
	    aps.addPermission(record._id, entry);	    
	}
	
	public void addRecord2(Member owner, Record record) throws ModelException {
	    AccessPermissionSet aps = AccessPermissionSet.getById(owner.myaps);	
	    validateReadable(aps, owner._id);
	    //Record.add(record);
	    
	    BasicDBObject entry = new BasicDBObject();	    
	    aps.addPermission(record._id, entry);	    
	}
	
	public void deleteAPS(ObjectId apsId, ObjectId ownerId) throws ModelException {
		//AccessPermissionSet aps = AccessPermissionSet.getById(apsId);	
		AccessPermissionSet.delete(apsId);
	}
					
	public Collection<Record> list(ObjectId who, ObjectId apsId, boolean withmetadata, boolean withId, boolean externId) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);	
		validateReadable(aps, who);
		
		Map<String, Record> result = new HashMap<String, Record>();
		for (String key : aps.permissions.keySet()) {
			Record md = new Record();
			BasicDBObject entry = aps.permissions.get(key);
			md.id = new RecordToken(key, apsId.toString()).encrypt();
			md.owner = entry.get("owner") != null ? (ObjectId) entry.get("owner") : who;
			result.put(key, md);
		}
		
		if (withmetadata) {
			Set<Record> records = Record.getAllByIds(ObjectIdConversion.toObjectIds(aps.permissions.keySet()), Sets.create("app","created","creator","description","format","name"));
	        for (Record rec : records) {
	        	Record md = result.get(rec._id.toString());
	          if (withId) md._id = rec._id;
	          md.app = rec.app;
	          md.created = rec.created;
	          md.creator = rec.creator;
	          md.description = rec.description;
	          md.format = rec.format;
	          md.name = rec.name;   
	           
	        }
		}
				
		return result.values();
	}
		
	
	public Set<String> listTokens(ObjectId who, ObjectId apsId) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);	
		validateReadable(aps, who);
		
		Set<String> result = new HashSet<String>();
		for (String key : aps.permissions.keySet()) {			
			result.add(new RecordToken(key, apsId.toString()).encrypt());
		}
		
		return result;
	}
	
	public Set<String> listRecordIds(ObjectId who, ObjectId apsId) throws ModelException {
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);	
		validateReadable(aps, who);		
		return aps.permissions.keySet();						
	}
	
	//public Map<String, Set<ObjectId>> toSearch(ObjectId who, )
	
	public Record fetch(ObjectId who, RecordToken token) throws ModelException {
		ObjectId apsId = new ObjectId(token.apsId);		
		
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		validateReadable(aps, who);
		
		BasicDBObject entry = aps.permissions.get(token.recordId);
		if (entry!=null) {
			Record result = Record.getById(new ObjectId(token.recordId)); 
			return result;
		}
		
		return null;
	}
	
	public Set<Record> fetchMultiple(ObjectId who, ObjectId apsId, Set<String> recordIds, Set<String> fields) throws ModelException {
				
		
		AccessPermissionSet aps = AccessPermissionSet.getById(apsId);
		validateReadable(aps, who);
		
		Set<Record> result = new HashSet<Record>();
		Set<String> ids = recordIds != null ? recordIds : aps.permissions.keySet();
				
		for (String id : ids) {
		  BasicDBObject entry = aps.permissions.get(id);
		  if (entry!=null) {
			result.addAll(LargeRecord.getAll(CMaps.map("_id", new ObjectId(id)), fields));			
		  }
		}
		
		return result;
	}
	
	private void validateReadable(AccessPermissionSet aps, ObjectId who) throws ModelException {
		if (aps.keys == null) return; // Old version support
		String key = aps.keys.get(who.toString());
		if (key==null) key = aps.keys.get("owner"); 
	    if (key==null || ! key.equals("key"+who.toString())) throw new ModelException("APS not readable by user");		
	}
}
