package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;

import models.MidataId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * data model for a consent between a MIDATA member and a healthcare professional.
 *
 */
public class MemberKey extends Consent {
		
    /**
     * constant with a fields of this type of consent
     */
	public @NotMaterialized final static Set<String> ALL = Sets.create("_id", "name", "owner", "organization", "authorized", "type", "status", "confirmDate", "comment");
	
	/**
	 * id of organization of the healthcare provider
	 */
	public MidataId organization;	

	/**
	 * date of confirmation of this consent
	 */
	public Date confirmDate;
	
	/**
	 * comment about this consent
	 */
	public String comment;	
	
	public MemberKey() {
		this.type = ConsentType.HEALTHCARE;
	}
	
	public static MemberKey getById(MidataId id) throws InternalServerException {
		return Model.get(MemberKey.class, collection, CMaps.map("_id", id), ALL);
	}
	
	public static Set<MemberKey> getByOwnerAndAuthorizedPerson(MidataId ownerId, MidataId authorizedId) throws InternalServerException {
		return Model.getAll(MemberKey.class, collection, CMaps.map("owner", ownerId).map("authorized", authorizedId).map("type",  ConsentType.HEALTHCARE), ALL);
	}
	
	public static Set<MemberKey> getByAuthorizedPerson(MidataId authorizedId, Set<String> fields) throws InternalServerException {
		return Model.getAll(MemberKey.class, collection, CMaps.map("authorized", authorizedId).map("type", ConsentType.HEALTHCARE), fields);
	}
	
	public static Set<MemberKey> getByOwner(MidataId ownerId) throws InternalServerException {
		return Model.getAll(MemberKey.class, collection, CMaps.map("owner", ownerId).map("type",  ConsentType.HEALTHCARE), ALL);
	}
	
	public static MemberKey getByIdAndOwner(MidataId consentId, MidataId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(MemberKey.class, collection, CMaps.map("_id", consentId).map("owner", ownerId), fields);
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
		
	
	public void setConfirmDate(Date confirmDate) throws InternalServerException {
		this.confirmDate = confirmDate;
		Model.set(MemberKey.class, collection, this._id, "confirmDate", confirmDate);
	}
	
	
}
