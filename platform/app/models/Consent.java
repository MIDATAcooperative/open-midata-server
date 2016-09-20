package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;

import models.MidataId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
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
	public @NotMaterialized final static Set<String> ALL = Sets.create("owner", "name", "authorized", "type", "status");
	
	/**
	 * id of owner of this consent. The owner is the person who shares data.
	 */
	public MidataId owner;
	
	/**
	 * a public name for this consent
	 */
	public String name;	
	
	/**
	 * a set containing the ids of all entities that are authorized to access the data shared with this consent.
	 */
	public Set<MidataId> authorized;
	
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
	public @NotMaterialized Date validUntil;
	
	/**
	 * Exclude all data created after this date
	 */
	public @NotMaterialized Date createdBefore;
	
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
	
	public static Set<Consent> getAllByOwner(MidataId owner, Map<String, Object> properties,  Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("owner", owner), fields);
	}
	
	public static Set<Consent> getAllActiveByAuthorized(MidataId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("status", ConsentStatus.ACTIVE), Sets.create("name", "order", "owner", "type"));
	}
	
	public static Set<Consent> getAllByAuthorized(MidataId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member), Sets.create("name", "order", "owner", "type"));
	}
	
	public static Set<Consent> getAllActiveByAuthorizedAndOwners(MidataId member, Set<MidataId> owners) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owners).map("status",  ConsentStatus.ACTIVE), Sets.create("name", "order", "owner", "type"));
	}
	
	public static Set<Consent> getHealthcareActiveByAuthorizedAndOwner(MidataId member, MidataId owner) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owner).map("status",  ConsentStatus.ACTIVE).map("type",  ConsentType.HEALTHCARE), Sets.create("name", "order", "owner", "type"));
	}
		
	public static void set(MidataId consentId, String field, Object value) throws InternalServerException {
		Model.set(Consent.class, collection, consentId, field, value);
	}
	
	public static boolean existsByOwnerAndName(MidataId owner, String name) throws InternalServerException {
		return Model.exists(Consent.class, collection, CMaps.map("owner", owner).map("name", name));
	}
	
	public void setStatus(ConsentStatus status) throws InternalServerException {
		this.status = status;
		Model.set(Consent.class, collection, this._id, "status", status);
	}
	
	public void add() throws InternalServerException {}
}
