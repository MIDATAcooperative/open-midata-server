package utils.auth;

import models.enums.UserRole;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Context;
import controllers.routes;

public class ProviderSecured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! UserRole.PROVIDER.toString().equals(role)) return null;
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}