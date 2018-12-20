package models;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class KeyInfoExtern extends Model {

	protected @NotMaterialized static final String collection = "keysext";
	
	/**
	 * the private key
	 */
	public String privateKey;
		
	
	public static KeyInfoExtern getById(MidataId id) throws InternalServerException {
		return Model.get(KeyInfoExtern.class, collection, CMaps.map("_id", id), Sets.create("privateKey"));
	}
	
	public static void add(KeyInfoExtern keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
	
	public static void update(KeyInfoExtern keyinfo) throws InternalServerException {
		Model.upsert(collection, keyinfo);
	}
	
	public static void delete(MidataId id) throws InternalServerException {
		Model.delete(KeyInfoExtern.class, collection, CMaps.map("_id", id));
	}
}
