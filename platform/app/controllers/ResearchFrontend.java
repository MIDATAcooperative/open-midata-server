package controllers;

import controllers.research.ResearchSecured;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.research.registration;
import views.html.research.login;
import views.html.research.messages;
import views.html.research.codes;
import views.html.research.createstudy;
import views.html.research.studies;
import views.html.research.studyfields;
import views.html.research.studymessages;
import views.html.research.studyoverview;
import views.html.research.studyparticipants;
import views.html.research.studyparticipant;
import views.html.research.studyrecords;
import views.html.research.studyresults;
import views.html.research.studyrules;

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
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studies() {
		return ok(studies.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result codes(String study) {
		return ok(codes.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result createstudy() {
		return ok(createstudy.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyfields(String study) {
		return ok(studyfields.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studymessages(String study) {
		return ok(studymessages.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyoverview(String study) {
		return ok(studyoverview.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyparticipants(String study) {
		return ok(studyparticipants.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyparticipant(String study, String participant) {
		return ok(studyparticipant.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyrecords(String study) {
		return ok(studyrecords.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyresults(String study) {
		return ok(studyresults.render());
	}
	
	@Security.Authenticated(ResearchSecured.class)
	public static Result studyrules(String study) {
		return ok(studyrules.render());
	}
}
