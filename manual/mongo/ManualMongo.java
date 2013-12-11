package mongo;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import utils.db.Database;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;

/**
 * Manually insert documents into MongoDB.
 */
public class ManualMongo {

	public static void main(String[] args) {
		String collection = "records";
		String field = "tags";
		String[] value = new String[0];

		start(fakeApplication(fakeGlobal()));
		Database.connect();
		DBCollection coll = Database.getCollection(collection);
		WriteResult wr = coll.updateMulti(new BasicDBObject(), new BasicDBObject("$set",
				new BasicDBObject(field, value)));
		if (wr.getLastError().getErrorMessage() == null) {
			System.out.println("Successful.");
		} else {
			System.out.println("Error: " + wr.getLastError().getErrorMessage());
		}
	}

}