package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Member;
import models.Plugin;
import models.Space;
import models.enums.UserRole;

import org.bson.BSONObject;
import models.MidataId;

import play.Play;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.PortalSessionToken;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * functions for managing spaces (instances of plugins)
 *
 */
@Security.Authenticated(AnyRoleSecured.class)
public class Spaces extends Controller {
	
    /**
     * retrieve a list of spaces of the current user matching some criteria
     * @return list of spaces
     * @throws JsonValidationException
     * @throws AppException
     */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		MidataId userId = new MidataId(request().username());
		JsonValidation.validate(json, "properties", "fields");
		
		// get parameters
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "visualization", "autoShare");
		// always restrict to current user
		properties.put("owner", userId);		
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
				
		List<Space> spaces = new ArrayList<Space>(Space.getAll(properties, fields));
		
		if (fields.contains("query")) {
			for (Space space : spaces) {
				BSONObject q = RecordManager.instance.getMeta(userId, space._id, "_query");
				if (q != null) space.query = q.toMap();
			}
		}
		Collections.sort(spaces);
		return ok(JsonOutput.toJson(spaces, "Space", fields));
	}

	/**
	 * create a new space for the current user
	 * @return the new space
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result add() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "name", "visualization");
		
		// validate request
		MidataId userId = new MidataId(request().username());
		String name = JsonValidation.getString(json, "name");		
		MidataId visualizationId = JsonValidation.getMidataId(json, "visualization" );					
		String context = JsonValidation.getString(json, "context");
		
		Map<String, Object> query = null;
		Map<String, Object> config = null;
		
		if (json.has("query")) query = JsonExtraction.extractMap(json.get("query"));
		if (json.has("config")) config = JsonExtraction.extractMap(json.get("config"));
		
		Plugin plg = Plugin.getById(visualizationId, Sets.create("type"));
				
		// execute		
		Space space = add(userId, name, visualizationId, plg.type, context);
		
		if (query != null) {
			RecordManager.instance.shareByQuery(userId, userId, space._id, query);		
		}
		if (config != null) {
			RecordManager.instance.setMeta(userId, space._id, "_config", config);
		}
				
		return ok(JsonOutput.toJson(space, "Space", Space.ALL));
	}
	
	/**
	 * helper function to create a new space
	 * @param userId ID of user owning the space
	 * @param name name of the space
	 * @param visualizationId plugin to be used for space
	 * @param appId input form to be used for space
	 * @param context name of dashboard where space should be shown
	 * @return
	 * @throws InternalServerException
	 */
	public static Space add(MidataId userId, String name, MidataId visualizationId, String type, String context) throws AppException {
						
		// create new space
		Space space = new Space();
		space._id = new MidataId();
		space.owner = userId;
		space.name = name;
		space.order = Space.getMaxOrder(userId) + 1;
		space.visualization = visualizationId;
		space.context = context;
		space.type = type;
		RecordManager.instance.createPrivateAPS(userId, space._id);
		
		Space.add(space);		
		return space;
	}
	
	/*
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result getPreviewUrlFromSetup() throws AppException {
		// validate json
		JsonNode json = request().body().asJson();				
		JsonValidation.validate(json, "name", "visualization", "context", "rules");
		
		// get parameters
		MidataId userId = new MidataId(request().username());
		MidataId visualizationId = JsonValidation.getMidataId(json, "visualization");
		String context = JsonValidation.getString(json, "context");
		String name = JsonValidation.getString(json, "name");
		
		// execute
		Space space = Space.getByOwnerVisualizationContext(userId, visualizationId, context, Sets.create("aps"));
		Plugin visualization = Plugin.getById(visualizationId, Sets.create("filename", "previewUrl", "type"));		
		if (space==null) {
		   Member member = Member.getByIdAndVisualization(userId, visualizationId, Sets.create("aps"));
		   if (member == null) throw new BadRequestException("error.internal", "Not installed");
		
		   space = Spaces.add(userId, name, visualizationId, visualization.type, context);
		   		   
		}
						
		if (visualization.previewUrl == null) return ok();
		
		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(space._id, userId);		
		String visualizationServer = Play.application().configuration().getString("visualizations.server");
		String url = "https://" + visualizationServer + "/" + visualization.filename + "/" + visualization.previewUrl;
		url = url.replace(":authToken", spaceToken.encrypt(request()));
		return ok(url);		
				
	}
	*/

	/**
	 * delete a space of the current user
	 * @param spaceIdString ID of space
	 * @return status ok
	 * @throws AppException
	 */
	@APICall
	public static Result delete(String spaceIdString) throws AppException {
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space == null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}
		
		RecordManager.instance.deleteAPS(space._id, userId);

		// delete space		
		Space.delete(userId, spaceId);
		
		return ok();
	}

	/**
	 * add records to a space of the current user
	 * @param spaceIdString ID of space
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addRecords(String spaceIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "records");
		
		// validate request
		MidataId userId = new MidataId(request().username());
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		Member owner = Member.getById(userId, Sets.create("myaps"));
		if (owner == null) {
			throw new BadRequestException("error.unknown.user", "Member does not exist");
		}		
		if (space == null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}
		
		// add records to space (implicit: if not already present)
		Set<MidataId> recordIds = ObjectIdConversion.toMidataIds(JsonExtraction.extractStringSet(json.get("records")));		
		RecordManager.instance.share(userId, owner.myaps, space._id, recordIds, true);
						
		return ok();
	}

	@APICall
	public static Result getToken(String spaceIdString) throws AppException {
		MidataId userId = new MidataId(request().username());
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space==null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(space._id, userId);
		return ok(spaceToken.encrypt(request()));
	}
	
	@APICall
	public static Promise<Result> getUrl(String spaceIdString) throws AppException {
		return getUrl(spaceIdString, true);
	}
	
	@APICall
	public static Promise<Result> regetUrl(String spaceIdString) throws AppException {
		return getUrl(spaceIdString, false);
	}
	
		
	public static Promise<Result> getUrl(String spaceIdString, boolean auth) throws AppException {
		MidataId userId = new MidataId(request().username());
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps", "visualization", "type", "name"));
		
		if (space==null) {
		  throw new InternalServerException("error.internal", "No space with this id exists.");
		}
		
		Plugin visualization = Plugin.getById(space.visualization, Sets.create("type", "name", "filename", "url", "previewUrl", "creator", "developmentServer", "accessTokenUrl", "authorizationUrl", "consumerKey", "scopeParameters"));

		boolean testing = PortalSessionToken.session().getRole().equals(UserRole.DEVELOPER) && visualization.creator.equals(userId) && visualization.developmentServer != null && visualization.developmentServer.length()> 0;
		
		
	    SpaceToken spaceToken = new SpaceToken(space._id, userId);
			
		String visualizationServer = "https://" + Play.application().configuration().getString("visualizations.server") + "/" + visualization.filename;
		if (testing) visualizationServer = visualization.developmentServer;
					
		ObjectNode obj = Json.newObject();
		obj.put("base", visualizationServer+"/");
		obj.put("token", spaceToken.encrypt(request()));
		obj.put("preview", visualization.previewUrl);
		obj.put("add", visualization.addDataUrl);
		obj.put("main", visualization.url);
		obj.put("type", visualization.type);
		obj.put("name", space.name);
		
		if (visualization.type != null && visualization.type.equals("oauth2")) {
  		  BSONObject oauthmeta = RecordManager.instance.getMeta(userId, new MidataId(spaceIdString), "_oauth");  		  
		  if (oauthmeta == null) return F.Promise.pure((Result) Plugins.oauthInfo(visualization, obj)); 
			 		
		  if (oauthmeta.containsField("refreshToken")) {					
		    return Plugins.requestAccessTokenOAuth2FromRefreshToken(spaceIdString, oauthmeta.toMap(), obj);
		  }
		} 
		if (visualization.type != null && visualization.type.equals("oauth1")) {
	  		  BSONObject oauthmeta = RecordManager.instance.getMeta(userId, new MidataId(spaceIdString), "_oauth");  		  
			  if (oauthmeta == null) return F.Promise.pure((Result) Plugins.oauthInfo(visualization, obj)); 
				 		
			  /*if (oauthmeta.containsField("refreshToken")) {					
			    return Plugins.requestAccessTokenOAuth2FromRefreshToken(spaceIdString, oauthmeta.toMap(), (Result) ok(obj));
			  }*/
		} 
					
		return F.Promise.pure((Result) ok(obj));
	
	}
	
}
