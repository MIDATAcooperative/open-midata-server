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
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Study;
import models.enums.UserRole;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Http.Request;
import utils.auth.Rights;
import utils.db.ObjectIdConversion;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;


/**
 * functions about studies.
 *
 */
public class Studies extends APIController {
	
	/**
	 * search for studies matching some criteria
	 * @return list of studies
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	@APICall
	
	@BodyParser.Of(BodyParser.Json.class)
	public Result search(Request request) throws JsonValidationException, InternalServerException, AuthException {
	   //MidataId user = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));	   
	   JsonNode json = request.body().asJson();
	   JsonValidation.validate(json, "properties", "fields");
							   		
	   Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
	   ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "createdBy", "studyKeywords");
	   Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
	   
	   Rights.chk("Studies.search", UserRole.MEMBER, properties, fields);	   	   
	   Set<Study> studies = Study.getAll(null, properties, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields)).as("application/json");
	}	
			
}
