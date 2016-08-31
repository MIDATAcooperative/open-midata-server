package utils.access;

import java.util.Set;

import models.Model;
import models.Record;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class VersionedDBRecord extends DBRecord {

	private static final String collection = "vrecords";
	
	@NotMaterialized
	public final static String INITIAL_VERSION = "0";
	
	@NotMaterialized
	public String version;
	
	public VersionedDBRecord() {}
	
	public VersionedDBRecord(DBRecord rec) {	
		this._id = rec._id;
		this.stream = rec.stream; 
		this.time = rec.time;
		this.document = rec.document;
		this.part = rec.part;
		this.meta = rec.meta;
		this.data = rec.data;
		this.owner = rec.owner;
		this.key = rec.key;
		this.security = rec.security;
		this.version = rec.meta.getString("version");
		if (this.version == null) this.version = INITIAL_VERSION;
	}
	
	/**
	 * getter for _id field
	 * @return
	 */
	public Object get_id() {
		return new BasicBSONObject("_id", _id).append("version", version);
	}
	
	/**
	 * setter for _id field
	 * @param _id value of _id field
	 */
	public void set_id(Object _id) {
		BasicBSONObject obj = (BasicBSONObject) _id;
		_id = obj.get("_id");
		version = (String) obj.getString("version");
	}
	
	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			VersionedDBRecord otherModel = (VersionedDBRecord) other;
			return _id.equals(otherModel._id) && version.equals(otherModel.version);
		}
		return false;
	}
	
	/**
	 * Fallback method for comparison of models.
	 */
	public int compareTo(VersionedDBRecord other) {
		int r = this._id.compareTo(other._id);
		return r == 0 ? this.version.compareTo(other.version) : r;
	}
	
	public static void add(VersionedDBRecord record) throws InternalServerException {
		Model.insert(collection, record);	
	}
	
	public static Set<VersionedDBRecord> getAllById(Set<ObjectId> ids, Set<String> fields) throws InternalServerException {
		return Model.getAll(VersionedDBRecord.class, collection, CMaps.map("_id", ids), fields);
	}
}
