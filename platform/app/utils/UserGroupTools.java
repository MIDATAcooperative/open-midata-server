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

import models.HealthcareProvider;
import models.MidataId;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.EntityType;
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
import utils.exceptions.InternalServerException;
import utils.fhir.GroupResourceProvider;
import utils.json.JsonValidation;

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
	
	public static UserGroupMember createUserGroupMember(AccessContext context, MidataId userGroupId) throws AppException {
		UserGroupMember member = new UserGroupMember();
		member._id = new MidataId();
		member.member = context.getAccessor();
		member.entityType = context.getAccessorEntityType();
		//if (member.entityType == EntityType.USERGROUP) throw new InternalServerException("error.internal", "Cannot create group membership from within usergroup.");
		member.userGroup = userGroupId;
		member.status = ConsentStatus.ACTIVE;
		member.startDate = new Date();
																				
		Map<String, Object> accessData = new HashMap<String, Object>();
		accessData.put("aliaskey", KeyManager.instance.generateAlias(userGroupId, member._id));
		RecordManager.instance.createPrivateAPS(context.getCache(), context.getAccessor(), member._id);
		RecordManager.instance.setMeta(context, member._id, "_usergroup", accessData);
						
		member.add();
				
		return member;		
		
	}
	
	public static HealthcareProvider createOrUpdateOrganizationUserGroup(AccessContext context, MidataId organizationId, String name, String description, boolean addAccessor) throws AppException {
		UserGroup existing = UserGroup.getById(organizationId, UserGroup.ALL);
		if (existing != null) {
			if (name != null && !existing.name.equals(name)) {
				existing.name = name;
				UserGroup.set(existing._id, "name", existing.name);
			} else return null;
		}
				
		HealthcareProvider provider = HealthcareProvider.getById(organizationId, HealthcareProvider.ALL);
		if (provider == null) {
			if (name != null) {
				provider = new HealthcareProvider();
				provider._id = organizationId;
				provider.name = name;
				provider.description = description;
				HealthcareProvider.add(provider);				
			} else throw new InternalServerException("error.internal", "Organization not found");
		} else {
			provider.name = name;
			provider.description = description;
			provider.setMultiple(Sets.create("name", "description"));
		}
		
		Set<User> members = User.getAllUser(CMaps.map("provider", organizationId), User.ALL_USER);
		if (addAccessor) {
			User owner = context.getRequestCache().getUserById(context.getAccessor());
			members.add(owner);
		}
		
		if (members.isEmpty()) {
			return provider;
		} else {
			UserGroup userGroup = createUserGroup(context, UserGroupType.ORGANIZATION, organizationId, provider.name);		
			for (User user : members) {
				ProjectTools.addToUserGroup(context, ResearcherRole.HC(), organizationId, EntityType.USER, user._id);
			}
			RecordManager.instance.createPrivateAPS(context.getCache(), userGroup._id, userGroup._id);
		}
		
		return provider;
	}
	
	public static boolean accessorIsMemberOfGroup(AccessContext context, MidataId targetGroup) throws InternalServerException {
		List<UserGroupMember> chain = context.getCache().getByGroupAndActiveMember(targetGroup, context.getAccessor());
		if (chain != null && !chain.isEmpty()) return true;
		return false;
	}
}
