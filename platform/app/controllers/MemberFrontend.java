package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.dashboard;
import views.html.members.configuration;
import views.html.members.providers;
import views.html.members.studies;
import views.html.members.studydetails;
import views.html.members.studyrecords;
import views.html.details.entercode;

public class MemberFrontend extends Controller {

	public static Result configuration() {
		return ok(configuration.render());
	}
	
	public static Result providers() {
		return ok(dashboard.render("providers", "Healthcare Providers"));
	}
	
	public static Result studies() {
		return ok(studies.render());
	}
	
	public static Result studydetails(String study) {
		return ok(studydetails.render());
	}
	
	public static Result studyrecords(String study) {
		return ok(studyrecords.render());
	}
	
	public static Result entercode() {
		return ok(entercode.render());
	}
}
