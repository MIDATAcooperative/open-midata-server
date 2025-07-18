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

public enum StudyType {

	/**
	 * Clinical study
	 */
	CLINICAL,
	
	/**
	 * Citizen science project
	 */
	CITIZENSCIENCE,
	
	/**
	 * Only automated services with backsharing. Data only used by the participants.
	 */
	COMMUNITY,
	
	/**
	 * Data gathering for registry
	 */
	REGISTRY,
	
	/**
	 * Meta project that just combines the results of other projects
	 */
	META
}
