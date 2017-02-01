package utils.auth;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

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
import utils.access.RecordManager;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public class ExecutionInfo {

	public MidataId executorId;
	
	public MidataId ownerId;
	
	public MidataId pluginId;
	
	public MidataId targetAPS;
	
	public MidataId recordId;
	
	public Space space;
	
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
		} else if (authToken.pluginId == null) {							
			Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.userId, Sets.create("visualization", "app", "aps", "autoShare"));
			if (space == null) throw new BadRequestException("error.unknown.space", "The current space does no longer exist.");
				
			result.pluginId = space.visualization;
			result.targetAPS = space._id;
			result.ownerId = authToken.userId;
			result.space = space;
			
			User targetUser = Member.getById(authToken.userId, Sets.create("myaps", "tokens"));
			if (targetUser == null) throw new BadRequestException("error.internal", "Invalid authToken.");
		} else {
			
			Consent consent = Consent.getByIdAndAuthorized(authToken.spaceId, authToken.userId, Sets.create("owner"));
			if (consent == null) throw new BadRequestException("error.unknown.consent", "The current consent does no longer exist.");
			
			result.pluginId = authToken.pluginId;
			result.targetAPS = consent._id;
			result.ownerId = consent.owner;
		}
	   AccessLog.logEnd("end check 'space' type session token");
	   return result;	
		
	}
	
	public static ExecutionInfo checkMobileToken(String token, boolean allowInactive) throws AppException {		
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(token);
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
			
		return checkMobileToken(authToken, allowInactive);		
	}
	
	public static ExecutionInfo checkMobileToken(MobileAppSessionToken authToken, boolean allowInactive) throws AppException {		
						
		AccessLog.logBegin("begin check 'mobile' type session token");
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare"));
        if (appInstance == null) throw new BadRequestException("error.invalid.token", "Invalid authToken.");

        if (!allowInactive && !appInstance.status.equals(ConsentStatus.ACTIVE)) throw new BadRequestException("error.noconsent", "Consent needs to be confirmed before creating records!");

        KeyManager.instance.login(60000l);
        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
        
        ExecutionInfo result = new ExecutionInfo();
		result.executorId = appInstance._id;
        
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
		AccessLog.logEnd("end check 'mobile' type session token");
		
        return result;						
		
	}
}
