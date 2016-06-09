package controllers;

import java.io.IOException;

import javax.servlet.ServletException;

import actions.MobileCall;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;
import utils.servlet.PlayHttpServletRequest;
import utils.servlet.PlayHttpServletResponse;

/**
 * FHIR Server
 *
 */
public class FHIR extends Controller {

	/**
	 * FHIR Servlet (uses HAPI FHIR)
	 */
	public static FHIRServlet servlet;
	
	@MobileCall
	public static Result checkPreflight(String all) {		
		return ok();
	}
	
	/**
	 * generic handler for all FHIR get requests.
	 * requests will be forwarded to the FHIR servlet.
	 * @param all request path after /fhir
	 * @return request result computed by servlet
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result get(String all) throws AppException, IOException, ServletException {
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		String param = req.getHeader("Authorization");
				
		if (param != null && param.startsWith("Bearer ")) {
          ExecutionInfo info = ExecutionInfo.checkMobileToken(param.substring("Bearer ".length()));        
          ResourceProvider.setExecutionInfo(info);
		}
        
		AccessLog.log(req.getRequestURI());
		servlet.doGet(req, res);
		
		if (res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		
		if (res.getResponseStream() != null) {
			return status(res.getStatus(), res.getResponseStream().toByteArray());
		}
		
		return status(res.getStatus());
	}
}
