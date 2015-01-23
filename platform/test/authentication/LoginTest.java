package authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
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

public class LoginTest {

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
	public void authenticateSuccess() throws Exception {
		Result result = callAction(controllers.routes.ref.Application.authenticate(),
				fakeRequest().withJsonBody(Json.parse("{\"email\": \"test1@example.com\", \"password\": \"secret\"}")));
		assertEquals(200, status(result));
		assertNotNull(session(result).get("id"));
		Member user = Member.get(new ChainedMap<String, String>().put("email", "test1@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		assertEquals(user._id.toString(), session(result).get("id"));
	}

	@Test
	public void authenticateFailureEmail() {
		Result result = callAction(controllers.routes.ref.Application.authenticate(),
				fakeRequest().withJsonBody(Json.parse("{\"email\": \"testA@example.com\", \"password\": \"secret\"}")));
		assertEquals(400, status(result));
		assertEquals("Invalid user or password.", contentAsString(result));
		assertNull(session(result).get("id"));
	}

	@Test
	public void authenticateFailurePassword() {
		Result result = callAction(
				controllers.routes.ref.Application.authenticate(),
				fakeRequest().withJsonBody(
						Json.parse("{\"email\": \"test1@example.com\", \"password\": \"badpassword\"}")));
		assertEquals(400, status(result));
		assertEquals("Invalid user or password.", contentAsString(result));
		assertNull(session(result).get("id"));
	}

	@Test
	public void authenticated() throws Exception {
		Member user = Member.get(new ChainedMap<String, String>().put("email", "test1@example.com").get(),
				new ChainedSet<String>().add("_id").get());
		Result result = callAction(controllers.routes.ref.Messages.index(),
				fakeRequest().withSession("id", user._id.toString()));
		assertEquals(200, status(result));
	}

	@Test
	public void notAuthenticated() {
		Result result = callAction(controllers.routes.ref.Messages.index(), fakeRequest());
		assertEquals(303, status(result));
		assertEquals("/welcome", header("Location", result));
	}

}
