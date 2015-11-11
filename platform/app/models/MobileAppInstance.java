package models;

import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.exceptions.InternalServerException;
import models.enums.SpaceType;

public class MobileAppInstance extends Space {

	public byte[] publicKey;
	public ObjectId appInstance;
	
	public MobileAppInstance() {
		this.type = SpaceType.MOBILEAPP;
		this.context = "mobile";
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
