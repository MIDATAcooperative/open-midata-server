package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

public class NewsItem extends Model implements Comparable<NewsItem> {

	private static final String collection = "news";

	public ObjectId creator;
	public Date created;
	public String title;
	public String content;
	public boolean broadcast; // broadcast to all users

	@Override
	public int compareTo(NewsItem other) {
		if (this.created != null && other.created != null) {
		// newest first
		return -this.created.compareTo(other.created);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(NewsItem.class, collection, properties);
	}

	public static NewsItem get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(NewsItem.class, collection, properties, fields);
	}

	public static Set<NewsItem> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(NewsItem.class, collection, properties, fields);
	}

	public static void set(ObjectId newsItemId, String field, Object value) throws ModelException {
		Model.set(NewsItem.class, collection, newsItemId, field, value);
	}

	public static void add(NewsItem newsItem) throws ModelException {
		Model.insert(collection, newsItem);

		// add broadcasts to public search index
		if (newsItem.broadcast) {
			try {
				Search.add(Type.NEWS, newsItem._id, newsItem.title, newsItem.content);
			} catch (SearchException e) {
				throw new ModelException(e);
			}
		}
	}

	public static void delete(ObjectId receiverId, ObjectId newsItemId, boolean broadcast) throws ModelException {
		// also remove from the search index if it was a broadcast
		if (broadcast) {
			Search.delete(Type.NEWS, newsItemId);
		}
		Model.delete(NewsItem.class, collection, new ChainedMap<String, ObjectId>().put("_id", newsItemId).get());
	}

}
