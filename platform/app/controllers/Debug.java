package controllers;

import models.Admin;
import models.Record;

import org.bson.types.ObjectId;

import actions.APICall;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Patient;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.EncryptedAPS;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.RecordToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.sandbox.Scripting;

/**
 * used for debugging. MUST be removed in productive system
 * @author alexander
 *
 */
public class Debug extends Controller {

	/**
	 * return APS content for debugging 
	 * @param id ID of APS
	 * @return
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result get(String id) throws JsonValidationException, AppException {
				
		ObjectId userId = new ObjectId(request().username());
		ObjectId apsId = id.equals("-") ? userId : new ObjectId(id);
		
		EncryptedAPS enc = new EncryptedAPS(apsId, userId);
								   			
		return ok(Json.toJson(enc.getPermissions()));
	}
		
	
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public static Result test() throws AppException {
		ObjectId userId = new ObjectId(request().username());
		Object res = Scripting.instance.eval(userId);
		return ok(res.toString());
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
