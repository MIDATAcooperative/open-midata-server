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
		String role = ctx.session().get("role");
		if (! UserRole.ADMIN.toString().equals(role)) return null;		
		return super.getUsername(ctx);
	}

}
