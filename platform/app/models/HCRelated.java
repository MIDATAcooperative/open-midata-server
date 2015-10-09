package models;

import java.util.Set;

import models.enums.ConsentType;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.ModelException;

public class HCRelated extends Consent {

	public HCRelated() {
		this.type = ConsentType.HCRELATED;
	}
	
	public void add() throws ModelException {
		Model.insert(collection, this);	
	}
	
	public static Set<HCRelated> getByAuthorizedAndOwner(ObjectId memberId, ObjectId hcId) throws ModelException {
		return Model.getAll(HCRelated.class, collection, CMaps.map("authorized", memberId).map("owner", hcId).map("type", ConsentType.HCRELATED), Sets.create("name", "owner"));
	}
}
