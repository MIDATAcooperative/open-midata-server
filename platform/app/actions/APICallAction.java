package actions;

import models.ModelException;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.RecordSharing;

import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.json.JsonValidation.JsonValidationException;

public class APICallAction extends Action<APICall> {

    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
    	try {
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
          return delegate.call(ctx);
    	} catch (JsonValidationException e) {
    		if (e.getField() != null) {
    		  return F.Promise.pure((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("message", e.getMessage())));
    		} else {
    		  return F.Promise.pure((Result) badRequest(e.getMessage()));
    		}
		} catch (ModelException e2) {			
			return F.Promise.pure((Result) internalServerError(e2.getMessage()));			
		} finally {
			RecordSharing.instance.clear();
		}
    }
}