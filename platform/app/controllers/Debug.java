package controllers;

import models.ModelException;
import models.Record;

import org.bson.types.ObjectId;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.EncryptedAPS;
import utils.auth.RecordToken;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class Debug extends Controller {

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get(String id) throws JsonValidationException, ModelException {
				
		ObjectId userId = new ObjectId(request().username());
		ObjectId apsId = id.equals("-") ? userId : new ObjectId(id);
		
		EncryptedAPS enc = new EncryptedAPS(apsId, userId);
								   			
		return ok(Json.toJson(enc.getPermissions()));
	}
}
