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

import models.MidataId;
import models.enums.SubUserRole;
import models.enums.UserFeature;

/**
 * exception that is thrown if the user is not authorized to do the current action or read requested data
 *
 */
public class AuthException extends AppException {

	private static final long serialVersionUID = 1L;
	private SubUserRole requiredSubUserRole = null;
	private UserFeature requiredFeature = null;
	private MidataId pluginId = null;
	

	public AuthException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public AuthException(String localeKey, String msg) {
		super(localeKey, msg);
	}
	
	public AuthException(String localeKey, String msg, SubUserRole required) {
		super(localeKey, msg);
		this.requiredSubUserRole = required;
	}
	
	public AuthException(String localeKey, String msg, UserFeature required) {
		super(localeKey, msg);
		this.requiredFeature = required;
	}
	
	public AuthException(String localeKey, String msg, UserFeature required, MidataId pluginId) {
		super(localeKey, msg);
		this.requiredFeature = required;
		this.pluginId = pluginId;
	}
	
	public SubUserRole getRequiredSubUserRole() {
		return requiredSubUserRole;
	}
	
	public UserFeature getRequiredFeature() {
		return requiredFeature;
	}
	
	public MidataId getPluginId() {
		return pluginId;
	}

}