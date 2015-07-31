package mongo;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import utils.db.DBLayer;
import utils.db.DatabaseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * Manually insert documents into MongoDB.
 */
public class ManualMongo {

	public static void main(String[] args) throws DatabaseException {
		String collection = "users";
		String field = "tokens";
		DBObject value = new BasicDBObject();

		start(fakeApplication(fakeGlobal()));
		DBLayer.connect();
		DBCollection coll = DBLayer.getCollection(collection);
		try {
			coll.updateMulti(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject(field, value)));
		} catch (MongoException e) {
			System.out.println("Error: " + e.getMessage());
		}
		System.out.println("Successful.");
	}

}
