package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.SearchException;

public class Message extends Model implements Comparable<Message> {

	private static final String collection = "messages";

	public ObjectId sender;
	public Set<ObjectId> receivers;
	public Date created;
	public String title;
	public String content;

	@Override
	public int compareTo(Message other) {
		if (this.created != null && other.created != null) {
			// newest first
			return -this.created.compareTo(other.created);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Message.class, collection, properties);
	}

	public static Message get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Message.class, collection, properties, fields);
	}

	public static Set<Message> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Message.class, collection, properties, fields);
	}

	public static void set(ObjectId messageId, String field, Object value) throws ModelException {
		Model.set(Message.class, collection, messageId, field, value);
	}

	public static void add(Message message) throws ModelException {
		Model.insert(collection, message);

		// also add this message to each receiver's search index
		for (ObjectId receiver : message.receivers) {
			try {
				Search.add(receiver, "message", message._id, message.title, message.content);
			} catch (SearchException e) {
				throw new ModelException(e);
			}
		}
	}

	public static void delete(ObjectId receiverId, ObjectId messageId) throws ModelException {
		// also remove from the search index
		Search.delete(receiverId, "message", messageId);
		Model.delete(Message.class, collection, new ChainedMap<String, ObjectId>().put("_id", messageId).get());
	}
}
