package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
		Model.insert(collection, user);
	}

	protected String getCollection() {
		return collection;
	}

	public UserRole getRole() {
		return UserRole.ADMIN;
	}
}
