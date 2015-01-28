package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;

import com.mongodb.DBObject;

public class LargeRecordChunk extends Model {

	private static final String collection = "records.chunks";

	public ObjectId masterRecord; // id of the master record
	public DBObject data; // chunk data

	public static LargeRecordChunk get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(LargeRecordChunk.class, collection, properties, fields);
	}

	public static Set<LargeRecordChunk> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(LargeRecordChunk.class, collection, properties, fields);
	}

	public static void set(ObjectId chunkId, String field, Object value) throws ModelException {
		Model.set(LargeRecordChunk.class, collection, chunkId, field, value);
	}

	public static void add(LargeRecordChunk chunk) throws ModelException {
		Model.insert(collection, chunk);
	}

	public static void delete(ObjectId chunkId) throws ModelException {
		Model.delete(collection, new ChainedMap<String, ObjectId>().put("_id", chunkId).get());
	}

	public static void deleteAll(ObjectId masterRecordId) throws ModelException {
		Model.delete(collection, new ChainedMap<String, ObjectId>().put("masterRecord", masterRecordId).get());
	}

}