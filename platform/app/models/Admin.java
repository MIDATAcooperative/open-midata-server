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
import java.util.Set;

import models.enums.SubUserRole;
import models.enums.UserRole;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

/**
 * Data model for admin users.
 *
 */
public class Admin extends User {

	public Admin() {
	}

	public Admin(String email) {
		this.email = email;
		this.emailLC = email.toLowerCase();
		
		login = new Date();		
		subroles = EnumSet.noneOf(SubUserRole.class);
	}

	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(Admin.class, collection, CMaps.map("emailLC", email.toLowerCase())
				.map("role", UserRole.ADMIN).map("status", NON_DELETED));
	}

	public static Admin getByEmail(String email, Set<String> fields)
			throws InternalServerException {
		return Model.get(Admin.class, collection, CMaps.map("emailLC", email.toLowerCase())
				.map("role", UserRole.ADMIN).map("status", NON_DELETED), fields);
	}

	public static Admin getById(MidataId id, Set<String> fields)
			throws InternalServerException {
		return Model.get(Admin.class, collection, CMaps.map("_id", id), fields);
	}

	public static void add(Admin user) throws InternalServerException {
		user.updateKeywords(false);
		Model.insert(collection, user);
	}

	protected String getCollection() {
		return collection;
	}

	public UserRole getRole() {
		return UserRole.ADMIN;
	}
}
