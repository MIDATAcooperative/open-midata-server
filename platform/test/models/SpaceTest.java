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
import utils.exceptions.InternalServerException;

import com.mongodb.DBCollection;

public class SpaceTest {

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
	public void exists() throws InternalServerException {
		DBCollection spaces = DBLayer.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space._id = new ObjectId();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.order = 1;
		space.records = new HashSet<ObjectId>();
		Space.add(space);
		assertEquals(1, spaces.count());
		assertTrue(Space.exists(new ChainedMap<String, ObjectId>().put("_id", space._id).put("owner", space.owner)
				.get()));
	}

	@Test
	public void notExists() throws InternalServerException {
		DBCollection spaces = DBLayer.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space._id = new ObjectId();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.order = 1;
		space.records = new HashSet<ObjectId>();
		Space.add(space);
		assertEquals(1, spaces.count());
		assertFalse(Space.exists(new ChainedMap<String, ObjectId>().put("_id", new ObjectId())
				.put("owner", space.owner).get()));
	}

	@Test
	public void add() throws InternalServerException {
		DBCollection spaces = DBLayer.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space._id = new ObjectId();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.order = 1;
		space.records = new HashSet<ObjectId>();
		Space.add(space);
		assertEquals(1, spaces.count());
		assertEquals(space.name, spaces.findOne().get("name"));
		assertNotNull(space._id);
	}

	@Test
	public void delete() throws InternalServerException {
		DBCollection spaces = DBLayer.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space._id = new ObjectId();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.order = 1;
		space.records = new HashSet<ObjectId>();
		Space.add(space);
		assertEquals(1, spaces.count());
		Space.delete(space.owner, space._id);
		assertEquals(0, spaces.count());
	}

}
