/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers;

import models.MidataId;
import models.User;
import models.enums.SubUserRole;
import models.enums.UserFeature;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.AccessContext;
import utils.access.RecordManager;
import utils.access.SessionAccessContext;
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
	
	public static AccessContext portalContext(Request request) throws InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(Security.USERNAME));
		SessionAccessContext session = RecordManager.instance.createSession(userId, getRole(), null, userId, null);
		return session.forAccount();
	}
	
	/**
	 * if current user does not have the given SubUserRole an AuthException is thrown
	 * @param subUserRole the SubUserRole the current user is required to have
	 * @throws AuthException if user does not have required SubUserRole
	 * @throws InternalServerException if a database error occurs
	 */
	public static void requireSubUserRole(Request request, SubUserRole subUserRole) throws AuthException, InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(Security.USERNAME));
		User user = User.getById(userId, Sets.create("subroles"));
		if (!user.subroles.contains(subUserRole)) throw new AuthException("error.notauthorized.action", "You need to have subrole '"+subUserRole.toString()+"' for this action.", subUserRole);
	}
	
	public static void requireUserFeature(Request request, UserFeature feature) throws AuthException, InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(Security.USERNAME));
		User user = User.getById(userId, User.ALL_USER);
		if (!feature.isSatisfiedBy(user)) throw new AuthException("error.notauthorized.action", "You need to have feature '"+feature.toString()+"' for this action.", feature);
	}
	
	public static void requireSubUserRoleForRole(Request request, SubUserRole subUserRole, UserRole role) throws AuthException, InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(Security.USERNAME));
		User user = User.getById(userId, Sets.create("role", "subroles"));
		if (user.role == role && !user.subroles.contains(subUserRole)) throw new AuthException("error.notauthorized.action", "You need to have subrole '"+subUserRole.toString()+"' for this action.", subUserRole);
	}
	
	/**
	 * if current user DOES have the given SubUserRole an AuthException is thrown
	 * @param subUserRole the SubUserRole the current user is not allowed to have
	 * @param requested subUserRole that is requested instead
	 * @throws AuthException if user does have forbidden SubUserRole
	 * @throws InternalServerException if a database error occurs
	 */
	public static void forbidSubUserRole(Request request, SubUserRole subUserRole, SubUserRole requested) throws AuthException, InternalServerException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		User user = User.getById(userId, Sets.create("subroles"));
		if (user.subroles.contains(subUserRole)) throw new AuthException("error.notauthorized.action", "Subrole '"+subUserRole.toString()+"' is not allowed for this action.", requested);
	}
	
	/**
	 * set content disposition header for attachments
	 * @param filename filename for attachment
	 */
	public static Result setAttachmentContentDisposition(Result result, String filename) {
		String fn = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9_\\-\\.üöäßÜÖÄ \\[\\]\\(\\)]", "");
		return result.withHeader("Content-Disposition", "attachment; filename=\"" + fn+"\"");
	}
	
	public static String getIPAdress(Request request) {
		Request req = request;
		if (req.hasHeader("X-Real-IP-LB")) {
			return req.header("X-Real-IP-LB").get();
		}
		if (req.hasHeader("X-Real-IP")) {
			return req.header("X-Real-IP").get();
		}
		return req.remoteAddress();
	}
	
	
}
