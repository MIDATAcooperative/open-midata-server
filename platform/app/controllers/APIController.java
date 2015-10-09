package controllers;

import models.enums.UserRole;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class APIController extends Controller {

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
	
	
}
