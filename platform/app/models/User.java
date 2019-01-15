package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.PasswordHash;
import utils.audit.AuditManager;
import utils.auth.FutureLogin;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.evolution.AccountPatches;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * Data model class for any user of the platform.
 *
 */
@JsonFilter("User")
public class User extends Model implements Comparable<User> {

	protected static final @NotMaterialized String collection = "users";
	public static final @NotMaterialized Set<String> NON_DELETED = Collections.unmodifiableSet(Sets.create(UserStatus.ACTIVE.toString(), UserStatus.NEW.toString(), UserStatus.BLOCKED.toString(), UserStatus.TIMEOUT.toString()));
	public static final @NotMaterialized Set<String> ALL_USER = Collections.unmodifiableSet(Sets.create("_id", "email", "emailLC", "name", "role", "subroles", "accountVersion", "registeredAt",  "status", "contractStatus", "agbStatus", "emailStatus", "confirmedAt", "firstname", "lastname",	"gender", "city", "zip", "country", "address1", "address2", "phone", "mobile", "language", "searchable", "developer", "midataID", "termsAgreed", "security"));
	public static final @NotMaterialized Set<String> ALL_USER_INTERNAL = Collections.unmodifiableSet(Sets.create("email", "emailLC", "name", "role", "subroles", "accountVersion", "registeredAt",  "status", "contractStatus", "agbStatus", "emailStatus", "confirmedAt", "firstname", "lastname",	"gender", "city", "zip", "country", "address1", "address2", "phone", "mobile", "language", "searchable", "developer", "initialApp", "password", "apps", "midataID", "failedLogins", "lastFailed", "termsAgreed", "publicExtKey", "recoverKey", "flags", "security"));
	public static final @NotMaterialized Set<String> PUBLIC = Collections.unmodifiableSet(Sets.create("email", "role", "status", "firstname", "lastname", "gender", "midataID"));
	public static final @NotMaterialized Set<String> FOR_LOGIN = Collections.unmodifiableSet(Sets.create("firstname", "lastname", "email", "role", "password", "status", "contractStatus", "agbStatus", "emailStatus", "confirmationCode", "accountVersion", "role", "subroles", "login", "registeredAt", "developer", "failedLogins", "lastFailed", "flags", "resettoken", "termsAgreed", "publicExtKey", "recoverKey", "security", "phone", "mobile"));
	
			
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
	 * the public id of this member. The member may give this ID (together with the birthday) to a healthcare professional for identification.
	 */
	public String midataID;
	
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
	 * Public external key of user
	 */
	public byte[] publicExtKey;
		
	/**
	 * Used to encrypt recover information on client
	 */
	public String recoverKey;
	
	/**
	 * Set of ids of installed forms/importers of the user
	 */
	public Set<MidataId> apps; // installed apps
	
	public Set<String> termsAgreed;
	
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
	public Map<String, Map<String, Object>> rqueries;
	
	/**
	 * App that was used to register this person
	 */
	public MidataId initialApp;
	
	/**
	 * Study that has been the reason for the user to register
	 */
	public MidataId initialStudy;
	
	/**
	 * old email address (if changed)
	 */
	public String previousEMail;
	
	/**
	 * Number of failed logins
	 */
	public int failedLogins;
	
	/**
	 * Timestamp of last failed login
	 */
	public Date lastFailed;
	
	/**
	 * Actions that must be done upon login
	 */
	public Set<AccountActionFlags> flags;

	@Override
	public int compareTo(User other) {
		String me = getPublicIdentifier();
		String ot = other.getPublicIdentifier();
		if (me != null && ot != null) {
			return me.compareTo(ot);
		} else {
			return super.compareTo(other);
		}
	}
	
