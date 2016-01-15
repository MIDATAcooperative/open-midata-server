package controllers;

import java.io.IOException;

import javax.servlet.ServletException;

import actions.MobileCall;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Patient;
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

public class FHIR extends Controller {

	//public static final FhirContext ctx = FhirContext.forDstu2();
	public static FHIRServlet servlet;
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result get(String all) throws AppException, IOException, ServletException {
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
        ExecutionInfo info = ExecutionInfo.checkSpaceToken("1026725e5e3e3330e73429258e0c786e8cdda484ee1ec4071ad7335f941b85c871fc0494377191203f047c757088612daa04d255ff62e1e2bbdd0b377e96c84c6d4536722e1eb71fa9f52615bf9084e0");
        
        ResourceProvider.setExecutionInfo(info);
        
		AccessLog.debug(req.getRequestURI());
		servlet.doGet(req, res);
		AccessLog.debug("A");
		if (res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		AccessLog.debug("B");
		if (res.getResponseStream() != null) {
			return status(res.getStatus(), res.getResponseStream().toByteArray());
		}
		AccessLog.debug("C");
		return status(res.getStatus());
	}
}
