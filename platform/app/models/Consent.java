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

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.WritePermissionType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.IncludeNullValues;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * Data model class for a MIDATA consent. A consent shares a set of records of the owner of the consent
 * with other entities (users or mobile apps).
 *
 */
@JsonFilter("Consent")
public class Consent extends Model implements Comparable<Consent> {

	protected @NotMaterialized static final String collection = "consents";
	/**
	 * constant for all fields of a consent
	 */
	public @NotMaterialized final static Set<String> ALL = Sets.create("owner", "ownerName", "name", "authorized", "entityType", "type", "status", "categoryCode", "creatorApp", "sharingQuery", "validUntil", "createdBefore", "createdAfter", "dateOfCreation", "sharingQuery", "querySignature", "externalOwner", "externalAuthorized", "writes", "dataupdate", "lastUpdated", "observers", "creator", "externals", "allowedReshares");
	
	public @NotMaterialized final static Set<String> SMALL = Sets.create("owner", "ownerName", "name", "entityType", "type", "status", "categoryCode", "creatorApp", "sharingQuery", "validUntil", "createdBefore", "createdAfter", "dateOfCreation", "sharingQuery", "querySignature", "externalOwner", "writes", "dataupdate", "lastUpdated");
	
	/**
	 * constant for all FHIR fields of a consent
	 */
	public @NotMaterialized final static Set<String> FHIR = Sets.create(ALL, "fhirConsent");
	
	public @NotMaterialized final static Set<ConsentStatus> NOT_DELETED = Collections.unmodifiableSet(EnumSet.of(ConsentStatus.ACTIVE, ConsentStatus.DRAFT, ConsentStatus.EXPIRED, ConsentStatus.FROZEN, ConsentStatus.REJECTED, ConsentStatus.INVALID, ConsentStatus.UNCONFIRMED, ConsentStatus.PRECONFIRMED));
	
