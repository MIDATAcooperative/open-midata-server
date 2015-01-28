package controllers.providers;

import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Context;
import controllers.routes;

public class ProviderSecured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {
		String role = ctx.session().get("role");
		if (! "provider".equals(role)) return null;
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return redirect(routes.Application.welcome());
	}

}
