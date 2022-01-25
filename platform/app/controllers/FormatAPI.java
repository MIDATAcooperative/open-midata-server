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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import actions.VisualizationCall;
import models.ContentCode;
import models.ContentInfo;
import models.FormatInfo;
import models.GroupContent;
import models.Loinc;
import models.MidataId;
import models.RecordGroup;
import models.enums.APSSecurityLevel;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.auth.AdminSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
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
	    Collection<ContentInfo> contents = ContentInfo.getAll(CMaps.map("deleted", CMaps.map("$ne", true)), Sets.create("content", "security", "comment", "label", "defaultCode", "resourceType", "subType", "defaultUnit", "category", "source"));
	    return ok(Json.toJson(contents));
	}
	
	/**
	 * public function to get a list of all codes supported by the platform
	 * @return
	 * @throws InternalServerException
	 */
	@APICall
	public Result listCodes() throws InternalServerException {
	    Collection<ContentCode> codes = ContentCode.getAll(CMaps.map("deleted", CMaps.map("$ne", true)), Sets.create("system", "code", "display", "content"));
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
	public Result createCode(Request request) throws AppException {
		JsonNode json = request.body().asJson();
		ContentCode cc = new ContentCode();
		MidataId id = JsonValidation.getMidataId(json, "_id");
		cc._id = id != null ? id : new MidataId();		
		cc.code = JsonValidation.getString(json, "code");
		cc.content = JsonValidation.getString(json, "content");
		cc.version = JsonValidation.getStringOrNull(json, "version");
		cc.display = JsonValidation.getString(json, "display");
		cc.system = JsonValidation.getString(json, "system");
		cc.lastUpdated = System.currentTimeMillis();
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
	public Result updateCode(Request request, String id) throws AppException {
		JsonNode json = request.body().asJson();
		ContentCode cc = new ContentCode();
		cc._id = new MidataId(id);
		cc.code = JsonValidation.getString(json, "code");
		cc.content = JsonValidation.getString(json, "content");
		cc.version = JsonValidation.getStringOrNull(json, "version");
		cc.display = JsonValidation.getString(json, "display");
		cc.system = JsonValidation.getString(json, "system");
		cc.lastUpdated = System.currentTimeMillis();
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
	public Result createContent(Request request) throws AppException {
		JsonNode json = request.body().asJson();
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
		cc.lastUpdated = System.currentTimeMillis();
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
	public Result updateContent(Request request, String id) throws AppException {
		JsonNode json = request.body().asJson();
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
		cc.lastUpdated = System.currentTimeMillis();
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
	public Result createGroup(Request request) throws AppException {
		JsonNode json = request.body().asJson();
		RecordGroup cc = new RecordGroup();
		MidataId id = JsonValidation.getMidataId(json, "_id");
		cc._id = id != null ? id : new MidataId();		
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.parent = JsonValidation.getString(json, "parent");
		//cc.contents = JsonExtraction.extractStringSet(json.get("contents"));
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		cc.lastUpdated = System.currentTimeMillis();
		RecordGroup.add(cc);
		Instances.cacheClear("content", null);
		
		return ok();
	}
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result updateGroup(Request request, String id) throws AppException {
		JsonNode json = request.body().asJson();
		RecordGroup cc = new RecordGroup();
		cc._id = new MidataId(id);
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.parent = JsonValidation.getStringOrNull(json, "parent");
		//cc.contents = JsonExtraction.extractStringSet(json.get("contents"));
		cc.label = JsonExtraction.extractStringMap(json.get("label"));
		cc.lastUpdated = System.currentTimeMillis();
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
	
	@APICall
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	public Result updateGroupContent(Request request) throws AppException {
		JsonNode json = request.body().asJson();
		GroupContent cc = new GroupContent();
				
		cc.name = JsonValidation.getString(json, "name");
		cc.system = JsonValidation.getString(json, "system");
		cc.content = JsonValidation.getString(json, "content");
		
		GroupContent old = GroupContent.getPrevious(cc);
		if (old != null) cc = old; else cc._id = new MidataId();		
		cc.deleted = JsonValidation.getBoolean(json, "deleted");		
		cc.lastUpdated = System.currentTimeMillis();
		
		GroupContent.upsert(cc);
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
	public Result searchCodingPortal(Request request) throws JsonValidationException, InternalServerException {
		return searchCoding(request);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result searchCoding(Request request) throws JsonValidationException, InternalServerException {
		JsonNode json = request.body().asJson();
		
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
	      Collection<ContentCode> contents = ContentCode.getAll(CMaps.map(properties).map("deleted", CMaps.map("$ne", true)), fields);
	      
	      
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
	public Result searchContent(Request request) throws JsonValidationException, InternalServerException {
		JsonNode json = request.body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));		
		ObjectIdConversion.convertMidataIds(properties, "_id");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
	    Collection<ContentInfo> contents = ContentInfo.getAll(CMaps.map(properties).map("deleted", CMaps.map("$ne", true)), fields);
	      	      
	    return ok(Json.toJson(contents));		
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result searchContents(Request request) throws JsonValidationException, InternalServerException {
		JsonNode json = request.body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));		
		ObjectIdConversion.convertMidataIds(properties, "_id");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		
				
	    Collection<ContentInfo> contents = ContentInfo.getAll(CMaps.map(properties).map("deleted", CMaps.map("$ne", true)), fields);
	      	      
	    return ok(Json.toJson(contents));		
	}
	
	@APICall
	@Security.Authenticated(AdminSecured.class)
	public Result exportChanges() throws InternalServerException {
		ObjectNode obj = Json.newObject();
		
		Set<ContentInfo> ci = ContentInfo.getAll(CMaps.map("lastUpdated", CMaps.map("$gt", 0)), ContentInfo.ALL);
		Set<ContentCode> cc = ContentCode.getAll(CMaps.map("lastUpdated", CMaps.map("$gt", 0)), ContentCode.ALL);
		Set<RecordGroup> rg = RecordGroup.getAll(CMaps.map("lastUpdated", CMaps.map("$gt", 0)), RecordGroup.ALL);
		Set<GroupContent> gc = GroupContent.getAllChanged();
		
		obj.set("contentinfo", JsonOutput.toJsonNode(ci, "ContentInfo", ContentInfo.ALL));
		obj.set("coding", JsonOutput.toJsonNode(cc, "ContentCode", ContentCode.ALL));
		obj.set("formatgroups", JsonOutput.toJsonNode(rg, "RecordGroup", RecordGroup.ALL));
		obj.set("groupcontent", JsonOutput.toJsonNode(gc, "GroupContent", GroupContent.ALL));
		
		return ok(obj);
		
	}
	
	@APICall
	@Security.Authenticated(AdminSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result importChanges(Request request) throws AppException {
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "base64");
		String base64 = JsonValidation.getString(json, "base64");
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode contentinfo = null;
		ArrayNode coding = null;
		ArrayNode formatgroups = null;
		ArrayNode groupcontent = null;
		JsonNode allJson = null;
		try {
			allJson = mapper.readTree(base64);
			contentinfo = (ArrayNode) allJson.get("contentinfo");
			coding = (ArrayNode) allJson.get("coding");
			formatgroups = (ArrayNode) allJson.get("formatgroups");
			groupcontent = (ArrayNode) allJson.get("groupcontent");
				
			for (JsonNode c : contentinfo) {
			   ContentInfo ci = mapper.treeToValue(c, ContentInfo.class);
			   ContentInfo.upsert(ci);
			}
			for (JsonNode code : coding) {
				ContentCode cc = mapper.treeToValue(code, ContentCode.class);
				ContentCode.upsert(cc);
			}
			for (JsonNode group : formatgroups) {
				RecordGroup fg = mapper.treeToValue(group, RecordGroup.class);
				RecordGroup.upsert(fg);
			}
			for (JsonNode groupConent : groupcontent) {
				GroupContent gc = mapper.treeToValue(groupConent, GroupContent.class);
				GroupContent.upsert(gc);
			}
						
			
		} catch (JsonProcessingException e) {
			AccessLog.logException("parse json", e);
		  throw new BadRequestException("error.invalid.json", "Invalid JSON provided");
		} catch (IOException e) {
		  throw new BadRequestException("error.invalid.json", "Invalid JSON provided");
		} finally {
			Instances.cacheClear("content", null);
		}
		return ok();
	}
}
