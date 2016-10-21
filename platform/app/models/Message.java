package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import utils.collections.ChainedMap;
import utils.exceptions.InternalServerException;

/**
 * data model for a message sent from one user to another
 * Currently not used. Messaging lacks encryption.
 *
 */
public class Message extends Model implements Comparable<Message> {

	private static final String collection = "messages";

	/**
	 * id of sender of message
	 */
	public MidataId sender;
	
	/**
	 * ids of recievers of the message
	 */
	public Set<MidataId> receivers;
	
	/**
	 * date of creation
	 */
	public Date created;
	
	/**
	 * title of this message
	 */
	public String title;
	
	/**
	 * body of this message
	 */
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

	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(Message.class, collection, properties);
	}

	public static Message get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(Message.class, collection, properties, fields);
	}

	public static Set<Message> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Message.class, collection, properties, fields);
	}

	public static void set(MidataId messageId, String field, Object value) throws InternalServerException {
		Model.set(Message.class, collection, messageId, field, value);
	}

	public static void add(Message message) throws InternalServerException {
		Model.insert(collection, message);
		
	}

	public static void delete(MidataId receiverId, MidataId messageId) throws InternalServerException {
		// also remove from the search index
		//Search.delete(receiverId, "message", messageId);
		Model.delete(Message.class, collection, new ChainedMap<String, MidataId>().put("_id", messageId).get());
	}
}
