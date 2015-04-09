package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.enums.UserRole;

import org.bson.types.ObjectId;

import utils.DateTimeUtils;
import utils.collections.CMaps;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

public class ResearchUser extends User {
			
	public ObjectId organization;
	
	public ResearchUser() { }
	
	public ResearchUser(String email) {		
		this.email = email;
		messages = new HashMap<String, Set<ObjectId>>();
		messages.put("inbox", new HashSet<ObjectId>());
		messages.put("archive", new HashSet<ObjectId>());
		messages.put("trash", new HashSet<ObjectId>());
		login = DateTimeUtils.now();	
		history = new ArrayList<History>();
	}
	
	public static boolean existsByEMail(String email) throws ModelException {
		return Model.exists(ResearchUser.class, collection, CMaps.map("email", email).map("role", UserRole.RESEARCH));
	}
	
	public static ResearchUser getByEmail(String email, Set<String> fields) throws ModelException {
		return Model.get(ResearchUser.class, collection, CMaps.map("email", email).map("role", UserRole.RESEARCH), fields);
	}
	
	public static ResearchUser getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(ResearchUser.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static void add(ResearchUser user) throws ModelException {
		Model.insert(collection, user);	
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return UserRole.RESEARCH;
	}
		

}
