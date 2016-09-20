package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;
import models.enums.InformationType;

import org.bson.BasicBSONObject;
import models.MidataId;

import com.mongodb.BasicDBObject;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.Search.Type;

/**
 * Data model class for an access permission set.
 *
 */
public class AccessPermissionSet extends Model {

	private static final String collection = "aps";
	public @NotMaterialized static final Set<String> ALL_FIELDS = Sets.create("keys", "version", "direct" ,"permissions", "encrypted", "security", "unmerged");
	
	/**
	 * security level of this APS.
	 * NONE : APS not encrypted, records not encrypted
	 * MEDIUM : APS is encrypted and all containing records are encrypted with the APS key
	 * HIGH : APS is encrypted and each record is encrypted with its own key
	 */
	public APSSecurityLevel security = APSSecurityLevel.NONE;
	
	/**
	 * timestamp of last change
	 */
	public long version;
	
	/**
	 * key table. one entry for each entity that has access to this APS.
	 * map keys are the object ids of the entities and the word "owner" for the owner of the APS.
	 * each map value is the RSA encrypted AES key of this APS.
	 */
	public Map<String, byte[]> keys;
	
	/**
	 * the encrypted body of this APS
	 */
	public byte[] encrypted;
	
	/**
	 * the unencrypted body of this APS.
	 */
	public Map<String, Object> permissions;
	
	/**
	 * additional blocks of data for this APS that need to be merged into this part (the main part)
	 */
	public List<AccessPermissionSet> unmerged;
	
	public static void add(AccessPermissionSet aps) throws InternalServerException {
		Model.insert(collection, aps);	
	}
	
	public static AccessPermissionSet getById(MidataId id) throws InternalServerException {
		return Model.get(AccessPermissionSet.class, collection, CMaps.map("_id", id), ALL_FIELDS);
	}
	
	public static Set<AccessPermissionSet> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(AccessPermissionSet.class, collection, properties, fields);
	}
		
	
	public void updatePermissions() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "permissions", "unmerged");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateEncrypted() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "encrypted", "unmerged");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateKeys() throws LostUpdateException, InternalServerException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "keys");
		} catch (DatabaseException e) {
				throw new InternalServerException("error.internal_db", e);
		}		
	}
	
	public void updateVersionOnly() throws LostUpdateException, InternalServerException {
		try {
		   DBLayer.secureUpdate(this, collection, "version");
		} catch (DatabaseException e) {
				throw new InternalServerException("error.internal_db", e);
		}		
	}
	
	public static void delete(MidataId appsId) throws InternalServerException {	
		Model.delete(AccessPermissionSet.class, collection, new ChainedMap<String, MidataId>().put("_id", appsId).get());
	}
}
