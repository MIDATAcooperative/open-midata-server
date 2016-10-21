package models;

import java.util.Map;
import java.util.Set;

import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Abstract base class for all classes that are stored in the mongo database.
 *
 */
public abstract class Model implements JsonSerializable {

	/**
	 * The unique _id field from the mongo record
	 */
	@NotMaterialized
	public MidataId _id;
	
	/**
	 * getter for _id field
	 * @return
	 */
	public Object to_db_id() {
		return _id.toObjectId();
	}
	
	/**
	 * setter for _id field
	 * @param _id value of _id field
	 */
	public void set_id(Object _id) {
		_id = new MidataId(_id.toString());
	}

	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			Model otherModel = (Model) other;
			return _id.equals(otherModel._id);
		}
		return false;
	}
	
	/**
	 * Fallback method for comparison of models.
	 */
	public int compareTo(Model other) {
		return this._id.compareTo(other._id);
	}

	protected static <T extends Model> void insert(String collection, T modelObject) throws InternalServerException {
		try {
			DBLayer.insert(collection, modelObject);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal.db", e);
		}
	}
	
	protected static <T extends Model> void upsert(String collection, T modelObject) throws InternalServerException {
		try {
			DBLayer.upsert(collection, modelObject);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

	protected static <T extends Model> void delete(Class<T> modelClass, String collection, Map<String, ? extends Object> properties) throws InternalServerException {
		try {
			DBLayer.delete(modelClass, collection, properties);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

	protected static <T extends Model> boolean exists(Class<T> modelClass, String collection, Map<String, ? extends Object> properties) throws InternalServerException {
		try {
			return DBLayer.exists(modelClass, collection, properties);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

	protected static <T extends Model> T get(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		try {
			return DBLayer.get(modelClass, collection, properties, fields);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

	protected static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		try {
			return DBLayer.getAll(modelClass, collection, properties, fields);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	protected static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		try {
			return DBLayer.getAll(modelClass, collection, properties, fields, limit);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

	protected static void set(Class model, String collection, MidataId modelId, String field, Object value) throws InternalServerException {
		try {
			DBLayer.set(model, collection, modelId.toObjectId(), field, value);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

}
