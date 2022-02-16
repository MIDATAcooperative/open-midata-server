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

package utils.evolution;

import controllers.PWRecovery;
import models.User;
import models.enums.AccountActionFlags;
import models.enums.UserRole;
import utils.AccessLog;
import utils.access.AccessContext;
import utils.exceptions.AppException;
import utils.fhir.PatientResourceProvider;

public class PostLoginActions {

	public static User check(AccessContext context, User user) throws AppException {
		boolean wasOld = AccountPatches.check(context, user);
		
		if (wasOld) {
			AccessLog.log("Finished patching account.");
			user = User.getById(user._id, User.ALL_USER_INTERNAL);
		}
		
		if (user.flags != null) {
			if (user.flags.contains(AccountActionFlags.UPDATE_FHIR) && user.role.equals(UserRole.MEMBER)) {
				AccessLog.log("Update of patient record required.");
				PatientResourceProvider.updatePatientForAccount(context, user._id);
				user.flags.remove(AccountActionFlags.UPDATE_FHIR);
			}
			
			if (user.flags.contains(AccountActionFlags.KEY_RECOVERY)) {				
				PWRecovery.finishRecovery(user);
			}
			
			if (user.flags.isEmpty()) user.flags = null;
			
			User.set(user._id, "flags", user.flags);
		}
		
		return user;
	}
}
