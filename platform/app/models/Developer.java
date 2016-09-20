package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.enums.UserRole;

import models.MidataId;

import utils.DateTimeUtils;
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
		messages = new HashMap<String, Set<MidataId>>();
		messages.put("inbox", new HashSet<MidataId>());
		messages.put("archive", new HashSet<MidataId>());
		messages.put("trash", new HashSet<MidataId>());
		login = DateTimeUtils.now();	
		history = new ArrayList<History>();
	}
	
	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(Developer.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.DEVELOPER));
	}
	
	public static Developer getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(Developer.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.DEVELOPER), fields);
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
		return UserRole.DEVELOPER;
	}
}
