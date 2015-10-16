package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.FilterRule;
import models.Member;
import models.Plugin;
import models.Space;
import models.enums.UserRole;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.AnyRoleSecured;
import utils.auth.AppToken;
import utils.auth.SpaceToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.ModelException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(AnyRoleSecured.class)
public class Spaces extends Controller {
	

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		ObjectId userId = new ObjectId(request().username());
		JsonValidation.validate(json, "properties", "fields");
		

		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		properties.put("owner", userId);		
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
				
		List<Space> spaces = new ArrayList<Space>(Space.getAll(properties, fields));
		
		if (fields.contains("query")) {
			for (Space space : spaces) {
				BSONObject q = RecordSharing.instance.getMeta(userId, space._id, "_query");
				if (q != null) space.query = q.toMap();
			}
		}
		Collections.sort(spaces);
		return ok(JsonOutput.toJson(spaces, "Space", fields));
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result add() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "name", "visualization");
		
		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = JsonValidation.getString(json, "name");		
		ObjectId visualizationId = JsonValidation.getObjectId(json, "visualization" );		
		ObjectId appId = JsonValidation.getObjectId(json,  "app");		
		String context = JsonValidation.getString(json, "context");
		
		Map<String, Object> query = null;
		Map<String, Object> config = null;
		
		if (json.has("query")) query = JsonExtraction.extractMap(json.get("query"));
		if (json.has("config")) config = JsonExtraction.extractMap(json.get("config"));
		
		// check
		
		if (Space.existsByNameAndOwner(name, userId)) {
			return badRequest("A space with this name already exists.");
		}
				
		// execute
		
		Space space = add(userId, name, visualizationId, appId, context);
		
		if (query != null) {
			RecordSharing.instance.shareByQuery(userId, userId, space._id, query);		
		}
		if (config != null) {
			RecordSharing.instance.setMeta(userId, space._id, "_config", config);
		}
				
		return ok(JsonOutput.toJson(space, "Space", Space.ALL));
	}
	
	public static Space add(ObjectId userId, String name, ObjectId visualizationId, ObjectId appId, String context) throws ModelException {
						
		// create new space
		Space space = new Space();
		space._id = new ObjectId();
		space.owner = userId;
		space.name = name;
		space.order = Space.getMaxOrder(userId) + 1;
		space.visualization = visualizationId;
		space.context = context;
		space.app = appId;
		RecordSharing.instance.createPrivateAPS(userId, space._id);
		
		Space.add(space);		
		return space;
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result getPreviewUrlFromSetup() throws ModelException, JsonValidationException {
		// validate json
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "name", "visualization", "context", "rules");
		
		// get parameters
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = JsonValidation.getObjectId(json, "visualization");
		String context = JsonValidation.getString(json, "context");
		String name = JsonValidation.getString(json, "name");
		
		// execute
		Space space = Space.getByOwnerVisualizationContext(userId, visualizationId, context, Sets.create("aps"));
		Plugin visualization = Plugin.getById(visualizationId, Sets.create("filename", "previewUrl"));		
		if (space==null) {
		   Member member = Member.getByIdAndVisualization(userId, visualizationId, Sets.create("aps"));
		   if (member == null) return badRequest("Not installed");
		
		   space = Spaces.add(userId, name, visualizationId, null, context);
		   		   
		}
						
		if (visualization.previewUrl == null) return ok();
		
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(space._id, userId);		
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualization.filename + "/" + visualization.previewUrl;
		url = url.replace(":authToken", spaceToken.encrypt());
		return ok(url);		
				
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
		
		RecordSharing.instance.deleteAPS(space._id, userId);

		// delete space		
		Space.delete(userId, spaceId);
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addRecords(String spaceIdString) throws JsonValidationException, AppException {
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
		RecordSharing.instance.share(userId, owner.myaps, space._id, recordIds, true);
						
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
		SpaceToken spaceToken = new SpaceToken(space._id, userId);
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
		
		Plugin visualization = Plugin.getById(space.visualization, Sets.create("type", "filename", "url", "creator", "developmentServer"));

		boolean testing = session().get("role").equals(UserRole.DEVELOPER.toString()) && visualization.creator.equals(userId) && visualization.developmentServer != null && visualization.developmentServer.length()> 0;
		
		if (visualization.type.equals("visualization")) {
			// create encrypted authToken
			SpaceToken spaceToken = new SpaceToken(space._id, userId);
			
			String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename;
			if (testing) visualizationServer = visualization.developmentServer;
			String url =  visualizationServer  + "/" + visualization.url;
			url = url.replace(":authToken", spaceToken.encrypt());
			return ok(url);	
		} else {
		    // create encrypted authToken
			AppToken appToken = new AppToken(visualization._id, userId);
			String authToken = appToken.encrypt();
 
			// put together url to load in iframe
			String appServer = "https://" + Play.application().configuration().getString("apps.server") + "/" + visualization.filename;
			if (testing) appServer = visualization.developmentServer;
			String url = visualization.url.replace(":authToken", authToken);
			return ok( appServer  + "/" + url);
		}
	}
	
	@APICall
	public static Result getPreviewUrl(String spaceIdString) throws ModelException {
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps", "visualization"));
		
		if (space==null) {
		  return badRequest("No space with this id exists.");
		}
		
		Plugin visualization = Plugin.getById(space.visualization, Sets.create("filename", "previewUrl", "type", "creator", "developmentServer"));
		
		if (visualization.previewUrl == null || visualization.previewUrl.equals("")) return ok();

		boolean testing = session().get("role").equals(UserRole.DEVELOPER.toString()) && visualization.creator.equals(userId) && visualization.developmentServer != null && visualization.developmentServer.length()> 0;
		if (visualization.type.equals("visualization")) {
		// create encrypted authToken
			SpaceToken spaceToken = new SpaceToken(space._id, userId);
			
			String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename;
			if (testing) visualizationServer = visualization.developmentServer;
			String url = visualizationServer  + "/" + visualization.previewUrl;
			url = url.replace(":authToken", spaceToken.encrypt());
			return ok(url);						
		
		} else {
		
		       // create encrypted authToken
				AppToken appToken = new AppToken(visualization._id, userId);
				String authToken = appToken.encrypt();

				// put together url to load in iframe
				String appServer = "https://" + Play.application().configuration().getString("apps.server") + "/" + visualization.filename;
				if (testing) appServer = visualization.developmentServer;
				String url = visualization.previewUrl.replace(":authToken", authToken);
				return ok( appServer + "/" + url);
		}
	}
	
	
}
