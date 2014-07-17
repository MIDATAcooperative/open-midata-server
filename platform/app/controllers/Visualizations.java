package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.details.visualization;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Visualizations extends Controller {

	public static Result details(String visualizationIdString) {
		return ok(visualization.render());
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

		// get visualizations
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Visualization> visualizations;
		try {
			visualizations = new ArrayList<Visualization>(Visualization.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(visualizations);
		return ok(Json.toJson(visualizations));
	}

	public static Result install(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("visualizations").get();
		try {
			User user = User.get(properties, fields);
			user.visualizations.add(new ObjectId(visualizationIdString));
			User.set(userId, "visualizations", user.visualizations);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result uninstall(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("visualizations").get();
		try {
			User user = User.get(properties, fields);
			user.visualizations.remove(new ObjectId(visualizationIdString));
			User.set(userId, "visualizations", user.visualizations);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result isInstalled(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Map<String, Object> properties = new ChainedMap<String, Object>().put("_id", userId)
				.put("visualizations", visualizationId).get();
		boolean isInstalled;
		try {
			isInstalled = User.exists(properties);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(Json.toJson(isInstalled));
	}

	public static Result getUrl(String visualizationIdString) {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", visualizationId).get();
		Set<String> fields = new ChainedSet<String>().add("url").get();
		Visualization visualization;
		try {
			visualization = Visualization.get(properties, fields);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualizationId + "/" + visualization.url;
		return ok(url);
	}

}
