package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.enums.UserRole;

import models.MidataId;

import utils.DateTimeUtils;
import utils.collections.CMaps;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

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
		messages = new HashMap<String, Set<MidataId>>();
		messages.put("inbox", new HashSet<MidataId>());
		messages.put("archive", new HashSet<MidataId>());
		messages.put("trash", new HashSet<MidataId>());
		login = DateTimeUtils.now();	
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
		Model.insert(collection, user);	
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return UserRole.RESEARCH;
	}
		

}
