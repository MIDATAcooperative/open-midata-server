package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Member;

import models.MidataId;

import actions.APICall;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.search.CompletionResult;
import utils.search.Search;
import utils.search.SearchResult;

@Security.Authenticated(AnyRoleSecured.class)
public class GlobalSearch extends Controller {


	/**
	 * Search in all the user's accessible data.
	 *//*
	@APICall
	public static Result search(String query) throws AppException {
		MidataId userId = new MidataId(request().username());
		
		Set<Circle> circles = Circle.getAllByMember(userId);
		Map<String, List<SearchResult>> searchResults = Search.search(userId, circles, query);
		return ok(Json.toJson(searchResults));
	}*/

	/**
	 * Suggests completions for the given query.
	 *//*
	 @APICall
	public static Result complete(String query) {
		Map<String, List<CompletionResult>> completions = Search.complete(new MidataId(request().username()), query);
		List<CompletionResult> results = new ArrayList<CompletionResult>();
		for (String type : completions.keySet()) {
			results.addAll(completions.get(type));
		}
		Collections.sort(results);
		return ok(Json.toJson(results));
	}*/
}
