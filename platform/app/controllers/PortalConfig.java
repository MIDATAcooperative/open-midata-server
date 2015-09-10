package controllers;

import java.util.Map;

import models.ModelException;
import models.Space;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

public class PortalConfig extends Controller {	
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result getConfig() throws JsonValidationException, ModelException {
	
		ObjectId userId = new ObjectId(request().username());
		
	    Space config = Space.getByOwnerSpecialContext(userId, "portal", Sets.create("name"));
	    if (config == null) return ok();
	    
		BSONObject meta = RecordSharing.instance.getMeta(userId, config._id, "_config");		
		if (meta != null) return ok(Json.toJson(meta.toMap()));
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result setConfig() throws JsonValidationException, ModelException {
		ObjectId userId = new ObjectId(request().username());

		// validate json
		JsonNode json = request().body().asJson();		
		JsonValidation.validate(json, "config");
		
		Space configspace = Space.getByOwnerSpecialContext(userId, "portal", Sets.create("name"));
		if (configspace == null) {			
			configspace = Spaces.add(userId, "portal", null, null, "portal");
		}
		
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		
		RecordSharing.instance.setMeta(userId, configspace._id, "_config", config);
						
		return ok();
	}
}
