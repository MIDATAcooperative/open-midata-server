package models;

import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.ModelException;

public class KeyInfo extends Model {

	protected static final String collection = "keys";
	
	public byte[] privateKey;
	public int type;
	
	public static KeyInfo getById(ObjectId id) throws ModelException {
		return Model.get(KeyInfo.class, collection, CMaps.map("_id", id), Sets.create("privateKey", "type"));
	}
	
	public static void add(KeyInfo keyinfo) throws ModelException {
		Model.insert(collection, keyinfo);
	}
}
