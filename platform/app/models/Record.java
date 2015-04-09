package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.search.Search;
import utils.search.SearchException;

import com.mongodb.DBObject;

public class Record extends Model implements Comparable<Record>, Cloneable {

	private static final String collection = "records";

	// Not encrypted part
	public ObjectId stream; // series this record belongs to
	public long time;
	public ObjectId document;
	public String part;
	public String encrypted;
	public String encryptedData;
	public boolean direct;
	
	// Not materialized part (derived from APS)
	public @NotMaterialized String id;
	public @NotMaterialized ObjectId owner; // person the record is about
	public @NotMaterialized String ownerName;
	public @NotMaterialized String key;
	
	// Encrypted part
	public  ObjectId app; // app that created the record		
	public  ObjectId creator; // user that imported the record	
	public  Date created; // date + time created TODO change to date
	public  String createdOld; // date + time created TODO change to date
	public  String name; // used to display a record and for autocompletion
	public  String format; // format of record
	public  String description; // this will be indexed in the search cluster
	public  Set<String> tags; // Optional tags describing the record
	
	// Contents
	public DBObject data; // arbitrary json data

	@Override
	public int compareTo(Record other) {
		if (this.created != null && other.created != null) {
			// newest first
			return -this.created.compareTo(other.created);
		} else {
			return super.compareTo(other);
		}
	}
	
	public void clearEncryptedFields() {
		this.app = null;		
		this.creator = null;	
		this.created = null;
		this.name = null;
		this.format = null;
		this.description = null;					
	}
	
	public void clearSecrets() {
		this.key = null;
		this.encrypted = null;
		this.encryptedData = null;
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Record.class, collection, properties);
	}

	
	public static Record get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Record.class, collection, properties, fields);
	}
	
	public static Record getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(Record.class, collection, CMaps.map("_id", id), fields);
	}

	public static Set<Record> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Record.class, collection, properties, fields);
	}
	
	public static Set<Record> getAllByIds(Set<ObjectId> ids, Set<String> fields) throws ModelException {
		return Model.getAll(Record.class, collection, CMaps.map("_id", ids), fields);
	}
	
	/*public static Record getById(ObjectId id, String encKey, Set<String> fields) throws ModelException {
		return Model.get(Record.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static Set<Record> getAll(Map<String, ? extends Object> properties, Set<String> fields, Map<String, String> keyMap, String defaultKey) throws ModelException {
		return Model.getAll(Record.class, collection, properties, fields);
	}*/
	
	public static void set(ObjectId recordId, String field, Object value) throws ModelException {
		Model.set(Record.class, collection, recordId, field, value);
	}

	public static void add(Record record) throws ModelException {
		Model.insert(collection, record);

		// also index the data for the text search
		/*
		try {
			Search.add(record.owner, "record", record._id, record.name, record.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
		*/
	}

	public static void delete(ObjectId ownerId, ObjectId recordId) throws ModelException {
		// also remove from search index
		Search.delete(ownerId, "record", recordId);

		// TODO remove from spaces and circles
		Model.delete(Record.class, collection, new ChainedMap<String, ObjectId>().put("_id", recordId).get());
	}

	@Override
	public Record clone() {	
		try {
			return (Record) super.clone();
		} catch (CloneNotSupportedException e) {			
			e.printStackTrace();
			return null;
		}
	}
	
	

}
