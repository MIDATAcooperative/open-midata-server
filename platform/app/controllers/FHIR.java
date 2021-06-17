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

package controllers;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletException;

import org.bson.BSONObject;

import actions.MobileCall;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.ServiceInstance;
import models.enums.ConsentStatus;
import models.enums.UsageAction;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.AccessContext;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.PortalSessionToken;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
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
	public static FHIRServlet servlet_r4;
	public static utils.fhir_stu3.FHIRServlet servlet_stu3;
	
	/**
	 * Handling of OPTIONS requests for all pathes except root path
	 * @param all
	 * @return
	 */
	@MobileCall
	public Result checkPreflight(String all) {		
		return ok();
	}
	
	@MobileCall
	public Result checkPreflightProject(String project, String all) {		
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
	 * Determine FHIR Version to be used by looking at content-type and accept headers
	 * @return fhir version as integer (3 or 4) or 0 for unknown
	 */
	public int getFhirVersion() {
		Optional<String> contentType = request().header("Content-Type");
		if (contentType.isPresent()) {
			if (contentType.get().indexOf("fhirVersion=4.")>0) return 4;
			if (contentType.get().indexOf("fhirVersion=3.")>0) return 3;
		}
		Optional<String> accept = request().header("Accept");
		if (accept.isPresent()) {
			if (accept.get().indexOf("fhirVersion=4.")>0) return 4;
			if (accept.get().indexOf("fhirVersion=3.")>0) return 3;
		}
		return 0;
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
		
		//Stats.startRequest(request());
		PlayHttpServletRequest req = new PlayHttpServletRequest(request());
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());				
		ExecutionInfo info = getExecutionInfo(req);
        if (info != null && info.pluginId != null) {
		
        BSONObject query = RecordManager.instance.getMeta(info.context, info.targetAPS, "_query");
        if (query != null) {        	
        	Object st = query.get("study");
        	if (st instanceof Collection) st = ((Collection) st).iterator().next(); 
        	MidataId studyId = MidataId.from(st);        	
        	String studyGroup = (String) query.get("study-group");
        	Plugin plug = Plugin.getById(info.pluginId);
        	if (plug == null || (!plug.type.equals("analyzer") && !plug.type.equals("endpoint"))) throw new BadRequestException("error.invalid.plugin", "Wrong plugin type");
        	String mode = plug.pseudonymize ? "pseudonymized" : "original";
        	String handle = KeyManager.instance.currentHandle(info.executorId);
        	
        	Date from = null;
        	Date to = null;
        	try {
        	String[] dates = req.getParameterValues("_lastUpdated");
        	if (dates != null && dates.length>0) {
        		for (int i=0;i<dates.length;i++) {
        		  DateParam dal = new DateParam(dates[i]);
        		  ParamPrefixEnum prefix = dal.getPrefix();
        		  if (prefix == ParamPrefixEnum.GREATERTHAN_OR_EQUALS) {
        			  from = dal.getValue();
        		  } else if (prefix == ParamPrefixEnum.LESSTHAN) {
        			  to = dal.getValue();
        		  } else throw new BadRequestException("error.invalid", "Only 'ge' and 'lt' supported on _lastUpdated");
        		}
        	}
        	} catch (ca.uhn.fhir.parser.DataFormatException e) {
        		throw new BadRequestException("error.invalid", e.getMessage());
        	}
        	return controllers.research.Studies.downloadFHIR(info, handle, studyId, info.role, from, to, studyGroup, mode);
        }
		
		//Stats.finishRequest(request(), String.valueOf(res.getStatus()));
		
        }
		return get("/");
		
		
		
	}
	
	
	private ExecutionInfo getExecutionInfo(PlayHttpServletRequest req) throws AppException {
		
		boolean cert_direct = false;
		String valid = req.getHeader("X-Client-Valid-LB");
		if (valid == null) {
			valid = req.getHeader("X-Client-Valid");
			cert_direct = true;
		}
		if (valid != null && valid.equals("SUCCESS")) {
		   String serial = cert_direct ? req.getHeader("X-Client-Serial") : req.getHeader("X-Client-Serial-LB");
  		  
		   if (serial!=null) {
			   String[] serial2 = serial.split(",");
			   MidataId instance;
			   for (String k : serial2) if (k.startsWith("CN=")) {
				   int p = k.indexOf(".");
				   if (p<0) break;
				   instance = MidataId.from(k.substring(3,p));
				   if (instance == null) break;
				   String key = k.substring(p+1);
				   MobileAppSessionToken tk = new MobileAppSessionToken(instance, key, System.currentTimeMillis()+60000, UserRole.ANY);
				   ExecutionInfo inf = ExecutionInfo.checkMobileToken(tk, false);
				   Stats.setPlugin(inf.pluginId);
			       ResourceProvider.setExecutionInfo(inf);
			       return inf;
			   }
		   }
		}
		
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
        if (info != null && info.pluginId != null) UsageStatsRecorder.protokoll(info.pluginId, UsageAction.GET);		        
		AccessLog.logBegin("begin FHIR get request: "+req.getRequestURI());
		switch(getFhirVersion()) {
		  case 4:servlet_r4.doGet(req, res);break;
		  default: servlet_stu3.doGet(req, res);
		}
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
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public Result getRootWithEndpoint(String endpoint, String all) throws AppException, IOException, ServletException {
		return getWithEndpoint(endpoint, "/");
	}
	
	@MobileCall
	@BodyParser.Of(BodyParser.Raw.class) 
	public Result getWithEndpoint(String endpoint, String all) throws AppException, IOException, ServletException {
		Stats.startRequest(request());
		
		ServiceInstance si = ServiceInstance.getByEndpoint(endpoint, ServiceInstance.ALL);
		if (si == null || si.status != UserStatus.ACTIVE) return notFound();
					
		Set<MobileAppInstance> mi = MobileAppInstance.getByService(si._id, MobileAppInstance.APPINSTANCE_ALL);
		MobileAppInstance instance = null;
		for (MobileAppInstance inst : mi) {
			if (inst.status == ConsentStatus.ACTIVE) instance = inst;
		}
		if (instance == null) return notFound();
		
		String baseURL = "/opendata/"+si.endpoint+"/fhir";
		PlayHttpServletRequest req = new PlayHttpServletRequest(request(), baseURL);
		PlayHttpServletResponse res = new PlayHttpServletResponse(response());
				
		ExecutionInfo info = new ExecutionInfo();
				        
        KeyManager.instance.login(60000l, false);
        KeyManager.instance.unlock(RuntimeConstants.instance.publicUser, null);
        AccessContext tempContext = RecordManager.instance.createLoginOnlyContext(RuntimeConstants.instance.publicUser);
		info.executorId = instance._id;
		info.role = UserRole.ANY;
		info.overrideBaseUrl = baseURL;
        
		if (instance.sharingQuery == null) {
			instance.sharingQuery = RecordManager.instance.getMeta(tempContext, instance._id, "_query").toMap();
		}
		
		Map<String, Object> appobj = RecordManager.instance.getMeta(tempContext, instance._id, "_app").toMap();
		if (appobj.containsKey("aliaskey") && appobj.containsKey("alias")) {
			MidataId alias = new MidataId(appobj.get("alias").toString());
			byte[] key = (byte[]) appobj.get("aliaskey");
			if (appobj.containsKey("targetAccount")) {
				info.executorId = MidataId.from(appobj.get("targetAccount").toString());
			} else info.executorId = instance.owner;
			KeyManager.instance.unlock(info.executorId, alias, key);			
			RecordManager.instance.clearCache();			
		} else {
			RecordManager.instance.setAccountOwner(instance._id, instance.owner);
		}
		                                                
		info.ownerId = instance.owner;
		info.pluginId = instance.applicationId;
		info.targetAPS = instance._id;
		info.context = RecordManager.instance.createContextFromApp(info.executorId, instance);
		ResourceProvider.setExecutionInfo(info);
										
        if (info != null && info.pluginId != null) UsageStatsRecorder.protokoll(info.pluginId, UsageAction.GET);		        
		AccessLog.logBegin("begin FHIR get request: "+req.getRequestURI());
		switch(getFhirVersion()) {
		  case 3:servlet_stu3.doGet(req, res);break;
		  case 4:
		  default:
			  servlet_r4.doGet(req, res);break;
		}
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
		if (info != null && info.pluginId != null) UsageStatsRecorder.protokoll(info.pluginId, UsageAction.POST);   
		
		AccessLog.logBegin("begin FHIR post request: "+req.getRequestURI());
		switch(getFhirVersion()) {
		  case 4:servlet_r4.doPost(req, res);break;
		  default: servlet_stu3.doPost(req, res);
		}
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
		if (info != null && info.pluginId != null) UsageStatsRecorder.protokoll(info.pluginId, UsageAction.PUT);        
		
		AccessLog.log(req.getRequestURI());
		switch(getFhirVersion()) {
		  case 4:servlet_r4.doPut(req, res);break;
		  default: servlet_stu3.doPut(req, res);
		}
		
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
		if (info != null && info.pluginId != null) UsageStatsRecorder.protokoll(info.pluginId, UsageAction.DELETE);
		
		AccessLog.log(req.getRequestURI());
		switch(getFhirVersion()) {
		  case 4:servlet_r4.doDelete(req, res);break;
		  default: servlet_stu3.doDelete(req, res);
		}
		
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
