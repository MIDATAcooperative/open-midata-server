package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DatabaseException;
import utils.db.OrderOperations;
import utils.search.Search;
import utils.search.SearchException;

public class Circle extends Model implements Comparable<Circle> {

	private static final String collection = "circles";

	public ObjectId owner;
	public String name;
	public int order;
	public Set<ObjectId> members;
	public Set<ObjectId> shared; // records shared with this circle

	@Override
	public int compareTo(Circle other) {
		if (this.order > 0 && other.order > 0) {
			return this.order - other.order;
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Circle.class, collection, properties);
	}

	public static Circle get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Circle.class, collection, properties, fields);
	}

	public static Set<Circle> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Circle.class, collection, properties, fields);
	}

	public static void set(ObjectId circleId, String field, Object value) throws ModelException {
		Model.set(Circle.class, collection, circleId, field, value);
	}

	public static void add(Circle circle) throws ModelException {
		Model.insert(collection, circle);

		// also add this circle to the user's search index
		try {
			Search.add(circle.owner, "circle", circle._id, circle.name);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId ownerId, ObjectId circleId) throws ModelException {
		// find order first
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", circleId).get();
		Circle circle = get(properties, new ChainedSet<String>().add("order").get());

		// decrement all order fields greater than the removed circle
		try {
			OrderOperations.decrement(collection, ownerId, circle.order, 0);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}

		// also remove from search index
		Search.delete(ownerId, "circle", circleId);
		Model.delete(Circle.class, collection, properties);
	}

	public static int getMaxOrder(ObjectId ownerId) {
		return OrderOperations.getMax(collection, ownerId);
	}

}
