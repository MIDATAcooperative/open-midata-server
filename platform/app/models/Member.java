package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.ParticipationInterest;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.SearchException;
import utils.search.Search.Type;

public class Member extends User {

	private static final String collection = "users";
	
	public Map<String, Set<ObjectId>> visible; // map from users (DBObject requires string) to their shared records
	public Set<ObjectId> apps; // installed apps
	public Map<String, Map<String, String>> tokens; // map from apps to app details
	public Set<ObjectId> visualizations; // installed visualizations
	
	public Set<ObjectId> news; // visible news items
	public Set<ObjectId> pushed; // records pushed by apps (since last login)
	public Set<ObjectId> shared; // records shared by users (since last login)
	
	public Set<ObjectId> studyKeywords; // Keywords for studies
	public Set<ObjectId> tags; // Tags describing account
	
	public String midataID;
	public Date birthday;
	public String ssn; // social security number
	public ParticipationInterest partInterest;
	
	
	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(collection, properties);
	}
	
	public static boolean existsByEMail(String email) throws ModelException {
		return Model.exists(collection, CMaps.map("email", email));
	}
	
	public static Member getByEmail(String email, Set<String> fields) throws ModelException {
		return Model.get(Member.class, collection, CMaps.map("email", email), fields);
	}
	
	public static Member getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(Member.class, collection, CMaps.map("_id", id), fields);
	}

	public static Member get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Member.class, collection, properties, fields);
	}

	public static Set<Member> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Member.class, collection, properties, fields);
	}

	public static void set(ObjectId userId, String field, Object value) throws ModelException {
		Model.set(collection, userId, field, value);
	}

	public static void add(Member user) throws ModelException {
		Model.insert(collection, user);

		// add to search index (email is document's content, so that it is searchable as well)
		try {
			Search.add(Type.USER, user._id, user.name, user.email);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId userId) throws ModelException {
		// remove from search index
		Search.delete(Type.USER, userId);

		// TODO remove all the user's messages, records, spaces, circles, apps (if published, ask whether to leave it in
		// the marketplace), ...
		Model.delete(collection, new ChainedMap<String, ObjectId>().put("_id", userId).get());
	}
	
	protected String getCollection() {
		return collection;
	}

}
