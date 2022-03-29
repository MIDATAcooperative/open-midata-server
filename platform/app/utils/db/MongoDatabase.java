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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import models.Model;
import utils.AccessLog;
import utils.access.DBIterator;

/**
 * Connection to a mongoDB database
 *
 */
public class MongoDatabase extends Database {
	
	private String host;
	private int port;
	private String database; // database currently in use
	
	private MongoClient mongoClient; // mongo client is already a connection pool
	private DatabaseConversion conversion = new DatabaseConversion();
	private MongoCredential credential;
	
	private final static UpdateOptions UPSERT = new UpdateOptions().upsert(true);
	
	private final static int MAX_TRIES = 3;
	
	private boolean logQueries = true;
	
	public MongoDatabase(String host, int port, String database, String user, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		if (user != null && password != null) {
		  this.credential = MongoCredential.createCredential(user, database, password.toCharArray());
		}
	}

	/**
	 * Open mongo client.
	 */
	protected void openConnection() throws DatabaseException {
		
		
		if (host.indexOf(",") >= 0) {
			host = host.substring(host.indexOf("/"));
			String hosts[] = host.split(",");
			List<ServerAddress> addr = new ArrayList<ServerAddress>();
			for (String h : hosts) {
				int p = h.indexOf(":");
				addr.add(new ServerAddress(h.substring(0, p), Integer.parseInt(h.substring(p+1))));
			}
			
			 MongoClientOptions.Builder builder = new MongoClientOptions.Builder();			
			 builder.maxConnectionIdleTime(60000);			 
			 MongoClientOptions options = builder.build();
			
			if (credential != null) {		
				mongoClient = new MongoClient(addr, Arrays.asList(this.credential), options);
			} else mongoClient = new MongoClient(addr, options);			
			mongoClient.setReadPreference(ReadPreference.primaryPreferred());
			mongoClient.setWriteConcern(WriteConcern.W1);
			
		} else { 
		
			if (credential != null) {		
				mongoClient = new MongoClient(new ServerAddress(host, port), Arrays.asList(this.credential));
			} else mongoClient = new MongoClient(new ServerAddress(host, port));
				mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
			
		}		
	}
	
	/**
	 * Closes all connections.
	 */
	protected void close() {
		mongoClient.close();
	}

	/**
	 * Sets up the collections and creates all indices.
	 */
	protected void initialize() {		
	}	

	/**
	 * Get a connection to the database in use.
	 */
	com.mongodb.client.MongoDatabase getDB() {
		return mongoClient.getDatabase(database);
	}

	/**
	 * Gets the specified collection.
	 */
	public MongoCollection<DBObject> getCollection(String collection) {
		return getDB().getCollection(collection, DBObject.class);
	}

