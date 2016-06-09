package actions;


import com.fasterxml.jackson.databind.JsonNode;


import play.Play;
import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.RecordManager;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation.JsonValidationException;

/**
 * Default wrapper for requests done by the portal
 *
 */
public class APICallAction extends Action<APICall> {

	private static final String defaultHost = Play.application().configuration().getString("portal.originUrl");
    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
    	try {
    		AccessLog.log("API");    	
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  String host = ctx.request().getHeader("Origin");
    	  //AccessLog.log(ctx.request().remoteAddress());
    	  //AccessLog.log(ctx.request().getHeader("X-Real-IP"));
    	  /*if (host != null) {
	  		  if (host.startsWith("https://localhost") || host.startsWith("http://localhost") || host.equals("https://demo.midata.coop") || host.equals("https://demo.midata.coop:9002")) {
	  		    ctx.response().setHeader("Access-Control-Allow-Origin", host);
	  		  } else 
    	  }*/
    	  ctx.response().setHeader("Access-Control-Allow-Origin", defaultHost);
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, X-Session-Token");
    	  try {
            return delegate.call(ctx);
    	  } catch (RuntimeException ex) {
    		  if (ex.getCause() != null) throw ex.getCause(); else throw ex;
    	  }
    	} catch (JsonValidationException e) {
    		if (e.getField() != null) {
    		  return F.Promise.pure((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("code", e.getLocaleKey())
                                        .put("message", e.getMessage())));
    		} else {
    		  return F.Promise.pure((Result) badRequest(e.getMessage()));
    		}
    	} catch (BadRequestException e5) {
    		return F.Promise.pure((Result) badRequest(
				    Json.newObject()
                    .put("code", e5.getLocaleKey())
                    .put("message", e5.getMessage())));
    	} catch (AuthException e3) {
    		ErrorReporter.report("Portal", ctx, e3);	
    		return F.Promise.pure((Result) forbidden(e3.getMessage()));    
		} catch (Exception e2) {	
			ErrorReporter.report("Portal", ctx, e2);					
			return F.Promise.pure((Result) internalServerError(e2.getMessage()));			
		} finally {
			RecordManager.instance.clear();
			AccessLog.newRequest();	
		}
    }
}