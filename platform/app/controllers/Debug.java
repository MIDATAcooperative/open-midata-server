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
