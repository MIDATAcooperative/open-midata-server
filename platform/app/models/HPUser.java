package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.DateTimeUtils;
import utils.collections.CMaps;

public class HPUser extends User {
	
	private static final String collection = "hpusers";
			
	public ObjectId provider;
	
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
		return Model.exists(collection, CMaps.map("email", email));
	}
	
	public static HPUser getByEmail(String email, Set<String> fields) throws ModelException {
		return Model.get(HPUser.class, collection, CMaps.map("email", email), fields);
	}
	
	public static HPUser getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(HPUser.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static void add(HPUser user) throws ModelException {
		Model.insert(collection, user);	
	}
	
	protected String getCollection() {
		return collection;
	}
}
