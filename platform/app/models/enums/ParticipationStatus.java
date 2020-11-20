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
 * Status of the participation of a MIDATA member in a study.
 *
 */
public enum ParticipationStatus {
	
	/**
	 * MIDATA member has been chosen as a candidate for the study by the platform
	 */
	MATCH, 
	
	/**
	 * MIDATA member requests to participate in the study.
	 */
    REQUEST,
    
    /**
     * MIDATA member has entered a participation code for the study.
     */
    CODE,
    
    /**
     * MIDATA member has been accepted as particpant
     */
	ACCEPTED,
	
	/**
	 * the MIDATA member rejected participation in the study.
	 */
    MEMBER_REJECTED,
    
    /**
     * The research organization rejected participation of the member in the study.
     */
    RESEARCH_REJECTED,
    
    /**
     * Member retreated from participation
     */
    MEMBER_RETREATED
}
