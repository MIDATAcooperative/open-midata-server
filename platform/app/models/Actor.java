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


package models;

import models.enums.EntityType;
import models.enums.UserRole;
import utils.context.AccessContext;
import utils.exceptions.AppException;

/**
 * Interface for all entities that may be actor on the platform
 * @author alexander
 *
 */
public interface Actor {

	/**
	 * Return public key for actor
	 * @return
	 */
	public byte[] getPublicKey();
	
	/**
	 * Get id of actor
	 * @return
	 */
	public MidataId getId();
	
	/**
	 * Get FHIR Resource Type of Actor
	 * @return
	 */
	public String getResourceType();
	
	/**
	 * Get Entity Type of Actor
	 * @return
	 */
	public EntityType getEntityType();
	
	/**
	 * public name of actor
	 * @return
	 */
	public String getDisplayName();
	
	public String getPublicIdentifier();
	
	public default UserRole getUserRole() {
		return UserRole.ANY;
	}
	
	/**
	 * Return local reference
	 * @return
	 */
	public default String getLocalReference() {
		return getResourceType()+"/"+getId();
	}
	
	public static Actor getActor(AccessContext context, MidataId target) throws AppException {
		User user = context != null ? context.getRequestCache().getUserById(target) : User.getById(target, User.ALL_USER);
		if (user != null) return user;
		
		//MobileAppInstance mai = MobileAppInstance.getById(target, MobileAppInstance.APPINSTANCE_ALL);
		//if (mai != null) return mai;
					
		UserGroup ug = UserGroup.getById(target, UserGroup.ALL);
		if (ug != null) return ug;

		ServiceInstance si = ServiceInstance.getById(target, ServiceInstance.ALL);
		if (si != null) return si;
		
		return null;
	}
}
