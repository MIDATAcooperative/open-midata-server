package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.enums.UserRole;

import org.bson.types.ObjectId;

import utils.DateTimeUtils;
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
		messages = new HashMap<String, Set<ObjectId>>();
		messages.put("inbox", new HashSet<ObjectId>());
		messages.put("archive", new HashSet<ObjectId>());
		messages.put("trash", new HashSet<ObjectId>());
		login = DateTimeUtils.now();
		history = new ArrayList<History>();
	}

	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(Admin.class, collection, CMaps.map("email", email)
				.map("role", UserRole.ADMIN));
	}

	public static Admin getByEmail(String email, Set<String> fields)
			throws InternalServerException {
		return Model.get(Admin.class, collection, CMaps.map("email", email)
				.map("role", UserRole.ADMIN), fields);
	}

	public static Admin getById(ObjectId id, Set<String> fields)
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
