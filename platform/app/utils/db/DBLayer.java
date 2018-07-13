package utils.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.typesafe.config.Config;

import models.Model;
import utils.stats.Stats;

/**
 * Main database access class
 * 
 * This class is called from the data model classes to insert, update or search data classes
 *
 */
public class DBLayer {
		
	private static Map<String,Database> dbnameToDB;
	private static Map<String,MongoDatabase> collectionsToDB;
	
	protected final static String DB_USER = "user";
	protected final static String DB_MAPPING = "mapping";
	protected final static String DB_RECORD = "record";
	protected final static String DB_ACCESS = "access";
	protected final static String DB_LOG = "log";
	
	private static void init(boolean debug, Config config) {
		dbnameToDB = new HashMap<String,Database>();
		collectionsToDB = new HashMap<String,MongoDatabase>();
		
		// User database
		String host = config.getString("mongo.user.host");
		int port = config.getInt("mongo.user.port");
		String database = config.getString("mongo.user.database");
		String user = config.hasPath("mongo.user.username") ? config.getString("mongo.user.username") : null;
		String password = config.hasPath("mongo.user.password") ? config.getString("mongo.user.password") : null;
		dbnameToDB.put(DB_USER, new MongoDatabase(host, port, database, user, password));
		
		// Mapping database
		host = config.getString("mongo.mapping.host");
		port = config.getInt("mongo.mapping.port");
		database = config.getString("mongo.mapping.database");
		user = config.hasPath("mongo.mapping.username") ? config.getString("mongo.mapping.username") : null;
		password = config.hasPath("mongo.mapping.password") ? config.getString("mongo.mapping.password") : null;
		
		dbnameToDB.put(DB_MAPPING, new MongoDatabase(host, port, database, user, password));		
		
		// Record database
		host = config.getString("mongo.record.host");
		port = config.getInt("mongo.record.port");
		database = config.getString("mongo.record.database");
		user = config.hasPath("mongo.record.username") ? config.getString("mongo.record.username") : null;
		password = config.hasPath("mongo.record.password") ? config.getString("mongo.record.password") : null;
		
		dbnameToDB.put(DB_RECORD, new MongoDatabase(host, port, database, user, password));
		
		// Access database
		host = config.getString("mongo.access.host");
		port = config.getInt("mongo.access.port");
		database = config.getString("mongo.access.database");
		user = config.hasPath("mongo.access.username") ? config.getString("mongo.access.username") : null;
		password = config.hasPath("mongo.access.password") ? config.getString("mongo.access.password") : null;
		
		dbnameToDB.put(DB_ACCESS, new MongoDatabase(host, port, database, user, password));
				
		// Log database
		
		Schema.init();
		
				
	}
	
	protected static void setCollectionsForDatabase(String dbname, String[] classnames) {
		MongoDatabase db = (MongoDatabase) dbnameToDB.get(dbname);
		for (String classname : classnames) {
			collectionsToDB.put(classname, db);
		}
	}
	
	/**
	 * Open mongo client.
	 */
	private static void openConnection(boolean debug, Config config) throws DatabaseException {
		init(debug, config);
		
		for (Database db : dbnameToDB.values()) { db.openConnection(); }		
	}

	/**
	 * Connects to the production database 'healthdata'.
	 */
	public static void connect(Config config) throws DatabaseException {
		openConnection(false, config);		
	}

	/**
	 * Connects to the test database 'test'.
	 */
	public static void connectToTest(Config config) throws DatabaseException {
		openConnection(true, config);		
	}

	/**
	 * Closes all connections.
	 */
	public static void close() {
		
		for (Database db : dbnameToDB.values()) { db.close(); }				
	}

	/**
	 * Sets up the collections and creates all indices.
	 */
	public static void initialize() {		
	}	

	public static MongoDatabase getDatabaseForCollection(String collection) {
		return collectionsToDB.get(collection);
	}

