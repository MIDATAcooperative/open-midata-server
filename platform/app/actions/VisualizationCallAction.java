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

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
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
import utils.stats.Stats;


/**
 * Default wrapper for requests done by plugins
 *
 */
public class VisualizationCallAction extends Action<VisualizationCall> {
	
    public CompletionStage<Result> call(Http.Context ctx) { 
    	long startTime = System.currentTimeMillis();
    	try {    	  
    		
    	  JsonNode json = ctx.request().body().asJson();
    	  ctx.args.put("json", json);
    	  String host = ctx.request().getHeader("Origin");
    	  //AccessLog.debug("VA:"+host);
    	  String visualizationsServer = "https://" + InstanceConfig.getInstance().getPluginServerDomain();
    	  if (host != null) {
	  		  if (host.startsWith("https://localhost:") || host.startsWith("http://localhost:") || host.equals(visualizationsServer)) {
	  		    ctx.response().setHeader("Access-Control-Allow-Origin", host);
	  		  } else ctx.response().setHeader("Access-Control-Allow-Origin", visualizationsServer);
    	  }
    	  ctx.response().setHeader("Allow", "*");
    	  ctx.response().setHeader("Access-Control-Allow-Credentials", "true");
    	  ctx.response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH");
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, Prefer, Location, IfMatch, ETag, LastModified, Pragma, Cache-Control, X-Filename");
    	  ctx.response().setHeader("Pragma", "no-cache");
    	  ctx.response().setHeader("Cache-Control", "no-cache, no-store");    	 
    	  try {
    		  AccessLog.logStart("api", "(Plugin) ["+ctx.request().method()+"] "+ctx.request().path());
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
    	} catch (RequestTooLargeException e4) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), "202");
    		return CompletableFuture.completedFuture((Result) status(202, e4.getMessage()));
    	} catch (BadRequestException e3) {
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), e3.getStatusCode()+"");
    		AuditManager.instance.fail(400, e3.getMessage(), e3.getLocaleKey());
    		return CompletableFuture.completedFuture((Result) badRequest(e3.getMessage()));
    	} catch (PluginException e4) {
    		ErrorReporter.reportPluginProblem("Plugin API", ctx, e4);
    		if (Stats.enabled) Stats.finishRequest(ctx.request(), "400");
    		AuditManager.instance.fail(400, e4.getMessage(), e4.getLocaleKey());
    		return CompletableFuture.completedFuture((Result) badRequest(e4.getMessage()));
    	} catch (InternalServerException e5) {					
			ErrorReporter.report("Plugin API", ctx, e5);
			if (Stats.enabled) Stats.finishRequest(ctx.request(), "500");
			AuditManager.instance.fail(500, e5.getMessage(), null);
			return CompletableFuture.completedFuture((Result) internalServerError("err:"+e5.getMessage()));	
		} catch (Exception e2) {					
			ErrorReporter.report("Plugin API", ctx, e2);
			if (Stats.enabled) Stats.finishRequest(ctx.request(), "500");
			AuditManager.instance.fail(500, e2.getMessage(), null);
			return CompletableFuture.completedFuture((Result) internalServerError("an internal error occured"));			
		} finally {
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
			   ErrorReporter.reportPerformance("Plugin API", ctx, endTime - startTime);
			}	
			
			ServerTools.endRequest();
			
					
		}
    }
}