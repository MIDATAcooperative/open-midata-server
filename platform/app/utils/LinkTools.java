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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import controllers.Circles;
import models.Consent;
import models.ConsentEntity;
import models.MemberKey;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.ServiceInstance;
import models.StudyAppLink;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.LinkTargetType;
import models.enums.Permission;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import utils.access.Feature_FormatGroups;
import utils.access.Query;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class LinkTools {

	public static Consent findConsentForAppLink(MidataId targetUser, StudyAppLink link) throws InternalServerException {
		if (link.linkTargetType == LinkTargetType.SERVICE) {
			Set<MobileAppInstance> inst = MobileAppInstance.getActiveByApplicationAndOwner(link.serviceAppId, targetUser, MobileAppInstance.APPINSTANCE_ALL);
			if (inst.isEmpty()) return null;
			return inst.iterator().next();
		} else {
			Set<Consent> consents = Consent.getAllByAuthorized(link.userId, CMaps.map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN, ConsentStatus.PRECONFIRMED)).map("owner", targetUser).map("categoryCode", link.identifier), Consent.ALL);
			if (consents.isEmpty()) return null;
			return consents.iterator().next();
		}
	}
	
	public static void createConsentForAppLink(AccessContext context, StudyAppLink link, Set<MidataId> observers) throws AppException {
	   MemberKey consent = new MemberKey();
	   consent.owner = context.getAccessor();
	   consent.categoryCode = link.identifier;
	   consent.name = "??";		
	   consent.authorized = new HashSet<MidataId>();
	   consent.authorized.add(link.userId);
	   consent.status = ConsentStatus.ACTIVE;
				
	   consent.entityType = EntityType.USER;
	   consent.writes =WritePermissionType.UPDATE_AND_CREATE;	
	   consent.creatorApp = link.appId;
	   consent.creator = context.getActor();
	   //consent.organization =link.providerId;
	   consent.observers = observers;
	   if (link.what != null) {
	      consent.sharingQuery = link.what;
	   } else {
		  Plugin pl = Plugin.getById(link.appId);
		  consent.sharingQuery = convertAppQueryToConsent(pl.defaultQuery);		  
	   }
			
 	   Circles.addConsent(context, consent, true, null, false);					
		
	}
	
	public static Map<String, Object> convertAppQueryToConsent(Map<String, Object> properties) throws AppException {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, Object>> complexResult = new ArrayList<Map<String, Object>>();
		Set<String> resultContents = new HashSet<String>();
		properties = ConsentQueryTools.filterQueryForUseInConsent(properties);
		convertAppQueryToConsent(complexResult, resultContents, properties);
		if (properties.containsKey("$or")) {
			Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) properties.get("$or");
    		for (Map<String, Object> part : parts) convertAppQueryToConsent(complexResult, resultContents, part);
		}
		if (complexResult.isEmpty()) {
		    result.put("content", resultContents);
		} else {
		    if (complexResult.size() == 1 && resultContents.isEmpty()) return complexResult.get(0);
		    if (!resultContents.isEmpty()) {
		      Map<String, Object> resultPart = new HashMap<String, Object>();
		      resultPart.put("content", resultContents);
		      complexResult.add(resultPart);
		    }
		    result.put("$or", complexResult);
		}
		return result;
	}
	
	private static void convertAppQueryToConsent(List<Map<String, Object>> result, Set<String> resultContents, Map<String, Object> input) throws AppException {		
		Map<String, Object> work = new HashMap<String, Object>(input);
		work = Feature_FormatGroups.convertQueryToContents(work, false);
		if (work.containsKey("group")) {
		    Map<String, Object> resultPart = new HashMap<String, Object>();
		    resultPart.put("group", work.get("group"));
		    resultPart.put("group-system", work.get("group-system"));
		    resultPart.put("group-dynamic", work.get("group-dynamic"));
		    if (work.containsKey("group-exclude")) resultPart.put("group-exclude", work.get("group-exclude"));
		    result.add(resultPart);
		}
		if (work.containsKey("content")) {
		  Set<String> content = Query.getRestriction(work.get("content"), "content");
		  resultContents.addAll(content);
		}
	}
	
	public static List<Consent> findConsentsForResharing(AccessContext context, Consent target) throws AppException {
		AccessLog.logBegin("begin find consents for resharing context=", context.toString());
		Collection<Consent> candidates = Circles.getConsentsAuthorized(context, CMaps.map("owner", target.owner).map("status", Consent.ACTIVE_STATUS).map("allowedReshares", CMaps.map("$exists", true)), Consent.ALL);
		AccessLog.log("found ", Integer.toString(candidates.size()), " candidates");
		List<Consent> result = new ArrayList<Consent>();
		for (Consent consent : candidates) {
			if (ConsentQueryTools.isSubQuery(consent.sharingQuery, target.sharingQuery) && checkReshareToEntitiesAllowed(context, consent, target)) result.add(consent);
		}
		AccessLog.logEnd("end find consents for resharing #=", Integer.toString(result.size()));
		return result;
	}
	
	public static List<Consent> findConsentAlreadyExists(AccessContext context, Consent target) throws AppException {
		AccessLog.logBegin("begin find consent duplicates context=", context.toString());
		Collection<Consent> candidates = Circles.getConsentsAuthorized(context, CMaps.map("owner", target.owner).map("status", Consent.WRITEABLE_STATUS).map("type", target.type), Consent.ALL);
		AccessLog.log("found ", Integer.toString(candidates.size()), " candidates");
		List<Consent> result = new ArrayList<Consent>();
		for (Consent consent : candidates) {
			if (ConsentQueryTools.isSubQuery(consent.sharingQuery, target.sharingQuery)) result.add(consent);
		}
		AccessLog.logEnd("end find consent duplicates #=", Integer.toString(result.size()));
		return result;
	}
	
	public static boolean checkReshareToEntitiesAllowed(AccessContext info, Consent base, Consent target) throws InternalServerException {
		boolean isValid = true;
	
		for (MidataId auth : target.authorized) {						
			boolean partValid = false;
			if (auth.equals(target.owner)) partValid = true;
			else {
				for (ConsentEntity allowedEntity : base.allowedReshares) {
					if (allowedEntity.id.equals(auth)) partValid = true;
					else if (allowedEntity.type == EntityType.SERVICES && (target.entityType == EntityType.USERGROUP || target.entityType == EntityType.ORGANIZATION)) {
						Set<ServiceInstance> insts = ServiceInstance.getByApp(allowedEntity.id, Sets.create("_id", "status"));
						if (!insts.isEmpty()) {
							for (ServiceInstance si : insts) {
								if (si.status == UserStatus.ACTIVE && info.getCache().getByGroupAndActiveMember(auth, si._id, Permission.READ_DATA) != null) partValid = true; 
							}
						}										
					}
				}	
			}
			AccessLog.log("verified access for ",auth.toString()," success=",Boolean.toString(partValid));
			if (!partValid) isValid = false;
	    }
		
		return isValid;
	}
}
