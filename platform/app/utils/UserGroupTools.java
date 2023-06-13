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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IIdType;

import models.HealthcareProvider;
import models.MidataId;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.UserGroupType;
import models.enums.UserStatus;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.UserGroupAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.GroupResourceProvider;
import utils.fhir.OrganizationResourceProvider;
import utils.json.JsonValidation;

/**
 * Tools for working with Midata UserGroups
 *
 */
public class UserGroupTools {

	public static UserGroup createUserGroup(AccessContext context, UserGroupType type, MidataId targetId, String name) throws AppException {
        UserGroup userGroup = new UserGroup();
		
		userGroup.name = name;
		
		userGroup.type = type;
		userGroup.status = UserStatus.ACTIVE;
		userGroup.creator = context.getActor();
		
		userGroup._id = targetId;
		userGroup.nameLC = userGroup.name.toLowerCase();	
		userGroup.keywordsLC = new HashSet<String>();
		userGroup.registeredAt = new Date();
		
		userGroup.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(userGroup._id, null);
				
		if (type == UserGroupType.CARETEAM) GroupResourceProvider.updateMidataUserGroup(userGroup);
		else userGroup.fhirGroup = null;
		
		userGroup.add();
		
		return userGroup;
	}
	
	public static UserGroupMember createUserGroupMember(AccessContext context, ResearcherRole role, MidataId userGroupId) throws AppException {
		return createUserGroupMember(context, context.getAccessor(), context.getAccessorEntityType(), role, userGroupId);					
	}
	
