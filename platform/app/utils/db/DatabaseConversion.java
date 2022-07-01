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

package utils.db;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import models.JsonSerializable;
import models.MidataId;
import models.Model;
import utils.AccessLog;
import utils.access.DBIterator;
import utils.access.ProcessingTools;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * Converter between data model classes and BSON objects
 *
 */
public class DatabaseConversion {
	
	private static final String DOT = Pattern.quote("[dot]"); 
	
	private Object resolveEnums(Class type, Object source) {
		if (type.isEnum()) {
			source = Enum.valueOf(type, (String) source);
		}
		return source;
	}
	
	private Object todb(Object inp) throws DatabaseConversionException {
		if (inp instanceof MidataId) {
			MidataId mid = (MidataId) inp;
			if (mid.isLocal()) return mid.toObjectId(); else return mid.toString();
		}
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
				Object key1 = key;
				if (key != null && key instanceof String) key1 = ((String) key).replace(".","[dot]");
				if (key1 != null && key1.toString().startsWith("$")) {
				  result.put("_"+key1.toString(), todb(((Map) inp).get(key)));
				} else {
				  result.put(key1, todb(((Map) inp).get(key)));
				}
			}
			return result;
		}		
		return inp;
	}
	
	/**
	 * Turns a model into a database object.
	 */
	public <T extends JsonSerializable> BasicDBObject toDBObject(T modelObject) throws DatabaseConversionException {
		BasicDBObject dbObject = new BasicDBObject();
		Class model = modelObject.getClass();
		for (Field field : model.getFields()) {
			if (field.getAnnotation(NotMaterialized.class)!=null) continue;			
			try {
				Object val = todb(field.get(modelObject));
				if (val != null) dbObject.put(field.getName(), val);
				else if (field.getAnnotation(IncludeNullValues.class)!=null) dbObject.put(field.getName(), val);
			} catch (IllegalArgumentException e) {
				throw new DatabaseConversionException(e);
			} catch (IllegalAccessException e) {
				throw new DatabaseConversionException(e);
			}
		}
		if (modelObject instanceof Model) {
			dbObject.put("_id", ((Model) modelObject).to_db_id()); 
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
		Converter[] conv = transformations.get(modelClass);
		if (conv == null) conv = build(modelClass);
		return toModel(modelClass, conv, dbObject);
		/*
		T modelObject;
		try {
			modelObject = modelClass.newInstance();
		
		for (Field field : modelClass.getFields()) {
			if (!field.getName().equals("_id")) {
				final Object value = dbObject.get(field.getName());
				if (value != null) field.set(modelObject, convert(modelClass, field.getName(), field.getGenericType(), resolveEnums(field.getType(), value)));			
			}
		}
		if (modelObject instanceof Model) {
			((Model) modelObject).set_id(dbObject.get("_id"));			
		}
		
		} catch (InstantiationException e) {
			throw new DatabaseConversionException(e);
		} catch (IllegalAccessException e) {
			throw new DatabaseConversionException(e);
		} catch (IllegalArgumentException e) {
			throw new DatabaseConversionException(e);
		} 
		
		return modelObject;*/
	}
	
	public <T extends JsonSerializable> T toModel(Class<T> modelClass, Converter[] converters, DBObject dbObject)
			throws DatabaseConversionException {
		T modelObject;
		try {
			modelObject = modelClass.newInstance();
			for (Converter conv : converters) {
				final Object value = dbObject.get(conv.fieldName);
				if (value != null) conv.field.set(modelObject, conv.convert(value));
			}
		    if (modelObject instanceof Model) {
			  ((Model) modelObject).set_id(dbObject.get("_id"));			
		    }		
		} catch (InstantiationException e) {
			throw new DatabaseConversionException(e);
		} catch (IllegalAccessException e) {
			throw new DatabaseConversionException(e);
		} catch (IllegalArgumentException e) {
			throw new DatabaseConversionException(e);
		} 
		
		return modelObject;
	}

	/**
	 * Converts a set of database objects to models.
	 */
	public <T extends Model> Set<T> toModel(Class<T> modelClass, Iterator<DBObject> dbObjects)
			throws DatabaseConversionException {
		Converter[] conv = transformations.get(modelClass);
		if (conv == null) conv = build(modelClass);
		Set<T> models = new HashSet<T>();
		while (dbObjects.hasNext()) {		
		  models.add(toModel(modelClass, conv, dbObjects.next()));
		}
		return models;
	}
	
	/**
	 * Converts a set of database objects to models.
	 */
	public <T extends Model> List<T> toModelList(Class<T> modelClass, Iterator<DBObject> dbObjects)
			throws DatabaseConversionException {
		Converter[] conv = transformations.get(modelClass);
		if (conv == null) conv = build(modelClass);
		if (!dbObjects.hasNext()) return Collections.emptyList();
		List<T> models = new ArrayList<T>();
		do {		
		  models.add(toModel(modelClass, conv, dbObjects.next()));
		} while (dbObjects.hasNext());
		return models;
	}
	
	public <T extends Model> DBIterator<T> toModelIterator(Class<T> modelClass, MongoCursor<DBObject> dbObjects)
			throws DatabaseConversionException {
		Converter[] conv = transformations.get(modelClass);
		if (conv == null) conv = build(modelClass);
		if (!dbObjects.hasNext()) return ProcessingTools.empty();
		return new ConvertIterator(this, dbObjects, modelClass, conv);		
	}
	
	public static class ConvertIterator<T extends Model> implements DBIterator<T> {
		MongoCursor<DBObject> source;
		Class<T> modelClass;
		Converter[] conv;
		DatabaseConversion me;
		
		ConvertIterator(DatabaseConversion me, MongoCursor<DBObject> source, Class<T> modelClass, Converter[] conv) {
			this.me = me;
			this.source = source;
			this.modelClass = modelClass;
			this.conv = conv;					
		}

		@Override
		public T next() throws AppException {
			try {
				return me.toModel(modelClass, conv, source.next());
			} catch (DatabaseConversionException e) {
				throw new InternalServerException("error.internal", e);
			}
		}

		@Override
		public boolean hasNext() throws AppException {
			return source.hasNext();
		}

		@Override
		public void close() {
			source.close();			
		}
		
		
		
	}

	/**
	 * Converts an object retrieved from the database to the corresponding type.
	 *//*
	private Object convert(Class model, String path, Type type, Object value) throws DatabaseConversionException {
		
		if (type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) type).getRawType();			
			if (rawType.equals(Map.class)) {
				return convertToMap(model, path, type, value);
			} else if (rawType.equals(Set.class)) {
				return convertToSet(model, path, type, value);
			} else if (rawType.equals(List.class)) {
				return convertToList(model, path, type, value);
			}
		}
		if (type instanceof Class) {
			Class c = (Class) type;
			if (JsonSerializable.class.isAssignableFrom(c)) {
			  return toModel(c, (DBObject) value);
			}			
		}
		if (value instanceof ObjectId) return new MidataId(value.toString());
		return value;
	}*/

	/**
	 * Converts a BasicDBObject into a map (keys are always strings because of JSON serialization).
	 *//*
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
	}*/

	/**
	 * Converts a BasicDBList into a set.
	 *//*
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
	}*/

	/**
	 * Converts a BasicDBList into a list.
	 *//*
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
	}*/

	private Map<Class, Converter[]> transformations = new HashMap<Class, Converter[]>();
	
	private Converter[] build(Class modelClass) {
		AccessLog.log("build: ", modelClass.getName());
		Converter[] c = new Converter[0];
		ArrayList<Converter> allConv = new ArrayList<Converter>();
		for (Field field : modelClass.getFields()) {
			if (!field.getName().equals("_id")) {
				final String name = field.getName();
				Converter conv = build(field.getType(), field.getGenericType());
												
				conv.fieldName = name;
				conv.field = field;
				allConv.add(conv);									
			}
		}
		Converter[] result = allConv.toArray(c);
		transformations.put(modelClass, result);
		return result;
	}
	
	private Converter build(Class type, Type genType) {
		Converter conv = null;
		
		if (type.isEnum()) {
			conv = new ConvertEnum(type);
		}
		
		if (genType instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) genType).getRawType();																				
			
			if (rawType.equals(Map.class)) {
				Type valueType = ((ParameterizedType) genType).getActualTypeArguments()[1];
				Class<?> cl = valueType instanceof Class ? (Class<?>) valueType : Object.class;
				Converter subconv = build(cl, valueType);
				if (subconv instanceof ConvertStd) subconv = new ConvertStdMap();
				conv = new ConvertMap(subconv);
			} else if (rawType.equals(Set.class)) {
				Type valueType = ((ParameterizedType) genType).getActualTypeArguments()[0];
				Class<?> cl = valueType instanceof Class ? (Class<?>) valueType : Object.class;
				Converter subconv = build(cl, valueType);
				conv = new ConvertSet(subconv);
			} else if (rawType.equals(List.class)) {
				Type valueType = ((ParameterizedType) genType).getActualTypeArguments()[0];
				Class<?> cl = valueType instanceof Class ? (Class<?>) valueType : Object.class;
				Converter subconv = build(cl, valueType);
				conv = new ConvertList(subconv);						
			}
		}
		if (genType instanceof Class) {
			Class c = (Class) genType;
			if (JsonSerializable.class.isAssignableFrom(c)) {
			  conv = new ConvertType(c);
			}			
		}
		
		if (conv == null) conv = new ConvertStd();		
		return conv;
	}
	
	abstract class Converter {
		String fieldName;
		Field field;
		abstract Object convert(Object input) throws DatabaseConversionException;
	}
	
	class ConvertEnum extends Converter {
		private Class type;
		
		ConvertEnum(Class type) { this.type = type; }
		
		public Object convert(Object input) throws DatabaseConversionException {
			return Enum.valueOf(type, (String) input);
		}		
	}
	
	class ConvertType extends Converter {
		private Class c;
		
		ConvertType(Class type) { this.c = type; }
		
		public Object convert(Object input) throws DatabaseConversionException {
			return toModel(c, (DBObject) input);
		}		
	}
	
	class ConvertMap extends Converter {		
		private Converter conv;
		
		
		
		ConvertMap(Converter conv) { 			
			this.conv = conv; 
		}
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input == null) return null;
			BasicDBObject dbObject = (BasicDBObject) input;
			
			Map<String, Object> map = new HashMap<String, Object>();
			for (Map.Entry<String,Object> entry : dbObject.entrySet()) {				
				String k= entry.getKey().replaceAll(DOT, ".");					
				if (k.startsWith("_$"))
					map.put(k.substring(1), conv.convert(entry.getValue()));
				else 
					map.put(k, conv.convert(entry.getValue()));
			}
			return map;			
		}		
	}
	
	class ConvertMapFast extends Converter {		
		
		ConvertMapFast() {  }
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input == null) return null;
			BasicDBObject dbObject = (BasicDBObject) input;			
			return new HashMap<String, Object>(dbObject);			
		}		
	}
	
	class ConvertList extends Converter {		
		private Converter conv;
		
		ConvertList(Converter conv) { 		
			this.conv = conv;
		}
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input == null) return null;
			BasicDBList dbList = (BasicDBList) input;
						
			List<Object> list = new ArrayList<Object>(dbList.size());		
			for (Object element : dbList) {
				list.add(conv.convert(element));
			}
			
			return list;			
		}		
	}
	
	class ConvertListFast extends Converter {		
		
		ConvertListFast() {  }
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input == null) return null;
			BasicDBList dbList = (BasicDBList) input;			
			return new ArrayList<Object>(dbList);			
		}		
	}
	
	class ConvertSet extends Converter {		
		private Converter conv;
		
		ConvertSet(Converter conv) { 			
			this.conv = conv; 
		}
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input == null) return null;
			BasicDBList dbList = (BasicDBList) input;
						
			Set<Object> list = new HashSet<Object>();		
			for (Object element : dbList) {
				list.add(conv.convert(element));
			}
			
			return list;			
		}		
	}
	
	class ConvertSetFast extends Converter {		
		
		ConvertSetFast() {  }
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input == null) return null;
			BasicDBList dbList = (BasicDBList) input;			
			return new HashSet<Object>(dbList);			
		}		
	}
				
	
	class ConvertStd extends Converter {
				
		ConvertStd() {  }
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input instanceof ObjectId) return new MidataId(input.toString());			
			return input;
		}		
	}
	
	class ConvertStdMap extends Converter {
		
		ConvertStdMap() {  }
		
		public Object convert(Object input) throws DatabaseConversionException {
			if (input instanceof ObjectId) return new MidataId(input.toString());
			else if (input instanceof Map) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (Map.Entry<String,Object> entry : ((Map<String, Object>) input).entrySet()) {					
					String k= entry.getKey().replaceAll(DOT, ".");					
					if (entry.getKey().startsWith("_$"))
						map.put(k.substring(1), convert(entry.getValue()));
					else 
						map.put(k, convert(entry.getValue()));
				}
				return map;
			} else if (input instanceof List) {
				List<Object> list = new ArrayList<Object>(((List) input).size());		
				for (Object element : (List) input) {
					list.add(convert(element));
				}
				return list;
			} else if (input instanceof Set) {
				Set<Object> list = new HashSet<Object>(((Set) input).size());		
				for (Object element : (Set) input) {
					list.add(convert(element));
				}
				return list;
			}
			return input;
		}		
	}
	
	
}
