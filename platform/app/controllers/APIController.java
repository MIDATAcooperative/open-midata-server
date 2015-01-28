package controllers;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class APIController extends Controller {

	public static Result inputerror(String field, String type, String message) {		
		return badRequest(Json.newObject().put("field", field)
                                          .put("type", type)
                                          .put("message", message));
	}
	
	
}
