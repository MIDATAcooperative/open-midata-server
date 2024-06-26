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

import models.Consent;
import models.HealthcareProvider;
import models.MidataId;
import models.TypedMidataId;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.UserGroupType;
import models.enums.UserRole;
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
import utils.fhir.FHIRTools;
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
	
	public static UserGroupMember createOrMergeUserGroupMember(AccessContext context, MidataId memberId, EntityType entityType, ResearcherRole role, MidataId userGroupId) throws AppException {
	  UserGroupMember ugm = UserGroupMember.getByGroupAndMember(userGroupId, memberId);
	  if (ugm == null) return createUserGroupMember(context, memberId, entityType, role, userGroupId);
	  ugm.role.merge(role);
	  ugm.set(ugm._id, "role", ugm.role);
	  return ugm;
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
	
	public static HealthcareProvider createOrUpdateOrganizationUserGroup(AccessContext context, MidataId organizationId, String name, HealthcareProvider infos, MidataId parent, boolean addAccessor, boolean accessorFullAccess) throws AppException {
		if (parent != null && !accessorIsMemberOfGroup(context, parent, Permission.SETUP) && context.getAccessorRole() != UserRole.ADMIN) throw new BadRequestException("error.notauthorized.action", "You are not authorized to manage parent organization.");
		AccessLog.logBegin("begin createOrUpdateOrganizationUserGroup org="+organizationId.toString()+" name="+name+" addAccessor="+addAccessor+" parent="+parent);
		try {
		MidataId oldParent = null;
		
		UserGroup existing = context.getRequestCache().getUserGroupById(organizationId);
		if (existing != null) {
			if (name != null && !existing.name.equals(name)) {
				existing.name = name;
				UserGroup.set(existing._id, "name", existing.name);
			} 
		}
				
		HealthcareProvider provider = HealthcareProvider.getByIdAlsoDeleted(organizationId, HealthcareProvider.ALL);
		if (provider == null) {
			if (name != null) {
				provider = new HealthcareProvider();
				provider._id = organizationId;
				provider.name = name;
				if (infos != null) {
					if (infos.description != null) provider.description = infos.description;					
					if (infos.city != null) provider.city = infos.city;
					if (infos.zip != null) provider.zip = infos.zip;
					if (infos.country != null) provider.country = infos.country;
					if (infos.address1 != null) provider.address1 = infos.address1;
					if (infos.address2 != null) provider.address2 = infos.address2;
					if (infos.phone != null) provider.phone = infos.phone;
					if (infos.mobile != null) provider.mobile = infos.mobile;
					if (infos.identifiers != null) provider.identifiers = infos.identifiers;
				}
				
				provider.parent = parent;
				provider.status = UserStatus.NEW;
				HealthcareProvider.add(provider);				
			} else throw new InternalServerException("error.internal", "Organization not found");
		} else {
			oldParent = provider.parent;
			provider.name = name;
			if (infos != null) {
				if (infos.description != null) provider.description = infos.description;					
				if (infos.city != null) provider.city = infos.city;
				if (infos.zip != null) provider.zip = infos.zip;
				if (infos.country != null) provider.country = infos.country;
				if (infos.address1 != null) provider.address1 = infos.address1;
				if (infos.address2 != null) provider.address2 = infos.address2;
				if (infos.phone != null) provider.phone = infos.phone;
				if (infos.mobile != null) provider.mobile = infos.mobile;
				if (infos.identifiers != null) provider.identifiers = infos.identifiers;
			}
			provider.parent = parent;
			//provider.status = UserStatus.ACTIVE;
			provider.setMultiple(Sets.create("name", "description", "parent", "status","city","zip","country","address1","address2","phone","mobile"));
			if (oldParent != null && oldParent.equals(parent)) {
				// No change
				oldParent = parent = null;
			}
		}
		
		 
		Set<User> members = User.getAllUser(CMaps.map("provider", organizationId).map("status", User.NON_DELETED), User.ALL_USER);
		User owner = null;
		if (addAccessor) {
			if (context.getAccessorEntityType() == EntityType.USER) {
				owner = context.getRequestCache().getUserById(context.getAccessor());
				
			} 
		}
		
		if (existing == null) {
			if (members.isEmpty() && owner == null && parent==null && context.getAccessorEntityType() != EntityType.SERVICES) {
				//return provider;
			} else {
				UserGroup userGroup = createUserGroup(context, UserGroupType.ORGANIZATION, organizationId, provider.name);
				if (owner != null) ProjectTools.addToUserGroup(context, accessorFullAccess ? ResearcherRole.HC() : ResearcherRole.MANAGER(), organizationId, EntityType.USER, owner._id);
				for (User user : members) {
					ProjectTools.addToUserGroup(context, ResearcherRole.HC(), organizationId, EntityType.USER, user._id);
				}			
				RecordManager.instance.createPrivateAPS(context.getCache(), userGroup._id, userGroup._id);
			}
				
			if (context.getAccessorEntityType() == EntityType.SERVICES) ProjectTools.addToUserGroup(context, ResearcherRole.HC(), organizationId, EntityType.SERVICES, context.getAccessor());
		}
		if (parent != null) {
			ProjectTools.addToUserGroup(context.forUserGroup(parent, Permission.SETUP), ResearcherRole.SUBORGANIZATION(), parent, EntityType.ORGANIZATION, organizationId);
			ProjectTools.addToUserGroup(context, ResearcherRole.PARENTORGANIZATION(), organizationId, EntityType.ORGANIZATION, parent);
		}
		if (oldParent != null) {
			UserGroupTools.removeMemberFromUserGroup(context.forUserGroup(oldParent, Permission.SETUP), organizationId, oldParent);
			UserGroupTools.removeMemberFromUserGroup(context.forUserGroup(organizationId, Permission.SETUP), oldParent, organizationId);
		}
		
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
					TypedMidataId mref = FHIRTools.getMidataIdFromReference(ref);
					if (mref == null) throw new BadRequestException("error.invalid.manager", "Cannot resolve group manager", 400);
					String resourceType = mref.getType();
					MidataId resourceId = mref.getMidataId();
					
					if (!oldEntries.containsKey(resourceId)) {
						switch(resourceType) {
						case "Practitioner":createOrMergeUserGroupMember(context, resourceId, EntityType.USER, role, targetGroup);break;
						case "Organization":createOrMergeUserGroupMember(context, resourceId, EntityType.ORGANIZATION, role, targetGroup);break;
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
		AccessLog.logBegin("BEGIN removeMemberFromUserGroup targetUserId="+targetUserId.toString()+" groupId="+groupId.toString());
		try {
			if (!UserGroupTools.accessorIsMemberOfGroup(context, groupId, Permission.CHANGE_TEAM)) {
				throw new BadRequestException("error.notauthorized.action", "User is not allowed to change team.");		
			}
										
			UserGroupMember target = UserGroupMember.getByGroupAndMember(groupId, targetUserId);
			if (target == null) throw new BadRequestException("error.invalid.user", "User is not member of group");
						
			Set<UserGroupMember> others = UserGroupMember.getAllActiveByGroup(groupId);
			boolean foundManager = false;
			boolean foundChangeTeam = false;			
			for (UserGroupMember other : others) {				
				if (other.getRole().mayChangeTeam() && !targetUserId.equals(other.member)) foundChangeTeam = true;
				if (other.getRole().maySetup() && !targetUserId.equals(other.member)) foundManager = true;
			}
			if (!foundManager || !foundChangeTeam) throw new BadRequestException("error.missing.manager", "No manager left");
			
			if (target.getRole().id.equals("SUBORGANIZATION")) {
				HealthcareProvider prov = HealthcareProvider.getByIdAlsoDeleted(targetUserId, HealthcareProvider.ALL);
				prov.parent = null;
				prov.set("parent", null);
				OrganizationResourceProvider.updateFromHP(context, prov);
			}
			
			target.status = ConsentStatus.EXPIRED;
			target.endDate = new Date();
			UserGroupMember.set(target._id, "status", target.status);
			UserGroupMember.set(target._id, "endDate", target.endDate);
		} finally {
			AccessLog.logEnd("END removeMemberFromUserGroup");
		}

	}
	
	public static void deleteUserGroup(AccessContext context, MidataId groupId, boolean force) throws AppException {
		AccessLog.logBegin("BEGIN deleteUserGroup groupId="+groupId.toString());
		try {
			List<UserGroupMember> path = context.getCache().getByGroupAndActiveMember(groupId, context.getAccessor(), Permission.SETUP);		
			if (path == null && context.getAccessorRole() != UserRole.ADMIN) throw new BadRequestException("error.invalid.usergroup", "Only members may delete a group");
			
			if (path != null) context = context.forUserGroup(path);
			
			UserGroup userGroup = UserGroup.getById(groupId, UserGroup.FHIR);
			
			if (userGroup == null) {
				AccessLog.log("userGroup does not exist.");
				return;
			}
			
			if (!force && userGroup.type != UserGroupType.CARETEAM) throw new BadRequestException("error.unsupported", "No group deletion for entities.");
			
			Set<Consent> consents = Consent.getAllByAuthorized(groupId);
			
			if (consents.isEmpty() && userGroup.type == UserGroupType.CARETEAM) {		
				Set<UserGroupMember> allMembers = UserGroupMember.getAllByGroup(groupId);		
				for (UserGroupMember member : allMembers) {
					RecordManager.instance.deleteAPS(context, member._id);
					member.delete();
				}
				
				RecordManager.instance.deleteAPS(context, groupId);
				UserGroup.delete(groupId);
			} else {
				Set<UserGroupMember> allMembers = UserGroupMember.getAllByGroup(groupId);		
				for (UserGroupMember member : allMembers) {
					if (member.status == ConsentStatus.ACTIVE) {
						member.status = ConsentStatus.EXPIRED;
						member.endDate = new Date();
						UserGroupMember.set(member._id, "status" , member.status);
						UserGroupMember.set(member._id, "endDate" , member.endDate);
					}
				}
									
				userGroup.status = UserStatus.DELETED;
				userGroup.searchable = false;
				UserGroup.set(userGroup._id, "searchable", userGroup.searchable);
				UserGroup.set(userGroup._id, "status", userGroup.status);
				
				GroupResourceProvider.updateMidataUserGroup(userGroup);
			}
			
			Set<UserGroupMember> allGroupIsMember = UserGroupMember.getAllActiveByMember(groupId);
			for (UserGroupMember member : allGroupIsMember) {
				if (member.status == ConsentStatus.ACTIVE) {
					member.status = ConsentStatus.EXPIRED;
					member.endDate = new Date();
					UserGroupMember.set(member._id, "status" , member.status);
					UserGroupMember.set(member._id, "endDate" , member.endDate);
				}
			}
			
		} finally {
			AccessLog.logEnd("END deleteUserGroup");
		}
	}
	
	public static void changeUserGroupStatus(AccessContext context, MidataId groupId, UserStatus target) throws AppException {
		AccessLog.logBegin("BEGIN changeUserGroupStatus groupId="+groupId.toString()+" status="+target);
		try {
			ConsentStatus status = (target == UserStatus.BLOCKED || target == UserStatus.NEW) ? ConsentStatus.INVALID : ConsentStatus.ACTIVE;
			ConsentStatus from = (target == UserStatus.BLOCKED || target == UserStatus.NEW) ? ConsentStatus.ACTIVE : ConsentStatus.INVALID;
			UserGroup userGroup = UserGroup.getById(groupId, UserGroup.FHIR);
			Set<UserGroupMember> allMembers = UserGroupMember.getAllByGroup(groupId);		
			for (UserGroupMember member : allMembers) {
				if (member.status == from) {
					UserGroupMember.set(member._id, "status", status);
				}				
			} 					
			userGroup.status = target;			
			UserGroup.set(userGroup._id, "status", userGroup.status);
			context.getRequestCache().update(userGroup);
			GroupResourceProvider.updateMidataUserGroup(userGroup);							
			
		} finally {
			AccessLog.logEnd("END changeUserGroupStatus");
		}
	}
	
	public static Set<MidataId> getConsentManagers(AccessContext context) throws AppException {
		Set<MidataId> result = new HashSet<MidataId>();
		result.add(context.getAccessor());
		Set<UserGroupMember> ugms = context.getCache().getAllActiveByMember();
		for (UserGroupMember ugm : ugms) {
			if (ugm.getConfirmedRole().manageParticipants()) {
				if (context.getCache().getByGroupAndActiveMember(ugm, context.getAccessor(), Permission.PARTICIPANTS) != null) {
					result.add(ugm.userGroup);
				}
			}
		}
		return result;
	}
	
	
		
}
