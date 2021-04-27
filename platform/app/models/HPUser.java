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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import models.enums.SubUserRole;
import models.enums.UserRole;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

/**
 * data model class for a health professional (person)
 *
 */
public class HPUser extends User {
	
	/**
	 * id of corresponding healthcare provider		
	 */
	public MidataId provider;
	
	
	
    public HPUser() { }
	
	public HPUser(String email) {		
		this.email = email;
		this.emailLC = email.toLowerCase();
		this.role = UserRole.PROVIDER;
		login = new Date();		
		subroles = EnumSet.noneOf(SubUserRole.class);
	}
	
	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(HPUser.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.PROVIDER).map("status", NON_DELETED));
	}
	
	public static HPUser getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(HPUser.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.PROVIDER).map("status", NON_DELETED), fields);
	}
	
	public static HPUser getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id).map("role", UserRole.PROVIDER), fields);
	}
	
	public static HPUser getByIdAndApp(MidataId id, MidataId appId, Set<String> fields) throws InternalServerException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id).map("apps", appId).map("role",  UserRole.PROVIDER), fields);
	}
	
	public static Set<HPUser> getAll(Map<String, Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(HPUser.class, collection, CMaps.map(properties).map("role", UserRole.PROVIDER), fields);
	}
	
	public static void add(HPUser user) throws InternalServerException {
		user.updateKeywords(false);
		Model.insert(collection, user);	
		
		// add to search index (email is document's content, so that it is searchable as well)
		/*try {
			Search.add(Type.USER, user._id, user.firstname + " " + user.lastname, user.email);
		} catch (SearchException e) {
			throw new InternalServerException("error.internal", e);
		}*/
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return UserRole.PROVIDER;
	}
}
