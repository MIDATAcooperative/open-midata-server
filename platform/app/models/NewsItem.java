package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

/**
 * Data model for a news item.
 * Currently not used.
 *
 */
public class NewsItem extends Model implements Comparable<NewsItem> {

	private static final String collection = "news";
	
	@NotMaterialized
	public final static Set<String> ALL = Sets.create("creator", "created", "expires", "language", "title", "content", "url", "studyId", "broadcast");

	/**
	 * the creator of the news item
	 */
	public ObjectId creator;
	
	/**
	 * The date of creation of this news item
	 */
	public Date created;
	
	/**
	 * The expiration date of this news item
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
	 * (optional) An external URL containing details of this news message
	 */
	public String url;
	
	/**
	 * (optional) id of study this news item is about
	 */
	public ObjectId studyId;
	
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

	public static void set(ObjectId newsItemId, String field, Object value) throws InternalServerException {
		Model.set(NewsItem.class, collection, newsItemId, field, value);
	}

	public static void add(NewsItem newsItem) throws InternalServerException {
		Model.insert(collection, newsItem);		
	}
	
	public static void update(NewsItem newsItem) throws InternalServerException {
		Model.upsert(collection, newsItem);		
	}

	public static void delete(ObjectId newsItemId) throws InternalServerException {
		Model.delete(NewsItem.class, collection, new ChainedMap<String, ObjectId>().put("_id", newsItemId).get());
	}

}
