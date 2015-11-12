package controllers;

import java.util.Collection;
import java.util.Collections;

import actions.APICall;

import models.ContentInfo;
import models.FormatGroup;
import models.FormatInfo;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * used by portal to retrieve data groups
 * @author alexander
 *
 */
public class FormatAPI extends Controller {

	/**
	 * public function to get a list of data groups
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public static Result listGroups() throws InternalServerException {
	    Collection<FormatGroup> groups = FormatGroup.getAll();
	    return ok(Json.toJson(groups));
	}
	
	@APICall
	public static Result listFormats() throws InternalServerException {
	    Collection<FormatInfo> formats = FormatInfo.getAll(Collections.EMPTY_MAP, Sets.create("format"));
	    return ok(Json.toJson(formats));
	}
	
	@APICall
	public static Result listContents() throws InternalServerException {
	    Collection<ContentInfo> contents = ContentInfo.getAll(Collections.EMPTY_MAP, Sets.create("content", "group", "security"));
	    return ok(Json.toJson(contents));
	}
}
