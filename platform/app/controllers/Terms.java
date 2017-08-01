package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Developer;
import models.MidataId;
import models.NewsItem;
import models.TermsOfUse;
import models.User;
import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.auth.Rights;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class Terms extends APIController {
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public static Result add() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		MidataId userId = new MidataId(request().username());
		
		JsonValidation.validate(json, "title", "text", "name", "version", "language");		

		User user = User.getById(userId, Sets.create("email"));
		
		TermsOfUse terms = new TermsOfUse();
		
		terms._id = new MidataId();
		terms.createdAt = new Date();
		terms.creator = userId;
		terms.creatorLogin = user.email; 
		terms.language = JsonValidation.getString(json, "language");
		terms.name = JsonValidation.getString(json,  "name");
		terms.text = JsonValidation.getString(json, "text");
		terms.title = JsonValidation.getString(json, "title");
		terms.version = JsonValidation.getString(json, "version");

		terms.add();
		
		return ok();
   }
	
	@BodyParser.Of(BodyParser.Json.class)		
	@APICall
	public static Result get() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name", "language");
		
		String name = JsonValidation.getString(json, "name");
		String version = JsonValidation.getStringOrNull(json, "version");
		String language = JsonValidation.getString(json, "language");
		
		if (version == null && name.startsWith("midata-")) {
			version = Play.application().configuration().getString("versions."+name,"1.0");
		}
		
        TermsOfUse result = TermsOfUse.getByNameVersionLanguage(name, version, language);
		
        if (result == null) result = TermsOfUse.getByNameVersionLanguage(name, version, InstanceConfig.getInstance().getDefaultLanguage());
        
		return ok(Json.toJson(result));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public static Result search() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));			
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
				
        Set<TermsOfUse> result = TermsOfUse.getAll(properties, fields);
		                
		return ok(Json.toJson(result));
	}

}
