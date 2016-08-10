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
	
	@MobileCall
	public static Result checkPreflightRoot() {		
		return ok();
	}
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result getRoot() throws AppException, IOException, ServletException {
		return get("/");
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
          ExecutionInfo info = ExecutionInfo.checkToken(request(), param.substring("Bearer ".length()));        
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
	
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result postRoot() throws AppException, IOException, ServletException {
		return post("/");
	}
	
	/**
	 * generic handler for all FHIR post requests.
	 * requests will be forwarded to the FHIR servlet.
	 * @param all request path after /fhir
	 * @return request result computed by servlet
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result post(String all) throws AppException, IOException, ServletException {
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		String param = req.getHeader("Authorization");
				
		if (param != null && param.startsWith("Bearer ")) {
          ExecutionInfo info = ExecutionInfo.checkToken(request(), param.substring("Bearer ".length()));        
          ResourceProvider.setExecutionInfo(info);
		}
        
		AccessLog.log(req.getRequestURI());
		servlet.doPost(req, res);
		
		if (res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		
		if (res.getResponseStream() != null) {
			return status(res.getStatus(), res.getResponseStream().toByteArray());
		}
		
		return status(res.getStatus());
	}
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result putRoot() throws AppException, IOException, ServletException {
		return put("/");
	}
	
	/**
	 * generic handler for all FHIR put requests.
	 * requests will be forwarded to the FHIR servlet.
	 * @param all request path after /fhir
	 * @return request result computed by servlet
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result put(String all) throws AppException, IOException, ServletException {
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		String param = req.getHeader("Authorization");
				
		if (param != null && param.startsWith("Bearer ")) {
          ExecutionInfo info = ExecutionInfo.checkToken(request(), param.substring("Bearer ".length()));        
          ResourceProvider.setExecutionInfo(info);
		}
        
		AccessLog.log(req.getRequestURI());
		servlet.doPut(req, res);
		
		if (res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		
		if (res.getResponseStream() != null) {
			return status(res.getStatus(), res.getResponseStream().toByteArray());
		}
		
		return status(res.getStatus());
	}
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result deleteRoot() throws AppException, IOException, ServletException {
		return delete("/");
	}
	
	/**
	 * generic handler for all FHIR delete requests.
	 * requests will be forwarded to the FHIR servlet.
	 * @param all request path after /fhir
	 * @return request result computed by servlet
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public static Result delete(String all) throws AppException, IOException, ServletException {
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		String param = req.getHeader("Authorization");
				
		if (param != null && param.startsWith("Bearer ")) {
          ExecutionInfo info = ExecutionInfo.checkToken(request(), param.substring("Bearer ".length()));        
          ResourceProvider.setExecutionInfo(info);
		}
        
		AccessLog.log(req.getRequestURI());
		servlet.doDelete(req, res);
		
		if (res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		
		if (res.getResponseStream() != null) {
			return status(res.getStatus(), res.getResponseStream().toByteArray());
		}
		
		return status(res.getStatus());
	}
}
