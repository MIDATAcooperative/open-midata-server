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

package utils.auth;

import java.util.Optional;

import models.enums.UserRole;
import play.mvc.Http.Request;

public class ResearchOrDeveloperSecured extends AnyRoleSecured {

	@Override
	public Optional<String> getUsername(Request ctx) {
		String result = super.getUsername(ctx).orElse(null);
		if (result != null) {
		  UserRole role = PortalSessionToken.session().getRole();
		  if (! UserRole.DEVELOPER.equals(role) &&  ! UserRole.ADMIN.equals(role) && ! UserRole.RESEARCH.equals(role)) return Optional.empty();
		  return Optional.of(result);
		}
		return Optional.empty();
	}

}