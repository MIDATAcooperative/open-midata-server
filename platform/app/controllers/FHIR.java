package controllers;

import java.io.IOException;

import javax.servlet.ServletException;

import actions.MobileCall;
import models.enums.UsageAction;
import models.enums.UserRole;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.access.EncryptedFileHandle;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;
import utils.largerequests.BinaryFileBodyParser;
import utils.largerequests.UnlinkedBinary;
import utils.servlet.PlayHttpServletRequest;
import utils.servlet.PlayHttpServletResponse;
import utils.stats.Stats;
import utils.stats.UsageStatsRecorder;

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
	public Result checkPreflight(String all) {		
		return ok();
	}
	
	/**
	 * Handling of OPTIONS requests for root path
	 * @return
	 */
	@MobileCall
	public Result checkPreflightRoot() {		
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
	public Result getRoot() throws AppException, IOException, ServletException {
		return get("/");
	}
	
	
	private ExecutionInfo getExecutionInfo(PlayHttpServletRequest req) throws AppException {
		String param = req.getHeader("Authorization");
		
		if (param != null && param.startsWith("Bearer ")) {
	          ExecutionInfo info = ExecutionInfo.checkToken(request(), param.substring("Bearer ".length()), false);
	          Stats.setPlugin(info.pluginId);
	          ResourceProvider.setExecutionInfo(info);
	          return info;
		} else {
		 	 String portal = req.getHeader("X-Session-Token");
			 if (portal != null) {
				PortalSessionToken tk = PortalSessionToken.decrypt(request());
			    if (tk == null || tk.getRole() == UserRole.ANY) return null;
			    try {
				      KeyManager.instance.continueSession(tk.getHandle(), tk.ownerId);
			    } catch (AuthException e) { return null; }
			    ExecutionInfo info = new ExecutionInfo(tk.getOwnerId(), tk.getRole());
			    ResourceProvider.setExecutionInfo(info);
			    return info;
			 }
		}
		if (param == null) {
		   AccessLog.log("No authorization header and no portal session");
		} else {
		   AccessLog.log("No valid authorization header and no portal session");
		}
		return null;
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
	public Result get(String all) throws AppException, IOException, ServletException {
		Stats.startRequest(request());
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		ExecutionInfo info = getExecutionInfo(req);
        UsageStatsRecorder.protokoll(info.pluginId, UsageAction.GET);		        
		AccessLog.logBegin("begin FHIR get request: "+req.getRequestURI());
		servlet.doGet(req, res);
		AccessLog.logEnd("end FHIR get request");
			
		
		Stats.finishRequest(request(), String.valueOf(res.getStatus()));
		if (res.getContentType() != null && res.getResponseWriter() != null) {
			return status(res.getStatus(), res.getResponseWriter().toString()).as(res.getContentType());		
		}
		
		if (res.getContentType() != null && res.getResponseStream() != null) {		
			return status(res.getStatus(), res.getResponseStream().toByteArray()).as(res.getContentType());
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
	@BodyParser.Of(value = BodyParser.Raw.class)
	public Result postRoot() throws AppException, IOException, ServletException {
		return post("/");
	}
	
	/**
	 * POST action for FHIR process message
	 * @return
	 * @throws AppException
	 * @throws IOException
	 * @throws ServletException
	 */
	@MobileCall(maxtime = 60l * 1000l)
	@BodyParser.Of(value = BodyParser.Raw.class)
	public Result postProcessMessage(String p) throws AppException, IOException, ServletException {
		return post("/$process-message");
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
	@BodyParser.Of(value = BodyParser.Raw.class)
	public Result post(String all) throws AppException, IOException, ServletException {
				
		Stats.startRequest(request());
		
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		ExecutionInfo info = getExecutionInfo(req);
		UsageStatsRecorder.protokoll(info.pluginId, UsageAction.POST);   
		
		AccessLog.logBegin("begin FHIR post request: "+req.getRequestURI());
		servlet.doPost(req, res);
		AccessLog.logEnd("end FHIR post request");
		
		Stats.finishRequest(request(), String.valueOf(res.getStatus()));
		if (res.getContentType() != null && res.getResponseWriter() != null) {			
			return status(res.getStatus(), res.getResponseWriter().toString()).as(res.getContentType());		
		}
		
		if (res.getContentType() != null && res.getResponseStream() != null) {			
			byte[] bytes = res.getResponseStream().toByteArray();			
			return status(res.getStatus(), bytes).as(res.getContentType());
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
	@BodyParser.Of(value = BodyParser.Raw.class)
	public Result putRoot() throws AppException, IOException, ServletException {
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
	@BodyParser.Of(value = BodyParser.Raw.class)
	public Result put(String all) throws AppException, IOException, ServletException {
		Stats.startRequest(request());
		
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
			
		ExecutionInfo info = getExecutionInfo(req);
		UsageStatsRecorder.protokoll(info.pluginId, UsageAction.PUT);        
		
		AccessLog.log(req.getRequestURI());
		servlet.doPut(req, res);
		
		Stats.finishRequest(request(), String.valueOf(res.getStatus()));
		
		if (res.getContentType() != null && res.getResponseWriter() != null) {			
			return status(res.getStatus(), res.getResponseWriter().toString()).as(res.getContentType());		
		}
		
		if (res.getContentType() != null && res.getResponseStream() != null) {			
			byte[] bytes = res.getResponseStream().toByteArray();			
			return status(res.getStatus(), bytes).as(res.getContentType());
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
	public Result deleteRoot() throws AppException, IOException, ServletException {
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
	public Result delete(String all) throws AppException, IOException, ServletException {
		Stats.startRequest(request());
		
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		ExecutionInfo info = getExecutionInfo(req);		
		UsageStatsRecorder.protokoll(info.pluginId, UsageAction.DELETE);
		
		AccessLog.log(req.getRequestURI());
		servlet.doDelete(req, res);
		
		Stats.finishRequest(request(), String.valueOf(res.getStatus()));
		
		if (res.getContentType() != null && res.getResponseWriter() != null) {			
			return status(res.getStatus(), res.getResponseWriter().toString()).as(res.getContentType());		
		}
		
		if (res.getContentType() != null && res.getResponseStream() != null) {			
			byte[] bytes = res.getResponseStream().toByteArray();			
			return status(res.getStatus(), bytes).as(res.getContentType());
		}
		
		return status(res.getStatus());
	}
	
	@MobileCall
	@BodyParser.Of(value = BinaryFileBodyParser.class)
	public Result binaryUpload() throws AppException, IOException, ServletException {
				
		Stats.startRequest(request());
		
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
        	
		EncryptedFileHandle handle = request().body().as(EncryptedFileHandle.class);
		
		if (handle != null) {
			AccessLog.log("File handle present");
		} else {
			AccessLog.log("No file handle present");
		}
		
		ExecutionInfo info = null;
		try {
			AccessLog.log("Validating session...");
		    info = getExecutionInfo(req);
		    if (info != null) {
		    	AccessLog.log("Execution context okay.");
		    } else {
		    	AccessLog.log("Execution context missing.");
		    }
		} finally {
			if (info == null && handle != null) handle.removeAfterFailure();
		}
		if (info == null) return unauthorized();
		
		AccessLog.logBegin("begin FHIR binary post request: "+req.getRequestURI());	
		String url = handle.serializeAsURL(info.executorId);
		UnlinkedBinary file = new UnlinkedBinary();
		file._id = handle.getId();
		file.created = System.currentTimeMillis();
		file.owner = info.executorId;
		file.add();
		
		AccessLog.logEnd("end FHIR binary post request");
		
		Stats.finishRequest(request(), "201");	
		
		return created().withHeader("Location", url);
	}
}
