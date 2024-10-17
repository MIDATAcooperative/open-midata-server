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
 * Status of MIDATA plugin
 *
 */
public enum PluginStatus {
	
	/**
	 * Plugin is still in development. Should not be shown in market
	 */
    DEVELOPMENT,
    
    /**
     * Plugin should be shown on beta (demo) servers but not in production
     */
    BETA,
    
    /**
     * Plugin should be shown in market
     */
    ACTIVE,
    
    /**
     * Plugin is deprecated and should no longer be used
     */
    DEPRECATED,
    
    /**
     * Plugin may no longer be used
     */
    END_OF_LIFE,
    
    /**
     * Plugin has been deleted
     */
    DELETED;
    
    public boolean isUsable() {
    	return this == DEVELOPMENT || this == BETA || this == ACTIVE || this == DEPRECATED;
    }
}
