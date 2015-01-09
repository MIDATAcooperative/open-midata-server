package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import models.Space;
import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;
import utils.LoadData;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DBLayer;
import utils.db.DatabaseConversion;
import utils.db.DatabaseConversionException;
import utils.db.OrderOperations;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class SpacesTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		DBLayer.connectToTest();
		LoadData.load();
	}

	@After
	public void tearDown() {
		DBLayer.close();
	}

	@Test
	public void addSpace() throws DatabaseConversionException, Exception {
		User user = User.get(new ChainedMap<String, String>().put("email", "test1@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		ObjectId visualizationId = new ObjectId();
		Result result = callAction(
				controllers.routes.ref.Spaces.add(),
				fakeRequest().withSession("id", user._id.toString()).withJsonBody(
						Json.parse("{\"name\": \"Test space\", \"visualization\": \"" + visualizationId.toString()
								+ "\"}")));
		assertEquals(200, status(result));
		DBObject foundSpace = DBLayer.getCollection("spaces").findOne(new BasicDBObject("name", "Test space"));
		Space space = DatabaseConversion.toModel(Space.class, foundSpace);
		assertNotNull(space);
		assertEquals("Test space", space.name);
		assertEquals(user._id, space.owner);
		assertEquals(visualizationId, space.visualization);
		assertEquals(OrderOperations.getMax("spaces", space.owner), space.order);
		assertEquals(0, space.records.size());
	}

	@Test
	public void deleteSpaceSuccess() {
		DBCollection spaces = DBLayer.getCollection("spaces");
		long originalCount = spaces.count();
		DBObject space = spaces.findOne();
		ObjectId id = (ObjectId) space.get("_id");
		ObjectId userId = (ObjectId) space.get("owner");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.delete(spaceId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(200, status(result));
		assertNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount - 1, spaces.count());
	}

	@Test
	public void deleteSpaceForbidden() throws Exception {
		DBCollection spaces = DBLayer.getCollection("spaces");
		long originalCount = spaces.count();
		User user = User.get(new ChainedMap<String, String>().put("email", "test2@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", user._id));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.delete(spaceId),
				fakeRequest().withSession("id", user._id.toString()));
		assertEquals(400, status(result));
		assertEquals("No space with this id exists.", contentAsString(result));
		assertNotNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount, spaces.count());
	}

	@Test
	public void addRecordSuccess() {
		DBObject record = DBLayer.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");
		DBCollection spaces = DBLayer.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		ObjectId userId = (ObjectId) space.get("owner");
		int order = (Integer) space.get("order");
		BasicDBList records = (BasicDBList) space.get("records");
		int oldSize = records.size();
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.addRecords(spaceId),
				fakeRequest().withSession("id", userId.toString()).withJsonBody(
						Json.parse("{\"records\": [{\"$oid\": \"" + recordId.toString() + "\"}]}")));
		assertEquals(200, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize + 1, ((BasicDBList) foundSpace.get("records")).size());
	}
}
