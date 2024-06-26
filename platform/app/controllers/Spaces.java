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
import models.SubscriptionData;
import models.enums.PluginStatus;
import models.enums.UserFeature;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.LicenceChecker;
import utils.auth.PortalSessionToken;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
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
	public Result get(Request request) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
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
				BSONObject q = RecordManager.instance.getMeta(context, space._id, "_query");
				if (q != null) space.query = q.toMap();
			}
		}
		if (fields.contains("autoImport")) {
			for (Space space : spaces) {	
				if (space.autoImport) continue;
				List<SubscriptionData> subs = SubscriptionData.getByOwnerAndFormatAndInstance(userId, "time", space._id, SubscriptionData.ALL);
				for (SubscriptionData subData : subs) {
					if (subData.active) space.autoImport = true;
				}
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
	public Result add(Request request) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "name", "visualization");
		
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context1 = portalContext(request);
		String name = JsonValidation.getString(json, "name");		
		MidataId visualizationId = JsonValidation.getMidataId(json, "visualization" );					
		String context = JsonValidation.getString(json, "context");
		
		Map<String, Object> query = null;
		Map<String, Object> config = null;
		
		if (json.has("query")) query = JsonExtraction.extractMap(json.get("query"));
		if (json.has("config")) config = JsonExtraction.extractMap(json.get("config"));
		
		Plugin plg = Plugin.getById(visualizationId, Sets.create("type","licenceDef","status"));
		if (plg.status == PluginStatus.DELETED || plg.status == PluginStatus.END_OF_LIFE) throw new BadRequestException("error.expired.app", "Plugin expired");
		MidataId licence = null;
		if (LicenceChecker.licenceRequired(plg)) {
			licence = LicenceChecker.hasValidLicence(userId, plg, null);
			if (licence == null) throw new AuthException("error.missing.licence", "No licence found.", UserFeature.VALID_LICENCE);
		}
		
		// execute		
		Space space = add(userId, name, visualizationId, plg.type, context, licence);
		
		if (query != null) {
			RecordManager.instance.shareByQuery(context1, space._id, query);		
		}
		if (config != null) {
			RecordManager.instance.setMeta(context1, space._id, "_config", config);
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
	public static Space add(MidataId userId, String name, MidataId visualizationId, String type, String context, MidataId licence) throws AppException {
						
		// create new space
		Space space = new Space();
		space._id = new MidataId();
		space.owner = userId;
		space.name = name;
		space.order = Space.getMaxOrder(userId) + 1;
		space.visualization = visualizationId;
		space.context = context;
		space.type = type;
		space.licence = licence;
		RecordManager.instance.createPrivateAPS(null, userId, space._id);
		
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
	public Result delete(Request request, String spaceIdString) throws AppException {
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId spaceId = new MidataId(spaceIdString);
		AccessContext context = portalContext(request);
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space == null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}
		
		Circles.removeQueries(userId, spaceId);
		//SubscriptionManager.deactivateSubscriptions(userId, spaceId);
		RecordManager.instance.deleteAPS(context, space._id);
		
		// delete space		
		Space.delete(userId, spaceId);
		
		return ok();
	}
	
	@APICall
	public Result reset(Request request) throws AppException {
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		Set<Space> spaces = Space.getAllByOwner(userId, Sets.create("_id"));
		
		for (Space space : spaces) {		
		  Circles.removeQueries(userId, space._id);
		  RecordManager.instance.deleteAPS(context, space._id);		
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
	public Result addRecords(Request request, String spaceIdString) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "records");
		
		// validate request
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
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
		RecordManager.instance.share(context, owner.myaps, space._id, recordIds, true);
						
		return ok();
	}

	@APICall
	public Result getToken(Request request, String spaceIdString) throws AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		MidataId spaceId = new MidataId(spaceIdString);
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps"));
		
		if (space==null) {
			throw new BadRequestException("error.unknown.space", "Space does not exist");
		}

		// create encrypted authToken
		SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, space._id, userId, getRole());
		return ok(spaceToken.encrypt(request));
	}
	
	@APICall
	public CompletionStage<Result> getUrl(Request request, String spaceIdString, String userId) throws AppException {
		return getUrl(request, spaceIdString, true, userId);
	}
	
	@APICall
	public CompletionStage<Result> regetUrl(Request request, String spaceIdString) throws AppException {
		return getUrl(request, spaceIdString, false, null);
	}
	
		
	public static CompletionStage<Result> getUrl(Request request, String spaceIdString, boolean auth, String targetUser) throws AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId spaceId = new MidataId(spaceIdString);
		MidataId targetUserId = (targetUser != null && targetUser.trim().length()>0) ? MidataId.from(targetUser) : userId;		
		
		Space space = Space.getByIdAndOwner(spaceId, userId, Sets.create("aps", "visualization", "type", "name","licence"));
		
		if (space==null) {
		  throw new InternalServerException("error.internal", "No space with this id exists.");
		}
		
		Plugin visualization = Plugin.getById(space.visualization, Sets.create("type", "name", "filename", "url", "previewUrl", "creator", "developmentServer", "accessTokenUrl", "authorizationUrl", "consumerKey", "scopeParameters", "licenceDef", "developerTeam"));		
	
		boolean testing = visualization.isDeveloper(PortalSessionToken.session().getDeveloperId()) || visualization.isDeveloper(userId); 
		AccessLog.log("get url devid="+PortalSessionToken.session().getDeveloperId()+" userid="+userId+" testing="+testing);
		if (!testing && LicenceChecker.licenceRequired(visualization)) {
			LicenceChecker.checkSpace(userId, visualization, space);
		}
		
	    SpaceToken spaceToken = new SpaceToken(PortalSessionToken.session().handle, space._id, userId, targetUserId, getRole(), true);
			
		String visualizationServer = "https://" + InstanceConfig.getInstance().getConfig().getString("visualizations.server") + "/" + visualization.filename;
		if (testing) visualizationServer = visualization.developmentServer;
					
		ObjectNode obj = Json.newObject();
		obj.put("base", visualizationServer+"/");
		obj.put("token", spaceToken.encrypt(request));
		obj.put("preview", visualization.previewUrl);
		obj.put("add", visualization.addDataUrl);
		obj.put("main", visualization.url);
		obj.put("type", visualization.type);
		obj.put("name", space.name);
		obj.put("owner", targetUserId.toString());
		
		if (visualization.type != null && visualization.type.equals("oauth2")) {
  		  BSONObject oauthmeta = RecordManager.instance.getMeta(context, new MidataId(spaceIdString), "_oauth");  		  
		  if (oauthmeta == null) return CompletableFuture.completedFuture((Result) Plugins.oauthInfo(visualization, obj)); 
			 
		  Map<String, Object> data = oauthmeta.toMap();
		  if (data != null && data.get("refreshToken") != null) {					
		    return Plugins.requestAccessTokenOAuth2FromRefreshToken(request, spaceIdString, data, obj);
		  }
		  AccessLog.log("No refresh token requested.");
		} 
		if (visualization.type != null && visualization.type.equals("oauth1")) {
	  		  BSONObject oauthmeta = RecordManager.instance.getMeta(context, new MidataId(spaceIdString), "_oauth");  		  
			  if (oauthmeta == null) return CompletableFuture.completedFuture((Result) Plugins.oauthInfo(visualization, obj)); 
				 		
			  Map<String, Object> oauthData = oauthmeta.toMap();
			  if (oauthData == null || oauthData.get("appId") == null || oauthData.get("oauthToken") == null || oauthData.get("oauthTokenSecret") == null) return CompletableFuture.completedFuture((Result) Plugins.oauthInfo(visualization, obj));

		} 
					
		return CompletableFuture.completedFuture((Result) ok(obj));
	
	}
	
}
