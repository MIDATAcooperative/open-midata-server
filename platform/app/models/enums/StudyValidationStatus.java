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
 * Status of validation process for a study
 *
 */
public enum StudyValidationStatus {
	
	/**
	 * Validation has not yet been requested
	 */
	DRAFT,
	
	/**
	 * Study may be changed again after it was already validated
	 */
	PATCH,
	
	/**
	 * Study validation is currently in progress
	 */
	VALIDATION,
	
	/**
	 * Study has been successfully validated.
	 */
	VALIDATED,
	
	/**
	 * Study has been rejected by MIDATA
	 */
	REJECTED
}
