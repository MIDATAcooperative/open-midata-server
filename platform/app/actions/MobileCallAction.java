/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package actions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.audit.AuditManager;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.exceptions.RequestTooLargeException;
import utils.json.JsonValidation.JsonValidationException;
import utils.stats.ActionRecorder;
import utils.stats.Stats;

/**
 * request wrapper for requests from external services
 *
 */
public class MobileCallAction extends Action<MobileCall> {

	public CompletionStage<Result> withHeaders(CompletionStage<Result> in) {
		return in.thenApply(result -> {
			  return result
					  .withHeader("Access-Control-Allow-Origin", "*")
	    	          .withHeader("Allow", "*")
	    	          .withHeader("Access-Control-Allow-Credentials", "true")
	    	          .withHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH")
	    	          .withHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, Authorization, Prefer, Location, IfMatch, ETag, LastModified, Pragma, Cache-Control, X-Session-Token, X-Filename, X-MIDATA-AUDIT-PRACTITIONER-REFERENCE, X-MIDATA-AUDIT-PRACTITIONER-NAME, X-MIDATA-AUDIT-ORGANIZATION-NAME, X-MIDATA-AUDIT-ORGANIZATION-REFERENCE, X-MIDATA-AUDIT-LOCATION-NAME, X-MIDATA-AUDIT-LOCATION-REFERENCE, X-MIDATA-AUDIT-PURPOSE-CODING, X-MIDATA-AUDIT-PURPOSE-NAME")
	    	          .withHeader("Pragma", "no-cache")
	    	          .withHeader("Cache-Control", "no-cache, no-store");
	    	  
		});
	}
	
    public CompletionStage<Result> call(Request request)  { 
    	long startTime = System.currentTimeMillis();
    	String path = "(Mobile) ["+request.method()+"] "+request.path();
    	long st = ActionRecorder.start(path);
    	try {    	  
    	  //JsonNode json = ctx.request().body().asJson();
    	  //ctx.args.put("json", json);
    	  
    	  try {
    		  AccessLog.logStart("api", path);
              return withHeaders(delegate.call(request));
      	  } catch (RuntimeException ex) {
      		  if (ex.getCause() != null) throw (Exception) ex.getCause(); else throw ex;
      	  }
    	} catch (JsonValidationException e) {
    		if (Stats.enabled) Stats.finishRequest(request, "400");
    		if (e.getField() != null) {
    		  return withHeaders(CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("message", e.getMessage()))));
    		} else {
    		  return withHeaders(CompletableFuture.completedFuture((Result) badRequest(e.getMessage())));
    		}
    	} catch (BadRequestException e3) {
    		if (Stats.enabled) Stats.finishRequest(request, e3.getStatusCode()+"");
    		AuditManager.instance.fail(400, e3.getMessage(), e3.getLocaleKey());
    		return withHeaders(CompletableFuture.completedFuture((Result) status(e3.getStatusCode(), e3.getMessage())));
    	} catch (RequestTooLargeException e4) {
    		if (Stats.enabled) Stats.finishRequest(request, "202");
    		return withHeaders(CompletableFuture.completedFuture((Result) status(202, e4.getMessage())));
    	} catch (PluginException e5) {
    		ErrorReporter.reportPluginProblem("Mobile API", request, e5);
    		if (Stats.enabled) Stats.finishRequest(request, "400");
			AuditManager.instance.fail(400, e5.getMessage(), e5.getLocaleKey());
			return withHeaders(CompletableFuture.completedFuture((Result) status(400, e5.getMessage())));
    	} catch (InternalServerException e4) {			
			ErrorReporter.report("Mobile API", request, e4);
			if (Stats.enabled) Stats.finishRequest(request, "500");
			AuditManager.instance.fail(500, e4.getMessage(), null);
			return withHeaders(CompletableFuture.completedFuture((Result) internalServerError("err:"+e4.getMessage())));		
		} catch (Exception e2) {			
			ErrorReporter.report("Mobile API", request, e2);
			if (Stats.enabled) Stats.finishRequest(request, "500");
			AuditManager.instance.fail(500, e2.getMessage(), null);
			return withHeaders(CompletableFuture.completedFuture((Result) internalServerError("an internal error occured")));			
		} finally {
			try {
				long endTime = System.currentTimeMillis();
				if (endTime - startTime > configuration.maxtime()) {
				   ErrorReporter.reportPerformance("Mobile API", request, endTime - startTime);
				}	
			} finally {
				
			    ServerTools.endRequest();
			    ActionRecorder.end(path, st);
			}											 							 
		}
    }
}