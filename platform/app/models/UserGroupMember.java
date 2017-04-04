package models;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.ConsentStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Membership relation between user groups and users
 *
 */
@JsonFilter("UserGroupMember")
public class UserGroupMember extends Model {
	
	protected static final @NotMaterialized String collection = "groupmember";
	public static final @NotMaterialized Set<String> ALL = Sets.create("userGroup", "member", "status", "user", "startDate", "endDate");

	/**
	 * Id of user group a group member belongs to
	 */
	public MidataId userGroup;	
	
	/**
	 * Id of member who belongs to the user group
	 */
	public MidataId member;
	
	/**
	 * Status of membership
	 */
	public ConsentStatus status;
	
	/**
	 * Start date of membership
	 */
	public Date startDate;
	
	/**
	 * End date of membership
	 */
	public Date endDate;
	
	
	@NotMaterialized
	public User user;
		
			
	public static Set<UserGroupMember> getAllByMember(MidataId member) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("member", member), ALL);
	}
	
	public static Set<UserGroupMember> getAllActiveByMember(MidataId member) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("member", member).map("status", ConsentStatus.ACTIVE), ALL);
	}
	
	public static Set<UserGroupMember> getAllByGroup(MidataId group) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("userGroup", group), ALL);
	}
	
	public static UserGroupMember getByGroupAndMember(MidataId group, MidataId member) throws InternalServerException {
		return Model.get(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("member", member), ALL);
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public void delete() throws InternalServerException {			
		Model.delete(UserGroupMember.class, collection, CMaps.map("_id", _id));
	}
	
	public static void set(MidataId userId, String field, Object value) throws InternalServerException {
		Model.set(UserGroupMember.class, collection, userId, field, value);
	}
}
