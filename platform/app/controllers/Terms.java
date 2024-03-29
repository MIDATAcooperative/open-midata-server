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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;

import actions.APICall;
import models.MidataId;
import models.TermsOfUse;
import models.User;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.PreLoginSecured;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class Terms extends APIController {
		
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result add(Request request) throws AppException {
		// validate json
		JsonNode json = request.body().asJson();
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		
		JsonValidation.validate(json, "title", "text", "name", "version", "language");		

		User user = User.getById(userId, Sets.create("email"));
		
		TermsOfUse terms = new TermsOfUse();
		
		terms._id = new MidataId();
		terms.createdAt = new Date();
		terms.creator = userId;
		terms.creatorLogin = user.email; 
		terms.language = JsonValidation.getString(json, "language");
		terms.name = JsonValidation.getString(json,  "name");
		terms.text = JsonValidation.getUnboundString(json, "text");
		terms.title = JsonValidation.getString(json, "title");
		terms.version = JsonValidation.getString(json, "version");

		boolean replace = JsonValidation.getBoolean(json, "replace");

		TermsOfUse result = TermsOfUse.getByNameVersionLanguage(terms.name, terms.version, terms.language);
		if (result != null) {
		    if (replace) {
				terms._id = result._id;
				terms.upsert();
				return ok();
			} else throw new BadRequestException("error.exists.terms", "Terms with this name,version and language already exist.");
		}
		terms.add();
		
		return ok();
   }
	
	@BodyParser.Of(BodyParser.Json.class)		
	@APICall
	public Result get(Request request) throws JsonValidationException, AppException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "name", "language");
		
		String name = JsonValidation.getString(json, "name");
		String version = JsonValidation.getStringOrNull(json, "version");
		String language = JsonValidation.getString(json, "language");
		Config config = InstanceConfig.getInstance().getConfig();
		if (version == null && name.startsWith("midata-")) {
			version = config.hasPath("versions."+name) ? config.getString("versions."+name) : "1.0";
		}
		
        TermsOfUse result = TermsOfUse.getByNameVersionLanguage(name, version, language);
		
        if (result == null) result = TermsOfUse.getByNameVersionLanguage(name, version, InstanceConfig.getInstance().getDefaultLanguage());
        
        if (result == null) throw new BadRequestException("error.missing.terms", "Requested terms not found.");
        
		return ok(Json.toJson(result)).as("application/json");
	}
			
	@APICall
	public Result currentTerms()  {
		
		ObjectNode obj = Json.newObject();	
        
		for (UserRole role : UserRole.values()) {
			ObjectNode terms = Json.newObject();
			terms.put("termsOfUse", InstanceConfig.getInstance().getTermsOfUse(role));
			terms.put("privacyPolicy", InstanceConfig.getInstance().getPrivacyPolicy(role));
			obj.set(role.toString().toLowerCase(), terms);
		}		
		return ok(obj).as("application/json");
	}
	
	public static void addAgreedToDefaultTerms(User user) throws AppException {
		String terms = InstanceConfig.getInstance().getTermsOfUse(user.role);
		String ppolicy = InstanceConfig.getInstance().getPrivacyPolicy(user.role);
				
		user.agreedToTerms(terms, user.initialApp, true);
		user.agreedToTerms(ppolicy, user.initialApp, true);		
	}
	
	
	@APICall
	@Security.Authenticated(PreLoginSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result agreedToTerms(Request request) throws AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request.body().asJson();
		JsonValidation.validate(json, "terms", "app");
		
		String terms = JsonValidation.getString(json, "terms");
				
		MidataId app = JsonValidation.getMidataId(json, "app");
		
		User user = User.getById(userId, Sets.create(User.FOR_LOGIN));
		if (user == null) throw new InternalServerException("error.internal", "Session user does not exist.");
		
		//if (terms.equals("midata-privacy-policy-*")) terms = InstanceConfig.getInstance().getPrivacyPolicy(user.role);
		//else if (terms.equals("midata-terms-of-use-*")) terms = InstanceConfig.getInstance().getTermsOfUse(user.role);
				
		user.agreedToTerms(terms, app, false);
		
		return OAuth2.loginHelper(request);	
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result search(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));			
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
				
        Set<TermsOfUse> result = TermsOfUse.getAll(properties, fields);
		                
		return ok(Json.toJson(result)).as("application/json");
	}

}
