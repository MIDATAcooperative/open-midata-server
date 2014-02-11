package utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.ModelException;
import models.Record;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import utils.db.Database;

import com.mongodb.DBCollection;

public class CreateDBObjects {

	public static ObjectId[] insertUsers(int numUsers) throws ModelException {
		DBCollection users = Database.getCollection("users");
		long originalCount = users.count();
		ObjectId[] userIds = new ObjectId[numUsers];
		for (int i = 0; i < numUsers; i++) {
			User user = new User();
			user._id = new ObjectId();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = User.encrypt("password");
			user.visible = new HashMap<String, Set<ObjectId>>();
			user.apps = new HashSet<ObjectId>();
			user.visualizations = new HashSet<ObjectId>();
			user.messages = new HashMap<String, Set<ObjectId>>();
			user.news = new HashSet<ObjectId>();
			user.pushed = new HashSet<ObjectId>();
			user.shared = new HashSet<ObjectId>();
			User.add(user);
			userIds[i] = user._id;
		}
		assertEquals(originalCount + numUsers, users.count());
		return userIds;
	}

	public static ObjectId[] insertRecords(int numRecords) throws ModelException {
		return insertRecords(numRecords, new ObjectId());
	}

	public static ObjectId[] insertRecords(int numRecords, ObjectId owner) throws ModelException {
		return insertRecords(numRecords, owner, new ObjectId());
	}

	public static ObjectId[] insertRecords(int numRecords, ObjectId owner, ObjectId creator) throws ModelException {
		DBCollection records = Database.getCollection("records");
		long originalCount = records.count();
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record._id = new ObjectId();
			record.app = new ObjectId();
			record.owner = owner;
			record.creator = creator;
			record.created = DateTimeUtils.now();
			record.data = "{\"title\":\"Test record\",\"data\":\"Test data.\"}";
			record.name = "Test record";
			record.description = "Test data.";
			Record.add(record);
			recordIds[i] = record._id;
		}
		assertEquals(originalCount + numRecords, records.count());
		return recordIds;
	}

	public static ObjectId[] insertVisualizations(int numVisualizations) throws ModelException {
		DBCollection visualizations = Database.getCollection("visualizations");
		long originalCount = visualizations.count();
		ObjectId[] visualizationIds = new ObjectId[numVisualizations];
		for (int i = 0; i < numVisualizations; i++) {
			Visualization visualization = new Visualization();
			visualization._id = new ObjectId();
			visualization.creator = new ObjectId();
			visualization.name = "Test Visualization " + (i + 1);
			visualization.description = "Test description";
			visualization.url = "www.test.com";
			Visualization.add(visualization);
			visualizationIds[i] = visualization._id;
		}
		assertEquals(originalCount + numVisualizations, visualizations.count());
		return visualizationIds;
	}

}
