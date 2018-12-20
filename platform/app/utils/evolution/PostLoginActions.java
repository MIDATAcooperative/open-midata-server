package utils.evolution;

import controllers.PWRecovery;
import models.User;
import models.enums.AccountActionFlags;
import models.enums.UserRole;
import utils.AccessLog;
import utils.exceptions.AppException;
import utils.fhir.PatientResourceProvider;

public class PostLoginActions {

	public static User check(User user) throws AppException {
		boolean wasOld = AccountPatches.check(user);
		
		if (wasOld) {
			AccessLog.log("Finished patching account.");
			user = User.getById(user._id, User.ALL_USER_INTERNAL);
		}
		
		if (user.flags != null) {
			if (user.flags.contains(AccountActionFlags.UPDATE_FHIR) && user.role.equals(UserRole.MEMBER)) {
				AccessLog.log("Update of patient record required.");
				PatientResourceProvider.updatePatientForAccount(user._id);
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
