package models;

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
	public static final @NotMaterialized Set<String> ALL = Sets.create("userGroup", "member", "status", "user");

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
	
	
	@NotMaterialized
	public User user;
		
			
	public static Set<UserGroupMember> getAllByMember(MidataId member) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("member", member), ALL);
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
}
