package controllers.admin;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class AdminSecured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! "admin".equals(role)) return null;
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
