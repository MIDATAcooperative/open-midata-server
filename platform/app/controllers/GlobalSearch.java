package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.search.CompletionResult;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.search;

@Security.Authenticated(Secured.class)
public class GlobalSearch extends Controller {

	/**
	 * Load site and give control to JS controller.
	 */
	public static Result index(String query) {
		return ok(search.render());
	}

	/**
	 * Search in all the user's accessible data.
	 */
	public static Result search(String query) {
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("visible").get();
		User user;
		try {
			user = User.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Map<String, List<SearchResult>> searchResults = Search.search(userId, user.visible, query);
		return ok(Json.toJson(searchResults));
	}

	/**
	 * Suggests completions for the given query.
	 */
	public static Result complete(String query) {
		Map<String, List<CompletionResult>> completions = Search.complete(new ObjectId(request().username()), query);
		List<CompletionResult> results = new ArrayList<CompletionResult>();
		for (String type : completions.keySet()) {
			results.addAll(completions.get(type));
		}
		Collections.sort(results);
		return ok(Json.toJson(results));
	}
}
