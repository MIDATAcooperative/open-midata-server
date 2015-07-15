package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.MemberKeyStatus;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;

public class MemberKey extends Model {
	
	private static final String collection = "memberkeys";

	public ObjectId owner;
	public ObjectId organization;
	public Set<ObjectId> authorized;
	public MemberKeyStatus status;	
	//public Map<String,String> key; //key used to identify this member
	public Date confirmDate;
	public String comment;
	public ObjectId aps;
	public String name;
	
	public static MemberKey getById(ObjectId id) throws ModelException {
		return Model.get(MemberKey.class, collection, CMaps.map("_id", id), Sets.create("owner", "organization", "authorized", "status", "confirmDate", "aps", "comment"));
	}
	
	public static MemberKey getByOwnerAndAuthorizedPerson(ObjectId ownerId, ObjectId authorizedId) throws ModelException {
		return Model.get(MemberKey.class, collection, CMaps.map("owner", ownerId).map("authorized", authorizedId), Sets.create("owner", "organization", "authorized", "status", "confirmDate", "aps", "comment"));
	}
	
	public static Set<MemberKey> getByAuthorizedPerson(ObjectId authorizedId, Set<String> fields) throws ModelException {
		return Model.getAll(MemberKey.class, collection, CMaps.map("authorized", authorizedId), fields);
	}
	
	public static Set<MemberKey> getByOwner(ObjectId ownerId) throws ModelException {
		return Model.getAll(MemberKey.class, collection, CMaps.map("owner", ownerId), Sets.create("owner", "organization", "authorized", "status", "confirmDate", "aps", "comment", "name"));
	}
	
	public static void add(MemberKey memberKey) throws ModelException {
		Model.insert(collection, memberKey);
	}
	
	public void setStatus(MemberKeyStatus status) throws ModelException {
		this.status = status;
		Model.set(MemberKey.class, collection, this._id, "status", status);
	}
	
	public void setConfirmDate(Date confirmDate) throws ModelException {
		this.confirmDate = confirmDate;
		Model.set(MemberKey.class, collection, this._id, "confirmDate", confirmDate);
	}
	
	
}
