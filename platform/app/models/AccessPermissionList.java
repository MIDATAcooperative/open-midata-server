package models;

import java.util.Map;
import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class AccessPermissionList extends Model {

	private static final String collection = "apslist";
			
	/**
	 * timestamp of last change
	 */
	public long version;
	
	public MidataId apsId;
	public MidataId recordOwner;
	
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
	
	public static Set<AccessPermissionList> getByApsIdAndRecordOwner(MidataId apsId, MidataId recordOwnerId) throws InternalServerException {
		return Model.getAll(AccessPermissionList.class, collection, CMaps.map("apsId", apsId).map("recordOwner", recordOwnerId), Sets.create("version", "encrypted" ,"apsId", "recordOwner"));
	}
		
		
}
