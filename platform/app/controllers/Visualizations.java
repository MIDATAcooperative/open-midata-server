package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Member;
import models.User;
import models.Space;
import models.Visualization;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.details.visualization;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

public class Visualizations extends APIController {

	@Security.Authenticated(Secured.class)
	public static Result details(String visualizationIdString) {
		return ok(visualization.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
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

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result install(String visualizationIdString) throws ModelException {
		JsonNode json = request().body().asJson();
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);

		String spaceName = JsonValidation.getString(json, "spaceName");
		boolean applyRules = JsonValidation.getBoolean(json, "applyRules");
		boolean createSpace = JsonValidation.getBoolean(json, "createSpace");
		
		Visualization visualization = Visualization.getById(visualizationId, Sets.create("defaultRules", "targetUserRole", "defaultSpaceName"));
		if (visualization == null) return badRequest("Unknown visualization");
		
		if (visualization.targetUserRole.equals(UserRole.MEMBER)) { 
			Member user = Member.getById(userId, Sets.create("visualizations", "myaps"));
			user.visualizations.add(visualizationId);
			Member.set(userId, "visualizations", user.visualizations);
					
			if (spaceName!=null && !spaceName.equals("") && visualization.defaultSpaceName != null) {
				Space space = null;
				if (createSpace) {
					space = Spaces.add(userId, spaceName, visualizationId, null);
				}
				if (applyRules && space!=null) {
					RuleApplication.instance.setupRules(userId, visualization.defaultRules, user.myaps, space.aps, true);
				}
			}
	
		} else {
			User user = User.getById(userId, Sets.create("visualizations","role"));
			
			if (!user.role.equals(visualization.targetUserRole) && !visualization.targetUserRole.equals(UserRole.ANY)) {
				return badRequest("Visualization is for a different role."+user.role);
			}
			
			user.visualizations.add(visualizationId);
			User.set(userId, "visualizations", user.visualizations);
								
		}
			// && visualization.targetUserRole != UserRole.ANY) return badRequest("Visualization is not for members");
		
				
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)
	public static Result uninstall(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());		
		Set<String> fields = new ChainedSet<String>().add("visualizations").get();
		try {
			User user = User.getById(userId, fields);
			user.visualizations.remove(new ObjectId(visualizationIdString));
			User.set(userId, "visualizations", user.visualizations);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@Security.Authenticated(AnyRoleSecured.class)
	public static Result isInstalled(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);		
		boolean isInstalled;
		try {
			isInstalled = Member.getByIdAndVisualization(userId, visualizationId, Sets.create()) != null;
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(Json.toJson(isInstalled));
	}

	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getUrl(String visualizationIdString) {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", visualizationId).get();
		Set<String> fields = new ChainedSet<String>().add("filename").add("url").get();
		Visualization visualization;
		try {
			visualization = Visualization.get(properties, fields);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualization.filename + "/" + visualization.url;
		return ok(url);
	}

}
