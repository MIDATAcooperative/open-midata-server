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
		String result = super.getUsername(ctx);
		if (result != null) {
		  UserRole role = PortalSessionToken.session().getRole();
		  if (! UserRole.MEMBER.equals(role)) return null;				  
		}
		return result;
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
