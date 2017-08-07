package actions;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.auth.PortalSessionToken;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.fhir.ResourceProvider;
import utils.json.JsonValidation.JsonValidationException;

/**
 * Default wrapper for requests done by the portal
 *
 */
public class APICallAction extends Action<APICall> {

	//private static final String defaultHost = Play.application().configuration().getString("portal.originUrl");
    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
    	long startTime = System.currentTimeMillis();
    	try {    		    	
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  
    	  ctx.response().setHeader("Access-Control-Allow-Origin", InstanceConfig.getInstance().getDefaultHost());
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
    			return F.Promise.pure((Result) badRequest(
    				    Json.newObject()
                        .put("code", e.getLocaleKey())
                        .put("message", e.getMessage())));
    		}
    	} catch (BadRequestException e5) {
    		return F.Promise.pure((Result) badRequest(
				    Json.newObject()
                    .put("code", e5.getLocaleKey())
                    .put("message", e5.getMessage())));
    	} catch (AuthException e3) {
    		if (e3.getRequiredSubUserRole() == null && e3.getRequiredFeature() == null) {
    			ErrorReporter.report("Portal", ctx, e3);
    			return F.Promise.pure((Result) forbidden(e3.getMessage()));
    		} else {
    			ObjectNode node = Json.newObject();
    			if (e3.getRequiredFeature() != null) node.put("requiredFeature", e3.getRequiredFeature().toString());
    			if (e3.getRequiredSubUserRole() != null) node.put("requiredSubUserRole", e3.getRequiredSubUserRole().toString());
    			return F.Promise.pure((Result) forbidden(node));
    		}
    		    
		} catch (Exception e2) {	
			ErrorReporter.report("Portal", ctx, e2);					
			return F.Promise.pure((Result) internalServerError(""+e2.getMessage()));			
		} finally {
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
			   ErrorReporter.reportPerformance("Portal", ctx, endTime - startTime);
			}
			
			RecordManager.instance.clear();	
			PortalSessionToken.clear();
			AccessLog.newRequest();	
			ResourceProvider.setExecutionInfo(null);
		}
    }
}