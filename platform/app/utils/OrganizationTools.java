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

import org.hl7.fhir.r4.model.Identifier;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import controllers.UserGroups;
import models.HealthcareProvider;
import models.MidataId;
import models.Model;
import models.enums.EntityType;
import models.enums.Permission;
import models.enums.ResearcherRole;
import models.enums.UserStatus;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.FHIRServlet;
import utils.fhir.FHIRTools;
import utils.fhir.ResourceProvider;
import utils.json.JsonValidation.JsonValidationException;

public class OrganizationTools {

	
	public static void prepareModel(AccessContext context, HealthcareProvider midataResource, MidataId oldParent) throws AppException {
		if (HealthcareProvider.existsByName(midataResource.name, midataResource._id)) throw new JsonValidationException("error.exists.organization", "name", "exists", "A healthcare provider organization with this name already exists.");
		if (midataResource.parent != null && !midataResource.parent.equals(oldParent)) {
			if (!UserGroupTools.accessorIsMemberOfGroup(context, midataResource.parent, Permission.SETUP)) {
				throw new BadRequestException("error.notauthorized.action", "Tried to change other healthcare provider organization!");
			}	
		}
		if (midataResource.identifiers != null) {
			if (!ResourceProvider.hasInfo()) ResourceProvider.setAccessContext(context);
			for (String ids : midataResource.identifiers) {
				checkIdentifier(midataResource, ids);
			}
		}
	}
	
	public static void checkIdentifier(Model record, String systemValue) throws AppException {
		String parts[] = systemValue.split("[\\s\\|]");
		
		if (parts.length==1) checkIdentifier(record, null, parts[0]);
		else if (parts.length>=2) checkIdentifier(record, parts[0], parts[1]);	    
	}
	
	public static void checkIdentifier(Model record, String system, String value) throws AppException {		
		MidataId existing = FHIRTools.resolveUniqueIdentifierToId("Organization", system, value);
		if (existing != null && !existing.equals(record._id)) throw new BadRequestException("error.not_unique.identifier", "Identifier is not unique");
		
	}

	/*
	public static void createModel(AccessContext context, HealthcareProvider midataResource) throws AppException {
		MidataId managerId = midataResource.managerId;
		EntityType managerType = midataResource.managerType;
        HealthcareProvider provider = UserGroupTools.createOrUpdateOrganizationUserGroup(context, midataResource._id, midataResource.name, midataResource, midataResource.parent, midataResource.managerId.equals(context.getAccessor()), false);		
		if (!managerId.equals(context.getAccessor())) ProjectTools.addOrMergeToUserGroup(context, ResearcherRole.MANAGER(), provider._id, managerType, managerId, true);	
	}*/
	
	public static HealthcareProvider loadModelFromId(AccessContext context, MidataId id) throws AppException {
		HealthcareProvider provider = HealthcareProvider.getByIdAlsoDeleted(id, HealthcareProvider.ALL);
		if (provider == null) throw new InternalServerException("error.internal", "Healthcare provider organization not found.");		
		return provider;		
	}

	
	public static HealthcareProvider updateModel(AccessContext context, HealthcareProvider midataResource) throws AppException {
		 AccessLog.logBegin("Begin update organization model");
		 if (midataResource.status == UserStatus.DELETED) {
			 UserGroupTools.deleteUserGroup(context, midataResource._id, true);
			 midataResource.set("status", UserStatus.DELETED);
		 } else if (midataResource.status == UserStatus.BLOCKED) {
			 
		 } else {
			 midataResource = UserGroupTools.createOrUpdateOrganizationUserGroup(context, midataResource._id, midataResource.name, midataResource, midataResource.parent, false, false);
		 }
		 AccessLog.logEnd("End update organization model");
         return midataResource;
	}
	
	public static MidataId resolve(AccessContext context, String ref) throws AppException {
		if (ref==null) return null;
		if (ref.indexOf('|') < 0) {
			return MidataId.parse(ref);
		}
		FHIRServlet.getProvider("Organization").setAccessContext(context);
		return FHIRTools.resolveUniqueIdentifierToId("Organization", ref);
	}
}
