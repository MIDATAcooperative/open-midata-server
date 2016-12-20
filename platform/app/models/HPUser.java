package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
		
		login = new Date();	
		history = new ArrayList<History>();
		subroles = EnumSet.noneOf(SubUserRole.class);
	}
	
	public static boolean existsByEMail(String email) throws InternalServerException {
		return Model.exists(HPUser.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.PROVIDER).map("status", NON_DELETED));
	}
	
	public static HPUser getByEmail(String email, Set<String> fields) throws InternalServerException {
		return Model.get(HPUser.class, collection, CMaps.map("emailLC", email.toLowerCase()).map("role", UserRole.PROVIDER).map("status", NON_DELETED), fields);
	}
	
	public static HPUser getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static HPUser getByIdAndApp(MidataId id, MidataId appId, Set<String> fields) throws InternalServerException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id).map("apps", appId).map("role",  UserRole.PROVIDER), fields);
	}
	
	public static Set<HPUser> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(HPUser.class, collection, properties, fields);
	}
	
	public static void add(HPUser user) throws InternalServerException {
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
