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


package utils.audit;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.MimeUtility;

import models.MidataId;
import models.enums.AuditEventType;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.servlet.PlayHttpServletRequest;

public class AuditHeaderTool {

	private static String decode(String header) {
		if (header == null) return null;
		try {
			return MimeUtility.decodeText(header);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	public static boolean createAuditEntryFromHeaders(AccessContext context, AuditEventType type, MidataId targetUserId) throws AppException {
		
		PlayHttpServletRequest request = PlayHttpServletRequest.getCurrent();
		if (request == null) return false;
		
		AuditExtraInfo extra = new AuditExtraInfo();
		
		// agent.who
		extra.practitionerReference = decode(request.getHeader("X-MIDATA-AUDIT-PRACTITIONER-REFERENCE"));
		extra.practitionerName = decode(request.getHeader("X-MIDATA-AUDIT-PRACTITIONER-NAME"));
		
		// agent.who
		extra.organizationName = decode(request.getHeader("X-MIDATA-AUDIT-ORGANIZATION-NAME"));
		extra.organizationReference = decode(request.getHeader("X-MIDATA-AUDIT-ORGANIZATION-REFERENCE"));
		
		// agent.location.display/reference
		extra.locationName = decode(request.getHeader("X-MIDATA-AUDIT-LOCATION-NAME"));
		extra.locationReference = decode(request.getHeader("X-MIDATA-AUDIT-LOCATION-REFERENCE"));
		
		// agent.purposeOfUse CC
		extra.purposeCoding = decode(request.getHeader("X-MIDATA-AUDIT-PURPOSE-CODING"));
		extra.purposeName = decode(request.getHeader("X-MIDATA-AUDIT-PURPOSE-NAME"));
		
		if (extra.isUsed()) {
			AuditManager.instance.addAuditEvent(
				AuditEventBuilder
				  .withType(type)
				  .withModifiedActor(context, targetUserId)
				  .withApp(context.getUsedPlugin())
				  .withExtraInfo(extra)
			 );
			
			return true;
			
		}	
		return false;
		
	}
}
