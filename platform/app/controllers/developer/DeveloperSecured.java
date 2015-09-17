package controllers.developer;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class DeveloperSecured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! "developer".equals(role)) return null;
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
