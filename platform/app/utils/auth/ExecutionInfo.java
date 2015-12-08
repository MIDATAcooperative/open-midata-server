package utils.auth;

import models.Consent;
import models.Member;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

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
		result.executorId = authToken.userId;
			
		if (authToken.recordId != null) {
			result.targetAPS = authToken.spaceId;
			result.recordId = authToken.recordId;
			result.executorId = authToken.userId;
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
}
