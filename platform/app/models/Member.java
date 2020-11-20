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

import models.enums.ParticipationInterest;
import models.enums.UserRole;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.exceptions.InternalServerException;

/**
 * data model class for a MIDATA member
 *
 */
public class Member extends User {
	
	/**
	 * ids of visible news items. Currently not used.
	 */
	public Set<MidataId> news;
		
	
	
	/**
	 * the birthday of this member.
	 */
	public Date birthday;
	
	/**
	 * social security number. To be removed
	 */
	public String ssn; 
	
	/**
	 * general level of interest in study participation
	 */
	public ParticipationInterest partInterest;
	
	/**
	 * id of main APS. Is the same as the id of this object. Will be removed.
	 */
	public MidataId myaps;
	
	public Member() {
		role =UserRole.MEMBER;
	}
	
	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(Member.class, collection, properties);
	}
	
	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(Member.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role",  UserRole.MEMBER).map("status", NON_DELETED));
	}
	
	public static boolean existsByMidataID(String midataID) throws InternalServerException {
		return Model.exists(User.class, collection, CMaps.map("midataID", midataID));
	}
	
	public static Member getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(Member.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.MEMBER).map("status", NON_DELETED), fields);
	}
			
	public static Member getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(Member.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static Member getByIdAndApp(MidataId id, MidataId appId, Set<String> fields) throws InternalServerException {
		return Model.get(Member.class, collection, CMaps.map("_id", id).map("apps", appId), fields);
	}
	
	public static Member getByIdAndVisualization(MidataId id, MidataId visualizationId, Set<String> fields) throws InternalServerException {
		return Model.get(Member.class, collection, CMaps.map("_id", id).map("visualizations", visualizationId), fields);
	}
	
	public static Member getByMidataIDAndBirthday(String midataID, Date birthday, Set<String> fields) throws InternalServerException {
		return Model.get(Member.class, collection, CMaps.map("midataID", midataID).map("birthday", birthday), fields);
	}

	public static Member get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(Member.class, collection, properties, fields);
	}

	public static Set<Member> getAll(Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Member.class, collection, properties, fields, limit);
	}
	
	public static Set<Member> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Member.class, collection, properties, fields);
	}

	public static void set(MidataId userId, String field, Object value) throws InternalServerException {
		Model.set(Member.class, collection, userId, field, value);
	}

	public static void add(Member user) throws InternalServerException {
		user.updateKeywords(false);
		Model.insert(collection, user);

		// add to search index (email is document's content, so that it is searchable as well)
		/*
		try {
			Search.add(Type.USER, user._id, user.firstname + " " + user.lastname, user.email);
		} catch (SearchException e) {
			throw new InternalServerException("error.internal", e);
		}*/
	}

	public static void delete(MidataId userId) throws InternalServerException {		
		Model.delete(Member.class, collection, new ChainedMap<String, MidataId>().put("_id", userId).get());
	}
	
	protected String getCollection() {
		return collection;
	}
	
	/*public UserRole getRole() {
		return UserRole.MEMBER;
	}*/

}
