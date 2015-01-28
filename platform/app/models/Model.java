package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.db.DBLayer;
import utils.db.DatabaseConversionException;
import utils.db.DatabaseException;

public abstract class Model {

	public ObjectId _id;

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

	protected static <T extends Model> void insert(String collection, T modelObject) throws ModelException {
		try {
			DBLayer.insert(collection, modelObject);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static void delete(String collection, Map<String, ? extends Object> properties) throws ModelException {
		try {
			DBLayer.delete(collection, properties);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static boolean exists(String collection, Map<String, ? extends Object> properties) throws ModelException {
		try {
			return DBLayer.exists(collection, properties);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static <T extends Model> T get(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		try {
			return DBLayer.get(modelClass, collection, properties, fields);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		try {
			return DBLayer.getAll(modelClass, collection, properties, fields);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static void set(Class model, String collection, ObjectId modelId, String field, Object value) throws ModelException {
		try {
			DBLayer.set(model, collection, modelId, field, value);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

}
