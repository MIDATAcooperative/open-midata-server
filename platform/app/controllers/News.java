package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.NewsItem;
import models.Member;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.index;
import views.html.dashboard;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class News extends Controller {

	public static Result index() {
		return ok(dashboard.render("overview", "Overview"));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<NewsItem> newsItems;
		try {
			newsItems = new ArrayList<NewsItem>(NewsItem.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(newsItems);
		return ok(Json.toJson(newsItems));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result add() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "title", "content", "broadcast");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// create new news item
		NewsItem item = new NewsItem();
		item._id = new ObjectId();
		item.creator = new ObjectId(request().username());
		item.created = DateTimeUtils.now();
		item.title = json.get("title").asText();
		item.content = json.get("content").asText();
		item.broadcast = json.get("broadcast").asBoolean();
		try {
			NewsItem.add(item);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Hide a news item from the current user's news stream.
	 */
	public static Result hide(String newsItemIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId newsItemId = new ObjectId(newsItemIdString);
		try {
			Member user = Member.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>()
					.add("news").get());
			user.news.remove(newsItemId);
			Member.set(userId, "news", user.news);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result delete(String newsItemIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId newsItemId = new ObjectId(newsItemIdString);
		try {
			if (!NewsItem.exists(new ChainedMap<String, ObjectId>().put("_id", newsItemId).put("creator", userId).get())) {
				return badRequest("No news item with this id exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// delete news item
		try {
			NewsItem item = NewsItem.get(new ChainedMap<String, ObjectId>().put("_id", newsItemId).get(),
					new ChainedSet<String>().add("broadcast").get());
			NewsItem.delete(userId, newsItemId, item.broadcast);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

}
