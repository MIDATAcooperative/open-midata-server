package controllers.research;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.research.registration;
import views.html.research.login;
import views.html.research.messages;

public class ResearchFrontend extends Controller {

	public static Result register() {
		return ok(registration.render());
	}
	
	public static Result login() {
		return ok(login.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result messages() {
		return ok(messages.render());
	}
}
