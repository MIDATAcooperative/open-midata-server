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

import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Data model for a news item.
 * Currently not used.
 *
 */
public class NewsItem extends Model implements Comparable<NewsItem> {

	private static final String collection = "news";
	
	@NotMaterialized
	public final static Set<String> ALL = Sets.create("creator", "created", "date", "layout", "expires", "language", "title", "content", "url", "studyId", "appId", "onlyParticipantsStudyId", "dynamicDate",/*"onlyUsersOfAppId",*/ "broadcast");

	/**
	 * the creator of the news item
	 */
	public MidataId creator;
	
	/**
	 * The date of creation of this news item
	 */
	public Date created;
	
	/**
	 * The official date for this news item
	 */
	public Date date;
	
	/**
	 * Does this news message expire
	 */
	public Date expires;
	
	/**
	 * The language this news is written in
	 */
	public String language;
	
	/**
	 * The title of this news item (internationalized)
	 */
	public String title;
	
	/**
	 * The content (text) of this news item
	 */
	public String content;
	
	/**
	 * Layout selector
	 */	
	public String layout;
	
	/**
	 * (optional) An external URL containing details of this news message
	 */
	public String url;
	
	/**
	 * (optional) id of study this news item is about
	 */
	public MidataId studyId;
	
	/**
	 * (optional) id of app this news item is about
	 */
	public MidataId appId;
	
	/**
	 * (optional) show only to participants of study
	 */
	public MidataId onlyParticipantsStudyId;
	
	/**
	 * Sets news date to project participation date or app use date or today
	 */
	public boolean dynamicDate; 
	
	/**
	 * (optional) show only to users of app
	 */
	//public MidataId onlyUsersOfAppId;
	
	/**
	 * whether this should be broadcast to all users
	 */
	public boolean broadcast; 

	@Override
	public int compareTo(NewsItem other) {
		if (this.created != null && other.created != null) {
		// newest first
		return -this.created.compareTo(other.created);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(NewsItem.class, collection, properties);
	}

	public static NewsItem get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(NewsItem.class, collection, properties, fields);
	}

	public static Set<NewsItem> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(NewsItem.class, collection, properties, fields);
	}

	public static void set(MidataId newsItemId, String field, Object value) throws InternalServerException {
		Model.set(NewsItem.class, collection, newsItemId, field, value);
	}

	public static void add(NewsItem newsItem) throws InternalServerException {
		Model.insert(collection, newsItem);		
	}
	
	public static void update(NewsItem newsItem) throws InternalServerException {
		Model.upsert(collection, newsItem);		
	}

	public static void delete(MidataId newsItemId) throws InternalServerException {
		Model.delete(NewsItem.class, collection, new ChainedMap<String, MidataId>().put("_id", newsItemId).get());
	}

}
