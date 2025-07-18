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

import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.PortalSessionToken;
import utils.context.ContextManager;
import utils.fhir.ResourceProvider;
import utils.servlet.PlayHttpServletRequest;

public class ServerTools {

	public static void endRequest() {
		try {
		   ContextManager.instance.clear();		   
		} finally {
		   AuditManager.instance.clear();
		   AccessLog.newRequest();	
		   ResourceProvider.setAccessContext(null);
		   PortalSessionToken.clear();
		   PlayHttpServletRequest.clear();
		}
	}
}