	public static UserGroupMember createUserGroupMember(AccessContext context, MidataId memberId, EntityType entityType, ResearcherRole role, MidataId userGroupId) throws AppException {
		UserGroupMember member = new UserGroupMember();
		member._id = new MidataId();
		member.member = memberId;
		member.entityType = entityType;
		//if (member.entityType == EntityType.USERGROUP) throw new InternalServerException("error.internal", "Cannot create group membership from within usergroup.");
		member.userGroup = userGroupId;
		member.status = ConsentStatus.ACTIVE;
		member.startDate = new Date();
		member.role = role;
																				
		Map<String, Object> accessData = new HashMap<String, Object>();
		accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroupId, member._id));
		RecordManager.instance.createPrivateAPS(context.getCache(), memberId, member._id);
		RecordManager.instance.setMeta(context, member._id, "_usergroup", accessData);
						
		member.add();
				
		return member;		
		
	}
	
	public static HealthcareProvider createOrUpdateOrganizationUserGroup(AccessContext context, MidataId organizationId, String name, String description, MidataId parent, boolean addAccessor) throws AppException {
		if (parent != null && !accessorIsMemberOfGroup(context, parent, Permission.SETUP)) throw new BadRequestException("error.notauthorized.action", "You are not authorized to manage parent organization.");
		AccessLog.logBegin("begin createOrUpdateOrganizationUserGroup org="+organizationId.toString()+" name="+name+" addAccessor="+addAccessor);
		try {
		MidataId oldParent = null;
		
		UserGroup existing = UserGroup.getById(organizationId, UserGroup.ALL);
		if (existing != null) {
			if (name != null && !existing.name.equals(name)) {
				existing.name = name;
				UserGroup.set(existing._id, "name", existing.name);
			} 
		}
				
		HealthcareProvider provider = HealthcareProvider.getById(organizationId, HealthcareProvider.ALL);
		if (provider == null) {
			if (name != null) {
				provider = new HealthcareProvider();
				provider._id = organizationId;
				provider.name = name;
				provider.description = description;
				provider.parent = parent;
				HealthcareProvider.add(provider);				
			} else throw new InternalServerException("error.internal", "Organization not found");
		} else {
			oldParent = provider.parent;
			provider.name = name;
			provider.description = description;
			provider.parent = parent;
			provider.setMultiple(Sets.create("name", "description", "parent"));
			if (oldParent != null && oldParent.equals(parent)) {
				// No change
				oldParent = parent = null;
			}
		}
		
		Set<User> members = User.getAllUser(CMaps.map("provider", organizationId), User.ALL_USER);
		if (addAccessor) {
			User owner = context.getRequestCache().getUserById(context.getAccessor());
			members.add(owner);
		}
		
		if (members.isEmpty() && parent==null) {
			//return provider;
		} else {
			UserGroup userGroup = createUserGroup(context, UserGroupType.ORGANIZATION, organizationId, provider.name);			
			for (User user : members) {
				ProjectTools.addToUserGroup(context, ResearcherRole.HC(), organizationId, EntityType.USER, user._id);
			}
			RecordManager.instance.createPrivateAPS(context.getCache(), userGroup._id, userGroup._id);
		}
		
		if (parent != null) ProjectTools.addToUserGroup(context.forUserGroup(parent, Permission.SETUP), ResearcherRole.SUBORGANIZATION(), parent, EntityType.ORGANIZATION, organizationId);
		if (oldParent != null) UserGroupTools.removeMemberFromUserGroup(context.forUserGroup(oldParent, Permission.SETUP), organizationId, oldParent);
		
		return provider;
		} finally {
		   AccessLog.logEnd("end createOrUpdateOrganizationUserGroup");
		}
	}
	
	public static boolean accessorIsMemberOfGroup(AccessContext context, MidataId targetGroup, Permission permission) throws InternalServerException {
		List<UserGroupMember> chain = context.getCache().getByGroupAndActiveMember(targetGroup, context.getAccessor(), permission);
		if (chain != null && !chain.isEmpty()) return true;
		return false;
	}
	
	public static void updateManagers(AccessContext context, MidataId targetGroup, List<IBaseExtension> extensions) throws AppException {
		Set<UserGroupMember> old = UserGroupMember.getAllActiveByGroup(targetGroup);
		Map<MidataId, UserGroupMember> oldEntries = new HashMap<MidataId, UserGroupMember>();
		for (UserGroupMember ugm : old) oldEntries.put(ugm.member, ugm);
		
		for (IBaseExtension ext : extensions) {
			ResearcherRole role = null;
			if (ext.getUrl().equals("http://midata.coop/extensions/managed-by")) {
				role = ResearcherRole.MANAGER();
			}
			if (role != null) {
				IBaseDatatype value = ext.getValue();
				if (value instanceof IBaseReference) {
					IIdType ref = ((IBaseReference) value).getReferenceElement();
					String resourceType = ref.getResourceType();
					MidataId resourceId = MidataId.parse(ref.getIdPart());
					
					if (!oldEntries.containsKey(resourceId)) {
						switch(resourceType) {
						case "Practitioner":createUserGroupMember(context, resourceId, EntityType.USER, role, targetGroup);break;
						case "Organization":createUserGroupMember(context, resourceId, EntityType.ORGANIZATION, role, targetGroup);break;
						default:
						}												
					} else {
						oldEntries.remove(resourceId);
					}
				}
			}
		}
		
		for (UserGroupMember ugm : oldEntries.values()) {
			// TODO eventually remove old?
		}
	}
		
	
	/**
	 * is this resource managed by a group with the same id?
	 * @param format
	 * @param content
	 * @return
	 */
	public static boolean isGroupManaged(String format, String content) {
		if (format.equals("fhir/Organization") && content.equals("Organization/HP")) return true;
		return false;
	}
	
	public static void removeMemberFromUserGroup(AccessContext context, MidataId targetUserId, MidataId groupId) throws AppException {
		if (!UserGroupTools.accessorIsMemberOfGroup(context, groupId, Permission.CHANGE_TEAM)) {
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to change team.");		
		}
									
		UserGroupMember target = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
		if (target == null) throw new BadRequestException("error.invalid.user", "User is not member of group");
		
		if (target.getRole().id.equals("SUBORGANIZATION")) {
			HealthcareProvider prov = HealthcareProvider.getById(targetUserId, HealthcareProvider.ALL);
			prov.parent = null;
			prov.set("parent", null);
			OrganizationResourceProvider.updateFromHP(context, prov);
		}
		
		target.status = ConsentStatus.EXPIRED;
		target.endDate = new Date();
		UserGroupMember.set(target._id, "status", target.status);
		UserGroupMember.set(target._id, "endDate", target.endDate);

	}
	
	
		
}
