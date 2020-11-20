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

import models.enums.UserRole;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

/**
 * data model class for a MIDATA developer.
 *
 */
public class Developer extends User {
					
    public Developer() { }
	
	public Developer(String email) {		
		this.email = email;
		this.emailLC = email.toLowerCase();
		
		login = new Date();			
	}
	
	/**
	 * For what purpose has this account been opened
	 */
	public String reason;
	
	/**
	 * Contact person at MIDATA
	 */
	public String coach;
	
	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(Developer.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", EnumSet.of(UserRole.DEVELOPER, UserRole.ADMIN)).map("status", NON_DELETED));
	}
	
	public static Developer getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(Developer.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", EnumSet.of(UserRole.DEVELOPER, UserRole.ADMIN)).map("status", NON_DELETED), fields);
	}
	
	public static Developer getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(Developer.class, collection, CMaps.map("_id", id), fields);
	}
		
	
	public static void add(Developer user) throws InternalServerException {
		
		Model.insert(collection, user);				
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return role != null ? role : UserRole.DEVELOPER;
	}
	
	public static Set<Developer> getAll(Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Developer.class, collection, properties, fields, limit);
	}
}
