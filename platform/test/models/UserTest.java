package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DBLayer;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class UserTest {

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
	public void add() throws ModelException {
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
		User.add(user);
		assertEquals(1, users.count());
		DBObject query = new BasicDBObject("_id", user._id);
		assertEquals(user.email, users.findOne(query).get("email"));
	}

	@Test
	public void delete() throws ModelException {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		User.delete(userIds[0]);
		assertEquals(0, users.count());
	}

	@Test
	public void exists() throws ModelException {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		assertFalse(User.exists(new ChainedMap<String, ObjectId>().put("_id", new ObjectId()).get()));
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		assertTrue(User.exists(new ChainedMap<String, ObjectId>().put("_id", userId).get()));
	}

	@Test
	public void findSuccess() throws ModelException {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		assertEquals(1, users.count());
		User foundUser = User.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>()
				.add("name").get());
		assertEquals("Test User 1", foundUser.name);
	}

	@Test
	public void findFailure() throws ModelException {
		DBCollection users = DBLayer.getCollection("users");
		assertEquals(0, users.count());
		CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		boolean exceptionCaught = false;
		try {
			User.get(new ChainedMap<String, ObjectId>().put("_id", new ObjectId()).get(),
					new ChainedSet<String>().add("name").get());
		} catch (NullPointerException e) {
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
	}

}
