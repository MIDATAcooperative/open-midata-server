package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.AccountSecurityLevel;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.PasswordHash;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.evolution.AccountPatches;
import utils.exceptions.InternalServerException;

/**
 * Data model class for any user of the platform.
 *
 */
@JsonFilter("User")
public class User extends Model implements Comparable<User> {

	protected static final @NotMaterialized String collection = "users";
	protected static final @NotMaterialized Set<String> NON_DELETED = Sets.create(UserStatus.ACTIVE.toString(), UserStatus.NEW.toString(), UserStatus.BLOCKED.toString(), UserStatus.TIMEOUT.toString());
	public static final @NotMaterialized Set<String> ALL_USER = Sets.create("email", "emailLC", "name", "role", "subroles", "accountVersion", "registeredAt",  "status", "contractStatus", "agbStatus", "emailStatus", "confirmedAt", "firstname", "lastname",	"gender", "city", "zip", "country", "address1", "address2", "phone", "mobile", "language", "searchable", "developer");
	
			
	/**
	 * Email address of the user
	 * 
	 * (email, role) must be unique
	 */
	public String email; 
	
	/**
	 * Email address of the user in lower case
	 */
	public String emailLC;
	
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
	 * sub role(s) (doctor, nurse, monitor,...)
	 */
	public Set<SubUserRole> subroles;
	
	/**
	 * Account version timestamp. May be used for schema evolution
	 */
	public int accountVersion = AccountPatches.currentAccountVersion;
	
	/**
	 * Message inbox. Currently not used. May be removed in the future.
	 */
	public Map<String, Set<MidataId>> messages; // keys (folders) are: inbox, archive, trash
	
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
    public ContractStatus contractStatus; 
    
    /**
     * Status of AGB of user with MIDATA
     */
    public ContractStatus agbStatus;
    
    /**
     * Status of email validation
     */
    public EMailStatus emailStatus;
    
    /**
     * Code needed to confirm account. 
     */
    public String confirmationCode; 
    
    /**
     * Date of confirmation
     */
    public Date confirmedAt;
	
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
	 * Language to be used
	 */
	public String language;
	
	/**
	 * User may be found by a user search function
	 */
	public boolean searchable;
	
	/**
	 * If set : This is a test account registered by this developer
	 */
	public MidataId developer;
	
	/**
	 * FHIR: Person
	 */
	public BSONObject person;
	
	/**
	 * lower case words from name and address for indexing for improved search speed
	 */
	public Set<String> keywordsLC;
	
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
	public Set<MidataId> apps; // installed apps
	
	/**
	 * Security token map
	 * 
	 * Map is from plugin id to Map token name to token value
	 */
	public Map<String, Map<String, String>> tokens; // map from apps to app details
	
	/**
	 * Set of ids of installed plugins
	 */
	public Set<MidataId> visualizations; // installed visualizations
	
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
	
	public static User getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(User.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static User getByIdAndApp(MidataId id, MidataId appId, Set<String> fields) throws InternalServerException {
		return Model.get(User.class, collection, CMaps.map("_id", id).map("apps", appId), fields);
	}
	
	public static Set<User> getAllUser(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(User.class, collection, properties, fields);
	}
	
	public static void set(MidataId userId, String field, Object value) throws InternalServerException {
		Model.set(User.class, collection, userId, field, value);
	}
	
	public void addHistory(History newhistory) throws InternalServerException {
		if (history == null) history = new ArrayList<History>();
    	this.history.add(newhistory);
    	Model.set(User.class, collection, this._id, "history", this.history);
    }
	
	public static void delete(MidataId userId) throws InternalServerException {			
		Model.delete(User.class, collection, CMaps.map("_id", userId));
	}

		
	
}
