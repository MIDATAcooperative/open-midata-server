package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Circles;
import controllers.MobileAPI;
import models.Consent;
import models.Member;
import models.MidataId;
import models.MobileAppInstance;
import models.Space;
import models.User;
import models.enums.ConsentStatus;
import play.libs.Json;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.AccessContext;
import utils.access.RecordManager;
import utils.collections.RequestCache;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class ExecutionInfo {

	public MidataId executorId;
	
	public MidataId ownerId;
	
	public MidataId pluginId;
	
	public MidataId targetAPS;
	
	public MidataId recordId;
	
	public Space space;
	
	public RequestCache cache = new RequestCache();
	
	public AccessContext context = null;
	
	public ExecutionInfo() {}
	
	public ExecutionInfo(MidataId executor) throws InternalServerException {
		this.executorId = executor;
		this.ownerId = executor;
		this.targetAPS = executor;
		this.pluginId = RuntimeConstants.instance.portalPlugin;
		this.context = RecordManager.instance.createContextFromAccount(executor);
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
		AccessLog.logBegin("begin check 'space' type session token");
		KeyManager.instance.continueSession(authToken.handle);
		ExecutionInfo result = new ExecutionInfo();
		result.executorId = authToken.executorId;
			
		if (authToken.recordId != null) {
			result.targetAPS = authToken.spaceId;
			result.recordId = authToken.recordId;			
			result.ownerId = authToken.userId;
			result.context = RecordManager.instance.createContextFromAccount(authToken.executorId);
			
		} else if (authToken.pluginId == null) {							
			Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.autoimport ? authToken.userId : authToken.executorId, Sets.create("visualization", "app", "aps", "autoShare", "sharingQuery", "writes"));
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
			
		} else {
			
			Consent consent = Consent.getByIdAndAuthorized(authToken.spaceId, authToken.userId, Sets.create("owner"));
			if (consent == null) throw new BadRequestException("error.unknown.consent", "The current consent does no longer exist.");
			
			result.pluginId = authToken.pluginId;
			result.targetAPS = consent._id;
			result.ownerId = consent.owner;
			result.context = RecordManager.instance.createContextFromConsent(result.executorId, consent);
		}
	   AccessLog.logEnd("end check 'space' type session token");
	   return result;	
		
	}
	
	public static ExecutionInfo checkMobileToken(String token, boolean allowInactive) throws AppException {		
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(token);
		if (authToken == null) MobileAPI.invalidToken(); 
				
		return checkMobileToken(authToken, allowInactive);		
	}
	
	public static ExecutionInfo checkMobileToken(MobileAppSessionToken authToken, boolean allowInactive) throws AppException {		
		if (authToken == null) MobileAPI.invalidToken();				
		
		AccessLog.logBegin("begin check 'mobile' type session token");
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare", "status", "sharingQuery", "writes"));
        if (appInstance == null) MobileAPI.invalidToken(); 

        if (!allowInactive && !appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");

        KeyManager.instance.login(60000l);
        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
        
        ExecutionInfo result = new ExecutionInfo();
		result.executorId = appInstance._id;
        
		if (appInstance.sharingQuery == null) {
			appInstance.sharingQuery = RecordManager.instance.getMeta(authToken.appInstanceId, authToken.appInstanceId, "_query").toMap();
		}
		
		Map<String, Object> appobj = RecordManager.instance.getMeta(authToken.appInstanceId, authToken.appInstanceId, "_app").toMap();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			MidataId alias = new MidataId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("aliaskey");
			KeyManager.instance.unlock(appInstance.owner, alias, key);
			RecordManager.instance.clearCache();
			result.executorId = appInstance.owner;
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
