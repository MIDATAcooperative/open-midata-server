package models;

import java.util.Map;
import java.util.Set;

import models.enums.ConsentType;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.NotMaterialized;
import utils.db.OrderOperations;
import utils.search.Search;
import utils.search.SearchException;

public class Circle extends Consent implements Comparable<Circle> {
		
	public int order;
	//public ObjectId aps;

	@Override
	public int compareTo(Circle other) {
		if (this.order > 0 && other.order > 0) {
			return this.order - other.order;
		} else {
			return super.compareTo(other);
		}
	}
	
	public Circle() {
		this.type = ConsentType.CIRCLE;
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Circle.class, collection, properties);
	}
		

	public static Circle get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Circle.class, collection, properties, fields);
	}
	
	public static Circle getByIdAndOwner(ObjectId circleId, ObjectId ownerId, Set<String> fields) throws ModelException {
		return Model.get(Circle.class, collection, CMaps.map("_id", circleId).map("owner", ownerId), fields);
	}


	public static Set<Circle> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Circle.class, collection, properties, fields);
	}
	
	public static Set<Circle> getAllByOwner(ObjectId owner) throws ModelException {
		return Model.getAll(Circle.class, collection, CMaps.map("owner", owner).map("type",  ConsentType.CIRCLE), Sets.create("name", "authorized", "order"));
	}
	
	public static Set<Circle> getAllByMember(ObjectId member) throws ModelException {
		return Model.getAll(Circle.class, collection, CMaps.map("authorized", member).map("type",  ConsentType.CIRCLE), Sets.create("name", "order", "owner"));
	}

	public static void set(ObjectId circleId, String field, Object value) throws ModelException {
		Model.set(Circle.class, collection, circleId, field, value);
	}

	public void add() throws ModelException {
		Model.insert(collection, this);

		// also add this circle to the user's search index
		try {
			Search.add(this.owner, "circle", this._id, this.name);
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
