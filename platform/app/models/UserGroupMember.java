/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.ResearcherRole;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Membership relation between user groups and users
 *
 */
@JsonFilter("UserGroupMember")
public class UserGroupMember extends Model implements Comparable<Model> {
	
	protected static final @NotMaterialized String collection = "groupmember";
	public static final @NotMaterialized Set<String> ALL = Sets.create("userGroup", "member", "entityType", "status", "user", "entityName", "startDate", "endDate", "role", "confirmedUntil");

	/**
	 * Id of user group a group member belongs to
	 */
	public MidataId userGroup;	
	
	/**
	 * Id of member who belongs to the user group
	 */
	public MidataId member;
	
	/**
	 * Type of member: user or service
	 */
	public EntityType entityType;
	
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
	
	/**
	 * Membership is confirmed until which date; required for protected user groups
	 */
	public Date confirmedUntil;
	
	/**
	 * Who confirmed the membership; required for protected user groups
	 */
	public MidataId confirmedBy; 
	
	/**
	 * Role for study access
	 */
	public ResearcherRole role;
	
	public ResearcherRole getRole() {	    
		if (role != null) return role;
		role = ResearcherRole.HC();		
		return role;
	}
	
	public ResearcherRole getConfirmedRole() {
	    if (confirmedUntil != null && confirmedUntil.before(new Date(System.currentTimeMillis()))) return ResearcherRole.UNCONFIRMED();
		return getRole();
	}
	
	
	@NotMaterialized
	public User user;
	
	@NotMaterialized
	public String entityName;
	
	public static UserGroupMember getById(MidataId id) throws InternalServerException {
		return Model.get(UserGroupMember.class, collection, CMaps.map("_id", id), ALL);
	}
			
	public static Set<UserGroupMember> getAllByMember(MidataId member) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("member", member), ALL);
	}
	
	public static Set<UserGroupMember> getAllActiveByMember(MidataId member) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("member", member).map("status", ConsentStatus.ACTIVE), ALL);
	}
	
	public static Set<UserGroupMember> getAllActiveByMember(Set<MidataId> members) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("member", members).map("status", ConsentStatus.ACTIVE), ALL);
	}
	
	public static Set<UserGroupMember> getAllByGroup(MidataId group) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("userGroup", group), ALL);
	}
	
	public static Set<UserGroupMember> getAllByGroup(MidataId group, Set<EntityType> type) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("entityType", type), ALL);
	}
	
	public static Set<UserGroupMember> getAllUserByGroup(MidataId group) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("entityType", Sets.create(EntityType.USER, null)), ALL);
	}
	
	public static Set<UserGroupMember> getAllActiveByGroup(MidataId group) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("status", ConsentStatus.ACTIVE), ALL);
	}
	
	public static Set<UserGroupMember> getAllActiveUserByGroup(MidataId group) throws InternalServerException {
		return Model.getAll(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("status", ConsentStatus.ACTIVE).map("entityType", Sets.create(EntityType.USER, null)), ALL);
	}
	
	public static UserGroupMember getByGroupAndMember(MidataId group, MidataId member) throws InternalServerException {
		return Model.get(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("member", member), ALL);
	}
	
	public static UserGroupMember getByGroupAndActiveMember(MidataId group, MidataId member) throws InternalServerException {
		return Model.get(UserGroupMember.class, collection, CMaps.map("userGroup", group).map("member", member).map("status", ConsentStatus.ACTIVE), ALL);
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
