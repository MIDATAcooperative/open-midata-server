package models;

import java.util.Map;
import java.util.Set;

import models.enums.InformationType;

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
	public long version;
	
	public Map<String, String> keys;
	public Map<String, BasicDBObject> permissions;
	
	public static void add(AccessPermissionSet aps) throws ModelException {
		Model.insert(collection, aps);	
	}
	
	public static AccessPermissionSet getById(ObjectId id) throws ModelException {
		return Model.get(AccessPermissionSet.class, collection, CMaps.map("_id", id), Sets.create("keys", "version", "direct" ,"permissions"));
	}
		
	
	public void updatePermissions() throws ModelException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "permissions");
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
