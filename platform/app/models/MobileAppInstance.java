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

package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import utils.PasswordHash;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model class for a consent between a MIDATA member and a mobile application.
 *
 */
@JsonFilter("MobileAppInstance")
public class MobileAppInstance extends Consent {

	public @NotMaterialized final static Set<String> APPINSTANCE_ALL = Sets.create(Consent.FHIR, "applicationId", "appVersion", "licence", "serviceId", "deviceId", "passcode", "sharingQuery", "comment");
	
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
	
	/**
	 * First 3 characters of device string
	 */
	public String deviceId;
	
	/**
	 * Additional comment like organization linkage
	 */
	public String comment;
		
	
	public MobileAppInstance() {
		this.type = ConsentType.EXTERNALSERVICE;
		this.status = ConsentStatus.UNCONFIRMED;
		this.authorized = new HashSet<MidataId>();
	}
	
	public void add() throws InternalServerException {
		assertNonNullFields();
		Model.insert(collection, this);		
	}
	
	public void updateLoginInfo() throws InternalServerException {		
		this.setMultiple(collection, Sets.create("publicKey", "passcode", "lastUpdated"));		
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
		return Model.getAll(MobileAppInstance.class, collection, CMaps.map("serviceId", serviceId).map("status", NOT_DELETED).map("reportedStatus", CMaps.map("$exists", false)), fields);
	}
	
	public static MobileAppInstance getByIdUncheckedAlsoDeleted(MidataId consentId, Set<String> fields) throws InternalServerException {
		return Model.get(MobileAppInstance.class, collection, CMaps.map("_id", consentId), fields);
	}
	
	public static String hashDeviceId(String deviceId) throws InternalServerException {
		try {
			return PasswordHash.createInsecureQuickHash(deviceId);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeySpecException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
	}
}
