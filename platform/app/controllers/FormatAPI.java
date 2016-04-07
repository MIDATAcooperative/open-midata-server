package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import actions.VisualizationCall;

import models.ContentCode;
import models.ContentInfo;
import models.RecordGroup;
import models.FormatInfo;
import models.Loinc;
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
	    Collection<RecordGroup> groups = RecordGroup.getAll();
	    return ok(Json.toJson(groups));
	}
	
	/**
	 * public function to get a list of formats supported by the platform
	 * @return 
	 * @throws InternalServerException
	 */
	@APICall
	public static Result listFormats() throws InternalServerException {
	    Collection<FormatInfo> formats = FormatInfo.getAll(Collections.<String, String> emptyMap(), Sets.create("format", "comment", "visName"));
	    return ok(Json.toJson(formats));
	}
	
	/**
	 * public function to get a list of content types supported by the platform
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public static Result listContents() throws InternalServerException {
	    Collection<ContentInfo> contents = ContentInfo.getAll(Collections.<String, String> emptyMap(), Sets.create("content", "security", "comment", "label", "defaultCode"));
	    return ok(Json.toJson(contents));
	}
	
	/**
	 * public function to get a list of all codes supported by the platform
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public static Result listCodes() throws InternalServerException {
	    Collection<ContentCode> codes = ContentCode.getAll(Collections.<String, String> emptyMap(), Sets.create("system", "code", "display", "content"));
	    return ok(Json.toJson(codes));
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
		
		// http://loinc.org
		// http://unitsofmeasure.org
		
		if (properties.containsKey("$text")) {
		  properties.remove("system");
		  Collection<Loinc> loincCodes = Loinc.getAll(properties, Sets.create("LOINC_NUM","LONG_COMMON_NAME"));
		  Collection<ContentCode> results = new ArrayList<ContentCode>(loincCodes.size());
		  for (Loinc loinc : loincCodes) {
			  ContentCode code = new ContentCode();
			  code.system = "http://loinc.org";
			  code.code = loinc.LOINC_NUM;
			  code.display = loinc.LONG_COMMON_NAME;
			  results.add(code);
		  }
		  return ok(Json.toJson(results));
		} else {		
	      Collection<ContentCode> contents = ContentCode.getAll(properties, fields);
	      
	      
	      return ok(Json.toJson(contents));
		}
	}
}
