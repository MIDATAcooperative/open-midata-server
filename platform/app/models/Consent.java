package models;

import java.util.Map;
import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;

import org.bson.types.ObjectId;

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
	public ObjectId owner;
	
	/**
	 * a public name for this consent
	 */
	public String name;	
	
	/**
	 * a set containing the ids of all entities that are authorized to access the data shared with this consent.
	 */
	public Set<ObjectId> authorized;
	
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
	
	public static Consent getByIdAndOwner(ObjectId consentId, ObjectId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("owner", ownerId), fields);
	}
	
	public static Consent getByIdAndAuthorized(ObjectId consentId, ObjectId executorId, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("authorized", executorId), fields);
	}
	
	public static Consent getByOwnerAndPasscode(ObjectId ownerId, String passcode, Set<String> fields) throws InternalServerException {
		return Model.get(Consent.class, collection, CMaps.map("owner", ownerId).map("passcode", passcode), fields);
	}
	
	public static Set<Consent> getAllByOwner(ObjectId owner, Map<String, Object> properties,  Set<String> fields) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map(properties).map("owner", owner), fields);
	}
	
	public static Set<Consent> getAllByAuthorized(ObjectId member) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member), Sets.create("name", "order", "owner"));
	}
	
	public static Set<Consent> getAllByAuthorizedAndOwners(ObjectId member, Set<ObjectId> owners) throws InternalServerException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owners), Sets.create("name", "order", "owner"));
	}
	
	public static void set(ObjectId consentId, String field, Object value) throws InternalServerException {
		Model.set(Consent.class, collection, consentId, field, value);
	}
	
	public static boolean existsByOwnerAndName(ObjectId owner, String name) throws InternalServerException {
		return Model.exists(Consent.class, collection, CMaps.map("owner", owner).map("name", name));
	}
	
	public void setStatus(ConsentStatus status) throws InternalServerException {
		this.status = status;
		Model.set(Consent.class, collection, this._id, "status", status);
	}
	
	public void add() throws InternalServerException {}
}
