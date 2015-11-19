package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

/**
 * This authenticator allows only MIDATA member users
 *
 */
public class MemberSecured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! UserRole.MEMBER.toString().equals(role)) return null;
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
