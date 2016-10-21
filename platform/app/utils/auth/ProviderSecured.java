package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;

/**
 * This authenticator allows only health provider users
 *
 */
public class ProviderSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String result = super.getUsername(ctx);
		if (result != null) {
		  UserRole role = PortalSessionToken.session().getRole();
		  if (! UserRole.PROVIDER.equals(role)) return null;				  
		}
		return result;
	}


}
