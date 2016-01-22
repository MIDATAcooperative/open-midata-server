package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.UserRole;
import models.enums.UserStatus;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import utils.PasswordHash;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

/**
 * Data model class for any user of the platform.
 *
 */
@JsonFilter("User")
public class User extends Model implements Comparable<User> {

	protected static final String collection = "users";
	
	/**
	 * Email address of the user
	 * 
	 * (email, role) must be unique
	 */
	public String email; 
	
	/**
	 * firstname lastname of the user
	 * 
	 * This field is not stored in the database but computed from firstname and lastname fields.
	 */
	public @NotMaterialized String name;
	
	/**
	 * The password of the user.
	 * 
	 * Only a salted hash of the password is stored in the database.
	 */
	public String password;
	
	/**
	 * The role of the user
	 */
	public UserRole role;
	
	/**
	 * Account version timestamp. May be used for schema evolution
	 */
	public int accountVersion;
	
	/**
	 * Message inbox. Currently not used. May be removed in the future.
	 */
	public Map<String, Set<ObjectId>> messages; // keys (folders) are: inbox, archive, trash
	
	/**
	 * Timestamp of last login
	 */
	public Date login;
	
	/**
	 * Timestamp of registration
	 */
	public Date registeredAt; 
			
	/**
	 * Token for password reset.
	 * 
	 * Is only set if a reset token has been requested.
	 */
	public String resettoken; 
	
	/**
	 * Timestamp of password reset token request
	 */
	public long resettokenTs; 
	
	/**
	 * Status of user account
	 */
	public UserStatus status;
	
	/**
	 * Status of contract of user with MIDATA
	 */
    public ContractStatus contractStatus; //enum: new, printed, signed	1	
    
    /**
     * Status of email validation
     */
    public EMailStatus emailStatus;
    
    /**
     * Code needed to confirm account. Currently not used
     */
    public String confirmationCode; 
	
    /**
     * First name of user
     */
    public String firstname;
    
    /**
     * Last name of user
     */
	public String lastname;
	
	/**
	 * Gender of user
	 */
	public Gender gender;
	
	/**
	 * City of user address
	 */
	public String city;	 
	
	/**
	 * Zip code of user address
	 */
	public String zip;	 
	
	/**
	 * Country of user address
	 */
	public String country;
	
	/**
	 * Address line 1 (Street) of user address
	 */
	public String address1;
	
	/**
	 * Address line 2 of user address
	 */
	public String address2;
	
	/**
	 * Phone number of user
	 */
	public String phone;
	
	/**
	 * Mobile phone number of user
	 */
	public String mobile;
	
	/**
	 * FHIR: Person
	 */
	public BSONObject person;
	
	/**
	 * Security level of user account
	 */
	public AccountSecurityLevel security = AccountSecurityLevel.NONE;
	
	/**
	 * Public key of user
	 */
	public byte[] publicKey;
	
	/**
	 * History of important changes to user account
	 */
	public List<History> history;
	
	/**
	 * Set of ids of installed forms/importers of the user
	 */
	public Set<ObjectId> apps; // installed apps
	
	/**
	 * Security token map
	 * 
	 * Map is from plugin id to Map token name to token value
	 */
	public Map<String, Map<String, String>> tokens; // map from apps to app details
	
	/**
	 * Set of ids of installed plugins
	 */
	public Set<ObjectId> visualizations; // installed visualizations
	
	/**
	 * Queries for sharing records with consents
	 * 
	 * Map consent id to query map.
	 */
	public Map<String, Map<String, Object>> queries;

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
	public static boolean authenticationValid(String givenPassword, String savedPassword) throws InternalServerException {
		try {
			return PasswordHash.validatePassword(givenPassword, savedPassword);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);
		} catch (InvalidKeySpecException e) {
			throw new InternalServerException("error.internal", e);
		}
	}

	public static String encrypt(String password) throws InternalServerException {
		try {
			return PasswordHash.createHash(password);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);
		} catch (InvalidKeySpecException e) {
			throw new InternalServerException("error.internal", e);
		}
	}
	
	protected String getCollection() {
		return "users";
	}
	
	public UserRole getRole() {
		return role;
	}
	
	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(User.class, collection, properties);
	}
			
	public void set(String field, Object value) throws InternalServerException {
		Model.set(this.getClass(), getCollection(), this._id, field, value);
	}
	
	public static User getById(ObjectId id, Set<String> fields) throws InternalServerException {
		return Model.get(User.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static User getByIdAndApp(ObjectId id, ObjectId appId, Set<String> fields) throws InternalServerException {
		return Model.get(User.class, collection, CMaps.map("_id", id).map("apps", appId), fields);
	}
	
	public static Set<User> getAllUser(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(User.class, collection, properties, fields);
	}
	
	public static void set(ObjectId userId, String field, Object value) throws InternalServerException {
		Model.set(User.class, collection, userId, field, value);
	}
	
}