	/* Database operations */
	/**
	 * Insert a document into the given collection.
	 */
	public <T extends Model> void insert(String collection, T modelObject) throws DatabaseException {
		DBObject dbObject;
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			if (logQueries) AccessLog.logDB("insert into ",collection);
			dbObject = conversion.toDBObject(modelObject);
			getCollection(collection).insertOne(dbObject);	
			return;
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		}
		}
	}
	
	/**
	 * Insert a document into the given collection.
	 */
	public <T extends Model> void upsert(String collection, T modelObject) throws DatabaseException {
		BasicDBObject dbObject;
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			if (logQueries) AccessLog.logDB("upsert ",collection, " ",modelObject.to_db_id().toString());
			dbObject = conversion.toDBObject(modelObject);
			BasicDBObject query = new BasicDBObject();
			query.put("_id", modelObject.to_db_id());
			getCollection(collection).replaceOne(query, dbObject, UPSERT); 
			return;
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		}
		}
	}

	/**
	 * Remove all documents with the given properties from the given collection.
	 */
	public void delete(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			if (logQueries) AccessLog.logDB("delete ",collection," ",properties.toString());
			Bson query = toDBObject(model, properties);
			getCollection(collection).deleteMany(query);
			return;
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		}
		}
	}

	/**
	 * Check whether a document exists that has the given properties.
	 */
	public boolean exists(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			Bson query = toDBObject(model, properties);
			Bson projection = new BasicDBObject("_id", 1);
			if (logQueries) AccessLog.logDB("exists ",collection," ",query.toString());
			return getCollection(collection).find(query).projection(projection).first() != null;
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
		}
		throw new DatabaseException();
	}

	/**
	 * Return the given fields of the object that has the given properties.
	 */
	public <T extends Model> T get(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			Bson query = toDBObject(modelClass, properties);
			Bson projection = toDBObject(fields);
			DBObject dbObject = getCollection(collection).find(query).limit(1).projection(projection).first();
			if (logQueries) AccessLog.logDB("single ",collection," ",query.toString());
			if (dbObject == null) return null;
			return conversion.toModel(modelClass, dbObject);
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
		}
		throw new DatabaseException();
	}

	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public <T extends Model> Set<T> getAll(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {	
	   return getAll(modelClass, collection, properties, fields, 0);
	}
	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public <T extends Model> Set<T> getAll(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields, int limit) throws DatabaseException {
		for (int tries = 0;tries <= MAX_TRIES;tries++) {
			try {
				Bson query = toDBObject(modelClass, properties);
				Bson projection = toDBObject(fields);
				if (logQueries) AccessLog.logDB("all ",collection," ",query.toString());
				FindIterable<DBObject> cursor = getCollection(collection).find(query).projection(projection);
				if (limit!=0) cursor = cursor.limit(limit);			
				return conversion.toModel(modelClass, cursor.iterator());
			} catch (MongoException e) {
				if (tries == MAX_TRIES) throw new DatabaseException(e);
				delay();
			} catch (DatabaseConversionException e) {
				throw new DatabaseException(e);
			}
		}
		throw new DatabaseException();
	}
	
	public <T extends Model> List<T> getAllList(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields, int limit, String sortField, int order) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			Bson query = toDBObject(modelClass, properties);
			Bson projection = toDBObject(fields);
			if (logQueries) AccessLog.logDB("all ",collection," ",query.toString());
			FindIterable<DBObject> cursor = getCollection(collection).find(query).projection(projection);
			if (sortField != null) cursor = cursor.sort(new BasicDBObject(sortField, order));
			if (limit!=0) cursor = cursor.limit(limit);			
			return conversion.toModelList(modelClass, cursor.iterator());
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
		}
		throw new DatabaseException();
	}
	
	public <T extends Model> DBIterator<T> getAllCursor(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields, int limit, String sortField, int order) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			Bson query = toDBObject(modelClass, properties);
			Bson projection = toDBObject(fields);
			if (logQueries) AccessLog.logDB("iter ",collection," ",query.toString());
			FindIterable<DBObject> cursor = getCollection(collection).find(query).projection(projection);
			if (sortField != null) cursor = cursor.sort(new BasicDBObject(sortField, order));
			if (limit!=0) cursor = cursor.limit(limit);			
			return conversion.toModelIterator(modelClass, cursor.iterator());
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
		}
		throw new DatabaseException();
	}
	
	public long count(Class modelClass, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		try {
		  Bson query = toDBObject(modelClass, properties);
		  return getCollection(collection).count(query);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
	}


	/**
	 * Set the given field of the object with the given id.
	 */
	public <T extends Model> void set(Class<T> model, String collection, Object modelId, String field, Object value) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++){
		try {
			Bson query = new BasicDBObject("_id", modelId);
			Bson update = new BasicDBObject("$set", conversion.toDBObject(field, value));
			if (logQueries) AccessLog.logDB("set field ",collection," field=",field," value=",Objects.toString(value));
			getCollection(collection).updateOne(query, update);
			return;
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		}
		}
	}
	
	public <T extends Model> void set(Class<T> model, String collection, Map<String, Object> properties, String field, Object value) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			Bson query = toDBObject(model, properties);
			Bson update = new BasicDBObject("$set", conversion.toDBObject(field, value));
			if (logQueries) AccessLog.logDB("set all ",collection," query=",query.toString()," field=",field," to=", Objects.toString(value));
			getCollection(collection).updateMany(query, update);
			return;
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		}
		}
	}
	
	/**
	 * Set the given fields of the object with the given id.
	 */
	public <T extends Model> void update(T model, String collection, Collection<String> fields) throws DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			if (logQueries) AccessLog.logDB("update ",collection, " ",model.to_db_id().toString());
			BasicDBObject query = new BasicDBObject();
			query.put("_id", model.to_db_id());
			
			
			
			BasicDBObject updateContent = new BasicDBObject();
			for (String field : fields) {
				updateContent.put(field, conversion.toDBObjectValue(model.getClass().getField(field).get(model)));
			}
			long ts = System.currentTimeMillis();
			
			BasicDBObject update = new BasicDBObject("$set", updateContent);
		
			getCollection(collection).updateOne(query, update);
			return;														
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		} catch (IllegalAccessException e3) {
			throw new DatabaseException(e3);
		} catch (NoSuchFieldException e4) {
			throw new DatabaseException(e4);
		}
		}
	}
	
	/**
	 * Set the given field of the object with the given id.
	 */
	public <T extends Model> void secureUpdate(T model, String collection, String timestampField, String[] fields) throws LostUpdateException, DatabaseException {
		for (int tries=0;tries<=MAX_TRIES;tries++) {
		try {
			if (logQueries) AccessLog.logDB("secure update ",collection," ",model.to_db_id().toString());
			BasicDBObject query = new BasicDBObject();
			query.put("_id", model.to_db_id());
			
			Object oldTimeStamp = model.getClass().getField(timestampField).get(model);
			query.put(timestampField, oldTimeStamp);
			
			BasicDBObject updateContent = new BasicDBObject();
			for (String field : fields) {
				updateContent.put(field, conversion.toDBObjectValue(model.getClass().getField(field).get(model)));
			}
			long ts = System.currentTimeMillis();
			updateContent.put(timestampField, ts);
			BasicDBObject update = new BasicDBObject("$set", updateContent);
		
			DBObject result = getCollection(collection).findOneAndUpdate(query, update);
			if (result == null) {
				if (logQueries) AccessLog.log("failed secure update version: ", Objects.toString(oldTimeStamp)); 
				throw new LostUpdateException();
			}
			
			model.getClass().getField(timestampField).set(model, ts);
			if (logQueries) AccessLog.log("secure updated: ", Objects.toString(oldTimeStamp), " -> ", Long.toString(ts));
		    return;									
		} catch (MongoException e) {
			if (tries==MAX_TRIES) throw new DatabaseException(e);
			delay();
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		} catch (IllegalAccessException e3) {
			throw new DatabaseException(e3);
		} catch (NoSuchFieldException e4) {
			throw new DatabaseException(e4);
		}
		}
	}

	/**
	 * Convert the properties map to a database object. If an array is given as the value, use the $in operator.
	 */
	private BasicDBObject toDBObject(Class model, Map<String, ? extends Object> properties) throws DatabaseConversionException {
		BasicDBObject dbObject = new BasicDBObject();
		for (String key : properties.keySet()) {
			Object property = properties.get(key);
			if (property instanceof Collection<?> && !key.startsWith("$")) {
				if (((Collection<?>) property).size() == 1) {
					dbObject.put(key, conversion.toDBObjectValue(((Collection<?>) property).iterator().next()));
				} else {				
					ArrayList al = new ArrayList();
					for (Object v : ((Collection<?>) property)) al.add(conversion.toDBObjectValue(v));
					dbObject.put(key, new BasicDBObject("$in", al));
				}
			} else if (key.equals("$and") || key.equals("$or")) {
				ArrayList al = new ArrayList();
				for (Object v : ((Collection<?>) property)) al.add(toDBObject(model, (Map<String, Object>) v));
				dbObject.put(key, al);
			} else if (property instanceof Map<?, ?>) {
				BasicDBObject dbo = new BasicDBObject();
				Map propertyMap = (Map<?,?>) property;
				for (Object k : propertyMap.keySet()) {
					dbo.put(k.toString(), conversion.toDBObjectValue(propertyMap.get(k)));
				}
				dbObject.put(key,  dbo);
			} else {
				dbObject.put(key, conversion.toDBObjectValue(property));
			}
		}
		return dbObject;
	}

	/**
	 * Convert the fields set to a database object. Project to all fields given.
	 */
	private BasicDBObject toDBObject(Set<String> fields) {
		BasicDBObject dbObject = new BasicDBObject();
		for (String field : fields) {
			dbObject.put(field, 1);
		}
		return dbObject;
	}
	
	private void delay() {
		try { Thread.sleep(100); } catch (InterruptedException e) {}
	}
		
}