	public @NotMaterialized final static Set<ConsentStatus> ACTIVE_STATUS = Collections.unmodifiableSet(Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.PRECONFIRMED));
	
	protected @NotMaterialized final static Set<ConsentStatus> SHARING_STATUS = Collections.unmodifiableSet(Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN, ConsentStatus.PRECONFIRMED));
	
	public @NotMaterialized final static Set<ConsentStatus> WRITEABLE_STATUS = Collections.unmodifiableSet(Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.PRECONFIRMED));
	/**
	 * When this consent was created
	 */
	public Date dateOfCreation;

	/**
	 * When this consent was last updated
	 */
	public Date lastUpdated;
	
	/**
	 * id of owner of this consent. The owner is the person who shares data.
	 */
	@IncludeNullValues
	public MidataId owner;
	
	/**
	 * id of app that created the consent (optional)
	 */
	public MidataId creatorApp; 
	
	/**
	 * id of user that created the consent
	 */
	public  MidataId creator; 
	
	/**
	 * a code for the category of the consent. 
	 */
	public String categoryCode;
	
	/**
	 * a public name for this consent
	 */
	public String name;	
	
	/**
	 * a set containing the ids of all entities that are authorized to access the data shared with this consent.
	 */
	public Set<MidataId> authorized;
	
	/**
	 * a set containing all ids of external service type apps having access to this consent
	 */
	public Set<MidataId> observers;
	
	/**
	 * a set containing the emails of external (non midata) entities that are authorized by this consent.
	 */
	public Set<String> externalAuthorized;
	
	/**
	 * the email of a non midata consent owner
	 */
	public String externalOwner;
	
	/**
	 * extra information about external entities
	 */
	public Map<String, ConsentExternalEntity> externals;
	
	/**
	 * Type of entity that is authorized
	 */
	public EntityType entityType;
	
	/**
	 * the type of this consent.
	 */
	public ConsentType type;
	
	/**
	 * the status of this consent. Whether it has been confirmed by the user or is expired.
	 */
	public ConsentStatus status;
	
	/**
	 * firstname and lastname of the owner of this consent. Not materialized.
	 */
	public @NotMaterialized String ownerName;
	
	/**
	 * passcode that can be given away by the owner to healthcare providers in order to add those to the consent.
	 */
	public String passcode;
	
	/**
	 * The number of records in this consent. Calculated on request only
	 */
	public @NotMaterialized int records;
	
	/**
	 * The expiration date of this consent
	 */
	public Date validUntil;
	
	/**
	 * Exclude all data created after this date
	 */
	public Date createdBefore;
	
	/**
	 * Exclude all data created before this date
	 */
	public Date createdAfter;
	
	/**
	 * FHIR representation of Consent
	 */
	public BSONObject fhirConsent;
	
	/**
	 * Sharing Query
	 */
	public Map<String, Object> sharingQuery;
	
	/**
	 * Signature for query
	 */
	public Signed querySignature;
	
	/**
	 * Type of write permission
	 */
	public WritePermissionType writes;
	
	/**
	 * For consents with external grantees the system may automatically set consent status to active once all external
	 * grantees joined the platform. This field is in none of the default sets for reading.
	 */
	public byte[] autoConfirmHandle;
		
	
	/**
	 * Internal timestamp of last data change for faster queries
	 */
	public long dataupdate;
	
	/**
	 * list of entities that may also granted access to data from this consent (reshare)
	 */
	public List<ConsentEntity> allowedReshares;
	
	/**
	 * Resharing consent that was used to create the signature for this consent
	 */
	public @NotMaterialized Consent basedOn;
	
	public static Consent getByIdUnchecked(MidataId consentId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("status", NOT_DELETED), fields);
	}
	
	public static Consent getByIdAndOwner(MidataId consentId, MidataId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("owner", ownerId).map("status", NOT_DELETED), fields);
	}
	
	public static Consent getByIdAndAuthorized(MidataId consentId, MidataId executorId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("authorized", executorId).map("status", NOT_DELETED), fields);
	}
	
	public static Set<Consent> getByIdsAndAuthorized(Set<MidataId> consentIds, MidataId executorId, Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("_id", consentIds).map("authorized", executorId).map("status", NOT_DELETED), fields);
	}
	
	public static Set<Consent> getActiveByIdsAndOwner(Set<MidataId> consentIds, MidataId executorId, Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("_id", consentIds).map("owner", executorId).map("status", ConsentStatus.ACTIVE), fields);
	}
	
	public static Consent getByOwnerAndPasscode(MidataId ownerId, String passcode, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("owner", ownerId).map("passcode", passcode).map("status", NOT_DELETED), fields);
	}
	
	public static Set<Consent> getAllByOwner(MidataId owner, Map<String, Object> properties,  Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("owner", owner).map("status", NOT_DELETED), fields, limit);
	}
	
	public static Set<Consent> getAllByObserver(MidataId observer, Map<String, Object> properties,  Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("observers", observer).map("status", NOT_DELETED), fields, limit);
	}
	
	public static Set<Consent> getAllActiveByAuthorized(MidataId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("status", SHARING_STATUS), Consent.SMALL);
	}
	
	public static Set<Consent> getAllActiveByAuthorized(MidataId member, long since) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("status", SHARING_STATUS).map("dataupdate", CMaps.map("$gte", since)), Consent.SMALL);
	}
	
	public static Set<Consent> getAllByAuthorized(MidataId member, Map<String, Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("status", NOT_DELETED).map(properties).map("authorized", member), fields);
	}
	
	public static Set<Consent> getAllByAuthorized(Set<MidataId> member, Map<String, Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("status", NOT_DELETED).map(properties).map("authorized", member), fields, limit);
	}
	
	public static Set<Consent> getAllByAuthorized(MidataId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("status", NOT_DELETED), Consent.SMALL);
	}
	
	public static Set<Consent> getAllActiveByAuthorizedAndOwners(MidataId member, Set<MidataId> owners) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owners).map("status", SHARING_STATUS), Consent.SMALL);
	}
	
	public static Set<Consent> getAllWriteableByAuthorizedAndOwner(Set<MidataId> member, MidataId owner) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status", WRITEABLE_STATUS), Consent.SMALL);
	}
	
	public static Set<Consent> getHealthcareOrResearchActiveByAuthorizedAndOwner(Set<MidataId> member, MidataId owner) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status", SHARING_STATUS).map("type",  EnumSet.of(ConsentType.HEALTHCARE, ConsentType.STUDYPARTICIPATION, ConsentType.API, ConsentType.REPRESENTATIVE)), Consent.ALL);
	}
	
	public static Set<Consent> getByExternalEmail(String emailLC) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("externalAuthorized", emailLC).map("status", NOT_DELETED), FHIR);
	}
	
	public static Set<Consent> getByExternalOwnerEmail(String emailLC) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("externalOwner", emailLC).map("status", NOT_DELETED), FHIR);
	}
	
	public static List<Consent> getSome(MidataId min) throws InternalServerException {
		if (min!=null) {
		   return Model.getAllList(Consent.class, collection, CMaps.map("_id", CMaps.map("$gt", min)), ALL, 1000, "_id", 1);
		} else {
			return Model.getAllList(Consent.class, collection, CMaps.map(), ALL, 1000, "_id", 1);
		}
	}
	
	public static Consent getMessagingActiveByAuthorizedAndOwner(MidataId member, MidataId owner) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status",  ConsentStatus.ACTIVE).map("type",  ConsentType.IMPLICIT), Consent.SMALL);
	}
	
	public static Consent getRepresentativeActiveByAuthorizedAndOwner(MidataId member, MidataId owner) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status",  ConsentStatus.ACTIVE).map("type",  ConsentType.REPRESENTATIVE), Consent.SMALL);
	}
		
	public static void set(MidataId consentId, String field, Object value) throws InternalServerException {
		Model.set(Consent.class, collection, consentId, field, value);
	}
	
	public static void updateTimestamp(Set<MidataId> consentIds, long min, long value) throws InternalServerException {
		Model.setAll(Consent.class, collection, CMaps.map("_id", consentIds).map("dataupdate", CMaps.map("$lt", min)), "dataupdate", value);
	}
	
	public static boolean existsByOwnerAndName(MidataId owner, String name) throws InternalServerException {
		return Model.exists(Consent.class, collection, CMaps.map("owner", owner).map("name", name).map("status", NOT_DELETED));
	}
	
	public void setStatus(ConsentStatus status) throws InternalServerException {
		this.status = status;	
		this.lastUpdated = new Date();
		if (this.createdAfter != null) {
		   this.setMultiple(collection, Sets.create("status", "createdAfter", "lastUpdated"));
		} else {
		   this.setMultiple(collection, Sets.create("status", "lastUpdated"));
		}
	}
	
	public void updateMetadata() throws InternalServerException {
		if (this.fhirConsent != null) {
		  assertNonNullFields();
		  this.setMultiple(collection, Sets.create("status", "lastUpdated", "fhirConsent", "sharingQuery", "writes", "querySignature"));
		} else {
		  // assert except fhirConsent which is null, but not written into the DB
		  this.fhirConsent = new BasicBSONObject();
		  assertNonNullFields();
		  this.fhirConsent = null;
		  this.setMultiple(collection, Sets.create("status", "lastUpdated", "sharingQuery", "writes", "querySignature"));
		}
	}
	
	public void add() throws InternalServerException {
		assertNonNullFields();
		Model.insert(collection, this);	
	}
	
	public void assertNonNullFields() {
		if (dateOfCreation == null ||
		    lastUpdated == null || 
            status == null ||
		    fhirConsent == null ||
		    sharingQuery == null ||
		    writes == null) throw new NullPointerException();
	}
	
	/**
	 * Delete a consent
	 * @param ownerId id of owner of consent
	 * @param consentId id of consent
	 * @throws InternalServerException
	 */
	public static void delete(MidataId ownerId, MidataId consentId) throws InternalServerException {		
		Map<String, Object> properties = CMaps.map("_id", consentId);
		Model.delete(Consent.class, collection, properties);
	}
	
	public static void touch(MidataId consentId, long version) throws InternalServerException {
		set(consentId, "dataupdate", version);
	}
	
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	
	public String getOwnerName() { 
		return ownerName;
	}

	@Override
	public int compareTo(Consent arg0) {
		int r = owner.compareTo(arg0.owner);
		return r != 0 ? r : _id.compareTo(arg0._id);
	}
	
	public static long count(ConsentType type) throws AppException {
		return Model.count(Consent.class, collection, CMaps.map("type", type));
	}
	
	public static long count(MidataId owner) throws AppException {
		return Model.count(Consent.class, collection, CMaps.map("owner", owner));
	}
	
	public static long countAuth(Set<MidataId> auth) throws AppException {
		return Model.count(Consent.class, collection, CMaps.map("authorized", auth));
	}
	
	public static List<Consent> getBroken() throws AppException {
		return Model.getAllList(Consent.class, collection, CMaps.map("fhirConsent", CMaps.map("$exists", false)), FHIR, 1000);
	}
	
	public boolean isActive() {
		return this.status == ConsentStatus.ACTIVE || this.status == ConsentStatus.PRECONFIRMED;
	}
	
	public boolean isSharingData() {
		return this.status == ConsentStatus.ACTIVE || this.status == ConsentStatus.PRECONFIRMED || this.status == ConsentStatus.FROZEN;
	}
	
	public boolean isWriteable() {
		return this.status == ConsentStatus.ACTIVE || this.status == ConsentStatus.PRECONFIRMED;
	}
	
	public void addExternalAuthorized(String email, String display) {
		if (externalAuthorized == null) externalAuthorized = new HashSet<String>();
		externalAuthorized.add(email.toLowerCase());
		if (display != null) {
			if (externals == null) externals = new HashMap<String, ConsentExternalEntity>();
			ConsentExternalEntity entity = new ConsentExternalEntity();
			entity.name = display;
			externals.put(email.toLowerCase(), entity);
		}
	}
	
	public ConsentExternalEntity getExternal(String email) {
		if (externals == null || email == null) return null;
		ConsentExternalEntity result = externals.get(email.toLowerCase());
		if (result != null) return result;
		result = externals.get(email.replace(".","[dot]").toLowerCase());
		return result;
	}
}
