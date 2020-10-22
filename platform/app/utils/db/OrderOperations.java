/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.db;

import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

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
		MongoCollection<DBObject> coll = DBLayer.getCollection(collection);
		BasicDBObject query = new BasicDBObject("owner", userId.toObjectId());
		query.put("order", new BasicDBObject("$exists", true));
		Bson projection = new BasicDBObject("order", 1);
		FindIterable<DBObject> maxOrder = coll.find(query).projection(projection).sort(new BasicDBObject("order", -1)).limit(1);
		DBObject first = maxOrder.first();
		int max = 0;
		if (first != null) {
			max = (Integer) first.get("order");
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
		BasicDBObject query = new BasicDBObject("owner", userId.toObjectId());
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
		BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject("order", increment));
		MongoCollection<DBObject> coll = DBLayer.getCollection(collection);
		try {
			coll.updateMany(query, update);
		} catch (MongoException e) {
			throw new DatabaseException(e);
		}
	}

}
