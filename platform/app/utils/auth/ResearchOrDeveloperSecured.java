package utils.auth;

import models.enums.UserRole;
import play.mvc.Http.Context;

public class ResearchOrDeveloperSecured extends AnyRoleSecured {

	@Override
	public String getUsername(Context ctx) {
		String result = super.getUsername(ctx);
		if (result != null) {
		  UserRole role = PortalSessionToken.session().getRole();
		  if (! UserRole.DEVELOPER.equals(role) &&  ! UserRole.ADMIN.equals(role) && ! UserRole.RESEARCH.equals(role)) return null;				  
		}
		return result;
	}

}