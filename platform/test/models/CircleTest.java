package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashSet;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.collections.ChainedMap;
import utils.db.DBLayer;

import com.mongodb.DBCollection;

public class CircleTest {

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
	public void exists() throws ModelException {
		DBCollection circles = DBLayer.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = new ObjectId();
		circle.name = "Test circle";
		circle.order = 1;
		circle.members = new HashSet<ObjectId>();
		circle.shared = new HashSet<ObjectId>();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertTrue(Circle.exists(new ChainedMap<String, ObjectId>().put("_id", circle._id).put("owner", circle.owner)
				.get()));
	}

	@Test
	public void notExists() throws ModelException {
		DBCollection circles = DBLayer.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = new ObjectId();
		circle.name = "Test circle";
		circle.order = 1;
		circle.members = new HashSet<ObjectId>();
		circle.shared = new HashSet<ObjectId>();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertFalse(Circle.exists(new ChainedMap<String, ObjectId>().put("_id", circle._id)
				.put("owner", new ObjectId()).get()));
	}

	// not testing order any further, has already been done in SpaceTest
	@Test
	public void add() throws ModelException {
		DBCollection circles = DBLayer.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = new ObjectId();
		circle.name = "Test circle";
		circle.order = 1;
		circle.members = new HashSet<ObjectId>();
		circle.shared = new HashSet<ObjectId>();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertNotNull(circle._id);
	}

	@Test
	public void delete() throws ModelException {
		DBCollection circles = DBLayer.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = new ObjectId();
		circle.name = "Test circle";
		circle.order = 1;
		circle.members = new HashSet<ObjectId>();
		circle.shared = new HashSet<ObjectId>();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		Circle.delete(circle.owner, circle._id);
		assertEquals(0, circles.count());
	}

	@Test
	public void getMaxOrder() throws ModelException {
		DBCollection circles = DBLayer.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = new ObjectId();
		circle.name = "Test circle 1";
		circle.order = 1;
		circle.members = new HashSet<ObjectId>();
		circle.shared = new HashSet<ObjectId>();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals(1, Circle.getMaxOrder(circle.owner));
	}

}
