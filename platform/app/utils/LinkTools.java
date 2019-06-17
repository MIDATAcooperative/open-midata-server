package utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import controllers.Circles;
import models.Consent;
import models.MemberKey;
import models.MidataId;
import models.Plugin;
import models.StudyAppLink;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.WritePermissionType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;

public class LinkTools {

	public static Consent findConsentForAppLink(MidataId targetUser, StudyAppLink link) throws InternalServerException {
		Set<Consent> consents = Consent.getAllByAuthorized(link.userId, CMaps.map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)).map("owner", targetUser).map("categoryCode", link.identifier), Consent.ALL);
		if (consents.isEmpty()) return null;
		return consents.iterator().next();
	}
	
	public static void createConsentForAppLink(MidataId targetUser, StudyAppLink link) throws AppException {
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
	   if (link.what != null) {
	      consent.sharingQuery = link.what;
	   } else {
		  Plugin pl = Plugin.getById(link.appId);
		  consent.sharingQuery = pl.defaultQuery;		  
	   }
			
 	   Circles.addConsent(targetUser, consent, true, null, false);					
		
	}
}
