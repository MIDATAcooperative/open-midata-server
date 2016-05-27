package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

/**
 * This authenticator allows only ADMIN users
 *
 */
public class AdminSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String result = super.getUsername(ctx);
		if (result != null) {
		  UserRole role = PortalSessionToken.session().getRole();
		  if (! UserRole.ADMIN.equals(role)) return null;				  
		}
		return result;
	}

}
