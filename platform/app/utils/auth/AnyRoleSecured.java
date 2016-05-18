package utils.auth;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

/**
 * This authenticator allows all users that are logged in
 *
 */
public class AnyRoleSecured extends Security.Authenticator {

	public final static long MAX_SESSION = 1000 * 60 * 60 * 8;
	
	@Override
	public String getUsername(Context ctx) {
		try {
			long ts = Long.parseLong(ctx.session().get("ts"));
			long now = System.currentTimeMillis();
			if (now - ts > MAX_SESSION || now - ts < 0) return null;
		} catch (NumberFormatException e) {
			return null;
		}
		// id is the user id in String form
		return ctx.session().get("id");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
