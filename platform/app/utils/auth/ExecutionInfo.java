package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Circles;
import controllers.OAuth2;
import models.Consent;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Space;
import models.User;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.AccessContext;
import utils.access.RecordManager;
import utils.access.SpaceAccessContext;
import utils.collections.CMaps;
import utils.collections.RequestCache;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class ExecutionInfo {

	public MidataId executorId;
	
	public MidataId ownerId;
	
	public MidataId pluginId;
	
	public MidataId targetAPS;
	
	public MidataId recordId;
	
	public UserRole role;
	
	public Space space;
	
	public RequestCache cache = new RequestCache();
	
	public AccessContext context = null;
	
	public String overrideBaseUrl = null;
	
	public ExecutionInfo() {}
	
	public ExecutionInfo(MidataId executor, UserRole role) throws InternalServerException {
		this.executorId = executor;
		this.ownerId = executor;
		this.targetAPS = executor;
		this.pluginId = RuntimeConstants.instance.portalPlugin;
		this.context = RecordManager.instance.createContextFromAccount(executor);
		this.role = role;
	}
	
	public static ExecutionInfo checkToken(Request request, String token, boolean allowInactive) throws AppException {
		String plaintext = TokenCrypto.decryptToken(token);
		if (plaintext == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");	
		JsonNode json = Json.parse(plaintext);
		if (json == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		
		if (json.has("instanceId")) {
			return checkSpaceToken(SpaceToken.decrypt(request, json));
		} else {
			return checkMobileToken(MobileAppSessionToken.decrypt(json), allowInactive);
		}
	}
	
	public static ExecutionInfo checkSpaceToken(Request request, String token) throws AppException {
		// decrypt authToken 
		SpaceToken authToken = SpaceToken.decrypt(request, token);
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		
		return checkSpaceToken(authToken);
		
	}
	
	public static ExecutionInfo checkSpaceToken(SpaceToken authToken) throws AppException {	
		if (authToken == null) throw new BadRequestException("error.notauthorized.account", "You are not authorized.");
		AccessLog.logBegin("begin check 'space' type session token");
		KeyManager.instance.continueSession(authToken.handle, authToken.executorId);
		ExecutionInfo result = new ExecutionInfo();
		result.executorId = authToken.executorId;
		result.role = authToken.role;	
		if (authToken.recordId != null) {
			result.targetAPS = authToken.spaceId;
			result.recordId = authToken.recordId;			
			result.ownerId = authToken.userId;
			
			Consent consent = Circles.getConsentById(authToken.userId, authToken.spaceId, Consent.ALL);
			if (consent != null) {
			  result.context = RecordManager.instance.createContextFromConsent(authToken.executorId, consent);
			} else {
			  result.context = RecordManager.instance.createContextFromAccount(authToken.executorId);
			}
			
		} else if (authToken.pluginId == null) {							
			Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.autoimport ? authToken.userId : authToken.executorId, Sets.create("visualization", "app", "aps", "autoShare", "sharingQuery", "writes", "owner"));
			if (space == null) throw new BadRequestException("error.unknown.space", "The current space does no longer exist.");
				
			result.pluginId = space.visualization;
			result.targetAPS = space._id;
			result.ownerId = authToken.userId;
			result.space = space;
					
			User targetUser = Member.getById(authToken.userId,Sets.create("myaps", "tokens"));
			if (targetUser == null) {
				Consent c = Circles.getConsentById(authToken.executorId, authToken.userId, Sets.create("owner", "authorized"));
				if (c != null) {
					result.ownerId = c.owner;
				} else throw new BadRequestException("error.internal", "Invalid authToken.");
			}
			
			result.context = RecordManager.instance.createContextFromSpace(result.executorId, space, result.ownerId);
			
			if (result.role.equals(UserRole.PROVIDER) && !result.ownerId.equals(result.executorId)) {
				result.context = ((SpaceAccessContext) result.context).withRestrictions(CMaps.map("consent-type-exclude", "HEALTHCARE"));
			}
			
		} else if (authToken.autoimport) {
			MobileAppInstance appInstance = MobileAppInstance.getById(authToken.spaceId, Sets.create("owner", "applicationId", "autoShare", "status", "sharingQuery", "writes"));
			if (appInstance == null) OAuth2.invalidToken(); 

		    if (!appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");
		        
		        
			if (appInstance.sharingQuery == null) {
				appInstance.sharingQuery = RecordManager.instance.getMeta(authToken.executorId, authToken.spaceId, "_query").toMap();
			}
									                                               
			result.ownerId = appInstance.owner;
			result.pluginId = appInstance.applicationId;
			result.targetAPS = appInstance._id;
			result.context = RecordManager.instance.createContextFromApp(result.executorId, appInstance);
			
		} 
	   AccessLog.log("using as context:"+result.context.toString());
	   AccessLog.logEnd("end check 'space' type session token");
	   return result;	
		
	}
	
	public static ExecutionInfo checkMobileToken(String token, boolean allowInactive) throws AppException {		
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(token);
		if (authToken == null) OAuth2.invalidToken(); 
				
		return checkMobileToken(authToken, allowInactive);		
	}
	
	public static ExecutionInfo checkMobileToken(MobileAppSessionToken authToken, boolean allowInactive) throws AppException {		
		if (authToken == null) OAuth2.invalidToken();				
		
		AccessLog.logBegin("begin check 'mobile' type session token");
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare", "status", "sharingQuery", "writes"));
        if (appInstance == null) OAuth2.invalidToken(); 
        if (appInstance.status.equals(ConsentStatus.REJECTED) || appInstance.status.equals(ConsentStatus.EXPIRED) || appInstance.status.equals(ConsentStatus.FROZEN)) OAuth2.invalidToken();
        
        if (!allowInactive && !appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");
        
        
        KeyManager.instance.login(60000l, false);
        if (KeyManager.instance.unlock(appInstance._id, authToken.aeskey) == KeyManager.KEYPROTECTION_FAIL) {
        	OAuth2.invalidToken(); 
        }
        
        ExecutionInfo result = new ExecutionInfo();
		result.executorId = appInstance._id;
		result.role = authToken.role;
        
		if (appInstance.sharingQuery == null) {
			appInstance.sharingQuery = RecordManager.instance.getMeta(authToken.appInstanceId, authToken.appInstanceId, "_query").toMap();
		}
		
		Map<String, Object> appobj = RecordManager.instance.getMeta(authToken.appInstanceId, authToken.appInstanceId, "_app").toMap();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			MidataId alias = new MidataId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("aliaskey");
			if (appobj.containsKey("targetAccount")) {
				result.executorId = MidataId.from(appobj.get("targetAccount").toString());
			} else result.executorId = appInstance.owner;
			KeyManager.instance.unlock(result.executorId, alias, key);			
			RecordManager.instance.clearCache();			
		} else {
			RecordManager.instance.setAccountOwner(appInstance._id, appInstance.owner);
		}
		
                                                
		result.ownerId = appInstance.owner;
		result.pluginId = appInstance.applicationId;
		result.targetAPS = appInstance._id;
		result.context = RecordManager.instance.createContextFromApp(result.executorId, appInstance);
		AccessLog.logEnd("end check 'mobile' type session token");
		
        return result;						
		
	}
}
