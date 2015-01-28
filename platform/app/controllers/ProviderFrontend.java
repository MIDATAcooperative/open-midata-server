package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.providers.registration;
import views.html.providers.login;

public class ProviderFrontend extends Controller {

	public static Result register() {
		return ok(registration.render());
	}
	
	public static Result login() {
		return ok(login.render());
	}
	

}
