/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package models.enums;

/**
 * How may be study be joined
 *
 */
public enum JoinMethod {

	/**
	 * Participant joins by installing an app
	 */
	APP,
	
	/**
	 * Participant joins by manually applying in the portal
	 */
	PORTAL,
	
	/**
	 * Participant is added by a member of the research team
	 */
	RESEARCHER,
	
	/**
	 * Participant is added using FHIR API
	 */
	API,
	
	/**
	 * Participant was proposed by an algorithm
	 */
	ALGORITHM,
	
	/**
	 * Participant joined by entering a participation code
	 */
	CODE,
	
	/**
	 * Participant joined by app provided participation code
	 */
	APP_CODE,			
	
	/**
	 * Data is transferred from another study
	 */
	TRANSFER
	
}
