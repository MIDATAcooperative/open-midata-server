package utils.auth;

import models.enums.UserRole;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Context;
import controllers.routes;

/**
 * This authenticator allows only researchers as users
 *
 */
public class ResearchSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! UserRole.RESEARCH.toString().equals(role)) return null;
		return super.getUsername(ctx);
	}
	
}
