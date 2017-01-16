package actions;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.exceptions.BadRequestException;
import utils.fhir.ResourceProvider;
import utils.json.JsonValidation.JsonValidationException;


/**
 * Default wrapper for requests done by plugins
 *
 */
public class VisualizationCallAction extends Action<VisualizationCall> {
	
    public F.Promise<Result> call(Http.Context ctx) throws Throwable { 
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
    	  ctx.response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, Set-Cookie, Cookie");
    
    	  AccessLog.log("path: ["+ctx.request().method()+"] "+ctx.request().path());
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
		} catch (Exception e2) {
			AccessLog.logException("VC", e2);
			ErrorReporter.report("Plugin API", ctx, e2);
			return F.Promise.pure((Result) internalServerError("err:"+e2.getMessage()));			
		} finally {
			long endTime = System.currentTimeMillis();
			if (endTime - startTime > 1000l * 4l) {
			   ErrorReporter.reportPerformance("Plugin API", ctx, endTime - startTime);
			}
			
			RecordManager.instance.clear();
			AccessLog.newRequest();	
			ResourceProvider.setExecutionInfo(null);
		}
    }
}