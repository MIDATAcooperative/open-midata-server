package utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import models.MidataId;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ResearcherRole;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * Tools for project management
 */
public class ProjectTools {

    public static void addToUserGroup(MidataId executorId, UserGroupMember self, ResearcherRole role, Set<MidataId> targetUserIds) throws AppException {
        BSONObject meta = RecordManager.instance.getMeta(executorId, self._id, "_usergroup");
        byte[] key = (byte[]) meta.get("aliaskey");
        MidataId groupId = self.userGroup;
		KeyManager.instance.unlock(groupId, self._id, key);
		
		for (MidataId targetUserId : targetUserIds) {
			UserGroupMember old = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
			if (old == null) {	
				addToUserGroup(executorId, role, groupId, targetUserId);
			} else {
								
				AuditManager.instance.addAuditEvent(AuditEventType.UPDATED_ROLE_IN_TEAM, null, executorId, targetUserId, null, groupId);
				
				if (old.member.equals(self.member)) {
					int size = UserGroupMember.getAllActiveByGroup(self.userGroup).size();
					if (size > 1) throw new BadRequestException("error.notauthorized.action", "You may only change your rights as long as you are sole member.");
					
					if (!role.mayChangeTeam()) throw new BadRequestException("error.notauthorized.action", "You may not remove team management feature from yourself.");
				}
				
				old.status = ConsentStatus.ACTIVE;
				old.startDate = new Date();
				old.endDate = null;
				old.role = role;
				UserGroupMember.set(old._id, "status", old.status);
				UserGroupMember.set(old._id, "startDate", old.startDate);
				UserGroupMember.set(old._id, "endDate", old.endDate);
				UserGroupMember.set(old._id, "role", old.role);
			}
		}
				
    }

    public static void addToUserGroup(MidataId executorId, ResearcherRole role, MidataId groupId, MidataId targetUserId)
            throws AppException, AuthException, InternalServerException {
        AuditManager.instance.addAuditEvent(AuditEventType.ADDED_AS_TEAM_MEMBER, null, executorId, targetUserId, null, groupId);
        
        UserGroupMember member = new UserGroupMember();
        member._id = new MidataId();
        member.member = targetUserId;
        member.userGroup = groupId;
        member.status = ConsentStatus.ACTIVE;
        member.startDate = new Date();
        member.role = role;
        																					
        Map<String, Object> accessData = new HashMap<String, Object>();
        accessData.put("aliaskey", KeyManager.instance.generateAlias(groupId, member._id));
        RecordManager.instance.createAnonymizedAPS(targetUserId, executorId, member._id, false);
        RecordManager.instance.setMeta(executorId, member._id, "_usergroup", accessData);
        
        member.add();
    }
}