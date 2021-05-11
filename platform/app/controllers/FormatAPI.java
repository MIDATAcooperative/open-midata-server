/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import models.FormatInfo;
import models.Loinc;
import models.MidataId;
import models.RecordGroup;
import models.enums.APSSecurityLevel;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AdminSecured;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.sync.Instances;

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
	public Result listGroups() throws AppException {
	    Collection<RecordGroup> groups = RecordGroup.getAll();
	    return ok(Json.toJson(groups));
	}
	
	/**
	 * public function to get a list of formats supported by the platform
	 * @return 
	 * @throws InternalServerException
	 */
	@APICall
	public Result listFormats() throws InternalServerException {
	    Collection<FormatInfo> formats = FormatInfo.getAll(Collections.<String, String> emptyMap(), Sets.create("format", "comment", "visName"));
	    return ok(Json.toJson(formats));
	}
	
	/**
	 * public function to get a list of content types supported by the platform
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public Result listContents() throws InternalServerException {
	    Collection<ContentInfo> contents = ContentInfo.getAll(Collections.<String, String> emptyMap(), Sets.create("content", "security", "comment", "label", "defaultCode", "resourceType", "subType", "defaultUnit", "category", "source"));
	    return ok(Json.toJson(contents));
	}
	
	/**
	 * public function to get a list of all codes supported by the platform
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public Result listCodes() throws InternalServerException {
	    Collection<ContentCode> codes = ContentCode.getAll(Collections.<String, String> emptyMap(), Sets.create("system", "code", "display", "content"));
	    return ok(Json.toJson(codes));
	}
	
	/**
	 * Create a new accepted code (Admin)
	 * @return
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result createCode() throws AppException {
		JsonNode json = request().body().asJson();
		ContentCode cc = new ContentCode();
		MidataId id = JsonValidation.getMidataId(json, "_id");
		cc._id = id != null ? id : new MidataId();		
		cc.code = JsonValidation.getString(json, "code");
		cc.content = JsonValidation.getString(json, "content");
		cc.version = JsonValidation.getStringOrNull(json, "version");
		cc.display = JsonValidation.getString(json, "display");
		cc.system = JsonValidation.getString(json, "system");
		
		ContentCode.add(cc);
		
		return ok();
	}
	
	/**
	 * Update an accepted code
	 * @param id id of code to update
	 * @return
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result updateCode(String id) throws AppException {
		JsonNode json = request().body().asJson();
		ContentCode cc = new ContentCode();
		cc._id = new MidataId(id);
		cc.code = JsonValidation.getString(json, "code");
		cc.content = JsonValidation.getString(json, "content");
		cc.version = JsonValidation.getStringOrNull(json, "version");
		cc.display = JsonValidation.getString(json, "display");
		cc.system = JsonValidation.getString(json, "system");
		
		ContentCode.upsert(cc);
		
		return ok();
	}
	
	/**
	 * Delete an accepted code from the database
	 * @param id id of code to delete
	 * @return
	 * @throws AppException
	 */
	@APICall	
	@Security.Authenticated(AdminSecured.class)
	public Result deleteCode(String id) throws AppException {
		ContentCode.delete(new MidataId(id));				
		return ok();
	}
	
	/**
	 * Create a new MIDATA content type
	 * @return
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result createContent() throws AppException {
		JsonNode json = request().body().asJson();
		ContentInfo cc = new ContentInfo();
		MidataId id = JsonValidation.getMidataId(json, "_id");
		cc._id = id != null ? id : new MidataId();
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
		Instances.cacheClear("content", null);
		
		return ok();
	}
	
	/**
	 * Update an existing MIDATA content type
	 * @param id id of content type to update
	 * @return
	 * @throws AppException
	 */
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result updateContent(String id) throws AppException {
		JsonNode json = request().body().asJson();
		ContentInfo cc = new ContentInfo();
		cc._id = new MidataId(id);
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
		Instances.cacheClear("content", null);
		
		return ok();
	}
	
	/**
	 * Delete a MIDATA content type
	 * @param id id of content type to delete
	 * @return
	 * @throws AppException
	 */
	@APICall	
	@Security.Authenticated(AdminSecured.class)
	public Result deleteContent(String id) throws AppException {
		ContentInfo.delete(new MidataId(id));				
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result createGroup() throws AppException {
		JsonNode json = request().body().asJson();
		RecordGroup cc = new RecordGroup();
		MidataId id = JsonValidation.getMidataId(json, "_id");
		cc._id = id != null ? id : new MidataId();		
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.parent = JsonValidation.getString(json, "parent");
		cc.contents = JsonExtraction.extractStringSet(json.get("contents"));
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		
		RecordGroup.add(cc);
		Instances.cacheClear("content", null);
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result updateGroup(String id) throws AppException {
		JsonNode json = request().body().asJson();
		RecordGroup cc = new RecordGroup();
		cc._id = new MidataId(id);
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.parent = JsonValidation.getStringOrNull(json, "parent");
		cc.contents = JsonExtraction.extractStringSet(json.get("contents"));
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		
		RecordGroup.upsert(cc);
		Instances.cacheClear("content", null);
		return ok();
	}
	
	@APICall	
	@Security.Authenticated(AdminSecured.class)
	public Result deleteGroup(String id) throws AppException {
		RecordGroup.delete(new MidataId(id));	
		Instances.cacheClear("content", null);
		return ok();
	}
	
	/**
	 * function used by plugins to lookup information for coded values
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result searchCodingPortal() throws JsonValidationException, InternalServerException {
		return searchCoding();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result searchCoding() throws JsonValidationException, InternalServerException {
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));	
		ObjectIdConversion.convertMidataIds(properties, "_id");
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
	public Result searchContent() throws JsonValidationException, InternalServerException {
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));		
		ObjectIdConversion.convertMidataIds(properties, "_id");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
	    Collection<ContentInfo> contents = ContentInfo.getAll(properties, fields);
	      	      
	    return ok(Json.toJson(contents));		
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result searchContents() throws JsonValidationException, InternalServerException {
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));		
		ObjectIdConversion.convertMidataIds(properties, "_id");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
	    Collection<ContentInfo> contents = ContentInfo.getAll(properties, fields);
	      	      
	    return ok(Json.toJson(contents));		
	}
}
