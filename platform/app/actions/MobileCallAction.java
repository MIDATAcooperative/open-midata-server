/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package actions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.audit.AuditManager;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.exceptions.RequestTooLargeException;
import utils.json.JsonValidation.JsonValidationException;
import utils.stats.Stats;

/**
 * request wrapper for requests from external services
 *
 */
public class MobileCallAction extends Action<MobileCall> {

    public CompletionStage<Result> call(Http.Context ctx)  { 
    	long startTime = System.currentTimeMillis();
    	try {    	  
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  ctx.response().setHeader("Access-Control-Allow-Origin", "*");    	  
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, Authorization, Prefer, Location, IfMatch, ETag, LastModified, Pragma, Cache-Control, X-Session-Token, X-Filename");
    	  ctx.response().setHeader("Pragma", "no-cache");
    	  ctx.response().setHeader("Cache-Control", "no-cache, no-store");
    	  try {
              return delegate.call(ctx);
      	  } catch (RuntimeException ex) {
      		  if (ex.getCause() != null) throw (Exception) ex.getCause(); else throw ex;
      	  }
    	} catch (JsonValidationException e) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), "400");
    		if (e.getField() != null) {
    		  return CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("message", e.getMessage())));
    		} else {
    		  return CompletableFuture.completedFuture((Result) badRequest(e.getMessage()));
    		}
    	} catch (BadRequestException e3) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), e3.getStatusCode()+"");
    		AuditManager.instance.fail(400, e3.getMessage(), e3.getLocaleKey());
    		return CompletableFuture.completedFuture((Result) status(e3.getStatusCode(), e3.getMessage()));
    	} catch (RequestTooLargeException e4) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), "202");
    		return CompletableFuture.completedFuture((Result) status(202, e4.getMessage()));
    	} catch (PluginException e5) {
    		ErrorReporter.reportPluginProblem("Mobile API", ctx, e5);
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), "400");
			AuditManager.instance.fail(400, e5.getMessage(), e5.getLocaleKey());
			return CompletableFuture.completedFuture((Result) status(400, e5.getMessage()));
    	} catch (InternalServerException e4) {			
			ErrorReporter.report("Mobile API", ctx, e4);
			if (Stats.enabled) Stats.finishRequest(ctx.request(), "500");
			AuditManager.instance.fail(500, e4.getMessage(), null);
			return CompletableFuture.completedFuture((Result) internalServerError("err:"+e4.getMessage()));		
		} catch (Exception e2) {			
			ErrorReporter.report("Mobile API", ctx, e2);
			if (Stats.enabled) Stats.finishRequest(ctx.request(), "500");
			AuditManager.instance.fail(500, e2.getMessage(), null);
			return CompletableFuture.completedFuture((Result) internalServerError("an internal error occured"));			
		} finally {
			try {
				long endTime = System.currentTimeMillis();
				if (endTime - startTime > configuration.maxtime()) {
				   ErrorReporter.reportPerformance("Mobile API", ctx, endTime - startTime);
				}	
			} finally {
			    ServerTools.endRequest();
			}											 							 
		}
    }
}