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

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.Consent;
import models.MidataId;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditExtraInfo;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.ExecutionInfo;
import utils.context.AccessContext;
import utils.context.ActionTokenAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class TokenActions extends Controller {
	
	/**
	 * Execute action using an action-token
	 * @param request
	 * @return
	 * @throws AppException
	 */
	@APICall
	public Result action(Request request) throws AppException {
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "token");
									
		String token = JsonValidation.getString(json, "token");
		ActionTokenAccessContext context = ExecutionInfo.checkActionToken(request, token);
	    AuditEventType action = context.getActionToken().action;
	    
	    if (action == AuditEventType.CONSENT_REJECTED) { // Reject consent
	    	AuditExtraInfo extra = new AuditExtraInfo();
	    	extra.setExternalUser(context.getActionToken().handle);
		    MidataId consentId =  context.getActionToken().resourceId;
		    String mail = context.getActionToken().handle;
		    Consent consent = Consent.getByIdUnchecked(consentId, Consent.ALL);
	        AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.CONSENT_REJECTED).withConsent(consent).withExtraInfo(extra));
		    if (consent != null && consent.externalAuthorized!=null && consent.externalAuthorized.contains(mail)) {
		    	boolean wasActive = consent.isActive();
		    	consent.externalAuthorized.remove(mail);
		    	consent.lastUpdated = new Date();
				Consent.set(consent._id, "externalAuthorized", consent.externalAuthorized);
				Consent.set(consent._id, "lastUpdated", consent.lastUpdated);	
				if (consent.externalAuthorized.isEmpty() && consent.authorized.isEmpty()) {
					Circles.consentStatusChange(context, consent, ConsentStatus.REJECTED);
					Circles.sendConsentNotifications(context, consent, ConsentStatus.REJECTED, wasActive);
				} else {
					Circles.persistConsentMetadataChange(context, consent, false);
				}
		    } else throw new BadRequestException("error.invalid.token", "Token expired");
	    
	    } else if (action == AuditEventType.ACCESS_CONFIRMATION) { // Confirmation for protected UserGroups
	    	UserGroups.confirmation(context.getActionToken().userId, context.getActionToken().resourceId);
	    }
	   	AuditManager.instance.success();
       return ok();	
	}


}
