package models;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model class for storing private keys 
 *
 */
public class KeyInfo extends Model {

	protected @NotMaterialized static final String collection = "keys";
	
	/**
	 * the private key
	 */
	public byte[] privateKey;
	
	/**
	 * the type of the private key.
	 * 0 = plain private key
	 * 1 = passphrase protected private key
	 */
	public int type;
	
	public static KeyInfo getById(MidataId id) throws InternalServerException {
		return Model.get(KeyInfo.class, collection, CMaps.map("_id", id), Sets.create("privateKey", "type"));
	}
	
	public static void add(KeyInfo keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
	
	public static void update(KeyInfo keyinfo) throws InternalServerException {
		Model.upsert(collection, keyinfo);
	}
}
