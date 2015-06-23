package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;
import models.enums.InformationType;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.search.Search;
import utils.search.Search.Type;

public class AccessPermissionSet extends Model {

	private static final String collection = "aps";
	
	public boolean direct;
	public APSSecurityLevel security = APSSecurityLevel.NONE;
	public long version;
	
	public Map<String, byte[]> keys;
	public byte[] encrypted;
	public Map<String, BasicBSONObject> permissions;
	
	public List<AccessPermissionSet> unmerged;
	
	public static void add(AccessPermissionSet aps) throws ModelException {
		Model.insert(collection, aps);	
	}
	
	public static AccessPermissionSet getById(ObjectId id) throws ModelException {
		return Model.get(AccessPermissionSet.class, collection, CMaps.map("_id", id), Sets.create("keys", "version", "direct" ,"permissions", "encrypted", "security", "unmerged"));
	}
		
	
	public void updatePermissions() throws ModelException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "permissions", "unmerged");
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}
	
	public void updateEncrypted() throws ModelException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "encrypted", "unmerged");
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}
	
	public void updateKeys() throws LostUpdateException, ModelException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "keys");
		} catch (DatabaseException e) {
				throw new ModelException(e);
		}		
	}
	
	public static void delete(ObjectId appsId) throws ModelException {	
		Model.delete(AccessPermissionSet.class, collection, new ChainedMap<String, ObjectId>().put("_id", appsId).get());
	}
}
