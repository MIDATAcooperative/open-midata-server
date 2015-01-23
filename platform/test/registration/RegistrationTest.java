package registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.session;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import models.Member;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;
import utils.LoadData;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DBLayer;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RegistrationTest {

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
	public void register() throws Exception {
		String newEmail = "new@example.com";
		DBCollection users = DBLayer.getCollection("users");
		DBObject query = new BasicDBObject("email", newEmail);
		assertNull(users.findOne(query));
		long oldSize = users.count();
		Result result = callAction(
				controllers.routes.ref.Application.register(),
				fakeRequest().withJsonBody(
						Json.parse("{\"email\": \"" + newEmail
								+ "\", \"firstName\": \"First\", \"lastName\": \"Last\", \"password\": \"secret\"}")));
		assertEquals(200, status(result));
		Member user = Member.get(new ChainedMap<String, String>().put("email", newEmail).get(), new ChainedSet<String>()
				.add("_id").get());
		assertEquals(user._id.toString(), session(result).get("id"));
		assertEquals(oldSize + 1, users.count());
	}

	@Test
	public void registerSameEmail() {
		DBCollection users = DBLayer.getCollection("users");
		long oldSize = users.count();
		assertTrue(oldSize > 0);
		String existingEmail = (String) users.findOne().get("email");
		Result result = callAction(
				controllers.routes.ref.Application.register(),
				fakeRequest().withJsonBody(
						Json.parse("{\"email\": \"" + existingEmail
								+ "\", \"firstName\": \"First\", \"lastName\": \"Last\", \"password\": \"secret\"}")));
		assertEquals(400, status(result));
		assertEquals("A user with this email address already exists.", contentAsString(result));
		assertEquals(oldSize, users.count());
	}
}
