package controllers;

import controllers.providers.ProviderSecured;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.providers.registration;
import views.html.providers.login;
import views.html.providers.messages;

public class ProviderFrontend extends Controller {

	public static Result register() {
		return ok(registration.render());
	}
	
	public static Result login() {
		return ok(login.render());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result messages() {
		return ok(messages.render());
	}
}