	/**
	 * Gets the specified collection.
	 */
	public static MongoCollection<DBObject> getCollection(String collection) {
		return getDatabaseForCollection(collection).getDB().getCollection(collection, DBObject.class);
	}

	/* Database operations */
	/**
	 * Insert a document into the given collection.
	 */
	public static <T extends Model> void insert(String collection, T modelObject) throws DatabaseException {
		Stats.reportDb("insert 1", collection);
		getDatabaseForCollection(collection).insert(collection, modelObject);		
	}
	
	/**
	 * Upserts a document into the given collection.
	 */
	public static <T extends Model> void upsert(String collection, T modelObject) throws DatabaseException {
		Stats.reportDb("upsert 1","collection");
		getDatabaseForCollection(collection).upsert(collection, modelObject);		
	}

	/**
	 * Remove all documents with the given properties from the given collection.
	 */
	public static void delete(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		Stats.reportDb("delete",collection);
		getDatabaseForCollection(collection).delete(model, collection, properties);		
	}

	/**
	 * Check whether a document exists that has the given properties.
	 */
	public static boolean exists(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		Stats.reportDb("exists", collection);
		return getDatabaseForCollection(collection).exists(model, collection, properties);		
	}

	/**
	 * Return the given fields of the object that has the given properties.
	 */
	public static <T extends Model> T get(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {
		Stats.reportDb("read 1", collection);
		return getDatabaseForCollection(collection).get(modelClass, collection, properties, fields);
	}

	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {
		Stats.reportDb("read N", collection);
		return getDatabaseForCollection(collection).getAll(modelClass, collection, properties, fields);
	}
	
	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields, int limit) throws DatabaseException {
		Stats.reportDb("read N", collection);
		return getDatabaseForCollection(collection).getAll(modelClass, collection, properties, fields, limit);
	}
	
	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public static <T extends Model> List<T> getAllList(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields, int limit, String sortField, int order) throws DatabaseException {
		Stats.reportDb("read N", collection);
		return getDatabaseForCollection(collection).getAllList(modelClass, collection, properties, fields, limit, sortField, order);
	}
	
	/**
	 * Return the count of all objects that have the given properties.
	 */
	public static long count(Class modelClass, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		Stats.reportDb("count", collection);
		return getDatabaseForCollection(collection).count(modelClass, collection, properties);
	}

	/**
	 * Set the given field of the object with the given id.
	 */
	public static <T extends Model> void set(Class<T> model, String collection, ObjectId modelId, String field, Object value) throws DatabaseException {
		Stats.reportDb("update 1", collection);
		getDatabaseForCollection(collection).set(model, collection, modelId, field, value);		
	}

	/**
	 * Sets a field to a given value for all matching objects
	 * @param model model class
	 * @param collection which collection
	 * @param properties restrictions on update
	 * @param field name of field to change
	 * @param value target value
	 * @throws DatabaseException
	 */
	public static <T extends Model> void set(Class<T> model, String collection, Map<String, Object> properties, String field, Object value) throws DatabaseException {
		Stats.reportDb("update N",collection);
		getDatabaseForCollection(collection).set(model, collection, properties, field, value);		
	}
	
	
	/**
	 * Sets the given fields of the object (does not prevent lost update)
	 * @return
	 */
	public static <T extends Model> void update(T model, String collection, Collection<String> fields) throws DatabaseException {
		Stats.reportDb("update 1", collection);
		getDatabaseForCollection(collection).update(model, collection, fields);
	}
	
	/**
	 * Sets the given fields of the object and prevents lost updates
	 * @return
	 */
	public static <T extends Model> void secureUpdate(T model, String collection, String timestampField, String... fields) throws LostUpdateException, DatabaseException {
		Stats.reportDb("update 1", collection);
		getDatabaseForCollection(collection).secureUpdate(model, collection, timestampField, fields);
	}

	public static com.mongodb.client.MongoDatabase getFSDB() {
		return ((MongoDatabase) DBLayer.dbnameToDB.get(DB_RECORD)).getDB();
	}

}
