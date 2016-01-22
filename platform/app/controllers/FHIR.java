package controllers;

import java.io.IOException;

import javax.servlet.ServletException;

import actions.MobileCall;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.access.AccessLog;
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
				
		String param = req.getParameter("authToken");
		
		if (param == null) param = "1026725e5e3e3330e73429258e0c786e8cdda484ee1ec4071ad7335f941b85c871fc0494377191203f047c757088612daa04d255ff62e1e2bbdd0b377e96c84c6d4536722e1eb71fa9f52615bf9084e0"; 
		
        ExecutionInfo info = ExecutionInfo.checkSpaceToken(param);
        
        ResourceProvider.setExecutionInfo(info);
        
		AccessLog.debug(req.getRequestURI());
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
