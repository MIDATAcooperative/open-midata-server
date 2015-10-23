package models;

import java.util.Set;

import models.enums.ConsentType;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class HCRelated extends Consent {

	public HCRelated() {
		this.type = ConsentType.HCRELATED;
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static Set<HCRelated> getByAuthorizedAndOwner(ObjectId memberId, ObjectId hcId) throws InternalServerException {
		return Model.getAll(HCRelated.class, collection, CMaps.map("authorized", memberId).map("owner", hcId).map("type", ConsentType.HCRELATED), Sets.create("name", "owner"));
	}
}
