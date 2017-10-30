package models;

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.WritePermissionType;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.IncludeNullValues;
import utils.db.NotMaterialized;
import utils.db.OrderOperations;
import utils.exceptions.InternalServerException;

/**
 * Data model class for a MIDATA consent. A consent shares a set of records of the owner of the consent
 * with other entities (users or mobile apps).
 *
 */
@JsonFilter("Consent")
public class Consent extends Model {

	protected @NotMaterialized static final String collection = "consents";
	/**
	 * constant for all fields of a consent
	 */
	public @NotMaterialized final static Set<String> ALL = Sets.create("owner", "ownerName", "name", "authorized", "entityType", "type", "status", "categoryCode", "creatorApp", "sharingQuery", "validUntil", "createdBefore", "dateOfCreation", "sharingQuery", "externalOwner", "externalAuthorized", "writes", "dataupdate");
	
	/**
	 * constant for all FHIR fields of a consent
	 */
	public @NotMaterialized final static Set<String> FHIR = Sets.create("owner", "ownerName", "name", "authorized", "entityType", "type", "status", "categoryCode", "creatorApp", "fhirConsent", "validUntil", "createdBefore", "dateOfCreation", "sharingQuery", "externalOwner", "externalAuthorized", "writes");
	
	/**
	 * When this consent was created
	 */
	public Date dateOfCreation;
	
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
	 * a set containing the emails of external (non midata) entities that are authorized by this consent.
	 */
	public Set<String> externalAuthorized;
	
	/**
	 * the email of a non midata consent owner
	 */
	public String externalOwner;
	
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
	 * FHIR representation of Consent
	 */
	public BSONObject fhirConsent;
	
	/**
	 * Sharing Query
	 */
	public Map<String, Object> sharingQuery;
	
	/**
	 * Type of write permission
	 */
	public WritePermissionType writes;
		
	
	/**
	 * Internal timestamp of last data change for faster queries
	 */
	public long dataupdate;
	
	public static Consent getByIdUnchecked(MidataId consentId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId), fields);
	}
	
	public static Consent getByIdAndOwner(MidataId consentId, MidataId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("owner", ownerId), fields);
	}
	
	public static Consent getByIdAndAuthorized(MidataId consentId, MidataId executorId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("authorized", executorId), fields);
	}
	
	public static Set<Consent> getByIdsAndAuthorized(Set<MidataId> consentIds, MidataId executorId, Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("_id", consentIds).map("authorized", executorId), fields);
	}
	
	public static Consent getByOwnerAndPasscode(MidataId ownerId, String passcode, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("owner", ownerId).map("passcode", passcode), fields);
	}
	
	public static Set<Consent> getAllByOwner(MidataId owner, Map<String, Object> properties,  Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("owner", owner), fields, limit);
	}
	
	public static Set<Consent> getAllActiveByAuthorized(MidataId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("status", Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)), Sets.create("name", "order", "owner", "ownerName", "type", "writes", "sharingQuery"));
	}
	
	public static Set<Consent> getAllActiveByAuthorized(MidataId member, long since) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("status", Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)).map("dataupdate", CMaps.map("$gte", since)), Sets.create("name", "order", "owner", "ownerName", "type", "writes", "sharingQuery"));
	}
	
	public static Set<Consent> getAllByAuthorized(MidataId member, Map<String, Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("authorized", member), fields);
	}
	
	public static Set<Consent> getAllByAuthorized(Set<MidataId> member, Map<String, Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("authorized", member), fields, limit);
	}
	
	public static Set<Consent> getAllByAuthorized(MidataId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member), Sets.create("name", "order", "owner", "ownerName", "type", "status", "writes", "sharingQuery"));
	}
	
	public static Set<Consent> getAllActiveByAuthorizedAndOwners(MidataId member, Set<MidataId> owners) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owners).map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)), Sets.create("name", "order", "owner", "ownerName", "type", "writes", "sharingQuery"));
	}
	
	public static Set<Consent> getHealthcareOrResearchActiveByAuthorizedAndOwner(MidataId member, MidataId owner) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status",  Sets.createEnum(ConsentStatus.ACTIVE, ConsentStatus.FROZEN)).map("type",  EnumSet.of(ConsentType.HEALTHCARE, ConsentType.STUDYPARTICIPATION)), Sets.create("name", "order", "owner", "type", "writes", "sharingQuery"));
	}
	
	public static Set<Consent> getByExternalEmail(String emailLC) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("externalAuthorized", emailLC), FHIR);
	}
	
	public static Set<Consent> getByExternalOwnerEmail(String emailLC) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("externalOwner", emailLC), FHIR);
	}
	
	public static Consent getMessagingActiveByAuthorizedAndOwner(MidataId member, MidataId owner) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status",  ConsentStatus.ACTIVE).map("type",  ConsentType.IMPLICIT), Sets.create("name", "order", "owner", "ownerName", "type", "writes"));
	}
		
	public static void set(MidataId consentId, String field, Object value) throws InternalServerException {
		Model.set(Consent.class, collection, consentId, field, value);
	}
	
	public static void updateTimestamp(Set<MidataId> consentIds, long min, long value) throws InternalServerException {
		Model.setAll(Consent.class, collection, CMaps.map("_id", consentIds).map("dataupdate", CMaps.map("$lt", min)), "dataupdate", value);
	}
	
	public static boolean existsByOwnerAndName(MidataId owner, String name) throws InternalServerException {
		return Model.exists(Consent.class, collection, CMaps.map("owner", owner).map("name", name));
	}
	
	public void setStatus(ConsentStatus status) throws InternalServerException {
		this.status = status;		
		Model.set(Consent.class, collection, this._id, "status", status);
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
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
}
