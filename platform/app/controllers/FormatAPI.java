package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import actions.VisualizationCall;

import models.ContentCode;
import models.ContentInfo;
import models.RecordGroup;
import models.FormatInfo;
import models.Loinc;
import models.enums.APSSecurityLevel;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * used by portal to retrieve data groups
 *
 */
public class FormatAPI extends Controller {

	/**
	 * public function to get a list of data groups
	 * @return
	 * @throws AppException
	 */
	@APICall
	public static Result listGroups() throws AppException {
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
	    Collection<ContentInfo> contents = ContentInfo.getAll(Collections.<String, String> emptyMap(), Sets.create("content", "security", "comment", "label", "defaultCode", "resourceType", "subType", "defaultUnit", "category", "source"));
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
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result createCode() throws AppException {
		JsonNode json = request().body().asJson();
		ContentCode cc = new ContentCode();
		cc._id = new ObjectId();
		cc.code = JsonValidation.getString(json, "code");
		cc.content = JsonValidation.getString(json, "content");
		cc.display = JsonValidation.getString(json, "display");
		cc.system = JsonValidation.getString(json, "system");
		
		ContentCode.add(cc);
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result updateCode(String id) throws AppException {
		JsonNode json = request().body().asJson();
		ContentCode cc = new ContentCode();
		cc._id = new ObjectId(id);
		cc.code = JsonValidation.getString(json, "code");
		cc.content = JsonValidation.getString(json, "content");
		cc.display = JsonValidation.getString(json, "display");
		cc.system = JsonValidation.getString(json, "system");
		
		ContentCode.upsert(cc);
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result deleteCode(String id) throws AppException {
		ContentCode.delete(new ObjectId(id));				
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result createContent() throws AppException {
		JsonNode json = request().body().asJson();
		ContentInfo cc = new ContentInfo();
		cc._id = new ObjectId();
		cc.defaultCode = JsonValidation.getString(json, "defaultCode");
		cc.content = JsonValidation.getString(json, "content");
		cc.security = JsonValidation.getEnum(json, "security",  APSSecurityLevel.class);
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		cc.resourceType = JsonValidation.getStringOrNull(json,  "resourceType");
		cc.subType = JsonValidation.getStringOrNull(json,  "subType");
		cc.defaultUnit = JsonValidation.getStringOrNull(json,  "defaultUnit");
		cc.category = JsonValidation.getStringOrNull(json,  "category");
		cc.source = JsonValidation.getStringOrNull(json,  "source");
		
		ContentInfo.add(cc);
		RecordGroup.invalidate();
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result updateContent(String id) throws AppException {
		JsonNode json = request().body().asJson();
		ContentInfo cc = new ContentInfo();
		cc._id = new ObjectId(id);
		cc.defaultCode = JsonValidation.getString(json, "defaultCode");
		cc.content = JsonValidation.getString(json, "content");
		cc.security = JsonValidation.getEnum(json, "security",  APSSecurityLevel.class);
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		cc.resourceType = JsonValidation.getStringOrNull(json,  "resourceType");
		cc.subType = JsonValidation.getStringOrNull(json,  "subType");
		cc.defaultUnit = JsonValidation.getStringOrNull(json,  "defaultUnit");
		cc.category = JsonValidation.getStringOrNull(json,  "category");
		cc.source = JsonValidation.getStringOrNull(json,  "source");
		
		ContentInfo.upsert(cc);
		RecordGroup.invalidate();
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result deleteContent(String id) throws AppException {
		ContentInfo.delete(new ObjectId(id));				
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result createGroup() throws AppException {
		JsonNode json = request().body().asJson();
		RecordGroup cc = new RecordGroup();
		cc._id = new ObjectId();
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.parent = JsonValidation.getString(json, "parent");
		cc.contents = JsonExtraction.extractStringSet(json.get("contents"));
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		
		RecordGroup.add(cc);
		RecordGroup.invalidate();
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result updateGroup(String id) throws AppException {
		JsonNode json = request().body().asJson();
		RecordGroup cc = new RecordGroup();
		cc._id = new ObjectId(id);
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.parent = JsonValidation.getStringOrNull(json, "parent");
		cc.contents = JsonExtraction.extractStringSet(json.get("contents"));
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		
		RecordGroup.upsert(cc);
		RecordGroup.invalidate();
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public static Result deleteGroup(String id) throws AppException {
		RecordGroup.delete(new ObjectId(id));	
		RecordGroup.invalidate();
		return ok();
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
	
	/**
	 * function used by plugins to lookup information for coded values
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public static Result searchContent() throws JsonValidationException, InternalServerException {
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));						
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
	    Collection<ContentInfo> contents = ContentInfo.getAll(properties, fields);
	      	      
	    return ok(Json.toJson(contents));		
	}
}
