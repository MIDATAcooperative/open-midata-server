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

package utils;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import utils.audit.AuditManager;
import utils.auth.PortalSessionToken;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.DoNotLogError;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.exceptions.RequestTooLargeException;
import play.mvc.Http.Request;

public class Errors {

	private static void log(String action, String path, AccessContext context, int status, String msg) {
		String session = (context != null) ? context.toString() : "-";
		AccessLog.logError(action+(path != null ? (" "+path) : ""), session, status, msg);
	}
	
	public static RuntimeException handle(String action, AccessContext context, Exception ex) {
		return handle(action, context, null, ex);
	}
	
	public static RuntimeException handleRequest(String action, Request request, Exception ex) {
		return handle(action, null, request, ex);
	}
	
	public static RuntimeException handle(String action, AccessContext context, Request request, Exception ex) {
		if (context == null) context = ContextManager.instance.currentForErrorReporting();
		
		String path = null;
		if (request != null) {
			   path = "["+request.method()+"] "+request.host()+request.path();
		}
		
	    try {
		    throw ex;
		} catch (BaseServerResponseException e) {			
			AuditManager.instance.fail(400, e.getMessage(), "error.failed");
			if (! (e instanceof DoNotLogError)) {
				log(action, path, context, 400, e.getClass().getName()+": "+e.getMessage());	
			}			
			return e;
		} catch (RequestTooLargeException e2) {
			log(action, path, context, 400, e2.getClass().getName()+": "+e2.getMessage());
			throw e2;
		} catch (BadRequestException e2) {
			log(action, path, context, 400, e2.getClass().getName()+": "+e2.getMessage());
			AuditManager.instance.fail(400, e2.getMessage(), e2.getLocaleKey());
			return new InvalidRequestException(e2.getMessage());
		} catch (PluginException e4) {
			log(action, path, context, 500, e4.getClass().getName()+": "+e4.getMessage());
			AuditManager.instance.fail(500, e4.getMessage(), e4.getLocaleKey());
			ErrorReporter.reportPluginProblem(action, request, e4);
			return new InternalErrorException(e4);
		} catch (InternalServerException e3) {
			log(action, path, context, 500, e3.getClass().getName()+": "+e3.getMessage());
			AuditManager.instance.fail(500, e3.getMessage(), e3.getLocaleKey());
			ErrorReporter.report(action, request, e3);
			return new InternalErrorException(e3.getMessage());
		} catch (Exception e4) {
			log(action, path, context, 500, e4.getClass().getName()+": "+e4.getMessage());
			AuditManager.instance.fail(500, e4.getMessage(), "error.failed");
			ErrorReporter.report(action, request, e4);
			return new InternalErrorException("internal error during "+action);
		}		
	}
	
	public static RuntimeException handleAllFatal(String action, AccessContext context, Exception ex) {
	   
			log(action, null, context, 500, ex.getClass().getName()+": "+ex.getMessage());
			AccessLog.logException(action, ex);
			String lk = (ex instanceof AppException) ? ((AppException) ex).getLocaleKey() : "errors.internal";
			AuditManager.instance.fail(500, ex.getMessage(), lk);
			ErrorReporter.report(action, null, ex);
			return new InternalErrorException(ex.getMessage());
		
	}
	
}
