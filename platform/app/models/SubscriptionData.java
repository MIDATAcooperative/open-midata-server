package models;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Data of subscription (automatic change notifications)
 *
 */
public class SubscriptionData extends Model {
	
	@NotMaterialized
	private static String collection = "subscriptions";
	
	@NotMaterialized
	public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "owner", "app", "instance", "format", "content", "lastUpdated", "active", "endDate", "fhirSubscription", "session"));

	/**
	 * The owner of the subscription
	 */
	public MidataId owner;
	
	/**
	 * The owner app of the subscription
	 */
	public MidataId app;
	
	/**
	 * The instance (appInstance or space)
	 */
	public MidataId instance;
	
	/**
	 * The type of data this subscription listens to
	 */
	public String format;
	
	/**
	 * The type of content this subscription listens to
	 */
	public String content;
	
	/**
	 * Last updated timestamp
	 */
	public long lastUpdated;
	
	/**
	 * IS this subscription active?
	 */
	public boolean active;
	
	/**
	 * When to terminate this subscription
	 */
	public Date endDate;
		
	/**
	 * THe FHIR resource holding the subscription
	 */
	public BSONObject fhirSubscription;
	
	/**
	 * Session information
	 */
	public byte[] session;
	
	public void add() throws InternalServerException {
		Model.upsert(collection, this);	
	}
	
	public void update() throws InternalServerException {
		setMultiple(collection, SubscriptionData.ALL);
	}
	
	public static SubscriptionData getById(MidataId subscriptionId, Set<String> fields) throws InternalServerException {
		return Model.get(SubscriptionData.class, collection, CMaps.map("_id", subscriptionId), fields);
	}
	
	public static SubscriptionData getByIdAndOwner(MidataId subscriptionId, MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.get(SubscriptionData.class, collection, CMaps.map("_id", subscriptionId).map("owner", owner), fields);
	}
	
	public static List<SubscriptionData> getByOwner(MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.getAllList(SubscriptionData.class, collection, CMaps.map("owner", owner), fields, 0);
	}
	
	public static List<SubscriptionData> getByOwner(MidataId owner, Map<String, Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAllList(SubscriptionData.class, collection, CMaps.map(properties).map("owner", owner), fields, 0);
	}
	
	public static boolean existsActiveByOwner(MidataId owner) throws InternalServerException {
		return Model.exists(SubscriptionData.class, collection, CMaps.map("active", true).map("owner", owner));
	}
	
	public static List<SubscriptionData> getByOwnerAndFormat(MidataId owner, String format, Set<String> fields) throws InternalServerException {
		return Model.getAllList(SubscriptionData.class, collection, CMaps.map("format", format).map("owner", owner), fields, 0);
	}
	
	public static List<SubscriptionData> getByOwnerAndFormatAndInstance(MidataId owner, String format, MidataId instance, Set<String> fields) throws InternalServerException {
		return Model.getAllList(SubscriptionData.class, collection, CMaps.map("format", format).map("owner", owner).map("instance", instance), fields, 0);
	}
		
		
	public static List<SubscriptionData> getAllActive(Set<String> fields) throws InternalServerException {
		return Model.getAllList(SubscriptionData.class, collection, CMaps.map("active", true), fields, 0);
	}
	
	public static List<SubscriptionData> getAllActiveFormat(String format, Set<String> fields) throws InternalServerException {
		return Model.getAllList(SubscriptionData.class, collection, CMaps.map("active", true).map("format", format), fields, 0);
	}
	
	public static void setError(MidataId id, String error) throws InternalServerException {
		Model.set(SubscriptionData.class, collection, id, "fhirSubscription.error", error);
	}
	
	public static void setOff(MidataId id) throws InternalServerException { 
		Model.set(SubscriptionData.class, collection, id, "fhirSubscription.status", "off");
		Model.set(SubscriptionData.class, collection, id, "active", false);
	}
	
	public void disable() throws InternalServerException { 
		setOff(this._id);
	}
	
	public void delete() throws InternalServerException { 
		Model.delete(SubscriptionData.class, collection, CMaps.map("_id", _id));
	}
		
	public static void deleteByOwner(MidataId owner) throws InternalServerException {
		if (owner == null) throw new NullPointerException();
		Model.delete(SubscriptionData.class, collection, CMaps.map("owner", owner));
	}
		
}
