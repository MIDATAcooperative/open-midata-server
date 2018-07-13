package actions;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.audit.AuditManager;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.json.JsonValidation.JsonValidationException;

/**
 * Default wrapper for requests done by the portal
 *
 */


public class APICallAction extends Action<APICall> {
	
    public CompletionStage<Result> call(Http.Context ctx) {
    	long startTime = System.currentTimeMillis();
    	try {    		    	
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  
    	  ctx.response().setHeader("Access-Control-Allow-Origin", InstanceConfig.getInstance().getDefaultHost());
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, X-Session-Token, Prefer");
    	  ctx.response().setHeader("Pragma", "no-cache");
    	  ctx.response().setHeader("Cache-Control", "no-cache, no-store");
    	  try {
            return delegate.call(ctx);
    	  } catch (RuntimeException ex) {
    		  if (ex.getCause() != null) throw (Exception) ex.getCause(); else throw ex;
    	  }
    	} catch (JsonValidationException e) {
    		if (e.getField() != null) {
    		  return CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("code", e.getLocaleKey())
                                        .put("message", e.getMessage())));
    		} else {
    			return CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject()
                        .put("code", e.getLocaleKey())
                        .put("message", e.getMessage())));
    		}
    		
    		
    	} catch (BadRequestException e5) {
    		AuditManager.instance.fail(400, e5.getMessage(), e5.getLocaleKey());
    		return CompletableFuture.completedFuture((Result) badRequest(
				    Json.newObject()
                    .put("code", e5.getLocaleKey())
                    .put("message", e5.getMessage())));
    	} catch (AuthException e3) {
    		if (e3.getRequiredSubUserRole() == null && e3.getRequiredFeature() == null) {
    			ErrorReporter.report("Portal", ctx, e3);
    			return CompletableFuture.completedFuture((Result) forbidden(e3.getMessage()));
    		} else {
    			ObjectNode node = Json.newObject();
    			if (e3.getRequiredFeature() != null) node.put("requiredFeature", e3.getRequiredFeature().toString());
    			if (e3.getRequiredSubUserRole() != null) node.put("requiredSubUserRole", e3.getRequiredSubUserRole().toString());
    			return CompletableFuture.completedFuture((Result) forbidden(node));
    		}
    		    
		} catch (Exception e2) {	
			ErrorReporter.report("Portal", ctx, e2);
			AuditManager.instance.fail(500, e2.getMessage(), null);
			return CompletableFuture.completedFuture((Result) internalServerError(""+e2.getMessage()));			
		} finally {
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
				 ErrorReporter.reportPerformance("Portal", ctx, endTime - startTime);
			}	
			ServerTools.endRequest();
					
		}
    	
    	
    }
}
