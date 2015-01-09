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
import models.Circle;
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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class CirclesTest {

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
	public void addCircle() throws Exception {
		User user = User.get(new ChainedMap<String, String>().put("email", "test1@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		Result result = callAction(
				controllers.routes.ref.Circles.add(),
				fakeRequest().withSession("id", user._id.toString()).withJsonBody(
						Json.parse("{\"name\": \"Test circle\"}")));
		assertEquals(200, status(result));
		DBObject foundCircle = DBLayer.getCollection("circles").findOne(new BasicDBObject("name", "Test circle"));
		Circle circle = DatabaseConversion.toModel(Circle.class, foundCircle);
		assertNotNull(circle);
		assertEquals("Test circle", circle.name);
		assertEquals(user._id, circle.owner);
		assertEquals(0, circle.members.size());
	}

	@Test
	public void deleteCircleSuccess() {
		DBCollection circles = DBLayer.getCollection("circles");
		DBObject circle = circles.findOne();
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId userId = (ObjectId) circle.get("owner");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.delete(circleId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(200, status(result));
		assertNull(circles.findOne(new BasicDBObject("_id", id)));
	}

	@Test
	public void deleteCircleForbidden() throws Exception {
		DBCollection circles = DBLayer.getCollection("circles");
		User user = User.get(new ChainedMap<String, String>().put("email", "test2@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", user._id));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.delete(circleId),
				fakeRequest().withSession("id", user._id.toString()));
		assertEquals(400, status(result));
		assertEquals("No circle with this id exists.", contentAsString(result));
		assertNotNull(circles.findOne(new BasicDBObject("_id", id)));
	}

	// forbidden requests are blocked by this controller, no longer testing this
	@Test
	public void addUser() throws Exception {
		DBCollection circles = DBLayer.getCollection("circles");
		User user = User.get(new ChainedMap<String, String>().put("email", "test3@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$nin", new ObjectId[] { user._id }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId ownerId = (ObjectId) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Circles.addUsers(circleId),
				fakeRequest().withSession("id", ownerId.toString()).withJsonBody(
						Json.parse("{\"users\": [{\"$oid\": \"" + user._id.toString() + "\"}]}")));
		assertEquals(200, status(result));
		assertEquals(oldSize + 1, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void removeMember() throws Exception {
		DBCollection circles = DBLayer.getCollection("circles");
		User user = User.get(new ChainedMap<String, String>().put("email", "test2@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$in", new ObjectId[] { user._id }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId ownerId = (ObjectId) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.removeMember(circleId, user._id.toString()),
				fakeRequest().withSession("id", ownerId.toString()));
		assertEquals(200, status(result));
		assertEquals(oldSize - 1, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

}