	/**
	 * Authenticate login data.
	 */
	public static boolean phraseValid(String givenPassword, String savedPassword) throws InternalServerException {
		try {
			if (givenPassword == null || savedPassword == null) return false;
			return PasswordHash.validatePassword(givenPassword, savedPassword);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);
		} catch (InvalidKeySpecException e) {
			throw new InternalServerException("error.internal", e);
		}
	}
	
	public boolean authenticationValid(String givenPassword) throws AppException {
		try {
			if (givenPassword == null || this.password == null) return false;
			
			if (this.failedLogins > 4) {
				long diff = System.currentTimeMillis() - this.lastFailed.getTime();
				switch (this.failedLogins) {
					case 5:
					case 6:
						if (diff < 1000l * 60l) throw new BadRequestException("error.blocked.password1", "Blocked for 1 minute.");
						break;				
					case 7:
					case 8:
						if (diff < 1000l * 60l * 5l) throw new BadRequestException("error.blocked.password5", "Blocked for 5 minutes.");					
						break;
					default:
						if (diff < 1000l * 60l * 60l) throw new BadRequestException("error.blocked.password60", "Blocked for 1 hour.");					
						break;
				}
			}
			
			boolean valid = PasswordHash.validatePassword(givenPassword, this.password);
			
			if (!valid) {
				this.failedLogins ++;
				this.lastFailed = new Date();
				setMultiple(collection, Sets.create("failedLogins", "lastFailed"));
			} else if (this.failedLogins > 0){
				this.failedLogins = 0;
				set("failedLogins", this.failedLogins);
			}
			
			return valid;
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
	
	public String getPublicIdentifier() {
		if (this.email != null) return this.email;
		if (this.midataID != null) return this.midataID;
		return "?";
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
	
	public void agreedToTerms(String terms, MidataId app) throws AppException {		
		User u2 = User.getById(this._id, Sets.create("termsAgreed"));
		if (u2 != null) this.termsAgreed = u2.termsAgreed;
		if (this.termsAgreed==null) this.termsAgreed = new HashSet<String>();
		
		if (!termsAgreed.contains(terms)) {
			AuditManager.instance.addAuditEvent(AuditEventType.USER_TERMS_OF_USE_AGREED, app, this, null, null, terms, null);
			termsAgreed.add(terms);
			Model.set(User.class, collection, this._id, "termsAgreed", this.termsAgreed);
			AuditManager.instance.success();
		}
					        	
    }
	
	/*public static void delete(MidataId userId) throws InternalServerException {			
		Model.delete(User.class, collection, CMaps.map("_id", userId));
	}*/
	
	public void delete() throws InternalServerException {
		
		this.email = "deleted"; 
		this.emailLC = "deleted";
		this.midataID = null;
		this.password = null;
		this.login = null;
		this.registeredAt = null;
		this.resettoken = null;
		this.resettokenTs = 0;
		this.status = UserStatus.WIPED;
		this.contractStatus = null;
		this.agbStatus = null;
		this.emailStatus = null;
        this.confirmationCode = null;
        this.confirmedAt = null;
        this.firstname = "deleted";
        this.lastname = "user";
	    this.city = null;
	    this.zip = null;
	    this.address1 = null;
	    this.address2 = null;
	    this.phone = null;
	    this.mobile = null;
	    this.searchable = false;
	    this.person = null;
	    this.keywordsLC = null;
	    this.publicKey = null;
	    this.apps = null;
	    this.termsAgreed = null;
        this.visualizations = null;
        this.queries = null;
        this.initialApp = null;
        this.initialStudy = null;
        this.previousEMail = null;

        this.setMultiple(collection, ALL_USER_INTERNAL);
	}

	/**
	 * Internally used to update a lower case keyword list for users to improve search speed.
	 * @param user the user 
	 * @param write set to true if the new keywords should be written to database
	 * @throws InternalServerException
	 */
	public void updateKeywords(boolean write) throws InternalServerException {
		Set<String> keywords = new HashSet<String>();
		keywords.add(firstname.toLowerCase());
		keywords.add(lastname.toLowerCase());
		if (address1 != null && address1.length() > 0) keywords.add(address1.toLowerCase());
		if (address2 != null && address2.length() > 0) keywords.add(address2.toLowerCase());
		if (city != null && city.length() > 0) keywords.add(city.toLowerCase());
		keywordsLC = keywords;
		if (write) User.set(_id, "keywordsLC", keywordsLC);
	}
	
	public static long count(UserRole role) throws AppException {
		return Model.count(User.class, collection, CMaps.map("role", role).map("status", EnumSet.of(UserStatus.ACTIVE, UserStatus.NEW)));
	}
	
	public static long countLanguage(String lang) throws AppException {
		return Model.count(User.class, collection, CMaps.map("language", lang).map("status", EnumSet.of(UserStatus.ACTIVE, UserStatus.NEW)));
	}
	
	public void addFlag(AccountActionFlags flag) throws AppException {
		if (this.flags == null) this.flags = EnumSet.of(flag);
		else this.flags.add(flag);
		User.set(_id, "flags", flags);
	}
	
	public void removeFlag(AccountActionFlags flag) throws AppException {
		this.flags.remove(flag);
		User.set(_id, "flags", flags);
	}
	
	public void updatePassword() throws AppException {
		this.setMultiple(collection, Sets.create("password", "publicExtKey", "security", "recoverKey"));
	}
	
}
