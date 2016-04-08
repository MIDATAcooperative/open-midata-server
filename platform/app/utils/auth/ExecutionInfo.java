package utils.auth;

import java.util.Map;

import models.Consent;
import models.Member;
import models.MobileAppInstance;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

import utils.access.RecordManager;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public class ExecutionInfo {

	public ObjectId executorId;
	
	public ObjectId ownerId;
	
	public ObjectId pluginId;
	
	public ObjectId targetAPS;
	
	public ObjectId recordId;
	
	public Space space;
	
	public static ExecutionInfo checkSpaceToken(String token) throws AppException {
		// decrypt authToken 
		SpaceToken authToken = SpaceToken.decrypt(token);
		if (authToken == null) {
			throw new BadRequestException("error.token", "Invalid authToken.");
		}
		
		ExecutionInfo result = new ExecutionInfo();
		result.executorId = authToken.executorId;
			
		if (authToken.recordId != null) {
			result.targetAPS = authToken.spaceId;
			result.recordId = authToken.recordId;			
			result.ownerId = authToken.userId;
		} else if (authToken.pluginId == null) {							
			Space space = Space.getByIdAndOwner(authToken.spaceId, authToken.userId, Sets.create("visualization", "app", "aps", "autoShare"));
			if (space == null) throw new BadRequestException("error.space.missing", "The current space does no longer exist.");
				
			result.pluginId = space.visualization;
			result.targetAPS = space._id;
			result.ownerId = authToken.userId;
			result.space = space;
			
			User targetUser = Member.getById(authToken.userId, Sets.create("myaps", "tokens"));
			if (targetUser == null) throw new BadRequestException("error.internal", "Invalid authToken.");
		} else {
			
			Consent consent = Consent.getByIdAndAuthorized(authToken.spaceId, authToken.userId, Sets.create("owner"));
			if (consent == null) throw new BadRequestException("error.consent.missing", "The current consent does no longer exist.");
			
			result.pluginId = authToken.pluginId;
			result.targetAPS = consent._id;
			result.ownerId = consent.owner;
		}
				
	   return result;	
		
	}
	
	public static ExecutionInfo checkMobileToken(String token) throws AppException {		
		MobileAppSessionToken authToken = MobileAppSessionToken.decrypt(token);
		if (authToken == null) {
			throw new BadRequestException("error.internal", "Invalid authToken.");
		}
					
		MobileAppInstance appInstance = MobileAppInstance.getById(authToken.appInstanceId, Sets.create("owner", "applicationId", "autoShare"));
        if (appInstance == null) throw new BadRequestException("error.internal", "Invalid authToken.");

        KeyManager.instance.unlock(appInstance._id, authToken.passphrase);
        
        ExecutionInfo result = new ExecutionInfo();
		result.executorId = appInstance._id;
        
		Map<String, Object> appobj = RecordManager.instance.getMeta(authToken.appInstanceId, authToken.appInstanceId, "_app").toMap();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			ObjectId alias = new ObjectId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("userkey");
			KeyManager.instance.unlock(appInstance.owner, alias, key);
			RecordManager.instance.clear();
			result.executorId = appInstance.owner;
		}
                                                
		result.ownerId = appInstance.owner;
		result.pluginId = appInstance.applicationId;
		result.targetAPS = appInstance._id;
		
        return result;						
		
	}
}
