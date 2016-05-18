package utils.auth;

import models.enums.UserRole;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Context;
import controllers.routes;

/**
 * This authenticator allows only health provider users
 *
 */
public class ProviderSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! UserRole.PROVIDER.toString().equals(role)) return null;
		return super.getUsername(ctx);
	}


}
