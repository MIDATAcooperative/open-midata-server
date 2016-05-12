package controllers;

import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

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
	
	public static UserRole getRole() {
		return UserRole.valueOf(session().get("role"));
	}
	
	public static void setAttachmentContentDisposition(String filename) {
		String fn = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
		response().setHeader("Content-Disposition", "attachment; filename=\"" + fn+"\"");
	}
	
	
}
