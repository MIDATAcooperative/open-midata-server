package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;
import utils.search.Search;
import utils.search.SearchException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Message extends Model implements Comparable<Message> {

	private static final String collection = "messages";

	public ObjectId sender;
	public ObjectId receiver;
	public String created;
	public String title;
	public String content;

	@Override
	public int compareTo(Message o) {
		// newest first
		return -this.created.compareTo(o.created);
	}

	public static Message find(ObjectId messageId) throws ModelException {
		DBObject query = new BasicDBObject("_id", messageId);
		DBObject result = Database.getCollection(collection).findOne(query);
		try {
			return ModelConversion.mapToModel(Message.class, result.toMap());
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
	}

	public static Set<Message> findSentTo(ObjectId userId) throws ModelException {
		Set<Message> messages = new HashSet<Message>();
		DBObject query = new BasicDBObject("receiver", userId);
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				messages.add(ModelConversion.mapToModel(Message.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return messages;
	}

	public static void add(Message newMessage) throws ModelException {
		DBObject insert;
		try {
			insert = new BasicDBObject(ModelConversion.modelToMap(newMessage));
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
		WriteResult result = Database.getCollection(collection).insert(insert);
		newMessage._id = (ObjectId) insert.get("_id");
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also add this circle to the user's search index
		try {
			Search.add(newMessage.receiver, "message", newMessage._id, newMessage.title, newMessage.content);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

}