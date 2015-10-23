package utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.Record;
import models.Member;
import models.Visualization;

import org.bson.types.ObjectId;

import utils.db.DBLayer;
import utils.exceptions.InternalServerException;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class CreateDBObjects {

	public static ObjectId[] insertUsers(int numUsers) throws InternalServerException {
		DBCollection users = DBLayer.getCollection("users");
		long originalCount = users.count();
		ObjectId[] userIds = new ObjectId[numUsers];
		for (int i = 0; i < numUsers; i++) {
			Member user = new Member();
			user._id = new ObjectId();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = Member.encrypt("password");
			user.visible = new HashMap<String, Set<ObjectId>>();
			user.apps = new HashSet<ObjectId>();
			user.visualizations = new HashSet<ObjectId>();
			user.messages = new HashMap<String, Set<ObjectId>>();
			user.news = new HashSet<ObjectId>();
			user.pushed = new HashSet<ObjectId>();
			user.shared = new HashSet<ObjectId>();
			Member.add(user);
			userIds[i] = user._id;
		}
		assertEquals(originalCount + numUsers, users.count());
		return userIds;
	}

	public static ObjectId[] insertRecords(int numRecords) throws InternalServerException {
		return insertRecords(numRecords, new ObjectId());
	}

	public static ObjectId[] insertRecords(int numRecords, ObjectId owner) throws InternalServerException {
		return insertRecords(numRecords, owner, new ObjectId());
	}

	public static ObjectId[] insertRecords(int numRecords, ObjectId owner, ObjectId creator) throws InternalServerException {
		DBCollection records = DBLayer.getCollection("records");
		long originalCount = records.count();
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record._id = new ObjectId();
			record.app = new ObjectId();
			record.owner = owner;
			record.creator = creator;
			record.created = DateTimeUtils.now();
			record.data = (DBObject) JSON.parse("{\"title\":\"Test record\",\"data\":\"Test data.\"}");
			record.name = "Test record";
			record.description = "Test data.";
			Record.add(record);
			recordIds[i] = record._id;
		}
		assertEquals(originalCount + numRecords, records.count());
		return recordIds;
	}

	public static ObjectId[] insertVisualizations(int numVisualizations) throws InternalServerException {
		DBCollection visualizations = DBLayer.getCollection("visualizations");
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
