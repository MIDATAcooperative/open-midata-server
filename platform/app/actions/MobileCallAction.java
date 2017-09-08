package actions;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.exceptions.BadRequestException;
import utils.fhir.ResourceProvider;
import utils.json.JsonValidation.JsonValidationException;
import utils.stats.Stats;

/**
 * request wrapper for requests from external services
 *
 */
public class MobileCallAction extends Action<MobileCall> {

    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
    	long startTime = System.currentTimeMillis();
    	try {    	  
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  ctx.response().setHeader("Access-Control-Allow-Origin", "*");    	  
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, Authorization, Prefer, Location, IfMatch, ETag, LastModified, Pragma, Cache-Control");
    	  ctx.response().setHeader("Pragma", "no-cache");
    	  ctx.response().setHeader("Cache-Control", "no-cache");
          return delegate.call(ctx);
    	} catch (JsonValidationException e) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), "400");
    		if (e.getField() != null) {
    		  return F.Promise.pure((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("message", e.getMessage())));
    		} else {
    		  return F.Promise.pure((Result) badRequest(e.getMessage()));
    		}
    	} catch (BadRequestException e3) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), e3.getStatusCode()+"");
    		return F.Promise.pure((Result) status(e3.getStatusCode(), e3.getMessage()));
		} catch (Exception e2) {			
			ErrorReporter.report("Mobile API", ctx, e2);
			if (Stats.enabled) Stats.finishRequest(ctx.request(), "500");
			return F.Promise.pure((Result) internalServerError(e2.getMessage()));			
		} finally {
			ServerTools.endRequest();
						
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
			   ErrorReporter.reportPerformance("Mobile API", ctx, endTime - startTime);
			}			 							  
		}
    }
}