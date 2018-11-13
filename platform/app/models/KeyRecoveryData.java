package models;

import java.util.Map;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class KeyRecoveryData extends Model {

	protected @NotMaterialized static final String collection = "keyrecover";
	
	public Map<String, String> shares;
	
	public static KeyRecoveryData getById(MidataId id) throws InternalServerException {
		return Model.get(KeyRecoveryData.class, collection, CMaps.map("_id", id), Sets.create("shares"));
	}
	
	public static void add(KeyRecoveryData keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
	
	public static void update(KeyRecoveryData keyinfo) throws InternalServerException {
		Model.upsert(collection, keyinfo);
	}
	
	public static void delete(MidataId id) throws InternalServerException {
		Model.delete(KeyRecoveryData.class, collection, CMaps.map("_id", id));
	}
}
