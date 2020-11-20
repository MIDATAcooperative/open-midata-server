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

package models.enums;

public enum WritePermissionType {

	/**
	 * Grantee is not allowed to write records
	 */
	NONE,
	
	/**
	 * Grantee is allowed to update existing records shared by consent
	 */
	UPDATE_EXISTING,
	
	/**
	 * Grantee is allowed to create records that will be shared by consent
	 */
	CREATE_SHARED,
	
	/**
	 * Combination of UPDATE_EXISTING an CREATE_SHARED
	 */
	UPDATE_AND_CREATE,
	
	/**
	 * Grantee is allowed to write any type of record even if not shared by consent
	 */
	WRITE_ANY;
	
	public boolean isUpdateAllowed() {
		return this == UPDATE_EXISTING || this == UPDATE_AND_CREATE || this == WritePermissionType.WRITE_ANY;
	}
	
	public boolean isCreateAllowed() {
		return this == CREATE_SHARED || this == WRITE_ANY || this == UPDATE_AND_CREATE;
	}
	
	public boolean isUnrestricted() {
		return this == WRITE_ANY;
	}
}
