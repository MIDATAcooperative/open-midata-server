package controllers;

import models.MidataId;

import models.User;
import models.enums.SubUserRole;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.auth.PortalSessionToken;
import utils.collections.Sets;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

/**
 * Baseclass for controller classes
 *
 */
public abstract class APIController extends Controller {

	public static Result inputerror(String field, String type, String message) {		
		return badRequest(Json.newObject().put("field", field)
                                          .put("type", type)
                                          .put("message", message));
	}
	
	public static Result statusError(String code, String message) {
		return badRequest(Json.newObject().put("type", code)        
                                          .put("message", message));
	}
	
	public static Result statusWarning(String code, String message) {
		return badRequest(Json.newObject().put("type", code)        
				                          .put("level", "warning")
                                          .put("message", message));
	}
	
	/**
	 * retrieve Role of current user
	 * @return role of current user
	 */
	public static UserRole getRole() {
		return PortalSessionToken.session().getRole();
	}
	
	/**
	 * if current user does not have the given SubUserRole an AuthException is thrown
	 * @param subUserRole the SubUserRole the current user is required to have
	 * @throws AuthException if user does not have required SubUserRole
	 * @throws InternalServerException if a database error occurs
	 */
	public static void requireSubUserRole(SubUserRole subUserRole) throws AuthException, InternalServerException {
		MidataId userId = new MidataId(request().username());
		User user = User.getById(userId, Sets.create("subroles"));
		if (!user.subroles.contains(subUserRole)) throw new AuthException("error.notauthorized.action", "You need to have subrole '"+subUserRole.toString()+"' for this action.");
	}
	
	/**
	 * set content disposition header for attachments
	 * @param filename filename for attachment
	 */
	public static void setAttachmentContentDisposition(String filename) {
		String fn = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9_\\-\\.üöäßÜÖÄ \\[\\]\\(\\)]", "");
		response().setHeader("Content-Disposition", "attachment; filename=\"" + fn+"\"");
	}
	
	
}
