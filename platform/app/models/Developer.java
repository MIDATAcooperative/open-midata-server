package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
		history = new ArrayList<History>();
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
		return Model.exists(Developer.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.DEVELOPER).map("status", NON_DELETED));
	}
	
	public static Developer getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(Developer.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.DEVELOPER).map("status", NON_DELETED), fields);
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
	
	public static Set<Developer> getAll(Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(Developer.class, collection, properties, fields, limit);
	}
}
