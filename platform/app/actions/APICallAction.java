package actions;


import com.fasterxml.jackson.databind.JsonNode;

import controllers.RecordSharing;

import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.access.AccessLog;
import utils.exceptions.AuthException;
import utils.exceptions.ModelException;
import utils.json.JsonValidation.JsonValidationException;

public class APICallAction extends Action<APICall> {

    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
    	try {
    		
    	  Thread.sleep(500);	
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  String host = ctx.request().getHeader("Origin");
    	  //AccessLog.debug(host);
    	  if (host != null) {
	  		  if (host.startsWith("http://localhost:") || host.equals("https://demo.midata.coop")) {
	  		    ctx.response().setHeader("Access-Control-Allow-Origin", host);
	  		  } else ctx.response().setHeader("Access-Control-Allow-Origin", "https://demo.midata.coop");
    	  }
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie");
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
    	} catch (AuthException e3) {
    		return F.Promise.pure((Result) forbidden(e3.getMessage()));    
		} catch (ModelException e2) {			
			return F.Promise.pure((Result) internalServerError(e2.getMessage()));			
		} finally {
			RecordSharing.instance.clear();
		}
    }
}