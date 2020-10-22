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

package models;

import java.util.HashSet;
import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model class for a consent between a MIDATA member and a mobile application.
 *
 */
public class MobileAppInstance extends Consent {

	public @NotMaterialized final static Set<String> APPINSTANCE_ALL = Sets.create(Consent.ALL, "applicationId", "appVersion","licence","serviceId");
	
	/**
	 * public key of the application instance
	 */
	public byte[] publicKey;
			
	/**
	 * id of the plugin
	 */
	public MidataId applicationId;
	
	/**
	 * version of plugin at time of consent creation
	 */
	public long appVersion;
	
	/**
	 * Licence
	 */
	public MidataId licence;

	/**
	 * Id of Service Instance (optional)
	 */
	public MidataId serviceId;
		
	
	public MobileAppInstance() {
		this.type = ConsentType.EXTERNALSERVICE;
		this.status = ConsentStatus.UNCONFIRMED;
		this.authorized = new HashSet<MidataId>();
	}
	
	public static void add(MobileAppInstance space) throws InternalServerException {
		Model.insert(collection, space);		
	}
	
	public static void upsert(MobileAppInstance space) throws InternalServerException {
		Model.upsert(collection, space);		
	}
	
	public static MobileAppInstance getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(MobileAppInstance.class, collection, CMaps.map("_id", id).map("type", Sets.createEnum(ConsentType.EXTERNALSERVICE, ConsentType.API)).map("status", NOT_DELETED), fields);
	}
	
	public static Set<MobileAppInstance> getByApplicationAndOwner(MidataId applicationId, MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(MobileAppInstance.class, collection, CMaps.map("applicationId", applicationId).map("owner", owner).map("status", NOT_DELETED), fields);
	}

	public static Set<MobileAppInstance> getActiveByApplicationAndOwner(MidataId applicationId, MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(MobileAppInstance.class, collection, CMaps.map("applicationId", applicationId).map("owner", owner).map("status", ConsentStatus.ACTIVE), fields);
	}
	
	public static Set<MobileAppInstance> getByOwner(MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(MobileAppInstance.class, collection, CMaps.map("owner", owner).map("type", Sets.createEnum(ConsentType.EXTERNALSERVICE, ConsentType.API)).map("status", NOT_DELETED), fields);
	}
	
	public static Set<MobileAppInstance> getByApplication(MidataId applicationId, Set<String> fields) throws InternalServerException {
		return Model.getAll(MobileAppInstance.class, collection, CMaps.map("applicationId", applicationId).map("status", NOT_DELETED), fields);
	}

	public static Set<MobileAppInstance> getByService(MidataId serviceId, Set<String> fields) throws InternalServerException {
		return Model.getAll(MobileAppInstance.class, collection, CMaps.map("serviceId", serviceId).map("status", NOT_DELETED), fields);
	}
}
