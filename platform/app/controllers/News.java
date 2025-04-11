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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import actions.MobileCall;
import actions.VisualizationCall;
import models.MidataId;
import models.NewsItem;
import models.StudyParticipation;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for a "news" system. 
 *
 */
public class News extends APIController {


	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result get(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "properties");
		
		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Date from = null;
		Date to = null;
		Date official = new Date();
				
		if (properties.containsKey("from") && properties.containsKey("to")) {
			from = JsonValidation.getDate(json.get("properties"), "from");
			to = JsonValidation.getDate(json.get("properties"), "to");
			
			properties.put("date", CMaps.map("$lt", to));
			properties.put("expires", CMaps.map("$gte", from));
			properties.remove("from");
			properties.remove("to");
		}
		if (to != null && to.before(official)) official = to;
		if (from != null && from.after(official)) official = from;
		
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "studyId");
		
		Set<NewsItem> newsItems = NewsItem.getAll(properties, NewsItem.ALL);
		List<NewsItem> result = new ArrayList<NewsItem>(newsItems.size());
		
		
		result.addAll(newsItems);			
		Collections.sort(result);
		
		return ok(Json.toJson(result));
	}
	
	@BodyParser.Of(BodyParser.Json.class)	
	@MobileCall
	public Result getPublic(Request request) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "properties", "fields", "authToken");
		AccessContext info = null;
		String param = json.get("authToken").asText();//request.header("Authorization").get();
		
		if (param != null) {
	          info = ExecutionInfo.checkToken(request, param, false, false);
		}
		
        JsonValidation.validate(json, "properties");
		
		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Date from = null;
		Date to = null;
		Date official = new Date();
				
		if (properties.containsKey("from") && properties.containsKey("to")) {
			from = JsonValidation.getDate(json.get("properties"), "from");
			to = JsonValidation.getDate(json.get("properties"), "to");
			
			properties.put("date", CMaps.map("$lt", to));
			properties.put("expires", CMaps.map("$gte", from));
			properties.remove("from");
			properties.remove("to");
		}
		if (to != null && to.before(official)) official = to;
		if (from != null && from.after(official)) official = from;
		
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "studyId");
		
		Set<NewsItem> newsItems = NewsItem.getAll(properties, NewsItem.ALL);
		List<NewsItem> result = new ArrayList<NewsItem>(newsItems.size());
		
		if (!newsItems.isEmpty()) {
						
			MidataId user = info.getAccessor();
			Set<StudyParticipation> participations = StudyParticipation.getAllActiveByMember(user, Sets.create("study", "dateOfCreation", "pstatus"));
			Map<MidataId, StudyParticipation> studyIds = new HashMap<MidataId, StudyParticipation>();
			for (StudyParticipation part : participations) {			 
			   studyIds.put(part.study, part);
			}
			for (NewsItem itm : newsItems) {
				if (itm.onlyParticipantsStudyId != null) {
					StudyParticipation part = studyIds.get(itm.onlyParticipantsStudyId);
					if (part != null) {
						if (itm.dynamicDate) {
							if (part.dateOfCreation.after(itm.date)) {
								itm.date = part.dateOfCreation;								
							}
							if (part.dateOfCreation.after(itm.expires)) continue;
							if (to != null && itm.date.after(to)) continue;
							if (from != null && itm.date.before(from)) continue;
						}						
						result.add(itm);
					}
				} else {
					if (itm.dynamicDate) {
						itm.date = official;
					}					
					result.add(itm);
				}
			}				 		 		 
			 
			Collections.sort(result);
		}
		return ok(Json.toJson(result));
	}
		

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result add(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "title", "content", "date");		

		// create new news item
		NewsItem item = new NewsItem();
		item._id = new MidataId();
		item.creator = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		item.created = new Date();
		item.date = JsonValidation.getDate(json, "date");
		item.expires = JsonValidation.getDate(json, "expires");
		item.appId = JsonValidation.getMidataId(json, "appId");
		item.studyId = JsonValidation.getMidataId(json, "studyId");
		item.onlyParticipantsStudyId = JsonValidation.getMidataId(json, "onlyParticipantsStudyId");
		//item.onlyUsersOfAppId = JsonValidation.getMidataId(json, "onlyUsersOfAppId");
		item.title = JsonValidation.getString(json, "title");
		item.content = JsonValidation.getString(json, "content");
		item.layout = JsonValidation.getString(json, "layout");
		item.language = JsonValidation.getString(json, "language");
		item.url = JsonValidation.getStringOrNull(json, "url");
		item.broadcast = JsonValidation.getBoolean(json, "broadcast");
		item.dynamicDate = JsonValidation.getBoolean(json, "dynamicDate");
		if (item.expires == null) item.expires = item.date;
		NewsItem.add(item);
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result update(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "_id", "title", "content", "date");		

		// create new news item
		NewsItem item = NewsItem.get(CMaps.map("_id", JsonValidation.getMidataId(json, "_id")), NewsItem.ALL);
		item.creator = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
		item.date = JsonValidation.getDate(json, "date");
		item.expires = JsonValidation.getDate(json, "expires");
		item.appId = JsonValidation.getMidataId(json, "appId");
		item.studyId = JsonValidation.getMidataId(json, "studyId");
		item.onlyParticipantsStudyId = JsonValidation.getMidataId(json, "onlyParticipantsStudyId");
		//item.onlyUsersOfAppId = JsonValidation.getMidataId(json, "onlyUsersOfAppId");
		item.title = JsonValidation.getString(json, "title");
		item.content = JsonValidation.getString(json, "content");
		item.layout = JsonValidation.getString(json, "layout");
		item.language = JsonValidation.getString(json, "language");
		item.url = JsonValidation.getStringOrNull(json, "url");
		item.broadcast = JsonValidation.getBoolean(json, "broadcast");
		item.dynamicDate = JsonValidation.getBoolean(json, "dynamicDate");
		
		NewsItem.update(item);
		
		return ok();
	}

	/*
	public static Result hide(String newsItemIdString) {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId newsItemId = new MidataId(newsItemIdString);
		try {
			Member user = Member.get(new ChainedMap<String, MidataId>().put("_id", userId).get(), new ChainedSet<String>()
					.add("news").get());
			user.news.remove(newsItemId);
			Member.set(userId, "news", user.news);
		} catch (InternalServerException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}*/

	/**
	 * Delete a news item
	 * @param newsItemIdString id of news item to delete
	 * @return status 200
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result delete(String newsItemIdString) throws InternalServerException {
			
		MidataId newsItemId = new MidataId(newsItemIdString);
		
		/*if (!NewsItem.exists(new ChainedMap<String, MidataId>().put("_id", newsItemId).put("creator", userId).get())) {
				return badRequest("No news item with this id exists.");
		}*/
		

		NewsItem.delete(newsItemId);
	
		return ok();
	}

}
