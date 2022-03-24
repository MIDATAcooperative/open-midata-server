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
import utils.InstanceConfig;
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
 * Default wrapper for requests done by plugins
 *
 */
public class VisualizationCallAction extends Action<VisualizationCall> {
	
	public CompletionStage<Result> withHeaders(String origin, CompletionStage<Result> in) {
		String visualizationsServer = "https://" + InstanceConfig.getInstance().getPluginServerDomain();
		System.out.println("ORIGIN: "+origin);
		return in.thenApply(result -> {
			  return result
					  .withHeader("Access-Control-Allow-Origin", 							  
							  (origin != null && (origin.startsWith("https://localhost:") || origin.startsWith("http://localhost:") || origin.equals(visualizationsServer)) ? origin : visualizationsServer))
	    	          .withHeader("Allow", "*")
	    	          .withHeader("Access-Control-Allow-Credentials", "true")
	    	          .withHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH")
	    	          .withHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, Authorization, Prefer, Location, IfMatch, ETag, LastModified, Pragma, Cache-Control, X-Session-Token, X-Filename")
	    	          .withHeader("Pragma", "no-cache")
	    	          .withHeader("Cache-Control", "no-cache, no-store");
	    	  
		});
	}
	
	@Override
    public CompletionStage<Result> call(Request request) { 
    	long startTime = System.currentTimeMillis();
    	String path = "(Plugin) ["+request.method()+"] "+request.path();
    	long st = ActionRecorder.start(path);
    	String origin = null;
    	try {    	  
    		
    	  //JsonNode json = ctx.request().body().asJson();
    	  //ctx.args.put("json", json);
    	  String host = request.header("Host").orElse(null);
    	  origin = request.header("Origin").orElse(null);
    	  //AccessLog.debug("VA:"+host);

    	  // XXX test for auto import
    	  if (host==null) return withHeaders(origin, CompletableFuture.completedFuture((Result) badRequest("missing http host header")));
    	      	  
    	  try {
    		  AccessLog.logStart("api", path);
              return withHeaders(origin, delegate.call(request));
      	  } catch (RuntimeException ex) {
      		  if (ex.getCause() != null) throw (Exception) ex.getCause(); else throw ex;
      	  }
    	      	  
    	} catch (JsonValidationException e) {
    		if (Stats.enabled) Stats.finishRequest(request, "400");
    		if (e.getField() != null) {
    		  return withHeaders(origin, CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("message", e.getMessage()))));
    		} else {
    		  return withHeaders(origin, CompletableFuture.completedFuture((Result) badRequest(e.getMessage())));
    		}
    	} catch (RequestTooLargeException e4) {
    		if (Stats.enabled) Stats.finishRequest(request, "202");
    		return withHeaders(origin, CompletableFuture.completedFuture((Result) status(202, e4.getMessage())));
    	} catch (BadRequestException e3) {
    		if (Stats.enabled) Stats.finishRequest(request, e3.getStatusCode()+"");
    		AuditManager.instance.fail(400, e3.getMessage(), e3.getLocaleKey());
    		return withHeaders(origin, CompletableFuture.completedFuture((Result) badRequest(e3.getMessage())));
    	} catch (PluginException e4) {
    		ErrorReporter.reportPluginProblem("Plugin API", request, e4);
    		if (Stats.enabled) Stats.finishRequest(request, "400");
    		AuditManager.instance.fail(400, e4.getMessage(), e4.getLocaleKey());
    		return withHeaders(origin, CompletableFuture.completedFuture((Result) badRequest(e4.getMessage())));
    	} catch (InternalServerException e5) {					
			ErrorReporter.report("Plugin API", request, e5);
			if (Stats.enabled) Stats.finishRequest(request, "500");
			AuditManager.instance.fail(500, e5.getMessage(), null);
			return withHeaders(origin, CompletableFuture.completedFuture((Result) internalServerError("err:"+e5.getMessage())));	
		} catch (Exception e2) {					
			ErrorReporter.report("Plugin API", request, e2);
			if (Stats.enabled) Stats.finishRequest(request, "500");
			AuditManager.instance.fail(500, e2.getMessage(), null);
			return withHeaders(origin, CompletableFuture.completedFuture((Result) internalServerError("an internal error occured")));			
		} finally {
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
			   ErrorReporter.reportPerformance("Plugin API", request, endTime - startTime);
			}	
			
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
					
		}
    }
}