/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.access.DBIterator;
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
		this._id = new MidataId(_id.toString());
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && this.getClass().equals(other.getClass())) {
			Model otherModel = (Model) other;
			return _id.equals(otherModel._id);
		}
		return false;
	}
	
	
	
	@Override
	public int hashCode() {
		return _id == null ? 0 : _id.hashCode();
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
	
	protected static <T extends Model> List<T> getAllList(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		try {
			return DBLayer.getAllList(modelClass, collection, properties, fields, limit, null, 1);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	protected static <T extends Model> DBIterator<T> getAllCursor(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		try {
			return DBLayer.getAllCursor(modelClass, collection, properties, fields, limit, null, 1);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	protected static <T extends Model> DBIterator<T> getAllCursor(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields, String sortField, int order, int limit) throws InternalServerException {
		try {
			return DBLayer.getAllCursor(modelClass, collection, properties, fields, limit, sortField, order);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	protected static <T extends Model> List<T> getAllList(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields, int limit, String sortField, int order) throws InternalServerException {
		try {
			return DBLayer.getAllList(modelClass, collection, properties, fields, limit, sortField, order);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	protected static long count(Class modelClass, String collection,
			Map<String, ? extends Object> properties) throws InternalServerException {
		try {
			return DBLayer.count(modelClass, collection, properties);
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
	
	protected void setMultiple(String collection, Collection<String> fields) throws InternalServerException {
		try {
			DBLayer.update(this, collection, fields);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	protected static void setAll(Class model, String collection, Map<String, Object> properties, String field, Object value) throws InternalServerException {
		try {
			DBLayer.set(model, collection, properties, field, value);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}

	@Override
	public String toString() {
		return _id != null ? "obj:"+_id.toString() : "obj-null";
	}

	
}
