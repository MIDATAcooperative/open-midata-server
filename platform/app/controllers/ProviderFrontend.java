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
import views.html.providers.market;
import views.html.providers.app;
import views.html.providers.visualization;
import views.html.providers.createrecords;

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
	public static Result market() {
		return ok(market.render());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result appDetails(String id) {
		return ok(app.render());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result visualizationDetails(String id) {
		return ok(visualization.render());
	}
	
	@Security.Authenticated(ProviderSecured.class)
	public static Result createRecord(String appIdString, String ownerIdString) {
		return ok(createrecords.render());
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
