/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import controllers.Circles;
import models.Consent;
import models.MemberKey;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.StudyAppLink;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.LinkTargetType;
import models.enums.WritePermissionType;
import utils.access.Feature_FormatGroups;
import utils.access.Query;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;

public class LinkTools {

	public static Consent findConsentForAppLink(MidataId targetUser, StudyAppLink link) throws InternalServerException {
		if (link.linkTargetType == LinkTargetType.SERVICE) {
			Set<MobileAppInstance> inst = MobileAppInstance.getActiveByApplicationAndOwner(link.serviceAppId, targetUser, MobileAppInstance.APPINSTANCE_ALL);
			if (inst.isEmpty()) return null;
			return inst.iterator().next();
		} else {
			Set<Consent> consents = Consent.getAllByAuthorized(link.userId, CMaps.map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)).map("owner", targetUser).map("categoryCode", link.identifier), Consent.ALL);
			if (consents.isEmpty()) return null;
			return consents.iterator().next();
		}
	}
	
	public static void createConsentForAppLink(MidataId targetUser, StudyAppLink link, Set<MidataId> observers) throws AppException {
	   MemberKey consent = new MemberKey();
	   consent.owner = targetUser;
	   consent.categoryCode = link.identifier;
	   consent.name = "??";		
	   consent.authorized = new HashSet<MidataId>();
	   consent.authorized.add(link.userId);
	   consent.status = ConsentStatus.ACTIVE;
				
	   consent.entityType = EntityType.USER;
	   consent.writes =WritePermissionType.UPDATE_AND_CREATE;	
	   consent.creatorApp = link.appId;
	   consent.organization =link.providerId;
	   consent.observers = observers;
	   if (link.what != null) {
	      consent.sharingQuery = link.what;
	   } else {
		  Plugin pl = Plugin.getById(link.appId);
		  consent.sharingQuery = convertAppQueryToConsent(pl.defaultQuery);		  
	   }
			
 	   Circles.addConsent(targetUser, consent, true, null, false);					
		
	}
	
	public static Map<String, Object> convertAppQueryToConsent(Map<String, Object> properties) throws AppException {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("content", new HashSet<String>());
		convertAppQueryToConsent(result, properties);
		if (properties.containsKey("$or")) {
			Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) properties.get("$or");
    		for (Map<String, Object> part : parts) convertAppQueryToConsent(result, part);
		}
		
		return result;
	}
	
	private static void convertAppQueryToConsent(Map<String, Object> result, Map<String, Object> input) throws AppException {		
		Map<String, Object> work = new HashMap<String, Object>(input);
		Feature_FormatGroups.convertQueryToContents(work);
		if (work.containsKey("content")) {
		  Set<String> content = Query.getRestriction(work.get("content"), "content");
		  ((Set<String>) result.get("content")).addAll(content);
		}
	}
}
