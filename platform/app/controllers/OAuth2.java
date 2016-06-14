package controllers;

import java.util.Map;

import models.HPUser;
import models.Member;
import models.MobileAppInstance;
import models.Plugin;
import models.User;
import models.enums.ConsentStatus;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.MobileAppToken;
import utils.auth.OAuthCodeToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonValidation;
import actions.APICall;
import actions.MobileCall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OAuth2 extends Controller {

	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@MobileCall
	public static Result checkPreflight() {		
		return ok();
	}
 	
	
	
	
		
	
	private static boolean verifyAppInstance(MobileAppInstance appInstance, ObjectId ownerId, ObjectId applicationId) {
		if (appInstance == null) return false;
        if (!appInstance.owner.equals(ownerId)) return false;
        if (!appInstance.applicationId.equals(applicationId)) return false;
        
        if (appInstance.status.equals(ConsentStatus.EXPIRED) || appInstance.status.equals(ConsentStatus.REJECTED)) return false;
        
        return true;
	}
	
	
	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result login() throws AppException {
				
        JsonNode json = request().body().asJson();		
        JsonValidation.validate(json, "appname", "username", "password", "state", "redirectUri");
						
        String name = JsonValidation.getString(json, "appname");
		String state = JsonValidation.getString(json, "state");
		String redirectUri = JsonValidation.getString(json, "redirectUri"); 
					
	    // Validate Mobile App	
		Plugin app = Plugin.getByFilename(name, Sets.create("type", "name", "redirectUri"));
		if (app == null) return badRequest("Unknown app");		
		if (!app.type.equals("mobile")) return internalServerError("Wrong app type");
		if (!redirectUri.equals(app.redirectUri)) return internalServerError("Wrong redirect uri");
		
		MobileAppInstance appInstance = null;		
		Map<String, Object> meta = null;
		
		
		String username = JsonValidation.getEMail(json, "username");
		String phrase = JsonValidation.getString(json, "password");
		UserRole role = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;
			
				
			User user = null;
			switch (role) {
			case MEMBER : user = Member.getByEmail(username, Sets.create("visualizations","tokens"));break;
			case PROVIDER : user = HPUser.getByEmail(username, Sets.create("visualizations","tokens"));break;
			}
			if (user == null) return badRequest("Unknown user or bad password");
			
			appInstance = MobileAppInstance.getByApplicationAndOwner(app._id, user._id, Sets.create("owner", "applicationId", "status", "passcode"));
			
			if (appInstance == null) {									
				appInstance = MobileAPI.installApp(null, app, user, phrase);				
	   		    meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			} else {
				if (appInstance.passcode != null && !User.authenticationValid(phrase, appInstance.passcode)) return badRequest("Wrong password.");
				if (!verifyAppInstance(appInstance, user._id, app._id)) return badRequest("Access denied");
				KeyManager.instance.unlock(appInstance._id, phrase);
				meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			}
					
		if (!phrase.equals(meta.get("phrase"))) return internalServerError("Internal error while validating consent");
		
		OAuthCodeToken tk = new OAuthCodeToken(appInstance._id, phrase, System.currentTimeMillis(), state);
					
		ObjectNode obj = Json.newObject();								
		obj.put("code", tk.encrypt());
		return ok(obj);
	}

	@BodyParser.Of(BodyParser.FormUrlEncoded.class)
	@MobileCall
	public static Result authenticate() throws AppException {
				
        Map<String, String[]> data = request().body().asFormUrlEncoded();
        ObjectId appInstanceId = null;
        MobileAppInstance appInstance = null;
        Map<String, Object> meta = null;
        String phrase = null;
        ObjectNode obj = Json.newObject();	
        
        if (!data.containsKey("grant_type")) throw new BadRequestException("error.internal", "Missing grant_type");
        
        String grant_type = data.get("grant_type")[0];
        if (grant_type.equals("refresh_token")) {
        	if (!data.containsKey("refresh_token")) throw new BadRequestException("error.internal", "Missing refresh_token");
        	String refresh_token = data.get("refresh_token")[0];
        	
        	MobileAppToken refreshToken = MobileAppToken.decrypt(refresh_token);
			appInstanceId = refreshToken.appInstanceId;
			
			appInstance = MobileAppInstance.getById(appInstanceId, Sets.create("owner", "applicationId", "status"));
			if (!verifyAppInstance(appInstance, refreshToken.ownerId, refreshToken.appId)) throw new BadRequestException("error.internal", "Bad refresh token.");                        
            
            phrase = refreshToken.phrase;
            KeyManager.instance.unlock(appInstance._id, phrase);	
            meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
                        
            if (refreshToken.created != ((Long) meta.get("created")).longValue()) {
            	return status(UNAUTHORIZED);
            }
        } else if (grant_type.equals("authorization_code")) {
        	if (!data.containsKey("redirect_uri")) throw new BadRequestException("error.internal", "Missing redirect_uri");
            if (!data.containsKey("client_id")) throw new BadRequestException("error.internal", "Missing client_id");
            if (!data.containsKey("code")) throw new BadRequestException("error.internal", "Missing code");
            
                            
            String code = data.get("code")[0];
            String redirect_uri = data.get("redirect_uri")[0];
            String client_id = data.get("client_id")[0];
    					
    		OAuthCodeToken tk = OAuthCodeToken.decrypt(code);
    				
    	    // Validate Mobile App	
    		Plugin app = Plugin.getByFilename(client_id, Sets.create("type", "name", "secret"));
    		if (app == null) return badRequest("Unknown app");		
    		if (!app.type.equals("mobile")) return internalServerError("Wrong app type");
    		
    		appInstance = MobileAppInstance.getById(tk.appInstanceId, Sets.create("owner", "applicationId", "status", "passcode"));
    		phrase = tk.passphrase;
    		obj.put("state", tk.state);
        } else return badRequest("Unknown grant_type");
               				
		if (appInstance == null) throw new NullPointerException();									
			
		if (appInstance.passcode != null && !User.authenticationValid(phrase, appInstance.passcode)) return badRequest("Wrong password.");
		//	if (!verifyAppInstance(appInstance, user._id, app._id)) return badRequest("Access denied");
		KeyManager.instance.unlock(appInstance._id, phrase);
		meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			
		//}
				
		if (!phrase.equals(meta.get("phrase"))) return internalServerError("Internal error while validating consent");
						
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis()); 
        MobileAppToken refresh = new MobileAppToken(appInstance.applicationId, appInstance._id, appInstance.owner, phrase, System.currentTimeMillis());
		
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(appInstance._id, appInstance._id, "_app", meta);
        
		// create encrypted authToken		
											
		obj.put("access_token", session.encrypt());
		obj.put("token_type", "Bearer");
		obj.put("scope", "XXX");
		
		//obj.put("expires_in", MobileAppSessionToken.);
		obj.put("patient", appInstance.owner.toString());
		obj.put("refresh_token", refresh.encrypt());
				
		response().setHeader("Cache-Control", "no-store");
		response().setHeader("Pragma", "no-cache."); 
		
		return ok(obj);
	}
}
