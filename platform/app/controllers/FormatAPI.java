package controllers;

import java.util.Collection;

import actions.APICall;

import models.FormatGroup;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.exceptions.ModelException;

public class FormatAPI extends Controller {

	@APICall
	public static Result listGroups() throws ModelException {
	    Collection<FormatGroup> groups = FormatGroup.getAll();
	    return ok(Json.toJson(groups));
	}
}
