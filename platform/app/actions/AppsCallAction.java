package actions;

import play.Play;
import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.access.RecordSharing;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Wrapper for requests done by input forms
 *
 */
public class AppsCallAction extends Action<AppsCall> {

    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
    	try {
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  String host = ctx.request().getHeader("Origin");
    	  String appsServer = "https://" + Play.application().configuration().getString("apps.server");
    	  if (host != null) {
	  		  if (host.startsWith("https://localhost:") || host.startsWith("http://localhost:") || host.equals(appsServer)) {
	  		    ctx.response().setHeader("Access-Control-Allow-Origin", host);
	  		  } else ctx.response().setHeader("Access-Control-Allow-Origin", appsServer);
    	  }
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie");

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
    	} catch (BadRequestException e3) {
    		return F.Promise.pure((Result) badRequest(e3.getMessage()));
		} catch (InternalServerException e2) {			
			return F.Promise.pure((Result) internalServerError(e2.getMessage()));			
		} finally {
			RecordSharing.instance.clear();
		}
    }
}