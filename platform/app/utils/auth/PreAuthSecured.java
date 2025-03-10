package utils.auth;

import java.util.Optional;

import java.util.Optional;

import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.exceptions.AppException;

public class PreAuthSecured extends Security.Authenticator {

	public final static long MAX_SESSION = 1000 * 60 * 60 * 8;
	
	@Override
	public Optional<String> getUsername(Request ctx) {
	    PortalSessionToken tk = PortalSessionToken.decrypt(ctx);
	    if (tk == null) return Optional.empty();	  
	    try {
	      if (tk.getHandle()!=null) KeyManager.instance.continueSession(tk.getHandle());
	    } catch (AppException e) { return Optional.empty(); }	    
	   
		// id is the user id in String form
		return Optional.of(tk.getOwnerId().toString());
	}

	@Override
	public Result onUnauthorized(Request ctx) {
		return unauthorized();
	}

}
