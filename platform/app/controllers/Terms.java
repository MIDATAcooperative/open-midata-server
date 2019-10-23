package controllers;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;

import actions.APICall;
import models.MidataId;
import models.TermsOfUse;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
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
	public Result add() throws AppException {
		// validate json
		JsonNode json = request().body().asJson();
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
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

		TermsOfUse result = TermsOfUse.getByNameVersionLanguage(terms.name, terms.version, terms.language);
		if (result != null) throw new BadRequestException("error.exists.terms", "Terms with this name,version and language already exist.");
		
		terms.add();
		
		return ok();
   }
	
	@BodyParser.Of(BodyParser.Json.class)		
	@APICall
	public Result get() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
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
        
		return ok(Json.toJson(result));
	}
	
	public static void addAgreedToDefaultTerms(User user) throws AppException {
		String terms = InstanceConfig.getInstance().getTermsOfUse(user.role);
		String ppolicy = InstanceConfig.getInstance().getPrivacyPolicy(user.role);
				
		user.agreedToTerms(terms, user.initialApp);
		user.agreedToTerms(ppolicy, user.initialApp);		
	}
	
	
	@APICall
	@Security.Authenticated(PreLoginSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result agreedToTerms() throws AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		JsonNode json = request().body().asJson();
		JsonValidation.validate(json, "terms", "app");
		
		String terms = JsonValidation.getString(json, "terms");
				
		MidataId app = JsonValidation.getMidataId(json, "app");
		
		User user = User.getById(userId, Sets.create(User.FOR_LOGIN));
		if (user == null) throw new InternalServerException("error.internal", "Session user does not exist.");
		
		if (terms.equals("midata-privacy-policy")) terms = InstanceConfig.getInstance().getPrivacyPolicy(user.role);
		else if (terms.equals("midata-terms-of-use")) terms = InstanceConfig.getInstance().getTermsOfUse(user.role);
		
		
		user.agreedToTerms(terms, app);
		
		return OAuth2.loginHelper();	
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AnyRoleSecured.class)
	@APICall
	public Result search() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
        JsonValidation.validate(json, "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));			
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
				
        Set<TermsOfUse> result = TermsOfUse.getAll(properties, fields);
		                
		return ok(Json.toJson(result));
	}

}
