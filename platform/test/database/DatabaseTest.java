package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Message;
import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.DateTimeUtils;
import utils.collections.ChainedSet;
import utils.db.DBLayer;
import utils.db.DatabaseConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DatabaseTest {

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
	public void getCollection() {
		String collection = "users";
		DBCollection coll = DBLayer.getCollection(collection);
		assertNotNull(coll);
		assertEquals(collection, coll.getName());
	}

	@Test
	public void createAndSaveObject() {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		users.insert(new BasicDBObject("name", "Test User"));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndSaveUser() throws Exception {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user._id = new ObjectId();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = User.encrypt("password");
		user.visible = new HashMap<String, Set<ObjectId>>();
		user.apps = new HashSet<ObjectId>();
		user.visualizations = new HashSet<ObjectId>();
		user.messages = new HashMap<String, Set<ObjectId>>();
		user.news = new HashSet<ObjectId>();
		user.pushed = new HashSet<ObjectId>();
		user.shared = new HashSet<ObjectId>();
		users.insert(DatabaseConversion.toDBObject(user));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndRetrieveUser() throws Exception {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user._id = new ObjectId();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = User.encrypt("password");
		user.visible = new HashMap<String, Set<ObjectId>>();
		user.apps = new HashSet<ObjectId>();
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		user.messages = new HashMap<String, Set<ObjectId>>();
		user.login = DateTimeUtils.now();
		user.news = new HashSet<ObjectId>();
		user.pushed = new HashSet<ObjectId>();
		user.shared = new HashSet<ObjectId>();
		users.insert(DatabaseConversion.toDBObject(user));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		User retrievedUser = DatabaseConversion.toModel(User.class, foundObject);
		assertEquals("Test User", retrievedUser.name);
	}

	@Test
	public void createAndRetrieveMessage() throws Exception {
		DBCollection messages = DBLayer.getCollection("messages");
		assertEquals(0, messages.count());
		Message message = new Message();
		message._id = new ObjectId();
		message.sender = new ObjectId();
		message.receivers = new ChainedSet<ObjectId>().add(new ObjectId()).get();
		message.created = DateTimeUtils.now();
		message.title = "Test";
		message.content = "This is a test message.";
		messages.insert(DatabaseConversion.toDBObject(message));
		assertEquals(1, messages.count());
		DBObject foundObject = messages.findOne();
		Message retrievedMessage = DatabaseConversion.toModel(Message.class, foundObject);
		assertEquals("Test", retrievedMessage.title);
	}

	@Test
	public void createAndRetrieveCircle() throws Exception {
		DBCollection circles = DBLayer.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = new ObjectId();
		circle.name = "Family";
		circle.order = 1;
		circle.members = new HashSet<ObjectId>();
		circle.members.add(new ObjectId());
		circle.members.add(new ObjectId());
		circle.members.add(new ObjectId());
		circle.shared = new HashSet<ObjectId>();
		circles.insert(DatabaseConversion.toDBObject(circle));
		assertEquals(1, circles.count());
		DBObject foundObject = circles.findOne();
		Circle retrievedCircle = DatabaseConversion.toModel(Circle.class, foundObject);
		assertEquals("Family", retrievedCircle.name);
	}

}
