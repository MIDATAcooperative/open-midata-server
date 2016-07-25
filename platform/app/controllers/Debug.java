package controllers;

import models.Admin;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.EncryptedAPS;
import utils.auth.AnyRoleSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;
import actions.APICall;

/**
 * used for debugging. Reading of APS content is not allowed on productive system.
 * @author alexander
 *
 */
public class Debug extends Controller {

	/**
	 * return APS content for debugging on a non productive system
	 * @param id ID of APS
	 * @return
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get(String id) throws JsonValidationException, AppException {
				
		if (Play.application().configuration().getBoolean("demoserver", false)) {
		
			ObjectId userId = new ObjectId(request().username());
			ObjectId apsId = id.equals("-") ? userId : new ObjectId(id);
			
			EncryptedAPS enc = new EncryptedAPS(apsId, userId);
									   			
			return ok(Json.toJson(enc.getPermissions()));
		
		} else return ok();
		
	}
				
	
	/**
	 * Do a database access for testing
	 * @return
	 * @throws AppException
	 */
	@APICall	
	public static Result ping() throws AppException {
	  if (Admin.getByEmail("autorun-service", Sets.create("_id")) == null) throw new InternalServerException("error.db", "Database error");
	  return ok();
	}
	
}
