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
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.exceptions.RequestTooLargeException;

public class Errors {

	private static void log(String action, AccessContext context, int status, String msg) {
		String session = (context != null) ? context.toString() : "-";
		AccessLog.logError(action, session, status, msg);
	}
	
	public static RuntimeException handle(String action, AccessContext context, Exception ex) {
	    try {
		    throw ex;
		} catch (BaseServerResponseException e) {
			log(action, context, 400, e.getClass().getName()+": "+e.getMessage());
			AuditManager.instance.fail(400, e.getMessage(), "error.failed");
			return e;
		} catch (RequestTooLargeException e2) {
			log(action, context, 400, e2.getClass().getName()+": "+e2.getMessage());
			throw e2;
		} catch (BadRequestException e2) {
			log(action, context, 400, e2.getClass().getName()+": "+e2.getMessage());
			AuditManager.instance.fail(400, e2.getMessage(), e2.getLocaleKey());
			return new InvalidRequestException(e2.getMessage());
		} catch (PluginException e4) {
			log(action, context, 500, e4.getClass().getName()+": "+e4.getMessage());
			AuditManager.instance.fail(500, e4.getMessage(), e4.getLocaleKey());
			ErrorReporter.reportPluginProblem(action, null, e4);
			return new InternalErrorException(e4);
		} catch (InternalServerException e3) {
			log(action, context, 500, e3.getClass().getName()+": "+e3.getMessage());
			AuditManager.instance.fail(500, e3.getMessage(), e3.getLocaleKey());
			ErrorReporter.report(action, null, e3);
			return new InternalErrorException(e3.getMessage());
		} catch (Exception e4) {
			log(action, context, 500, e4.getClass().getName()+": "+e4.getMessage());
			AuditManager.instance.fail(500, e4.getMessage(), "error.failed");
			ErrorReporter.report(action, null, e4);
			return new InternalErrorException("internal error during "+action);
		}		
	}
	
	public static RuntimeException handleAllFatal(String action, AccessContext context, Exception ex) {
	   
			log(action, context, 500, ex.getClass().getName()+": "+ex.getMessage());
			AccessLog.logException(action, ex);
			String lk = (ex instanceof AppException) ? ((AppException) ex).getLocaleKey() : "errors.internal";
			AuditManager.instance.fail(500, ex.getMessage(), lk);
			ErrorReporter.report(action, null, ex);
			return new InternalErrorException(ex.getMessage());
		
	}
	
}
