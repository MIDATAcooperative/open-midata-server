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
	
	/**
	 * Handling of OPTIONS requests for all pathes except root path
	 * @param all
	 * @return
	 */
	@MobileCall
	public static Result checkPreflight(String all) {		
		return ok();
	}
	
	/**
	 * Handling of OPTIONS requests for root path
	 * @return
	 */
	@MobileCall
	public static Result checkPreflightRoot() {		
		return ok();
	}
	
	/**
	 * GET Action on root path
	 * @return
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
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
        
		AccessLog.logBegin("begin FHIR get request: "+req.getRequestURI());
		servlet.doGet(req, res);
		AccessLog.logEnd("end FHIR get request");
	
		if (res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		
		if (res.getResponseStream() != null) {
			
			return status(res.getStatus(), res.getResponseStream().toByteArray());
		}
		
		
		return status(res.getStatus());
	}
	
	/**
	 * POST action of root path (FHIR transactions)
	 * @return
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
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
        
		AccessLog.logBegin("begin FHIR post request: "+req.getRequestURI());
		servlet.doPost(req, res);
		AccessLog.logEnd("end FHIR post request");
		if (res.getResponseWriter() != null) {			
			return status(res.getStatus(), res.getResponseWriter().toString());		
		}
		
		if (res.getResponseStream() != null) {			
			byte[] bytes = res.getResponseStream().toByteArray();			
			return status(res.getStatus(), bytes);
		}
		
		
		return status(res.getStatus());
	}
	
	/**
	 * PUT action on root path
	 * @return
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
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
	
	/**
	 * DELETE action on root path
	 * @return
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
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
