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

public enum AppReviewChecklist {

	/**
	 * Application concept as been reviewed
	 */
	CONCEPT,
	
	/**
	 * Data model has been reviewed
	 */
	DATA_MODEL,
	
	/**
	 * Access filter is correct and shares only necessary data
	 */
	ACCESS_FILTER,
	
	/**
	 * Queries are correct, contain all necessary restrictions
	 */
	QUERIES,
	
	/**
	 * App has a proper description 
	 */
	DESCRIPTION,
	
	/**
	 * App has acceptable icons
	 */
	ICONS,
	
	/**
	 * Mail texts are ok and available in all required languages
	 */
	MAILS,
	
	/**
	 * App is properly linked to projects 
	 */
	PROJECTS,
	
	/**
	 * Source code has been reviewed
	 */
	CODE_REVIEW,
	
	/**
	 * functional and ui tests have been formulated
	 */
	TEST_CONCEPT,
	
	/**
	 * tests have been run by 3rd party
	 */
	TEST_PROTOKOLL,
	
	/**
	 * contracts have been made
	 */
	CONTRACT
		
}
