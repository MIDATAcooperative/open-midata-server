package models;

import java.util.Date;
import java.util.Map;
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
		this.role = UserRole.RESEARCH;
		login = new Date();	
		
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
	
	public static Set<ResearchUser> getAll(Map<String, ? extends Object> properties, Set<String> fields, int limit) throws InternalServerException {
		return Model.getAll(ResearchUser.class, collection, properties, fields, limit);
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
