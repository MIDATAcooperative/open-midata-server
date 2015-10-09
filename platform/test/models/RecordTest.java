package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.DateTimeUtils;
import utils.collections.ChainedMap;
import utils.db.DBLayer;
import utils.exceptions.ModelException;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class RecordTest {

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
		DBCollection records = DBLayer.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record._id = new ObjectId();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.now();
		record.data = (DBObject) JSON.parse("{\"title\":\"Test record\",\"data\":\"Test data.\"}");
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertTrue(Record.exists(new ChainedMap<String, ObjectId>().put("_id", record._id).put("owner", record.owner).get()));
	}

	@Test
	public void notExists() throws ModelException {
		DBCollection records = DBLayer.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record._id = new ObjectId();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.now();
		record.data = (DBObject) JSON.parse("{\"title\":\"Test record\",\"data\":\"Test data.\"}");
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertFalse(Record.exists(new ChainedMap<String, ObjectId>().put("_id", record._id).put("owner", new ObjectId()).get()));
	}

	@Test
	public void add() throws ModelException {
		DBCollection records = DBLayer.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record._id = new ObjectId();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.now();
		record.data = (DBObject) JSON.parse("{\"title\":\"Test record\",\"data\":\"Test data.\"}");
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertEquals(record.creator, records.findOne().get("creator"));
		assertEquals(record.owner, records.findOne().get("owner"));
		assertEquals(record.data, records.findOne().get("data"));
		assertNotNull(record._id);
	}

	@Test
	public void delete() throws ModelException {
		DBCollection records = DBLayer.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record._id = new ObjectId();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.now();
		record.data = (DBObject) JSON.parse("{\"title\":\"Test record\",\"data\":\"Test data.\"}");
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertEquals(record.creator, records.findOne().get("creator"));
		Record.delete(record.owner, record._id);
		assertEquals(0, records.count());
	}

}
