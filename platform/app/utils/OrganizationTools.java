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

import controllers.UserGroups;
import models.HealthcareProvider;
import models.MidataId;
import models.enums.EntityType;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.UserStatus;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;

public class OrganizationTools {

	
	public static void prepareModel(AccessContext context, HealthcareProvider midataResource, MidataId oldParent) throws AppException {
		if (HealthcareProvider.existsByName(midataResource.name, midataResource._id)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider organization with this name already exists.");
		if (midataResource.parent != null && !midataResource.parent.equals(oldParent)) {
			if (!UserGroupTools.accessorIsMemberOfGroup(context, midataResource.parent, Permission.SETUP)) {
				throw new BadRequestException("error.notauthorized.action", "Tried to change other healthcare provider organization!");
			}	
		}
	}

	
	public static void createModel(AccessContext context, HealthcareProvider midataResource) throws AppException {
		MidataId managerId = midataResource.managerId;
		EntityType managerType = midataResource.managerType;
        HealthcareProvider provider = UserGroupTools.createOrUpdateOrganizationUserGroup(context, midataResource._id, midataResource.name, midataResource.description, midataResource.parent, midataResource.managerId.equals(context.getAccessor()));		
		if (!managerId.equals(context.getAccessor())) UserGroupTools.createUserGroupMember(context, managerId, managerType, ResearcherRole.MANAGER(), provider._id);	
	}

	
	public static HealthcareProvider loadModelFromId(AccessContext context, MidataId id) throws AppException {
		HealthcareProvider provider = HealthcareProvider.getByIdAlsoDeleted(id, HealthcareProvider.ALL);
		if (provider == null) throw new InternalServerException("error.internal", "Healthcare provider organization not found.");		
		return provider;		
	}

	
	public static HealthcareProvider updateModel(AccessContext context, HealthcareProvider midataResource) throws AppException {
		 if (midataResource.status == UserStatus.DELETED) {
			 UserGroupTools.deleteUserGroup(context, midataResource._id, true);
			 midataResource.set("status", UserStatus.DELETED);
		 } else {
			 midataResource = UserGroupTools.createOrUpdateOrganizationUserGroup(context, midataResource._id, midataResource.name, midataResource.description, midataResource.parent, false);
		 }
         return midataResource;
	}
}
