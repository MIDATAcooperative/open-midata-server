/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import models.enums.EntityType;
import models.enums.ResearcherRole;
import utils.access.Feature_UserGroups;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * Tools for project management
 */
public class ProjectTools {

    public static void addToUserGroup(AccessContext context, UserGroupMember self, ResearcherRole role, EntityType type, Set<MidataId> targetUserIds) throws AppException {
    	
    	context = context.forUserGroup(self);
    	    
        MidataId groupId = self.userGroup;
	
		
		for (MidataId targetUserId : targetUserIds) {
			UserGroupMember old = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
			if (old == null) {	
				addToUserGroup(context, role, groupId, type, targetUserId);
			} else {
								
				AuditManager.instance.addAuditEvent(AuditEventType.UPDATED_ROLE_IN_TEAM, null, context.getActor(), targetUserId, null, groupId);
				
				if (old.member.equals(self.member)) {
					int size = UserGroupMember.getAllActiveUserByGroup(self.userGroup).size();
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

    public static void addToUserGroup(AccessContext context, ResearcherRole role, MidataId groupId, EntityType type, MidataId targetUserId)
            throws AppException, AuthException, InternalServerException {
        AuditManager.instance.addAuditEvent(AuditEventType.ADDED_AS_TEAM_MEMBER, null, context.getActor(), targetUserId, null, groupId);
        
        UserGroupMember member = new UserGroupMember();
        member._id = new MidataId();
        member.member = targetUserId;
        member.userGroup = groupId;
        member.entityType = type;
        member.status = ConsentStatus.ACTIVE;
        member.startDate = new Date();
        member.role = role;
        																					
        Map<String, Object> accessData = new HashMap<String, Object>();
        accessData.put("aliaskey", KeyManager.instance.generateAlias(groupId, member._id));
        RecordManager.instance.createAnonymizedAPS(targetUserId, context.getAccessor(), member._id, false);
        RecordManager.instance.setMeta(context, member._id, "_usergroup", accessData);
        
        member.add();
    }
}