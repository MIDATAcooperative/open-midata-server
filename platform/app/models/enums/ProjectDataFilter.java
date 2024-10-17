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

public enum ProjectDataFilter {

    /**
     * Remove all time information from resources
     */
    NO_TIME,
    
    /**
     * Remove time and day information from resources. Keep only year and month.
     */
    ONLY_MONTH_YEAR,
    
    /**
     * Remove all practitioner references
     */
    NO_PRACTITIONER,
    
    /**
     * Remove all narratives
     */
    NO_NARRATIVES
    
}
