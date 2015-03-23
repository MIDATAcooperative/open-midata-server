package controllers;

import controllers.providers.ProviderSecured;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.providers.message;
import views.html.providers.createmessage;
import views.html.providers.registration;
import views.html.providers.login;
import views.html.providers.messages;
import views.html.providers.search;
import views.html.providers.memberdetails;

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
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result messageDetails(String messageIdString) {
		return ok(message.render());
	}

	@Security.Authenticated(ProviderSecured.class)
	public static Result createMessage() {
		return ok(createmessage.render());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result search() {
		return ok(search.render());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result member(String member) {
		return ok(memberdetails.render());
	}
}
