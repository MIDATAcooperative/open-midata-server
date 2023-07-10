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

/**
 * Type of a user group
 *
 */
public enum UserGroupType {

	/**
	 * A group of healthcare providers. Mapped to a FHIR "Group" resource.
	 */
	CARETEAM,
	
	/**
	 * A group of researchers having access to a study. (Belonging to a ResearchStudy resource)
	 */
	RESEARCHTEAM,
	
	/**
	 * People belonging to one organization (Belonging to an Organization resource)
	 */
	ORGANIZATION,
	
	/**
	 * Healthcare providers working for one HealthcareService (Belonging to a HealthcareService resource)
	 */
	HEALTHCARESERVICE
}
