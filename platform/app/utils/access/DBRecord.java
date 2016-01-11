package utils.access;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.Model;
import models.Record;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class DBRecord extends Model implements Comparable<DBRecord>, Cloneable {

	private static final String collection = "records";
	
	/**
	 * the id of the stream this record belongs to.
	 * 
	 * This field is null for stream records.
	 * This field is not encrypted but stored in the database
	 */
	public ObjectId stream; 
	
	/**
	 * a rounded timestamp
	 * 
	 * This field is only set for records stored in MEDIUM security streams, otherwise it is 0.
	 * This field is not encrypted but stored in the database.
	 */
	public long time;
	
	/**
	 * the id of the document record this record belongs to.
	 * 
	 * This field is null for all records that are not part of a document
	 * This field is not encrypted but stored in the database.
	 * 
	 */
	public ObjectId document;
	
	/**
	 * a part name for records that are part of a document,
	 * 
	 * This field is null for all records that are not part of a document
	 * This field is not encrypted but stored in the database
	 */
	public String part;

	public BSONObject meta;
		
	/**
	 * the encrypted meta data of this record
	 */
	public byte[] encrypted;
	
	
	/**
	 * the unencrypted record data. 
	 * 
	 * May be any JSON data.
	 * The contents of this field is encrypted into encryptedData field and not directly stored in the database.
	 */
	public BSONObject data; 
	
	public Map<String, BSONObject> versions;
	
	public Map<String, byte[]> encryptedVersions;
	
	/**
	 * the encrypted "data" field of this record
	 */
	public byte[] encryptedData;
	
	/**
	 * this field is true for records stored in medium security streams
	 */
	public @NotMaterialized boolean direct;
	
	/**
	 * an alternative ID for this record that has the access permission set ID also encoded in the ID.
	 * 
	 * This field is computed upon request and is neither stored in the database nor contained in the encrypted part
	 */
	public @NotMaterialized String id;
	
	/**
	 * the id of the owner of this record. 
	 * 
	 * The owner is the person the data of this record belongs to.
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * The owner information is derived from information of the APS this record is stored in or 
	 * from the owner of the stream this record is stored in.
	 */
	public @NotMaterialized ObjectId owner; // person the record is about
			
	
	/**
	 * The AES key that is used to encrypt/decrypt this record
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * This field is stored in the APS this record is stored in
	 * This field MUST be cleared by the RecordManager if the record is returned outside of the RecordManager. 
	 */
	public @NotMaterialized byte[] key;
		
	/**
	 * The group name where the record will be placed in the record tree
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 */
	public @NotMaterialized String group;
	
	/**
	 * Is this record a stream record?
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * This field is only internally used.
	 */
	public @NotMaterialized boolean isStream;
	
	/**
	 * Is this record
	 */
	public @NotMaterialized boolean isReadOnly;
	
	public DBRecord() { meta = new BasicBSONObject(); }			
	
	public void clearEncryptedFields() {
		this.meta = null;		
		this.versions = null;			
		this.data = null;
	}
	
	public void clearSecrets() {
		this.key = null;
		this.encrypted = null;
		this.encryptedData = null;
		this.encryptedVersions = null;
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(DBRecord.class, collection, properties);
	}

	
	public static DBRecord get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(DBRecord.class, collection, properties, fields);
	}
	
	public static DBRecord getById(ObjectId id, Set<String> fields) throws InternalServerException {
		return Model.get(DBRecord.class, collection, CMaps.map("_id", id), fields);
	}

	public static Set<DBRecord> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(DBRecord.class, collection, properties, fields);
	}
	
	public static Set<DBRecord> getAllByIds(Set<ObjectId> ids, Set<String> fields) throws InternalServerException {
		return Model.getAll(DBRecord.class, collection, CMaps.map("_id", ids), fields);
	}		
	
	public static void set(ObjectId recordId, String field, Object value) throws InternalServerException {
		Model.set(DBRecord.class, collection, recordId, field, value);
	}

	public static void add(DBRecord record) throws InternalServerException {
		Model.insert(collection, record);	
	}
	
	public static void upsert(DBRecord record) throws InternalServerException {
		Model.upsert(collection, record);
	}

	public static void delete(ObjectId ownerId, ObjectId recordId) throws InternalServerException {			
		Model.delete(DBRecord.class, collection, new ChainedMap<String, ObjectId>().put("_id", recordId).get());
	}

	@Override
	public DBRecord clone() {	
		try {
			return (DBRecord) super.clone();
		} catch (CloneNotSupportedException e) {			
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public int compareTo(DBRecord other) {
		Date dme = (Date) this.meta.get("created");
		Date dother = (Date) other.meta.get("created");
		if (dme != null && dother != null) {
			// newest first
			return -dme.compareTo(dother);
		} else {
			return super.compareTo(other);
		}
	}

}
