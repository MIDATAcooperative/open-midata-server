package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class AccessPermissionList extends Model {

	private static final String collection = "apslist";
			
	/**
	 * timestamp of last change
	 */
	public long version;
	
	public ObjectId apsId;
	public ObjectId recordOwner;
	
	/**
	 * the encrypted body of this APS
	 */
	public byte[] encrypted;
	
	/**
	 * the unencrypted body of this APS.
	 */
	public Map<String, Object> permissions;
		
	
	public static void add(AccessPermissionList aps) throws InternalServerException {
		Model.insert(collection, aps);	
	}
	
	public static Set<AccessPermissionList> getByApsIdAndRecordOwner(ObjectId apsId, ObjectId recordOwnerId) throws InternalServerException {
		return Model.getAll(AccessPermissionList.class, collection, CMaps.map("apsId", apsId).map("recordOwner", recordOwnerId), Sets.create("version", "encrypted" ,"apsId", "recordOwner"));
	}
		
		
}
