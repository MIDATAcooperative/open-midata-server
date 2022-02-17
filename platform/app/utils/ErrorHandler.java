package utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.typesafe.config.Config;

import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;
import utils.exceptions.BadRequestException;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

	  @Inject
	  public ErrorHandler(
	      Config config,
	      Environment environment,
	      OptionalSourceMapper sourceMapper,
	      Provider<Router> routes) {
	    super(config, environment, sourceMapper, routes);	    
	  }

	  protected CompletionStage<Result> onProdServerError(
	      RequestHeader request, UsefulException exception) {
		ErrorReporter.report("Status-500 Handler", null, exception);
	    return CompletableFuture.completedFuture(
	        Results.internalServerError("an internal error occured"));
	  }
	  
	  
	  @Override
	protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {
		  ErrorReporter.report("Status-500 Handler", null, exception);
		  return super.onDevServerError(request, exception);		  
	}

	protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
	    return CompletableFuture.completedFuture(
	        Results.forbidden("You're not allowed to access this resource."));
	}

	@Override
	protected CompletionStage<Result> onBadRequest(RequestHeader request, String message) {
        System.out.println("Bad request: "+message);		
		if (message != null) {
			if (message.indexOf("JsonParseException") >= 0) message = "Provided JSON is not valid.";
			else {				
				try {
					throw new BadRequestException("errors.badrequest", message);
				} catch (BadRequestException e) {
				    ErrorReporter.report("Status-400 Handler", null, e);
				}
				message = "Provided request is not valid.";
			}
		}
		return super.onBadRequest(request, message);
	}
	
	
}
