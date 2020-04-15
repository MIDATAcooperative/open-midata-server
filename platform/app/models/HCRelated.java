package models;

import java.util.Set;

import models.enums.ConsentType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * data model for consents that are created if a healthcare provider wants to share data with a MIDATA member.
 * Needed for records like surveys that do not contain data of the patient. 
 *
 */
public class HCRelated extends Consent {

	public HCRelated() {
		this.type = ConsentType.HCRELATED;
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static Set<HCRelated> getByAuthorizedAndOwner(MidataId memberId, MidataId hcId) throws InternalServerException {
		return Model.getAll(HCRelated.class, collection, CMaps.map("authorized", memberId).map("owner", hcId).map("type", ConsentType.HCRELATED).map("status", NOT_DELETED), Sets.create("name", "owner"));
	}
}
