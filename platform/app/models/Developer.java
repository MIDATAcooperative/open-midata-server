package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.enums.UserRole;

import org.bson.types.ObjectId;

import utils.DateTimeUtils;
import utils.collections.CMaps;
import utils.exceptions.ModelException;


public class Developer extends User {
					
    public Developer() { }
	
	public Developer(String email) {		
		this.email = email;
		messages = new HashMap<String, Set<ObjectId>>();
		messages.put("inbox", new HashSet<ObjectId>());
		messages.put("archive", new HashSet<ObjectId>());
		messages.put("trash", new HashSet<ObjectId>());
		login = DateTimeUtils.now();	
		history = new ArrayList<History>();
	}
	
	public static boolean existsByEMail(String email) throws ModelException {
		return Model.exists(Developer.class, collection, CMaps.map("email", email).map("role", UserRole.DEVELOPER));
	}
	
	public static Developer getByEmail(String email, Set<String> fields) throws ModelException {
		return Model.get(Developer.class, collection, CMaps.map("email", email).map("role", UserRole.DEVELOPER), fields);
	}
	
	public static Developer getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(Developer.class, collection, CMaps.map("_id", id), fields);
	}
		
	
	public static void add(Developer user) throws ModelException {
		Model.insert(collection, user);				
	}
	
	protected String getCollection() {
		return collection;
	}
	
	public UserRole getRole() {
		return UserRole.DEVELOPER;
	}
}
