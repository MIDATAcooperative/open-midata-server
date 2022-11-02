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

package controllers.members;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import controllers.APIController;
import controllers.Circles;
import models.Consent;
import models.HPUser;
import models.MemberKey;
import models.MidataId;
import models.User;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.UserRole;
import models.enums.UserStatus;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.ApplicationTools;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.MemberSecured;
import utils.auth.Rights;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

/**
 * functions about interaction with health providers
 *
 */
public class HealthProvider extends APIController {
		
	/**
	 * returns all consents of a member with health providers 
	 * @return list of consents (MemberKeys)
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public Result list(Request request) throws InternalServerException {
	      
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));	
		Set<MemberKey> memberkeys = MemberKey.getByOwner(userId);
		
		return ok(JsonOutput.toJson(memberkeys, "Consent", Sets.create("owner", "organization", "authorized", "status", "confirmDate", "aps", "comment", "name")));
	}
	
	/**
	 * search for health providers matching some criteria
	 * @return list of users
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result search(Request request) throws AppException, JsonValidationException {
		
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "properties", "fields");
		
		// get users
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		ObjectIdConversion.convertMidataIds(properties, "_id", "provider");		
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		Rights.chk("HealthProvider.search", getRole(), properties, fields);

		if (fields.contains("name")) { fields.add("firstname"); fields.add("lastname"); } 
						
		if (properties.containsKey("name") || properties.containsKey("city")) {
			String name = properties.containsKey("name") ? properties.get("name").toString() : null;
			String city = properties.containsKey("city") ? properties.get("city").toString() : null;
			properties.remove("name");
			properties.remove("city");
			if (name != null && city != null) {
			properties = CMaps.and(CMaps.or(CMaps.map("firstname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE)), CMaps.map("lastname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE))),
					               CMaps.or(CMaps.map("city", Pattern.compile("^"+city+"$", Pattern.CASE_INSENSITIVE)), CMaps.map("zip", Pattern.compile("^"+city+"$", Pattern.CASE_INSENSITIVE))),
					               CMaps.map("keywordsLC", name.toLowerCase())).map(properties);
			} else if (name != null) {
				properties = CMaps.and(
						        CMaps.or(CMaps.map("firstname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE)), CMaps.map("lastname", Pattern.compile("^"+name+"$", Pattern.CASE_INSENSITIVE))),
						        CMaps.map("keywordsLC", name.toLowerCase())		
						      ).map(properties);
			} else {
				properties = CMaps.or(
						       CMaps.and(
						    		 CMaps.map("keywordsLC", city.toLowerCase()), 
						             CMaps.map("city", Pattern.compile("^"+city+"$", Pattern.CASE_INSENSITIVE))
						       ), CMaps.map("zip", city)).map(properties);
			}
		}
		properties.put("role", UserRole.PROVIDER);
		properties.put("status", UserStatus.ACTIVE);
		
		List<HPUser> users = new ArrayList<HPUser>(HPUser.getAll(properties, fields));
		
		if (fields.contains("name")) {
			for (User user : users) user.name = (user.firstname + " "+ user.lastname).trim();
		}
				
		Collections.sort(users);
		return ok(JsonOutput.toJson(users, "User", fields));
	}
	
	/**
	 * accept a consent created by a health provider
	 * @return status ok
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result confirmConsent(Request request) throws AppException, JsonValidationException {
		
		//MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		JsonNode json = request.body().asJson();
		JsonValidation.validate(json, "consent");
		
		MidataId consentId = JsonValidation.getMidataId(json, "consent");		
		confirmConsent(context, consentId);
		
		AuditManager.instance.success();
		return ok();
	}
	
    public static void confirmConsent(AccessContext context, MidataId consentId) throws AppException, JsonValidationException {
										
		MemberKey target = MemberKey.getByIdAndOwner(consentId, context.getAccessor(), Consent.FHIR);
		
		if (target.type.equals(ConsentType.EXTERNALSERVICE)) {
			   //AuditManager.instance.addAuditEvent(AuditEventType.APP_, userId, target);
		} else {
			   AuditManager.instance.addAuditEvent(AuditEventType.CONSENT_APPROVED, context.getActor(), target);
		}
		
		if (target.status.equals(ConsentStatus.UNCONFIRMED) || target.status.equals(ConsentStatus.INVALID)) {
			
			if (target.externalAuthorized != null && !target.externalAuthorized.isEmpty()) {
				throw new BadRequestException("error.invalid.consent_members", "Consent has external persons.");
			}
			target.setConfirmDate(new Date());			
			Circles.consentStatusChange(context, target, ConsentStatus.ACTIVE);
			Circles.sendConsentNotifications(context.getAccessor(), target, ConsentStatus.ACTIVE);
		} else throw new BadRequestException("error.invalid.status_transition", "Wrong status");		
		
		
	}
	
	
	/**
	 * reject a consent created by a health provider
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result rejectConsent(Request request) throws AppException, JsonValidationException {
		
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		JsonNode json = request.body().asJson();
		JsonValidation.validate(json, "consent");
		
		MidataId consentId = JsonValidation.getMidataId(json, "consent");
		rejectConsent(context, userId, consentId);		
	
		return ok();
	}
	
    public static void rejectConsent(AccessContext context, MidataId userId, MidataId consentId) throws AppException, JsonValidationException {
		
		MemberKey target = MemberKey.getByIdAndOwner(consentId, userId, Consent.FHIR);
			
		if (target==null) {
			rejectConsentAsAuthorized(context, userId, consentId);
			return;
		}
		
		if (target.type.equals(ConsentType.EXTERNALSERVICE) || target.type.equals(ConsentType.API)) {
		   ApplicationTools.leaveInstalledService(context, consentId, true);		   
		} else {
		    AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.CONSENT_REJECTED).withActorUser(context.getActor()).withModifiedUser(userId).withConsent(target));
				
			if (target.status.equals(ConsentStatus.UNCONFIRMED) || target.status.equals(ConsentStatus.ACTIVE) || target.status.equals(ConsentStatus.INVALID)) {
				target.setConfirmDate(new Date());			
				Circles.consentStatusChange(context, target, ConsentStatus.REJECTED);
				Circles.sendConsentNotifications(userId, target, ConsentStatus.REJECTED);
			} else throw new BadRequestException("error.invalid.status_transition", "Wrong status");
		}
		AuditManager.instance.success();
	}
    
    public static void rejectConsentAsAuthorized(AccessContext context, MidataId userId, MidataId consentId) throws AppException, JsonValidationException {
	   Consent consent = Circles.getConsentById(context, consentId, Consent.FHIR);
	   if (consent == null) throw new BadRequestException("error.notfound.consent", "Consent not found");
	   
	   AccessContext contextConsent = context.forConsent(consent);
	   if (consent.status != ConsentStatus.ACTIVE) return;
	   AuditManager.instance.addAuditEvent(AuditEventType.CONSENT_REJECTED, userId, consent);
	   if (consent.authorized.contains(userId)) {
		   
		   if (consent.authorized.size() > 1) {
			   consent.authorized.remove(userId);		   		   		   
			   Consent.set(consent._id, "authorized", consent.authorized);
			   Consent.set(consent._id, "lastUpdated", new Date());				
			   RecordManager.instance.unshareAPSRecursive(contextConsent, consent._id, Collections.singleton(userId));
		   } else Circles.consentStatusChange(context, consent, ConsentStatus.REJECTED);
	   } else {
		   Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(userId);
		   for (UserGroupMember ugm : ugms) {
			   if (consent.authorized.contains(ugm.userGroup)) {
				   
				   if (consent.authorized.size() > 1) {
					   consent.authorized.remove(ugm.userGroup);
					   Consent.set(consent._id, "authorized", consent.authorized);
					   Consent.set(consent._id, "lastUpdated", new Date());				
					   RecordManager.instance.unshareAPSRecursive(contextConsent, consent._id, Collections.singleton(ugm.userGroup));
				   } else Circles.consentStatusChange(context, consent, ConsentStatus.REJECTED);
			   }
		   }
	   }
	   AuditManager.instance.success();
	   	   
    }
	

}
