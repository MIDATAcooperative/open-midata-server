package controllers;

import java.util.Collection;

import actions.APICall;

import models.FormatGroup;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.exceptions.ModelException;

/**
 * used by portal to retrieve data groups
 * @author alexander
 *
 */
public class FormatAPI extends Controller {

	/**
	 * public function to get a list of data groups
	 * @return
	 * @throws ModelException
	 */
	@APICall
	public static Result listGroups() throws ModelException {
	    Collection<FormatGroup> groups = FormatGroup.getAll();
	    return ok(Json.toJson(groups));
	}
}
