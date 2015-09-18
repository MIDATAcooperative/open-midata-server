package models;

import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;

public class Consent extends Model {

	protected static final String collection = "consents";
	
	public ObjectId owner;	
	public String name;	
	public Set<ObjectId> authorized;
	public ConsentType type;
	public ConsentStatus status;
	
	public String passcode;
	
	public static Consent getByIdAndOwner(ObjectId consentId, ObjectId ownerId, Set<String> fields) throws ModelException {
		return Model.get(Consent.class, collection, CMaps.map("_id", consentId).map("owner", ownerId), fields);
	}
	
	public static Consent getByOwnerAndPasscode(ObjectId ownerId, String passcode, Set<String> fields) throws ModelException {
		return Model.get(Consent.class, collection, CMaps.map("owner", ownerId).map("passcode", passcode), fields);
	}
	
	public static Set<Consent> getAllByAuthorized(ObjectId member) throws ModelException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member), Sets.create("name", "order", "owner"));
	}
	
	public static Set<Consent> getAllByAuthorizedAndOwners(ObjectId member, Set<ObjectId> owners) throws ModelException {
		return Model.getAll(Consent.class, collection, CMaps.map("authorized", member).map("owner", owners), Sets.create("name", "order", "owner"));
	}
	
	public static void set(ObjectId consentId, String field, Object value) throws ModelException {
		Model.set(Consent.class, collection, consentId, field, value);
	}
	
	public static boolean existsByOwnerAndName(ObjectId owner, String name) throws ModelException {
		return Model.exists(Consent.class, collection, CMaps.map("owner", owner).map("name", name));
	}
	
	public void add() throws ModelException {}
}
