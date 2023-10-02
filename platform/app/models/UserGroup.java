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
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.EntityType;
import models.enums.UserGroupType;
import models.enums.UserStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * A group of users sharing permissions
 *
 */
@JsonFilter("UserGroup")
public class UserGroup extends Model implements Actor {

	protected static final @NotMaterialized String collection = "usergroups";
	public static final @NotMaterialized Set<String> ALL = Sets.create("name", "registeredAt", "status", "type", "creator", "searchable", "protection");
	public static final @NotMaterialized Set<String> FHIR = Sets.create("fhirGroup");

	
	/**
	 * Name of the group
	 */
	public String name; 
	
	/**
	 * Name of the group in lower case
	 */
	public String nameLC;
			
	/**
	 * Timestamp of registration
	 */
	public Date registeredAt; 
				
	/**
	 * Status of user account
	 */
	public UserStatus status;
	
	/**
	 * Type of user group
	 */
	public UserGroupType type;
	
	/**
	 * Language to be used
	 */
	public String language;
	
	/**
	 * Group may be found by a group search function
	 */
	public boolean searchable;
	
	/**
	 * Use of group membership requires confirmation by second member 
	 */
	public boolean protection;
	
	/**
	 * for protected
	 */
	@NotMaterialized public Date currentUserAccessUntil;
	
	/**
	 * If set : This is a test group registered by this developer
	 */
	public MidataId developer;
	
	/**
	 * Who created the user group
	 */
	public MidataId creator;
			
	/**
	 * lower case words from name and address for indexing for improved search speed
	 */
	public Set<String> keywordsLC;
			
	/**
	 * Public key of group
	 */
	public byte[] publicKey;
		
	
	/**
	 * FHIR representation of UserGroup
	 */
	public BSONObject fhirGroup;
		
		
	/**
	 * Queries for sharing records with consents
	 * 
	 * Map consent id to query map.
	 */
	public Map<String, Map<String, Object>> queries;
	
	
	protected String getCollection() {
		return "usergroups";
	}
		
	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(UserGroup.class, collection, properties);
	}
			
	public void set(String field, Object value) throws InternalServerException {
		Model.set(this.getClass(), getCollection(), this._id, field, value);
	}
	
	public static UserGroup getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(UserGroup.class, collection, CMaps.map("_id", id), fields);
	}
		
	public static Set<UserGroup> getAllUserGroup(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(UserGroup.class, collection, properties, fields);
	}
	
	public static void set(MidataId userId, String field, Object value) throws InternalServerException {
		Model.set(UserGroup.class, collection, userId, field, value);
	}
		
	
	public static void delete(MidataId userId) throws InternalServerException {			
		Model.delete(UserGroup.class, collection, CMaps.map("_id", userId));
	}	
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static long count() throws AppException {
		return Model.count(UserGroup.class, collection, CMaps.map());
	}

	@Override
	public byte[] getPublicKey() {
		return publicKey;
	}

	@Override
	public MidataId getId() {
		return _id;
	}

	@Override
	public String getResourceType() {
		if (this.type == UserGroupType.ORGANIZATION) return "Organization";
		return "Group";
	}

	@Override
	public EntityType getEntityType() {
		if (this.type == UserGroupType.ORGANIZATION) return EntityType.ORGANIZATION;
		return EntityType.USERGROUP;
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	@Override
	public String getPublicIdentifier() {
		return "#"+_id.toString();
	}
}
