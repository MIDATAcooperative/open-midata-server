package controllers;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class AnyRoleSecured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {		
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return redirect(routes.Application.welcome());
	}

}
