package utils;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.db.DBLayer;
import utils.db.OrderOperations;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class OrderOperationsTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		DBLayer.connectToTest();
		DBLayer.destroy();
	}

	@After
	public void tearDown() {
		DBLayer.close();
	}

	@Test
	public void maxOfUser() {
		DBCollection collection = DBLayer.getCollection("order");
		Map<String, Object> values = new HashMap<String, Object>();
		ObjectId user1 = new ObjectId();
		ObjectId user2 = new ObjectId();
		values.put("owner", user1);
		values.put("order", 1);
		collection.insert(new BasicDBObject(values));
		values.put("order", 2);
		collection.insert(new BasicDBObject(values));
		values.put("order", 5);
		collection.insert(new BasicDBObject(values));
		values.put("order", 4);
		collection.insert(new BasicDBObject(values));
		values.put("owner", user2);
		values.put("order", 7);
		collection.insert(new BasicDBObject(values));
		assertEquals(5, OrderOperations.getMax("order", user1));
	}

	@Test
	public void maxNoEntries() {
		assertEquals(0, OrderOperations.getMax("order", new ObjectId()));
	}

	@Test
	public void maxNoEntriesForUser() {
		DBCollection collection = DBLayer.getCollection("order");
		Map<String, Object> values = new HashMap<String, Object>();
		ObjectId user1 = new ObjectId();
		ObjectId user2 = new ObjectId();
		ObjectId user3 = new ObjectId();
		values.put("owner", user1);
		values.put("order", 1);
		collection.insert(new BasicDBObject(values));
		values.put("owner", user2);
		values.put("order", 7);
		collection.insert(new BasicDBObject(values));
		assertEquals(0, OrderOperations.getMax("order", user3));
	}

	@Test
	public void incrementGreaterThan() throws Exception {
		ObjectId[] userIds = insertTestValues();
		DBCollection collection = DBLayer.getCollection("order");
		OrderOperations.increment("order", userIds[0], 4, 0);
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(6, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void incrementBetween() throws Exception {
		ObjectId[] userIds = insertTestValues();
		DBCollection collection = DBLayer.getCollection("order");
		OrderOperations.increment("order", userIds[0], 3, 4);
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void incrementFromGreaterTo() throws Exception {
		ObjectId[] userIds = insertTestValues();
		DBCollection collection = DBLayer.getCollection("order");
		OrderOperations.increment("order", userIds[0], 4, 1);
		assertEquals(2, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void decrementGreaterThan() throws Exception {
		ObjectId[] userIds = insertTestValues();
		DBCollection collection = DBLayer.getCollection("order");
		OrderOperations.decrement("order", userIds[0], 0, 5);
		assertEquals(0, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(4, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void decrementBetween() throws Exception {
		ObjectId[] userIds = insertTestValues();
		DBCollection collection = DBLayer.getCollection("order");
		OrderOperations.decrement("order", userIds[0], 2, 5);
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(4, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void decrementFromGreaterTo() throws Exception {
		ObjectId[] userIds = insertTestValues();
		DBCollection collection = DBLayer.getCollection("order");
		OrderOperations.decrement("order", userIds[0], 5, 3);
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(4, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	private ObjectId[] insertTestValues() {
		DBCollection collection = DBLayer.getCollection("order");
		Map<String, Object> values = new HashMap<String, Object>();
		ObjectId[] userIds = new ObjectId[] { new ObjectId(), new ObjectId(), new ObjectId() };
		values.put("_id", 1);
		values.put("owner", userIds[0]);
		values.put("order", 1);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 2);
		values.put("owner", userIds[0]);
		values.put("order", 5);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 3);
		values.put("owner", userIds[0]);
		values.put("order", 4);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 4);
		values.put("owner", userIds[1]);
		values.put("order", 7);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 5);
		values.put("owner", userIds[2]);
		values.put("order", 3);
		collection.insert(new BasicDBObject(values));
		return userIds;
	}
}
