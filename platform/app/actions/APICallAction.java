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

import org.springframework.context.annotation.ConditionContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.ActorSystem;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.audit.AuditManager;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.json.JsonValidation.JsonValidationException;

/**
 * Default wrapper for requests done by the portal
 *
 */


public class APICallAction extends Action<APICall> {
	
	public CompletionStage<Result> withHeaders(CompletionStage<Result> in) {
		return in.thenApply(result -> {
			  return result
					  .withHeader("Access-Control-Allow-Origin", InstanceConfig.getInstance().getDefaultHost())
	    	          .withHeader("Allow", "*")
	    	          .withHeader("Access-Control-Allow-Credentials", "true")
	    	          .withHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS, PATCH")
	    	          .withHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie, X-Session-Token, Prefer, X-Filename")
	    	          .withHeader("Pragma", "no-cache")
	    	          .withHeader("Cache-Control", "no-cache, no-store");
	    	  
		});
	}
	
	@Override
    public CompletionStage<Result> call(Request request) {
    	long startTime = System.currentTimeMillis();
    	try {    		    	
    	  //JsonNode json = request.body().asJson();
    	  //request.args.put("json", json);
    	      	  
    	  String host = request.header("Host").orElse(null);   
    	  System.out.println("host="+host);
    	  //System.out.println("B:"+InstanceConfig.getInstance().getDefaultHost());
    	  if (host==null) return CompletableFuture.completedFuture((Result) badRequest("missing http host header"));
    	  /*if (!(
    		  host.equals(InstanceConfig.getInstance().getPortalServerDomain())
    	  )) {    		  
    		  return withHeaders(CompletableFuture.completedFuture((Result) badRequest("http host header error")));
    	  }*/
    	      	  
    	  try {
    		AccessLog.logStart("api", "(Portal) ["+request.method()+"] "+request.path());    	
            return withHeaders(delegate.call(request));
    	  } catch (RuntimeException ex) {
    		  if (ex.getCause() != null) throw (Exception) ex.getCause(); else throw ex;
    	  }
    	} catch (JsonValidationException e) {
    		if (e.getField() != null) {
    		  return withHeaders(CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject().put("field", e.getField())
                                        .put("type", e.getType())
                                        .put("code", e.getLocaleKey())
                                        .put("message", e.getMessage()))));
    		} else {
    			return withHeaders(CompletableFuture.completedFuture((Result) badRequest(
    				    Json.newObject()
                        .put("code", e.getLocaleKey())
                        .put("message", e.getMessage()))));
    		}
    		
    		
    	} catch (BadRequestException e5) {
    		AuditManager.instance.fail(400, e5.getMessage(), e5.getLocaleKey());
    		return withHeaders(CompletableFuture.completedFuture((Result) badRequest(
				    Json.newObject()
                    .put("code", e5.getLocaleKey())
                    .put("message", e5.getMessage()))));
    	} catch (AuthException e3) {
    		if (e3.getRequiredSubUserRole() == null && e3.getRequiredFeature() == null) {
    			ErrorReporter.report("Portal", request, e3);
    			return withHeaders(CompletableFuture.completedFuture((Result) forbidden(e3.getMessage())));
    		} else {
    			ObjectNode node = Json.newObject();
    			if (e3.getRequiredFeature() != null) node.put("requiredFeature", e3.getRequiredFeature().toString());
    			if (e3.getRequiredSubUserRole() != null) node.put("requiredSubUserRole", e3.getRequiredSubUserRole().toString());
    			if (e3.getPluginId() != null) node.put("pluginId", e3.getPluginId().toString());
    			return withHeaders(CompletableFuture.completedFuture((Result) forbidden(node)));
    		}
    	} catch (PluginException e3) {
    		ErrorReporter.reportPluginProblem("Portal", request, e3);
    		AuditManager.instance.fail(400, e3.getMessage(), null);
    		return withHeaders(CompletableFuture.completedFuture((Result) badRequest(
				    Json.newObject()
                    .put("code", e3.getLocaleKey())
                    .put("message", e3.getMessage()))));
    	} catch (InternalServerException e4) {	
			ErrorReporter.report("Portal", request, e4);
			AuditManager.instance.fail(500, e4.getMessage(), null);
			return withHeaders(CompletableFuture.completedFuture((Result) internalServerError(
				    Json.newObject()
                    .put("code", e4.getLocaleKey())
                    .put("message", e4.getMessage()))));					
		} catch (Exception e2) {	
			ErrorReporter.report("Portal", request, e2);
			AuditManager.instance.fail(500, e2.getMessage(), null);
			return withHeaders(CompletableFuture.completedFuture((Result) internalServerError("an internal error occured")));			
		} finally {
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
				 ErrorReporter.reportPerformance("Portal", request, endTime - startTime);
			}	
			ServerTools.endRequest();
					
		}
    	
    	
    }
}
