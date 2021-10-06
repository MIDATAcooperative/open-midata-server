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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import actions.VisualizationCall;
import models.MidataId;
import models.NewsItem;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.collections.CMaps;
import utils.db.ObjectIdConversion;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions for a "news" system. 
 *
 */
public class News extends Controller {


	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result get(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "properties", "fields");
		
		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		
		if (properties.containsKey("from") && properties.containsKey("to")) {
			properties.put("created", CMaps.map("$gte", JsonValidation.getDate(json.get("properties"), "from")).map("$lt", JsonValidation.getDate(json.get("properties"), "to")));
			properties.remove("from");
			properties.remove("to");
		}
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "studyId");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<NewsItem> newsItems;
		
		newsItems = new ArrayList<NewsItem>(NewsItem.getAll(properties, fields));
		
		Collections.sort(newsItems);
		return ok(Json.toJson(newsItems));
	}
	
	@BodyParser.Of(BodyParser.Json.class)	
	@VisualizationCall
	public Result getPublic(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "properties", "fields");
		
		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		
		if (properties.containsKey("from") && properties.containsKey("to")) {
			properties.put("date", CMaps.map("$gte", JsonValidation.getDate(json.get("properties"), "from")).map("$lt", JsonValidation.getDate(json.get("properties"), "to")));
			properties.remove("from");
			properties.remove("to");
		}
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "studyId", "appId", "onlyParticipantsStudyId", "onlyUsersOfAppId");
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<NewsItem> newsItems;
		
		newsItems = new ArrayList<NewsItem>(NewsItem.getAll(properties, fields));
		
		Collections.sort(newsItems);
		return ok(Json.toJson(newsItems));
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
		//item.onlyParticipantsStudyId = JsonValidation.getMidataId(json, "onlyParticipantsStudyId");
		//item.onlyUsersOfAppId = JsonValidation.getMidataId(json, "onlyUsersOfAppId");
		item.title = JsonValidation.getString(json, "title");
		item.content = JsonValidation.getString(json, "content");
		item.layout = JsonValidation.getString(json, "layout");
		item.language = JsonValidation.getString(json, "language");
		item.url = JsonValidation.getStringOrNull(json, "url");
		item.broadcast = JsonValidation.getBoolean(json, "broadcast");
		
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
		//item.onlyParticipantsStudyId = JsonValidation.getMidataId(json, "onlyParticipantsStudyId");
		//item.onlyUsersOfAppId = JsonValidation.getMidataId(json, "onlyUsersOfAppId");
		item.title = JsonValidation.getString(json, "title");
		item.content = JsonValidation.getString(json, "content");
		item.layout = JsonValidation.getString(json, "layout");
		item.language = JsonValidation.getString(json, "language");
		item.url = JsonValidation.getStringOrNull(json, "url");
		item.broadcast = JsonValidation.getBoolean(json, "broadcast");
		
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
