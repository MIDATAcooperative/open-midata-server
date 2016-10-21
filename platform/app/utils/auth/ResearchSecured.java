package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;

/**
 * This authenticator allows only researchers as users
 *
 */
public class ResearchSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String result = super.getUsername(ctx);
		if (result != null) {
		  UserRole role = PortalSessionToken.session().getRole();
		  if (! UserRole.RESEARCH.equals(role)) return null;				  
		}
		return result;
	}
	
}
