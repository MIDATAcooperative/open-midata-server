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
import models.Study;
import models.SubProjectGroupMember;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.StudyValidationStatus;
import utils.access.Feature_UserGroups;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * Tools for project management
 */
public class ProjectTools {

    public static void addOrOverwriteToUserGroup(AccessContext context, UserGroupMember self, ResearcherRole role, EntityType type, Set<MidataId> targetUserIds, Map<String, String> projectGroupMapping) throws AppException {
    	MidataId accessor = context.getAccessor();
    	context = context.forUserGroup(self.userGroup, type.getChangePermission());
    	    
        MidataId groupId = self.userGroup;
			
		for (MidataId targetUserId : targetUserIds) {
			UserGroupMember old = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
			if (old == null) {	
				addNewToUserGroup(context, role, groupId, type, targetUserId, projectGroupMapping);
			} else {
								
				
				if (old.member.equals(self.member)) {
					int size = UserGroupMember.getAllActiveUserByGroup(self.userGroup).size();
					if (size > 1) throw new BadRequestException("error.notauthorized.action", "You may only change your rights as long as you are sole member.");
					
					if (!role.mayChangeTeam() && self.member.equals(accessor)) throw new BadRequestException("error.notauthorized.action", "You may not remove team management feature from yourself.");
				    Study study = Study.getById(old.userGroup, Study.ALL);
				    if (study != null && study.validationStatus != StudyValidationStatus.DRAFT && study.validationStatus != StudyValidationStatus.PATCH) throw new BadRequestException("error.notauthorized.action", "You may not update your own role after project was started.");
				}
				
				updateExistingGroup(context, old, role, projectGroupMapping);				
			}
		}
				
    }
    
    private static void updateExistingGroup(AccessContext context, UserGroupMember old, ResearcherRole role, Map<String, String> projectGroupMapping) throws AppException {
        if (!old.status.equals(ConsentStatus.ACTIVE) || !old.getRole().equals(role)) {
            AuditManager.instance.addAuditEvent(AuditEventType.UPDATED_ROLE_IN_TEAM, context, null, context.getActor(), old.member, null, old.userGroup);            
        }
        old.status = ConsentStatus.ACTIVE;
        old.startDate = new Date();
        old.endDate = null;
        old.role = role;
        UserGroupMember.set(old._id, "status", old.status);
        UserGroupMember.set(old._id, "startDate", old.startDate);
        UserGroupMember.set(old._id, "endDate", old.endDate);
        UserGroupMember.set(old._id, "role", old.role);
        if (projectGroupMapping != null) {
            UserGroupMember.set(old._id, "projectGroupMapping", projectGroupMapping);
        }  
        UserGroupMember.ensureUnique(old);
        
    }

    public static void addOrOverwriteToUserGroup(AccessContext context, ResearcherRole role, MidataId groupId, EntityType type, MidataId targetUserId)
            throws AppException, AuthException, InternalServerException {
        UserGroupMember old = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
        if (old == null) {
          addNewToUserGroup(context, role, groupId, type, targetUserId, null);
        } else {            
          updateExistingGroup(context, old, role, null); 
        }
    }
    
    public static UserGroupMember addOrMergeToUserGroup(AccessContext context, ResearcherRole role, MidataId groupId, EntityType type, MidataId targetUserId)
            throws AppException, AuthException, InternalServerException {
        UserGroupMember old = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
        if (old == null) {
            return addNewToUserGroup(context, role, groupId, type, targetUserId, null);
        } else {
            if (old.status==ConsentStatus.ACTIVE) {
              old.getRole().merge(role);
              role = old.getRole();
            }
            updateExistingGroup(context, old, role, null);
            return old;
        }
    }
    
    private static UserGroupMember addNewToUserGroup(AccessContext context, ResearcherRole role, MidataId groupId, EntityType type, MidataId targetUserId, Map<String, String> projectGroupMapping)
            throws AppException, AuthException, InternalServerException {
        AuditManager.instance.addAuditEvent(AuditEventType.ADDED_AS_TEAM_MEMBER, context, null, context.getActor(), targetUserId, null, groupId);
        AccessLog.log("add user to group group="+groupId+" user="+targetUserId+" type="+type);
        UserGroup ug = context.getRequestCache().getUserGroupById(groupId);
        if (ug == null) throw new InternalServerException("error.internal", "UserGroup not found");
        
        UserGroupMember member = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
        if (member != null) throw new InternalServerException("error.internal", "Already existing");
        
        if (projectGroupMapping != null) {
           member = new SubProjectGroupMember(projectGroupMapping);
        } else {
           member = new UserGroupMember();
        }
        member._id = new MidataId();
      
        member.member = targetUserId;
        member.userGroup = groupId;
        member.entityType = type;
        member.status = ConsentStatus.ACTIVE;
        member.startDate = new Date();
        member.role = role;
        if (ug.protection) {
        	member.confirmedUntil = new Date(System.currentTimeMillis()+1000l*10l);
        	member.confirmedBy = context.getActor();
        }
        
        Map<String, Object> accessData = new HashMap<String, Object>();
        accessData.put("aliaskey", KeyManager.instance.generateAlias(groupId, member._id));
        RecordManager.instance.createAnonymizedAPS(context.getCache(), targetUserId, context.getAccessor(), member._id, false);
        RecordManager.instance.setMeta(context, member._id, "_usergroup", accessData);
        
        member.add();
        
        return member;
    }
}