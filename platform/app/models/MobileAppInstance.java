package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.exceptions.InternalServerException;
import models.enums.ConsentStatus;
import models.enums.ConsentType;

/**
 * data model class for a consent between a MIDATA member and a mobile application.
 *
 */
public class MobileAppInstance extends Consent {

	/**
	 * public key of the application instance
	 */
	public byte[] publicKey;
	
	/**
	 * id of the plugin
	 */
	public ObjectId applicationId;
		
	
	public MobileAppInstance() {
		this.type = ConsentType.EXTERNALSERVICE;
		this.status = ConsentStatus.UNCONFIRMED;
		this.authorized = new HashSet<ObjectId>();
	}
	
	public static void add(MobileAppInstance space) throws InternalServerException {
		Model.insert(collection, space);		
	}
	
	public static MobileAppInstance getById(ObjectId id, Set<String> fields) throws InternalServerException {
		return Model.get(MobileAppInstance.class, collection, CMaps.map("_id", id), fields);
	}
	
	public static MobileAppInstance getByApplicationAndOwner(ObjectId applicationId, ObjectId owner, Set<String> fields) throws InternalServerException {
		return Model.get(MobileAppInstance.class, collection, CMaps.map("applicationId", applicationId).map("owner", owner), fields);
	}
}
