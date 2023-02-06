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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import controllers.Circles;
import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.Signed;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.UserRole;
import play.libs.Json;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;
import utils.messaging.MailSenderType;
import utils.messaging.MailUtils;
import utils.messaging.SubscriptionManager;

public class ConsentQueryTools {

	private static ObjectMapper mapper;
	
	private static ObjectMapper getMapper() {
		if (mapper == null) {
			mapper = Json.mapper().copy()
			.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
		}
		return mapper;
	}
	
	public static Map<String, Object> getSharingQuery(Consent consent, boolean verify) throws AppException {
		if (consent==null) return getEmptyQuery();
		
		Map<String, Object> query = consent.sharingQuery;
		switch(consent.type) {
			case REPRESENTATIVE: query = CMaps.map("group","all").map("group-system","v1");break;
			case STUDYRELATED:
			case IMPLICIT: query = getEmptyQuery();break;
		}					
		if (query == null) query = Circles.getQueries(consent.owner, consent._id);	
		consent.sharingQuery = query;
		
		if (consent.isActive() && verify && consent.status != ConsentStatus.PRECONFIRMED) {
			if (!verifyIntegrity(consent)) {
				consent.setStatus(ConsentStatus.INVALID);
				AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.SIGNATURE_FAILURE).withModifiedUser(consent.owner).withConsent(consent));
				MailUtils.sendTextMailAsync(MailSenderType.STATUS, InstanceConfig.getInstance().getConfig().getString("errorreports.targetemail"), InstanceConfig.getInstance().getConfig().getString("errorreports.targetname"), "Consent signature failure "+consent._id+" of "+consent.owner, "Consent signature failure: type="+consent.type+" creation="+consent.dateOfCreation+" lastUpdate="+consent.lastUpdated);
				return getEmptyQuery();
			}
		}
		
		return consent.sharingQuery;
	}
	
	public static Map<String,Object> getEmptyQuery() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("content", new ArrayList<String>());
		return result;
	}
	
	public static void updateSharingQuery(AccessContext context, Consent consent, Map<String, Object> query) throws AppException {
		AccessLog.logBegin("start update sharing query consent=",consent._id.toString());
		
		consent.sharingQuery = query;
		consent.lastUpdated = new Date();
		if (consent.status == ConsentStatus.FROZEN) throw new InternalServerException("error.internal", "Query cannot be changed for frozen consents");		
		// TODO PRECONFIRMED
		if (consent.status == ConsentStatus.ACTIVE) {
			AccessContext validAccessContext = ApplicationTools.actAsRepresentative(context, consent.owner, false);
			updateSignature(validAccessContext, consent);
			executeDataSharing(context, consent, true);
		} else removeSignature(consent);
		Circles.persistConsentMetadataChange(context, consent, false);		
		AccessLog.logEnd("end update sharing query");
	}
	
	public static void updateConsentStatusActive(AccessContext context, Consent consent) throws AppException {		
		AccessContext validAccessContext = ApplicationTools.actAsRepresentative(context, consent.owner, false);
		updateSignature(validAccessContext, consent);
		executeDataSharing(context, consent, false);		
	}
	
	public static void updateConsentStatusInactive(AccessContext context, Consent consent) throws AppException {		
		removeSignature(consent);
		disableDataSharing(consent);		
	}
	
	private static void executeDataSharing(AccessContext context, Consent consent, boolean removeOld) throws AppException {
		if (consent.status == ConsentStatus.PRECONFIRMED) return;
		
		Map<String, Object> query = getSharingQuery(consent, false);
		
		if (query!=null) {
			if (consent.type.equals(ConsentType.EXTERNALSERVICE)) {
			  AccessContext targetContext = context.forConsentReshare(consent);
			  if (removeOld) {
				  List<Record> recs = RecordManager.instance.list(UserRole.ANY, context, CMaps.map(query).map("flat", "true"), Sets.create("_id"));
				  Set<MidataId> remove = new HashSet<MidataId>();
				  for (Record r : recs)
						remove.add(r._id);
				  RecordManager.instance.unshare(targetContext, remove);
			  }
			  RecordManager.instance.shareByQuery(context, consent._id, query);				  
			} else {
			  Circles.setQuery(context, consent.owner, consent._id, query);
			  RecordManager.instance.applyQuery(context, query, consent.owner, consent, true);
			}
		}
	}
	
	private static void disableDataSharing(Consent consent) throws AppException {
		// for applications disable subscriptions
		if (consent.type.equals(ConsentType.EXTERNALSERVICE)) {
			Plugin app;
			if (consent instanceof MobileAppInstance) {
			  app = Plugin.getById(((MobileAppInstance) consent).applicationId);
			} else {
			  MobileAppInstance mai = MobileAppInstance.getById(consent._id, MobileAppInstance.APPINSTANCE_ALL);
			  app = Plugin.getById(mai.applicationId);
			}
			
			SubscriptionManager.deactivateSubscriptions(consent.owner, app, consent._id);				

		} else {
			Circles.removeQueries(consent.owner, consent._id);
		}
	}
	
	
	
	private static boolean verifyIntegrity(Consent consent) throws InternalServerException {
		String query = getSharingQueryString(consent);
		AccessLog.log("consent="+consent._id+" signature="+consent.querySignature+" doc="+consent.dateOfCreation+" lu="+consent.lastUpdated);
		//AccessLog.log("CHECK Sign ow="+consent.owner.toString()+" lu="+consent.lastUpdated.getTime()+" qu="+query);
		if (!consent.querySignature.check(consent._id, query, consent.owner, consent.dateOfCreation.getTime(), consent.lastUpdated.getTime())) {
			AccessLog.log("failed signature check for consent="+consent._id+" owner="+consent.owner.toString());
			return false;			
		}
		return true;
	}
	
	public static void updateSignature(AccessContext validAccessContext, Consent consent) throws InternalServerException, AuthException {
		AccessLog.log("update consent signature consent="+consent._id);
		if (!validAccessContext.getAccessor().equals(consent.owner)) throw new InternalServerException("error.internal", "Accessor is not consent owner");
		String query = getSharingQueryString(consent);
		//AccessLog.log("CREATE Sign ow="+consent.owner.toString()+" lu="+consent.lastUpdated.getTime()+" qu="+query);
		consent.querySignature = new Signed(consent._id, query, consent.owner, validAccessContext.getActor(), consent.lastUpdated.getTime());
	}
	
	public static void temporarySignature(Consent consent) throws InternalServerException, AuthException {				
		String query = getSharingQueryString(consent);
		consent.querySignature = new Signed(consent._id, query, RuntimeConstants.systemSignatureUser, RuntimeConstants.systemSignatureUser, consent.lastUpdated.getTime());
	}
	
	public static void removeSignature(Consent consent) {
		AccessLog.log("remove consent signature consent="+consent._id);
		consent.querySignature = null;
	}
	
	private static String getSharingQueryString(Consent consent) {
		ObjectMapper map = getMapper();
		try {
			String query = map.writer().writeValueAsString(map.valueToTree(consent.sharingQuery));			
			String writeMode = consent.writes.toString();
			String validUntil = consent.validUntil != null ? Long.toHexString(consent.validUntil.getTime()) : "null";
			String createdBefore = consent.createdBefore != null ? Long.toHexString(consent.createdBefore.getTime()) : "null";
			return query+"|"+writeMode+"|"+validUntil+"|"+createdBefore;
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Map<String, Object> getVerifiedSharingQuery(MidataId consentId) throws AppException {
		Consent c = Consent.getByIdUnchecked(consentId, Consent.SMALL);
		
		return getSharingQuery(c, true);
	}
	
}
