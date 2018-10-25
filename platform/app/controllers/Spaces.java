package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import models.Member;
import models.MidataId;
import models.Plugin;
import models.Space;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
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

/**
 * functions for managing spaces (instances of plugins)
 *
 */
@Security.Authenticated(AnyRoleSecured.class)
public class Spaces extends APIController {
	

	
    /**
     * retrieve a list of spaces of the current user matching some criteria
     * @return list of spaces
     * @throws JsonValidationException
     * @throws AppException
     */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result get() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
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
		return ok(JsonOutput.toJson(spaces, "Space", fields)).as("application/json");
	}

	/**
	 * create a new space for the current user
	 * @return the new space
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public Result add() throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "name", "visualization");
		
		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
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
				
		return ok(JsonOutput.toJson(space, "Space", Space.ALL)).as("application/json");
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
	

	/**
	 * delete a space of the current user
	 * @param spaceIdString ID of space
	 * @return status ok
	 * @throws AppException
	 */
	@APICall
	public Result delete(String spaceIdString) throws AppException {
		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space == null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}
		
		Circles.removeQueries(userId, spaceId);
		RecordManager.instance.deleteAPS(space._id, userId);
		
		// delete space		
		Space.delete(userId, spaceId);
		
		return ok();
	}
	
	@APICall
	public Result reset() throws AppException {
		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("_id"));
		
		for (Space space : spaces) {		
		  Circles.removeQueries(userId, space._id);
		  RecordManager.instance.deleteAPS(space._id, userId);		
		  Space.delete(userId, space._id);
		}
		
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
	public Result addRecords(String spaceIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "records");
		
		// validate request
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
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
	public Result getToken(String spaceIdString) throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space==null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, space._id, userId, getRole());
		return ok(spaceToken.encrypt(request()));
	}
	
	@APICall
	public CompletionStage<Result> getUrl(String spaceIdString, String userId) throws AppException {
		return getUrl(spaceIdString, true, userId);
	}
	
	@APICall
	public CompletionStage<Result> regetUrl(String spaceIdString) throws AppException {
		return getUrl(spaceIdString, false, null);
	}
	
		
	public static CompletionStage<Result> getUrl(String spaceIdString, boolean auth, String targetUser) throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		MidataId spaceId = new MidataId(spaceIdString);
		MidataId targetUserId = (targetUser != null) ? MidataId.from(targetUser) : userId;		
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps", "visualization", "type", "name"));
		
		if (space==null) {
		  throw new InternalServerException("error.internal", "No space with this id exists.");
		}
		
		Plugin visualization = Plugin.getById(space.visualization, Sets.create("type", "name", "filename", "url", "previewUrl", "creator", "developmentServer", "accessTokenUrl", "authorizationUrl", "consumerKey", "scopeParameters"));

		boolean testing = (visualization.creator.equals(PortalSessionToken.session().getDeveloper()) || visualization.creator.equals(userId)) && visualization.developmentServer != null && visualization.developmentServer.length()> 0; 
		
		
	    SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, space._id, userId, targetUserId, getRole(), true);
			
		String visualizationServer = "https://" + InstanceConfig.getInstance().getConfig().getString("visualizations.server") + "/" + visualization.filename;
		if (testing) visualizationServer = visualization.developmentServer;
					
		ObjectNode obj = Json.newObject();
		obj.put("base", visualizationServer+"/");
		obj.put("token", spaceToken.encrypt(request()));
		obj.put("preview", visualization.previewUrl);
		obj.put("add", visualization.addDataUrl);
		obj.put("main", visualization.url);
		obj.put("type", visualization.type);
		obj.put("name", space.name);
		obj.put("owner", targetUserId.toString());
		
		if (visualization.type != null && visualization.type.equals("oauth2")) {
  		  BSONObject oauthmeta = RecordManager.instance.getMeta(userId, new MidataId(spaceIdString), "_oauth");  		  
		  if (oauthmeta == null) return CompletableFuture.completedFuture((Result) Plugins.oauthInfo(visualization, obj)); 
			 
		  Map<String, Object> data = oauthmeta.toMap();
		  if (data != null && data.get("refreshToken") != null) {					
		    return Plugins.requestAccessTokenOAuth2FromRefreshToken(spaceIdString, data, obj);
		  }
		  AccessLog.log("No refresh token requested.");
		} 
		if (visualization.type != null && visualization.type.equals("oauth1")) {
	  		  BSONObject oauthmeta = RecordManager.instance.getMeta(userId, new MidataId(spaceIdString), "_oauth");  		  
			  if (oauthmeta == null) return CompletableFuture.completedFuture((Result) Plugins.oauthInfo(visualization, obj)); 
				 		
			  Map<String, Object> oauthData = oauthmeta.toMap();
			  if (oauthData == null || oauthData.get("appId") == null || oauthData.get("oauthToken") == null || oauthData.get("oauthTokenSecret") == null) return CompletableFuture.completedFuture((Result) Plugins.oauthInfo(visualization, obj));

		} 
					
		return CompletableFuture.completedFuture((Result) ok(obj));
	
	}
	
}
