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
 * Security level of a user account
 *
 */
public enum AccountSecurityLevel {
	/**
	 *  Account is not protected by keypair
	 */
	NONE,            
	
	/**
	 * Account is protected by key in keystore
	 */
	KEY,             
	
	/**
	 * Account is protected by key in keystore and passphrase
	 */
	KEY_PASSPHRASE,  
	
	/**
	 * Account is protected by externally stored keyfile (currently not supported)
	 */
	KEY_FILE,
	
	/**
	 * Account is protected by only externally available password
	 */
	KEY_EXT_PASSWORD
}
