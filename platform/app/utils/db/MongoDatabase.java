package utils.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

import models.Model;
import utils.AccessLog;
import utils.collections.CollectionConversion;

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
		//String host = Play.application().configuration().getString("mongo.host");
		//int port = Play.application().configuration().getInt("mongo.port");
		//database = Play.application().configuration().getString("mongo.database");
		//try {
		
		if (credential != null) {		
			mongoClient = new MongoClient(new ServerAddress(host, port), Arrays.asList(this.credential));
		} else mongoClient = new MongoClient(new ServerAddress(host, port));
			mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
		/*} catch (UnknownHostException e) {	
			e.printStackTrace();
			throw new DatabaseException(e);
		}*/
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
		// TODO
	}	

	/**
	 * Get a connection to the database in use.
	 */
	DB getDB() {
		return mongoClient.getDB(database);
	}

	/**
	 * Gets the specified collection.
	 */
	public DBCollection getCollection(String collection) {
		return getDB().getCollection(collection);
	}

	/* Database operations */
	/**
	 * Insert a document into the given collection.
	 */
	public <T extends Model> void insert(String collection, T modelObject) throws DatabaseException {
		DBObject dbObject;
		try {
			if (logQueries) AccessLog.logDB("insert into "+collection);
			dbObject = conversion.toDBObject(modelObject);
			getCollection(collection).insert(dbObject);			
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		}
	}
	
	/**
	 * Insert a document into the given collection.
	 */
	public <T extends Model> void upsert(String collection, T modelObject) throws DatabaseException {
		DBObject dbObject;
		try {
			if (logQueries) AccessLog.logDB("upsert "+collection+ " "+modelObject.to_db_id().toString());
			dbObject = conversion.toDBObject(modelObject);
			DBObject query = new BasicDBObject();
			query.put("_id", modelObject.to_db_id());
			getCollection(collection).update(query, dbObject);			
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Remove all documents with the given properties from the given collection.
	 */
	public void delete(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {		
		try {
			if (logQueries) AccessLog.logDB("delete "+collection+ " "+properties.toString());
			DBObject query = toDBObject(model, properties);
			getCollection(collection).remove(query);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		}
	}

	/**
	 * Check whether a document exists that has the given properties.
	 */
	public boolean exists(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {		
		try {
			DBObject query = toDBObject(model, properties);
			DBObject projection = new BasicDBObject("_id", 1);
			if (logQueries) AccessLog.logDB("exists "+collection+" "+query.toString());
			return getCollection(collection).findOne(query, projection) != null;
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Return the given fields of the object that has the given properties.
	 */
	public <T extends Model> T get(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {		
		try {
			DBObject query = toDBObject(modelClass, properties);
			DBObject projection = toDBObject(fields);
			DBObject dbObject = getCollection(collection).findOne(query, projection);
			if (logQueries) AccessLog.logDB("single "+collection+" "+query.toString());
			if (dbObject == null) return null;
			return conversion.toModel(modelClass, dbObject);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
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
		try {
			DBObject query = toDBObject(modelClass, properties);
			DBObject projection = toDBObject(fields);
			if (logQueries) AccessLog.logDB("all "+collection+" "+query.toString());
			DBCursor cursor = getCollection(collection).find(query, projection);
			if (limit!=0) cursor = cursor.limit(limit);
			Set<DBObject> dbObjects = CollectionConversion.toSet(cursor);
			return conversion.toModel(modelClass, dbObjects);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Set the given field of the object with the given id.
	 */
	public <T extends Model> void set(Class<T> model, String collection, Object modelId, String field, Object value) throws DatabaseException {
		try {
			DBObject query = new BasicDBObject("_id", modelId);
			DBObject update = new BasicDBObject("$set", conversion.toDBObject(field, value));
			if (logQueries) AccessLog.logDB("set field "+collection+" field="+field+" value="+value);
			getCollection(collection).update(query, update);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		}
	}
	
	public <T extends Model> void set(Class<T> model, String collection, Map<String, Object> properties, String field, Object value) throws DatabaseException {
		try {
			DBObject query = toDBObject(model, properties);
			DBObject update = new BasicDBObject("$set", conversion.toDBObject(field, value));
			if (logQueries) AccessLog.logDB("set all "+collection+" query="+query.toString()+" field="+field+" to="+value);
			getCollection(collection).update(query, update, false, true);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		}
	}
	
	/**
	 * Set the given field of the object with the given id.
	 */
	public <T extends Model> void secureUpdate(T model, String collection, String timestampField, String[] fields) throws LostUpdateException, DatabaseException {
		try {
			if (logQueries) AccessLog.logDB("secure update "+collection+ " "+model.to_db_id().toString());
			DBObject query = new BasicDBObject();
			query.put("_id", model.to_db_id());
			query.put(timestampField, model.getClass().getField(timestampField).get(model));
			
			DBObject updateContent = new BasicDBObject();
			for (String field : fields) {
				updateContent.put(field, conversion.toDBObjectValue(model.getClass().getField(field).get(model)));
			}
			long ts = System.currentTimeMillis();
			updateContent.put(timestampField, ts);
			DBObject update = new BasicDBObject("$set", updateContent);
		
			DBObject result = getCollection(collection).findAndModify(query, update);
			if (result == null) throw new LostUpdateException();
			
			model.getClass().getField(timestampField).set(model, ts);
											
		} catch (MongoException e) {
			throw new DatabaseException(e);
		} catch (DatabaseConversionException e2) {
			throw new DatabaseException(e2);
		} catch (IllegalAccessException e3) {
			throw new DatabaseException(e3);
		} catch (NoSuchFieldException e4) {
			throw new DatabaseException(e4);
		}
	}

	/**
	 * Convert the properties map to a database object. If an array is given as the value, use the $in operator.
	 */
	private DBObject toDBObject(Class model, Map<String, ? extends Object> properties) throws DatabaseConversionException {
		DBObject dbObject = new BasicDBObject();
		for (String key : properties.keySet()) {
			Object property = properties.get(key);
			if (property instanceof Collection<?> && !key.startsWith("$")) {
				ArrayList al = new ArrayList();
				for (Object v : ((Collection<?>) property)) al.add(conversion.toDBObjectValue(v));
				dbObject.put(key, new BasicDBObject("$in", al));
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
	private DBObject toDBObject(Set<String> fields) {
		DBObject dbObject = new BasicDBObject();
		for (String field : fields) {
			dbObject.put(field, 1);
		}
		return dbObject;
	}
	
}
