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

import java.util.Map;

import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.MidataId;
import models.Space;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * function for reading and writing the portal configuration of each user
 *
 */
public class PortalConfig extends APIController {	
	
	/**
	 * retrieve portal configuration json
	 * @return json with portal configuration
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result getConfig(Request request) throws JsonValidationException, AppException {
	
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
	    Space config = Space.getByOwnerSpecialContext(userId, "portal", Sets.create("name"));
	    if (config == null) return ok();
	    
		BSONObject meta = RecordManager.instance.getMeta(context, config._id, "_config");		
		if (meta != null) return ok(Json.toJson(meta.toMap()));
		
		return ok();
	}
	
	/**
	 * write portal configuration for current user
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result setConfig(Request request) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);

		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "config");
		
		Space configspace = Space.getByOwnerSpecialContext(userId, "portal", Sets.create("name"));
		if (configspace == null) {			
			configspace = Spaces.add(userId, "portal", null, null, "portal", null);
		}
		
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		
		RecordManager.instance.setMeta(context, configspace._id, "_config", config);
						
		return ok();
	}
}
