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

package utils.auth.auth2factor;

import models.enums.SecondaryAuthType;

/**
 * Factory for two factor authentication providers
 *
 */
public class Authenticators {

	/**
	 * Return authenticator of requested type
	 * @param authType
	 * @return
	 */
	public static Authenticator getInstance(SecondaryAuthType authType) {
		if (authType == SecondaryAuthType.SMS) return new SMSAuthenticator();
		return null;
	}
}
