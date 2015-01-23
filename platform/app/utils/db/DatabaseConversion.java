package utils.db;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class DatabaseConversion {

	private Object encrypt(Class model, String path, Object source) {
		if (source instanceof Enum) source = ((Enum) source).name();
		return source;
	}
	
	private Object decrypt(Class model, String path, Class type, Object source) {
		if (type.isEnum()) {
			source = Enum.valueOf(type, (String) source);
		}
		return source;
	}
	
	/**
	 * Turns a model into a database object.
	 */
	public <T extends Model> DBObject toDBObject(T modelObject) throws DatabaseConversionException {
		DBObject dbObject = new BasicDBObject();
		Class model = modelObject.getClass();
		for (Field field : model.getFields()) {
			try {
				dbObject.put(field.getName(), encrypt(model, field.getName(), field.get(modelObject)));
			} catch (IllegalArgumentException e) {
				throw new DatabaseConversionException(e);
			} catch (IllegalAccessException e) {
				throw new DatabaseConversionException(e);
			}
		}
		return dbObject;
	}

	/**
	 * Converts a database object back into a model.
	 */
	public <T extends Model> T toModel(Class<T> modelClass, DBObject dbObject)
			throws DatabaseConversionException {
		T modelObject;
		try {
			modelObject = modelClass.newInstance();
		} catch (InstantiationException e) {
			throw new DatabaseConversionException(e);
		} catch (IllegalAccessException e) {
			throw new DatabaseConversionException(e);
		}
		for (Field field : modelClass.getFields()) {
			if (dbObject.keySet().contains(field.getName())) {
				try {
					field.set(modelObject, convert(modelClass, field.getName(), field.getGenericType(), decrypt(modelClass, field.getName(), field.getType(), dbObject.get(field.getName()))));
				} catch (IllegalArgumentException e) {
					throw new DatabaseConversionException(e);
				} catch (IllegalAccessException e) {
					throw new DatabaseConversionException(e);
				}
			}
		}
		return modelObject;
	}

	/**
	 * Converts a set of database objects to models.
	 */
	public <T extends Model> Set<T> toModel(Class<T> modelClass, Set<DBObject> dbObjects)
			throws DatabaseConversionException {
		Set<T> models = new HashSet<T>();
		for (DBObject dbObject : dbObjects) {
			models.add(toModel(modelClass, dbObject));
		}
		return models;
	}

	/**
	 * Converts an object retrieved from the database to the corresponding type.
	 */
	private Object convert(Class model, String path, Type type, Object value) {
		
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;			
			if (parameterizedType.getRawType().equals(Map.class)) {
				return convertToMap(model, path, type, value);
			} else if (parameterizedType.getRawType().equals(Set.class)) {
				return convertToSet(model, path, type, value);
			} else if (parameterizedType.getRawType().equals(List.class)) {
				return convertToList(model, path, type, value);
			}
		}
		return value;
	}

	/**
	 * Converts a BasicDBObject into a map (keys are always strings because of JSON serialization).
	 */
	private Map<String, Object> convertToMap(Class model, String path, Type type, Object value) {
		BasicDBObject dbObject = (BasicDBObject) value;
		if (type instanceof ParameterizedType) {
			Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
			Map<String, Object> map = new HashMap<String, Object>();
			for (String key : dbObject.keySet()) {
				String fullpath = path+"."+key;
				map.put(key, convert(model, fullpath, valueType, decrypt(model, fullpath, Object.class, dbObject.get(key))));
			}
			return map;
		} else {
			return new HashMap<String, Object>(dbObject);
		}
	}

	/**
	 * Converts a BasicDBList into a set.
	 */
	private Set<Object> convertToSet(Class model, String path, Type type, Object value) {
		BasicDBList dbList = (BasicDBList) value;
		if (type instanceof ParameterizedType) {
			Type valueType = ((ParameterizedType) type).getActualTypeArguments()[0];
			Set<Object> set = new HashSet<Object>();
			String fullpath = path+"[]";
			for (Object element : dbList) {
				set.add(convert(model, fullpath, valueType, decrypt(model, fullpath,  Object.class, element)));
			}
			return set;
		} else {
			return new HashSet<Object>(dbList);
		}
	}

	/**
	 * Converts a BasicDBList into a list.
	 */
	private List<Object> convertToList(Class model, String path, Type type, Object value) {
		BasicDBList dbList = (BasicDBList) value;
		if (type instanceof ParameterizedType) {
			Type valueType = ((ParameterizedType) type).getActualTypeArguments()[0];
			List<Object> list = new ArrayList<Object>();
			String fullpath = path+"[]";
			for (Object element : dbList) {
				list.add(convert(model, fullpath, valueType, decrypt(model, fullpath,  Object.class, element)));
			}
			return list;
		} else {
			return new ArrayList<Object>(dbList);
		}
	}

}
