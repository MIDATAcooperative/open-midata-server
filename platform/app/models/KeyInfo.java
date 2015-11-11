package models;

import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class KeyInfo extends Model {

	protected @NotMaterialized static final String collection = "keys";
	
	public byte[] privateKey;
	public int type;
	
	public static KeyInfo getById(ObjectId id) throws InternalServerException {
		return Model.get(KeyInfo.class, collection, CMaps.map("_id", id), Sets.create("privateKey", "type"));
	}
	
	public static void add(KeyInfo keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
}
