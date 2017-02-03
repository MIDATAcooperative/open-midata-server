package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.enums.UserRole;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;

/**
 * data model class for a researcher.
 *
 */
public class ResearchUser extends User {
			
	/**
	 * id of organization this researcher belongs to.
	 */
	public MidataId organization;
	
	public ResearchUser() { }
	
	public ResearchUser(String email) {		
		this.email = email;
		this.emailLC = email.toLowerCase();
		
		login = new Date();	
		history = new ArrayList<History>();
	}
	
	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(ResearchUser.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.RESEARCH).map("status", NON_DELETED));
	}
	
	public static ResearchUser getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(ResearchUser.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.RESEARCH).map("status", NON_DELETED), fields);
	}
	
	public static ResearchUser getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(ResearchUser.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static void add(ResearchUser user) throws InternalServerException {
		//user.updateKeywords(false);
		Model.insert(collection, user);	
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return UserRole.RESEARCH;
	}
		

}
