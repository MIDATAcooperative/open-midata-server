package controllers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import actions.VisualizationCall;

import models.Coding;
import models.ContentInfo;
import models.FormatGroup;
import models.FormatInfo;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

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
	
	/**
	 * public function to get a list of formats supported by the platform
	 * @return 
	 * @throws InternalServerException
	 */
	@APICall
	public static Result listFormats() throws InternalServerException {
	    Collection<FormatInfo> formats = FormatInfo.getAll(Collections.EMPTY_MAP, Sets.create("format"));
	    return ok(Json.toJson(formats));
	}
	
	/**
	 * public function to get a list of content type prefixes supported by the platform
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public static Result listContents() throws InternalServerException {
	    Collection<ContentInfo> contents = ContentInfo.getAll(Collections.EMPTY_MAP, Sets.create("content", "group", "security"));
	    return ok(Json.toJson(contents));
	}
	
	/**
	 * function used by plugins to lookup information for coded values
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result searchCoding() throws JsonValidationException, InternalServerException {
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));						
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
	    Collection<Coding> contents = Coding.getAll(properties, fields);
	    return ok(Json.toJson(contents));
	}
}
