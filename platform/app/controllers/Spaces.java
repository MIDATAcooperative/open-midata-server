package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Member;
import models.ModelException;
import models.Space;
import models.Visualization;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.spaces;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Spaces extends Controller {

	public static Result index() {
		return ok(spaces.render());
	}

	public static Result details(String spaceIdString) {
		return index();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "properties", "fields");
		

		// get spaces
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Space> spaces;
		try {
			spaces = new ArrayList<Space>(Space.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(spaces);
		return ok(Json.toJson(spaces));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result add() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "visualization");
		
		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = json.get("name").asText();
		String visualizationIdString = json.get("visualization").asText();
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String appIdString = json.get("app").asText();
		ObjectId appId = (appIdString == null || appIdString.equals("null")) ? null : new ObjectId(appIdString);
		
		if (Space.existsByNameAndOwner(name, userId)) {
			return badRequest("A space with this name already exists.");
		}
				
		Space space = add(userId, name, visualizationId, appId);
				
		return ok(Json.toJson(space));
	}
	
	public static Space add(ObjectId userId, String name, ObjectId visualizationId, ObjectId appId) throws ModelException {
			
		if (Space.existsByNameAndOwner(name, userId)) {
			throw new ModelException("A space with this name already exists.");
		}		
		// create new space
		Space space = new Space();
		space._id = new ObjectId();
		space.owner = userId;
		space.name = name;
		space.order = Space.getMaxOrder(userId) + 1;
		space.visualization = visualizationId;
		space.app = appId;
		space.aps = RecordSharing.instance.createPrivateAPS(userId, space._id);
		
		Space.add(space);		
		return space;
	}

	@APICall
	public static Result delete(String spaceIdString) throws ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space == null) {
			return badRequest("No space with this id exists.");
		}
		
		RecordSharing.instance.deleteAPS(space.aps, userId);

		// delete space		
		Space.delete(userId, spaceId);
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addRecords(String spaceIdString) throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "records");
		
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		Member owner = Member.getById(userId, Sets.create("myaps"));
		if (owner == null) {
			return badRequest("Member does not exist");
		}		
		if (space == null) {
			return badRequest("No space with this id exists.");
		}
		
		// add records to space (implicit: if not already present)
		Set<ObjectId> recordIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("records")));
		
		RecordSharing.instance.share(userId, owner.myaps, space.aps, recordIds, true);
						
		return ok();
	}

	@APICall
	public static Result getToken(String spaceIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space==null) {
		  return badRequest("No space with this id exists.");
		}

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(space.aps, userId);
		return ok(spaceToken.encrypt());
	}
	
	@APICall
	public static Result getUrl(String spaceIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps", "visualization"));
		
		if (space==null) {
		  return badRequest("No space with this id exists.");
		}
		
		Visualization visualization = Visualization.getById(space.visualization, Sets.create("filename", "url"));
		
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(space.aps, userId);
		
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualization.filename + "/" + visualization.url;
		url = url.replace(":authToken", spaceToken.encrypt());
		return ok(url);						
	}
	
	@APICall
	public static Result getPreviewUrl(String spaceIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps", "visualization"));
		
		if (space==null) {
		  return badRequest("No space with this id exists.");
		}
		
		Visualization visualization = Visualization.getById(space.visualization, Sets.create("filename", "previewUrl"));
		
		if (visualization.previewUrl == null) return ok();
		
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(space.aps, userId);
		
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualization.filename + "/" + visualization.previewUrl;
		url = url.replace(":authToken", spaceToken.encrypt());
		return ok(url);						
	}
	
	
}
