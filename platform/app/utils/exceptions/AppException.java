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

package utils.exceptions;

/**
 * common superclass for application specific exceptions. Has some support for localisation.
 *
 */
public class AppException extends Exception {

	/**
	 * UID for serialization (not used)
	 */
	private static final long serialVersionUID = 1L;
	
	
	private String localeKey;
	
	public AppException(String localeKey, Throwable cause) {
		super(cause);
		this.localeKey = localeKey;
	}
	
	public AppException(String localeKey, String msg) {
		super(msg);
		this.localeKey = localeKey;		
	}
	
	public String getLocaleKey() {
		return localeKey;
	}
}
