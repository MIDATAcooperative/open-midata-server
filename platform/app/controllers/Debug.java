package controllers;

import actions.APICall;
import models.Admin;
import models.MidataId;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.EncryptedAPS;
import utils.auth.AnyRoleSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;

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
	public Result get(String id) throws JsonValidationException, AppException {
				
		if (InstanceConfig.getInstance().getInstanceType().getDebugFunctionsAvailable()) {
		
			MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
			MidataId apsId = id.equals("-") ? userId : new MidataId(id);
			
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
	public Result ping() throws AppException {
	  if (Admin.getByEmail(RuntimeConstants.BACKEND_SERVICE, Sets.create("_id")) == null) throw new InternalServerException("error.db", "Database error");
	  return ok();
	}
	
}
