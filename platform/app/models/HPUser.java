package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.enums.SubUserRole;
import models.enums.UserRole;

import org.bson.types.ObjectId;

import utils.DateTimeUtils;
import utils.collections.CMaps;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

public class HPUser extends User {
	
			
	public ObjectId provider;
	public SubUserRole subrole;
	
    public HPUser() { }
	
	public HPUser(String email) {		
		this.email = email;
		messages = new HashMap<String, Set<ObjectId>>();
		messages.put("inbox", new HashSet<ObjectId>());
		messages.put("archive", new HashSet<ObjectId>());
		messages.put("trash", new HashSet<ObjectId>());
		login = DateTimeUtils.now();	
		history = new ArrayList<History>();
	}
	
	public static boolean existsByEMail(String email) throws ModelException {
		return Model.exists(HPUser.class, collection, CMaps.map("email", email).map("role", UserRole.PROVIDER));
	}
	
	public static HPUser getByEmail(String email, Set<String> fields) throws ModelException {
		return Model.get(HPUser.class, collection, CMaps.map("email", email).map("role", UserRole.PROVIDER), fields);
	}
	
	public static HPUser getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static HPUser getByIdAndApp(ObjectId id, ObjectId appId, Set<String> fields) throws ModelException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id).map("apps", appId).map("role",  UserRole.PROVIDER), fields);
	}
	
	public static Set<HPUser> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(HPUser.class, collection, properties, fields);
	}
	
	public static void add(HPUser user) throws ModelException {
		Model.insert(collection, user);	
		
		// add to search index (email is document's content, so that it is searchable as well)
		try {
			Search.add(Type.USER, user._id, user.firstname + " " + user.sirname, user.email);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return UserRole.PROVIDER;
	}
}
