package models;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.EntityType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;

/**
 * A licence allows an entity (user/org/team) to use an application
 *
 */
public class Licence extends Model {

	@NotMaterialized
	private static final String collection = "licenses";
	
	@NotMaterialized
	public static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "appId", "appName", "granteeId", "licenseeName", "licenseeType", "expireDate", "creationDate", "status"));
	
	/**
	 * For which application is a licence granted
	 */
	public MidataId appId;
	
	/**
	 * Name of application
	 */
	public String appName;
	
	/**
	 * For which entity is the licence granted
	 */
	public MidataId licenseeId;
	
	/**
	 * Name of entity
	 */
	public String licenseeName;
	
	/**
	 * What type of entity is this licence granted to
	 */
	public EntityType licenseeType;
	
	/**
	 * Expire date of licence
	 */
	public Date expireDate;
	
	/**
	 * Creation date of licence
	 */
	public Date creationDate;
	
	/**
	 * Status of licence
	 */
	public ConsentStatus status;
	
	/**
	 * Who created this licence
	 */
	public MidataId grantedBy;
	
	/**
	 * Login name of person who added the licence
	 */
	public String grantedByLogin;
	
	public static Set<Licence> getLicenceByLicensee(MidataId licenseeId) throws AppException {
		return Model.getAll(Licence.class, collection, CMaps.map("licenseeId", licenseeId), ALL);
	}
	
	public static Licence getActiveLicenceByLicenseeAndApp(MidataId licenseeId, EntityType licenseeType, MidataId appId) throws AppException {
		return Model.get(Licence.class, collection, CMaps.map("licenseeId", licenseeId).map("appId", appId).map("licenseeType", licenseeType).map("status",ConsentStatus.ACTIVE), ALL);		
	}
	
	public static Set<Licence> getActiveUserGroupLicenceByApp(MidataId appId) throws AppException {
		return Model.getAll(Licence.class, collection, CMaps.map("appId", appId).map("licenseeType", EntityType.USERGROUP).map("status",ConsentStatus.ACTIVE), ALL);		
	}
	
	public static Licence getById(MidataId id) throws AppException {
		return Model.get(Licence.class, collection, CMaps.map("_id", id), ALL);
	}
	
	public static Set<Licence> getAll(Map<String, Object> properties) throws AppException {
		return Model.getAll(Licence.class, collection, properties, ALL);
	}
	
	public void add() throws AppException {
		Model.insert(collection, this);
	}
}
