package actions;

import models.ModelException;
import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.RecordSharing;

public class VisualizationCallAction extends Action<VisualizationCall> {

    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
    	try {
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  /*ctx.response().setHeader("Access-Control-Allow-Origin", "http://localhost:9002");
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie");
    	  */
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