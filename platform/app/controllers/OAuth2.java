package controllers;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import actions.MobileCall;
import models.HPUser;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.User;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.MobileAppToken;
import utils.auth.OAuthCodeToken;
import utils.auth.TokenCrypto;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation;

public class OAuth2 extends Controller {

	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@MobileCall
	public static Result checkPreflight() {		
		return ok();
	}
 	
	
	
	
		
	
	private static boolean verifyAppInstance(MobileAppInstance appInstance, MidataId ownerId, MidataId applicationId) {
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
        JsonValidation.validate(json, "appname", "username", "password", "device", "state", "redirectUri");
						
        String name = JsonValidation.getString(json, "appname");
		String state = JsonValidation.getString(json, "state");
		String redirectUri = JsonValidation.getString(json, "redirectUri"); 
		String device = JsonValidation.getString(json, "device");
		
		String code_challenge = JsonValidation.getStringOrNull(json, "code_challenge");
	    String code_challenge_method = JsonValidation.getStringOrNull(json, "code_challenge_method");
	   					
	    // Validate Mobile App	
		Plugin app = Plugin.getByFilename(name, Sets.create("type", "name", "redirectUri"));
		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
		if (!redirectUri.equals(app.redirectUri)) throw new InternalServerException("error.internal", "Wrong redirect uri");
		
		MobileAppInstance appInstance = null;		
		Map<String, Object> meta = null;
		
		
		String username = JsonValidation.getEMail(json, "username");
		String password = JsonValidation.getString(json, "password");
		UserRole role = json.has("role") ? JsonValidation.getEnum(json, "role", UserRole.class) : UserRole.MEMBER;
		String phrase = device;
					
			User user = null;
			switch (role) {
			case MEMBER : user = Member.getByEmail(username, Sets.create("visualizations","password"));break;
			case PROVIDER : user = HPUser.getByEmail(username, Sets.create("visualizations","password"));break;
			}
			if (user == null) throw new BadRequestException("error.invalid.credentials", "Unknown user or bad password");
			if (!Member.authenticationValid(password, user.password)) {
				throw new BadRequestException("error.invalid.credentials",  "Unknown user or bad password");
			}
			
			appInstance = MobileAPI.getAppInstance(phrase, app._id, user._id, Sets.create("owner", "applicationId", "status", "passcode"));
			KeyManager.instance.login(60000l);
			
			if (appInstance == null) {		
				boolean autoConfirm = KeyManager.instance.unlock(user._id, null) == KeyManager.KEYPROTECTION_NONE;
				MidataId executor = autoConfirm ? user._id : null;
				appInstance = MobileAPI.installApp(executor, app._id, user, phrase, autoConfirm);				
				if (executor == null) executor = appInstance._id;
	   		    meta = RecordManager.instance.getMeta(executor, appInstance._id, "_app").toMap();
			} else {				
				if (!verifyAppInstance(appInstance, user._id, app._id)) throw new BadRequestException("error.expired.token", "Access denied");
				KeyManager.instance.unlock(appInstance._id, phrase);
				meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();				
			}
					
		if (!phrase.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
		
		OAuthCodeToken tk = new OAuthCodeToken(appInstance._id, phrase, System.currentTimeMillis(), state, code_challenge, code_challenge_method);
									
		ObjectNode obj = Json.newObject();								
		obj.put("code", tk.encrypt());
		obj.put("status", appInstance.status.toString());
		return ok(obj);
	}

	@BodyParser.Of(BodyParser.FormUrlEncoded.class)
	@MobileCall
	public static Result authenticate() throws AppException {
				
        Map<String, String[]> data = request().body().asFormUrlEncoded();
        MidataId appInstanceId = null;
        MobileAppInstance appInstance = null;
        Map<String, Object> meta = null;
        String phrase = null;
        ObjectNode obj = Json.newObject();	
        
        KeyManager.instance.login(60000l);
        
        if (!data.containsKey("grant_type")) throw new BadRequestException("error.internal", "Missing grant_type");
        
        String grant_type = data.get("grant_type")[0];
        if (grant_type.equals("refresh_token")) {
        	if (!data.containsKey("refresh_token")) throw new BadRequestException("error.internal", "Missing refresh_token");
        	String refresh_token = data.get("refresh_token")[0];
        	
        	MobileAppToken refreshToken = MobileAppToken.decrypt(refresh_token);
        	if (refreshToken.created + MobileAPI.DEFAULT_REFRESHTOKEN_EXPIRATION_TIME < System.currentTimeMillis()) return MobileAPI.invalidToken();
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
    		AccessLog.log("cs:"+tk.codeChallenge);
    		AccessLog.log("csm:"+tk.codeChallengeMethod);
    		
    		if (tk.codeChallenge != null) {
    			String csa[] = data.get("code_verifier");
    			String csm = csa!=null && csa.length>0 ? csa[0] : null;
    			if (csm == null) throw new BadRequestException("error.internal", "invalid_grant");
    			
    			if (tk.codeChallengeMethod == null || tk.codeChallengeMethod.equals("plain")) {
    			  if (!csm.equals(tk.codeChallenge)) throw new BadRequestException("error.internal", "invalid_grant");
    			} else if (tk.codeChallengeMethod.equals("S256")) {
    			   if (!TokenCrypto.sha256ThenBase64(csm).equals(tk.codeChallenge)) throw new BadRequestException("error.internal", "invalid_grant");    			  
    			} else throw new BadRequestException("error.internal", "invalid_grant");
    		}
    				
    	    // Validate Mobile App	
    		Plugin app = Plugin.getByFilename(client_id, Sets.create("type", "name", "secret"));
    		if (app == null) throw new BadRequestException("error.unknown.app", "Unknown app");		
    		if (!app.type.equals("mobile")) throw new InternalServerException("error.internal", "Wrong app type");
    		
    		appInstance = MobileAppInstance.getById(tk.appInstanceId, Sets.create("owner", "applicationId", "status", "passcode"));
    		phrase = tk.passphrase;
    		obj.put("state", tk.state);
        } else throw new BadRequestException("error.internal", "Unknown grant_type");
               				
		if (appInstance == null) throw new NullPointerException();									
			
		if (appInstance.passcode != null && !User.authenticationValid(phrase, appInstance.passcode)) throw new BadRequestException("error.invalid.credentials", "Wrong password.");
		//	if (!verifyAppInstance(appInstance, user._id, app._id)) return badRequest("Access denied");
		
		KeyManager.instance.unlock(appInstance._id, phrase);
		meta = RecordManager.instance.getMeta(appInstance._id, appInstance._id, "_app").toMap();
			
		//}
				
		if (!phrase.equals(meta.get("phrase"))) throw new InternalServerException("error.internal", "Internal error while validating consent");
						
		MobileAppSessionToken session = new MobileAppSessionToken(appInstance._id, phrase, System.currentTimeMillis() + MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME); 
        MobileAppToken refresh = new MobileAppToken(appInstance.applicationId, appInstance._id, appInstance.owner, phrase, System.currentTimeMillis());
		
        meta.put("created", refresh.created);
        RecordManager.instance.setMeta(appInstance._id, appInstance._id, "_app", meta);
        
		// create encrypted authToken		
											
		obj.put("access_token", session.encrypt());
		obj.put("token_type", "Bearer");
		obj.put("scope", "user/*.*");
		
		obj.put("expires_in", MobileAPI.DEFAULT_ACCESSTOKEN_EXPIRATION_TIME / 1000l);
		obj.put("patient", appInstance.owner.toString());
		obj.put("refresh_token", refresh.encrypt());
				
		response().setHeader("Cache-Control", "no-store");
		response().setHeader("Pragma", "no-cache"); 
		
		return ok(obj);
	}
}
