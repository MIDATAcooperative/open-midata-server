package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
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

	/**
	 * the creator of the news item
	 */
	public ObjectId creator;
	
	/**
	 * The date of creation of this news item
	 */
	public Date created;
	
	/**
	 * The title of this news item
	 */
	public String title;
	
	/**
	 * The content (text) of this news item
	 */
	public String content;
	
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

		// add broadcasts to public search index
		if (newsItem.broadcast) {
			try {
				Search.add(Type.NEWS, newsItem._id, newsItem.title, newsItem.content);
			} catch (SearchException e) {
				throw new InternalServerException("error.internal", e);
			}
		}
	}

	public static void delete(ObjectId receiverId, ObjectId newsItemId, boolean broadcast) throws InternalServerException {
		// also remove from the search index if it was a broadcast
		if (broadcast) {
			Search.delete(Type.NEWS, newsItemId);
		}
		Model.delete(NewsItem.class, collection, new ChainedMap<String, ObjectId>().put("_id", newsItemId).get());
	}

}
