package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

/**
 * This authenticator allows only DEVELOPER users
 *
 */
public class DeveloperSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! UserRole.DEVELOPER.toString().equals(role)) return null;
		return super.getUsername(ctx);
	}

}
