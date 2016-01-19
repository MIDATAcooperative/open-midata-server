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

import models.JsonSerializable;
import models.Model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Converter between data model classes and BSON objects
 *
 */
public class DatabaseConversion {
	
	private Object resolveEnums(Class type, Object source) {
		if (type.isEnum()) {
			source = Enum.valueOf(type, (String) source);
		}
		return source;
	}
	
	private Object todb(Object inp) throws DatabaseConversionException {
		if (inp instanceof Enum) return ((Enum) inp).name();
		if (inp instanceof Set) {
			Set result = new HashSet();
			for (Object obj : (Set) inp) {
				result.add(todb(obj));
			}
			return result;
		}
			
		if (inp instanceof JsonSerializable) return toDBObject((JsonSerializable) inp);
		if (inp instanceof List) {
			List result = new ArrayList();
			for (Object obj : (List) inp) {
				result.add(todb(obj));
			}
			return result;
		}		
		if (inp instanceof Map) {
			Map result = new HashMap();
			for (Object key : ((Map) inp).keySet()) {
				result.put(key, todb(((Map) inp).get(key)));
			}
			return result;
		}
		return inp;
	}
	
	/**
	 * Turns a model into a database object.
	 */
	public <T extends JsonSerializable> DBObject toDBObject(T modelObject) throws DatabaseConversionException {
		DBObject dbObject = new BasicDBObject();
		Class model = modelObject.getClass();
		for (Field field : model.getFields()) {
			if (field.getAnnotation(NotMaterialized.class)!=null) continue;
			try {
				Object val = todb(field.get(modelObject));
				if (val != null) dbObject.put(field.getName(), val);
			} catch (IllegalArgumentException e) {
				throw new DatabaseConversionException(e);
			} catch (IllegalAccessException e) {
				throw new DatabaseConversionException(e);
			}
		}
		if (modelObject instanceof Model) {
			dbObject.put("_id", ((Model) modelObject).get_id()); 
		}
		return dbObject;
	}
	
	public BasicDBObject toDBObject(String field, Object value) throws DatabaseConversionException {
		return new BasicDBObject(field, todb(value));
	}
	
	public Object toDBObjectValue(Object value) throws DatabaseConversionException {
		return todb(value);
	}

	/**
	 * Converts a database object back into a model.
	 */
	public <T extends JsonSerializable> T toModel(Class<T> modelClass, DBObject dbObject)
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
					field.set(modelObject, convert(modelClass, field.getName(), field.getGenericType(), resolveEnums(field.getType(), dbObject.get(field.getName()))));
				} catch (IllegalArgumentException e) {
					throw new DatabaseConversionException(e);
				} catch (IllegalAccessException e) {
					throw new DatabaseConversionException(e);
				}
			}
		}
		if (modelObject instanceof Model) {
			((Model) modelObject).set_id(dbObject.get("_id"));
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
	private Object convert(Class model, String path, Type type, Object value) throws DatabaseConversionException {
		
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
		if (type instanceof Class) {
			Class c = (Class) type;
			if (JsonSerializable.class.isAssignableFrom(c)) {
			  return toModel(c, (DBObject) value);
			}
		}
		return value;
	}

	/**
	 * Converts a BasicDBObject into a map (keys are always strings because of JSON serialization).
	 */
	private Map<String, Object> convertToMap(Class model, String path, Type type, Object value) throws DatabaseConversionException {
		if (value == null) return null;
		BasicDBObject dbObject = (BasicDBObject) value;
		if (type instanceof ParameterizedType) {
			Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
			Map<String, Object> map = new HashMap<String, Object>();
			for (String key : dbObject.keySet()) {
				String fullpath = path+"."+key;
				Class<?> cl = valueType instanceof Class ? (Class<?>) valueType : Object.class;
				map.put(key, convert(model, fullpath, valueType, resolveEnums(cl, dbObject.get(key))));
			}
			return map;
		} else {
			return new HashMap<String, Object>(dbObject);
		}
	}

	/**
	 * Converts a BasicDBList into a set.
	 */
	private Set<Object> convertToSet(Class model, String path, Type type, Object value) throws DatabaseConversionException {
		if (value == null) return null;
		BasicDBList dbList = (BasicDBList) value;		
		if (type instanceof ParameterizedType) {
			Type valueType = ((ParameterizedType) type).getActualTypeArguments()[0];
						
			Set<Object> set = new HashSet<Object>();
			String fullpath = path+"[]";
			for (Object element : dbList) {
				set.add(convert(model, fullpath, valueType, resolveEnums((Class<?>) valueType, element)));
			}
			return set;
		} else {
			return new HashSet<Object>(dbList);
		}
	}

	/**
	 * Converts a BasicDBList into a list.
	 */
	private List<Object> convertToList(Class model, String path, Type type, Object value) throws DatabaseConversionException {
		if (value==null) return null;
		BasicDBList dbList = (BasicDBList) value;
		if (type instanceof ParameterizedType) {
			Type valueType = ((ParameterizedType) type).getActualTypeArguments()[0];
			List<Object> list = new ArrayList<Object>();
			String fullpath = path+"[]";
			for (Object element : dbList) {
				list.add(convert(model, fullpath, valueType, resolveEnums((Class<?>) valueType, element)));
			}
			return list;
		} else {
			return new ArrayList<Object>(dbList);
		}
	}

}
