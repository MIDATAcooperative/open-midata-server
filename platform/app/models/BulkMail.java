package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.enums.BulkMailStatus;
import models.enums.BulkMailType;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class BulkMail extends Model implements Comparable<BulkMail> {

	private static final String collection = "bulkmails";
	
	@NotMaterialized
	public final static Set<String> ALL = Sets.create("type", "country", "creator", "creatorName", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "progressId", "progressCount", "lastProgress");

	/**
	 * type of bulk mail
	 */
	public BulkMailType type;
	
	/**
	 * target country
	 */
	public String country;
	
	/**
	 * the creator of the bulk mail
	 */
	public MidataId creator;
	
	/**
	 * Name of the creator (email)
	 */
	public String creatorName;
	
	/**
	 * The date of creation of this bulk mail
	 */
	public Date created;

	/**
	 * The date this bulk mail has been started to be send
	 */
	public Date started;
	
	/**
	 * The date this bulk mail was completely sent
	 */
	public Date finished;
	
	/**
	 * Name for this bulk mail (internal title)
	 */
	public String name;
	
	/**
	 * Sending status of this bulk mail
	 */
	public BulkMailStatus status;
				
	/**
	 * The title of this mail
	 */
	public Map<String, String> title;
	
	/**
	 * The content (text) of this mail
	 */
	public Map<String, String> content;
		
	/**
	 * id of study this mail is about
	 */
	public MidataId studyId;
	
	public String studyName;
	
	public String studyCode;
	
	public String studyGroup;
		
	
	/**
	 * if of last user to whom the mail has been send
	 */
	public MidataId progressId;
	
	/**
	 * Number of emails sent
	 */
	public int progressCount;
	
	public long lastProgress;
		
	@Override
	public int compareTo(BulkMail other) {
		if (this.created != null && other.created != null) {
		// newest first
		return -this.created.compareTo(other.created);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(BulkMail.class, collection, properties);
	}

	public static BulkMail getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(BulkMail.class, collection, CMaps.map("_id", id), fields);
	}

	public static Set<BulkMail> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(BulkMail.class, collection, properties, fields);
	}

	public void setProgress() throws InternalServerException {
		this.setMultiple(collection, Sets.create("started", "finished", "status", "progressId", "progressCount", "lastProgress"));
	}

	public static void add(BulkMail mailCampaign) throws InternalServerException {
		Model.insert(collection, mailCampaign);		
	}
	
	public static void update(BulkMail mailCampaign) throws InternalServerException {
		Model.upsert(collection, mailCampaign);		
	}

	public static void delete(MidataId mailCampaignId) throws InternalServerException {
		Model.delete(BulkMail.class, collection, new ChainedMap<String, MidataId>().put("_id", mailCampaignId).get());
	}

}