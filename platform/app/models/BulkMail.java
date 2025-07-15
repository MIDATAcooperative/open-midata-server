/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	public final static Set<String> ALL = Sets.create("type", "country", "creator", "creatorName", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "appId", "appName", "progressId", "progressCount", "progressFailed", "lastProgress", "htmlFrame");

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
	 * HTML template for message
	 */
	public String htmlFrame;
		
	/**
	 * id of study this mail is about
	 */
	public MidataId studyId;
	
	public String studyName;
	
	public String studyCode;
	
	public String studyGroup;
	
	public MidataId appId;
	
	public String appName;
			
	/**
	 * if of last user to whom the mail has been send
	 */
	public MidataId progressId;
	
	/**
	 * Number of emails sent
	 */
	public int progressCount;
	
	public int progressFailed;
	
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
		this.setMultiple(collection, Sets.create("started", "finished", "status", "progressId", "progressCount", "progressFailed", "lastProgress"));
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