package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.db.DBLayer;

import com.mongodb.DBCollection;

public class VisualizationTest {

	private static final String collection = "visualizations";

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
		DBCollection visualizations = DBLayer.getCollection(collection);
		assertEquals(0, visualizations.count());
		Visualization visualization = new Visualization();
		visualization._id = new ObjectId();
		visualization.creator = new ObjectId();
		visualization.name = "Test visualization";
		visualization.description = "Test description";
		visualization.spotlighted = true;
		visualization.url = "www.test.url";
		Visualization.add(visualization);
		assertEquals(1, visualizations.count());
		assertEquals("Test visualization", visualizations.findOne().get("name"));
		assertNotNull(visualization._id);
	}

	@Test
	public void delete() throws ModelException {
		DBCollection visualizations = DBLayer.getCollection(collection);
		assertEquals(0, visualizations.count());
		Visualization visualization = new Visualization();
		visualization._id = new ObjectId();
		visualization.creator = new ObjectId();
		visualization.name = "Test visualization";
		visualization.description = "Test description";
		visualization.spotlighted = true;
		visualization.url = "www.test.url";
		Visualization.add(visualization);
		assertEquals(1, visualizations.count());
		Visualization.delete(visualization._id);
		assertEquals(0, visualizations.count());
	}
}
