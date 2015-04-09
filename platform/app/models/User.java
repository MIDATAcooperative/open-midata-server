package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.ContractStatus;
import models.enums.Gender;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.types.ObjectId;

import utils.PasswordHash;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.db.NotMaterialized;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

public class User extends Model implements Comparable<User> {

	protected static final String collection = "users";
	
	public String email; // must be unique
	public @NotMaterialized String name;
	public String password;
	public UserRole role;
	
	public Map<String, Set<ObjectId>> messages; // keys (folders) are: inbox, archive, trash
	public Date login; // timestamp of last login
	public Date registeredAt; // Date of registration
		
	public String resettoken; // token to reset password
	public long resettokenTs; // timestamp of password reset token
	
	public UserStatus status; //tatus	enum: new, active, blocked, deleted	1	-	new: account is new and match with real person is not yet confirmed
    public ContractStatus contractStatus; //enum: new, printed, signed	1	-	
    public String confirmationCode; //code needed to confirm account
	
    public String firstname;	 
	public String sirname;	 
	public Gender gender;	 
	public String city;	 
	public String zip;	 
	public String country;
	public String address1;
	public String address2;
	public String phone;
	public String mobile;
	
	public List<History> history;
	
	public Set<ObjectId> apps; // installed apps
	public Map<String, Map<String, String>> tokens; // map from apps to app details
	public Set<ObjectId> visualizations; // installed visualizations

	@Override
	public int compareTo(User other) {
		if (this.email != null && other.email != null) {
			return this.email.compareTo(other.email);
		} else {
			return super.compareTo(other);
		}
	}
	
	/**
	 * Authenticate login data.
	 */
	public static boolean authenticationValid(String givenPassword, String savedPassword) throws ModelException {
		try {
			return PasswordHash.validatePassword(givenPassword, savedPassword);
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);
		} catch (InvalidKeySpecException e) {
			throw new ModelException(e);
		}
	}

	public static String encrypt(String password) throws ModelException {
		try {
			return PasswordHash.createHash(password);
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);
		} catch (InvalidKeySpecException e) {
			throw new ModelException(e);
		}
	}
	
	protected String getCollection() {
		return "users";
	}
	
	public UserRole getRole() {
		return null;
	}
	
	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(User.class, collection, properties);
	}
	
	public void set(String field, Object value) throws ModelException {
		Model.set(this.getClass(), getCollection(), this._id, field, value);
	}
	
	public static User getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(User.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static User getByIdAndApp(ObjectId id, ObjectId appId, Set<String> fields) throws ModelException {
		return Model.get(User.class, collection, CMaps.map("_id", id).map("apps", appId), fields);
	}
	
	public static Set<User> getAllUser(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(User.class, collection, properties, fields);
	}
	
	public static void set(ObjectId userId, String field, Object value) throws ModelException {
		Model.set(User.class, collection, userId, field, value);
	}
	
}
