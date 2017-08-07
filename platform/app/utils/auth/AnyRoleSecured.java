package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;
import utils.exceptions.AuthException;

/**
 * This authenticator allows all users that are logged in
 *
 */
public class AnyRoleSecured extends Security.Authenticator {

	public final static long MAX_SESSION = 1000 * 60 * 60 * 8;
	
	@Override
	public String getUsername(Context ctx) {
	    PortalSessionToken tk = PortalSessionToken.decrypt(ctx.request());
	    if (tk == null || tk.getRole() == UserRole.ANY) return null;
	    try {
	      KeyManager.instance.continueSession(tk.getHandle());
	    } catch (AuthException e) { return null; }	    
	   
		// id is the user id in String form
		return tk.getUserId().toString();
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
