package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.exceptions.InternalServerException;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.SpaceType;

public class MobileAppInstance extends Consent {

	public byte[] publicKey;
	public ObjectId applicationId;
	public ObjectId appInstance;
	
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
	
	public static MobileAppInstance getByInstanceAndOwner(ObjectId instanceId, ObjectId owner, Set<String> fields) throws InternalServerException {
		return Model.get(MobileAppInstance.class, collection, CMaps.map("appInstance", instanceId).map("owner", owner), fields);
	}
}
