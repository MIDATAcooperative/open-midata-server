package utils.db;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.Model;

import org.bson.types.ObjectId;

import play.Play;
import utils.collections.CollectionConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

public class DBLayer {
		
	private static Map<String,Database> dbnameToDB;
	private static Map<String,MongoDatabase> collectionsToDB;
	
	protected final static String DB_USER = "user";
	protected final static String DB_MAPPING = "mapping";
	protected final static String DB_RECORD = "record";
	protected final static String DB_ACCESS = "access";
	protected final static String DB_LOG = "log";
	
	private static void init(boolean debug) {
		dbnameToDB = new HashMap<String,Database>();
		collectionsToDB = new HashMap<String,MongoDatabase>();
		
		// User database
		String host = Play.application().configuration().getString("mongo.user.host");
		int port = Play.application().configuration().getInt("mongo.user.port");
		String database = Play.application().configuration().getString("mongo.user.database");
		dbnameToDB.put(DB_USER, new MongoDatabase(host, port, database));
		
		// Mapping database
		host = Play.application().configuration().getString("mongo.mapping.host");
		port = Play.application().configuration().getInt("mongo.mapping.port");
		database = Play.application().configuration().getString("mongo.mapping.database");
		dbnameToDB.put(DB_MAPPING, new MongoDatabase(host, port, database));		
		
		// Record database
		host = Play.application().configuration().getString("mongo.record.host");
		port = Play.application().configuration().getInt("mongo.record.port");
		database = Play.application().configuration().getString("mongo.record.database");
		dbnameToDB.put(DB_RECORD, new MongoDatabase(host, port, database));
		
		// Access database
		host = Play.application().configuration().getString("mongo.access.host");
		port = Play.application().configuration().getInt("mongo.access.port");
		database = Play.application().configuration().getString("mongo.access.database");
		dbnameToDB.put(DB_ACCESS, new MongoDatabase(host, port, database));
				
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
	private static void openConnection(boolean debug) throws DatabaseException {
		init(debug);
		
		for (Database db : dbnameToDB.values()) { db.openConnection(); }		
	}

	/**
	 * Connects to the production database 'healthdata'.
	 */
	public static void connect() throws DatabaseException {
		openConnection(false);		
	}

	/**
	 * Connects to the test database 'test'.
	 */
	public static void connectToTest() throws DatabaseException {
		openConnection(true);		
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
		// TODO
	}

	/**
	 * Drops the database.
	 */
	public static void destroy() {
		for (Database db : dbnameToDB.values()) { db.destroy(); }		
	}

	public static MongoDatabase getDatabaseForCollection(String collection) {
		return collectionsToDB.get(collection);
	}

	/**
	 * Gets the specified collection.
	 */
	public static DBCollection getCollection(String collection) {
		return getDatabaseForCollection(collection).getDB().getCollection(collection);
	}

	/* Database operations */
	/**
	 * Insert a document into the given collection.
	 */
	public static <T extends Model> void insert(String collection, T modelObject) throws DatabaseException {
		getDatabaseForCollection(collection).insert(collection, modelObject);		
	}
	
	/**
	 * Upserts a document into the given collection.
	 */
	public static <T extends Model> void upsert(String collection, T modelObject) throws DatabaseException {
		getDatabaseForCollection(collection).upsert(collection, modelObject);		
	}

	/**
	 * Remove all documents with the given properties from the given collection.
	 */
	public static void delete(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		getDatabaseForCollection(collection).delete(model, collection, properties);		
	}

	/**
	 * Check whether a document exists that has the given properties.
	 */
	public static boolean exists(Class model, String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		return getDatabaseForCollection(collection).exists(model, collection, properties);		
	}

	/**
	 * Return the given fields of the object that has the given properties.
	 */
	public static <T extends Model> T get(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {
		return getDatabaseForCollection(collection).get(modelClass, collection, properties, fields);
	}

	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection, Map<String, ? extends Object> properties,
			Set<String> fields) throws DatabaseException {
		return getDatabaseForCollection(collection).getAll(modelClass, collection, properties, fields);
	}

	/**
	 * Set the given field of the object with the given id.
	 */
	public static <T extends Model> void set(Class<T> model, String collection, ObjectId modelId, String field, Object value) throws DatabaseException {
		getDatabaseForCollection(collection).set(model, collection, modelId, field, value);		
	}
	
	/**
	 * Sets the given fields of the object and prevents lost updates
	 * @return
	 */
	public static <T extends Model> void secureUpdate(T model, String collection, String timestampField, String... fields) throws LostUpdateException, DatabaseException {
		getDatabaseForCollection(collection).secureUpdate(model, collection, timestampField, fields);
	}

	public static DB getFSDB() {
		return ((MongoDatabase) DBLayer.dbnameToDB.get(DB_RECORD)).getDB();
	}

}
