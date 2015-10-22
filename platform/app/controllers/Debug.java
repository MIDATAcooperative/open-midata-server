package controllers;

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
import utils.auth.AnyRoleSecured;
import utils.auth.RecordToken;
import utils.exceptions.AppException;
import utils.exceptions.ModelException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * used for debugging. MUST be removed in productive system
 * @author alexander
 *
 */
public class Debug extends Controller {

	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get(String id) throws JsonValidationException, AppException {
				
		ObjectId userId = new ObjectId(request().username());
		ObjectId apsId = id.equals("-") ? userId : new ObjectId(id);
		
		EncryptedAPS enc = new EncryptedAPS(apsId, userId);
								   			
		return ok(Json.toJson(enc.getPermissions()));
	}
}
