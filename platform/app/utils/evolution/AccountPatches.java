package utils.evolution;

import utils.exceptions.InternalServerException;
import models.User;

public class AccountPatches {

	public static void check(User user) throws InternalServerException {
		if (user.accountVersion < 1) { formatPatch150717(user); }					
	}
	
	public static void makeCurrent(User user, int currentAccountVersion) throws InternalServerException {
		if (user.accountVersion < currentAccountVersion) {
			user.accountVersion = currentAccountVersion;
			User.set(user._id, "accountVersion", user.accountVersion);
		}
	}
	
	public static void formatPatch150717(User user) throws InternalServerException {
		
	}
}
