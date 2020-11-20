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
 * security level for an access permission set
 *
 */
public enum APSSecurityLevel {
	
	/**
	 * APS Not encrypted, records not encrypted
	 */
    NONE,  
    
    /**
     * APS encrypted, records not encrypted
     */
    LOW,   
    
    /**
     * APS encrypted, all records encrypted with same key
     */
    MEDIUM, 
    
    /**
     * APS encrypted, each records encrypted with own key
     */
    HIGH   
}
