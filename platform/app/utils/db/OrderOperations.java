package utils.db;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import models.MidataId;

/**
 * Helper functions for ordered objects
 *
 */
public class OrderOperations {

	/**
	 * Returns the maximum of the order fields in the given collection.
	 */
	public static int getMax(String collection, MidataId userId) {
		DBCollection coll = DBLayer.getCollection(collection);
		DBObject query = new BasicDBObject("owner", userId.toObjectId());
		query.put("order", new BasicDBObject("$exists", true));
		DBObject projection = new BasicDBObject("order", 1);
		DBCursor maxOrder = coll.find(query, projection).sort(new BasicDBObject("order", -1)).limit(1);
		int max = 0;
		if (maxOrder.hasNext()) {
			max = (Integer) maxOrder.next().get("order");
		}
		return max;
	}

	/**
	 * Decrements all order fields from (and including) 'fromLimit' to (and including) 'toLimit' by one. If either one
	 * is zero, only the other condition will be considered.
	 */
	public static void decrement(String collection, MidataId userId, int fromLimit, int toLimit)
			throws DatabaseException {
		int[] limits = getLimits(fromLimit, toLimit);
		incOperation(collection, userId, limits[0], limits[1], -1);
	}

	/**
	 * Increments all order fields from (and including) 'fromLimit' to (and including) 'toLimit' by one. If either one
	 * is zero, only the other condition will be considered.
	 */
	public static void increment(String collection, MidataId userId, int fromLimit, int toLimit)
			throws DatabaseException {
		int[] limits = getLimits(fromLimit, toLimit);
		incOperation(collection, userId, limits[0], limits[1], 1);
	}

	private static int[] getLimits(int fromLimit, int toLimit) {
		// fromLimit is never greater than toLimit
		if (toLimit != 0 && fromLimit > toLimit) {
			return new int[] { toLimit, fromLimit };
		} else {
			return new int[] { fromLimit, toLimit };
		}
	}

	private static void incOperation(String collection, MidataId userId, int fromLimit, int toLimit, int increment)
			throws DatabaseException {
		DBObject query = new BasicDBObject("owner", userId.toObjectId());
		query.put("order", new BasicDBObject("$exists", true));
		if (fromLimit == 0) {
			query.put("order", new BasicDBObject("$lte", toLimit));
		} else if (toLimit == 0) {
			query.put("order", new BasicDBObject("$gte", fromLimit));
		} else {
			DBObject[] and = { new BasicDBObject("order", new BasicDBObject("$gte", fromLimit)),
					new BasicDBObject("order", new BasicDBObject("$lte", toLimit)) };
			query.put("$and", and);
		}
		DBObject update = new BasicDBObject("$inc", new BasicDBObject("order", increment));
		DBCollection coll = DBLayer.getCollection(collection);
		try {
			coll.updateMulti(query, update);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		}
	}

}
