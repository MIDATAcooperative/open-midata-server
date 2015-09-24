package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.SpaceType;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DatabaseException;
import utils.db.NotMaterialized;
import utils.db.OrderOperations;
import utils.search.Search;
import utils.search.SearchException;

public class Space extends Model implements Comparable<Space> {

	private static final String collection = "spaces";

	public String name;
	public ObjectId owner;
	public ObjectId visualization;
	public ObjectId app;
	public int order;
	public ObjectId aps;
	public SpaceType type;
	public String context;
	public Set<ObjectId> autoShare;
	public @NotMaterialized Map<String, Object> query;
	
	//public Set<ObjectId> records;

	@Override
	public int compareTo(Space other) {
		if (this.order > 0 && other.order > 0) {
			return this.order - other.order;
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Space.class, collection, properties);
	}
	
	public static boolean existsByNameAndOwner(String name, ObjectId ownerId) throws ModelException {
		return Model.exists(Space.class, collection, CMaps.map("name", name).map("owner", ownerId));
	}

	public static Space get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Space.class, collection, properties, fields);
	}
	
	public static Space getByIdAndOwner(ObjectId spaceId, ObjectId ownerId, Set<String> fields) throws ModelException {
		return Model.get(Space.class, collection, CMaps.map("_id", spaceId).map("owner", ownerId), fields);
	}
	
	public static Space getByOwnerVisualizationContext(ObjectId ownerId, ObjectId visualizationId, String context, Set<String> fields) throws ModelException {
		return Model.get(Space.class, collection, CMaps.map("owner", ownerId).map("visualization", visualizationId).map("context", context), fields);
	}
	
	public static Space getByOwnerSpecialContext(ObjectId ownerId, String context, Set<String> fields) throws ModelException {
		return Model.get(Space.class, collection, CMaps.map("owner", ownerId).map("context", context), fields);
	}
	
	public static Set<Space> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Space.class, collection, properties, fields);
	}
	
	public static Set<Space> getAllByOwner(ObjectId owner, Set<String> fields) throws ModelException {
		return Model.getAll(Space.class, collection, CMaps.map("owner", owner), fields);
	}

	public static void set(ObjectId spaceId, String field, Object value) throws ModelException {
		Model.set(Space.class, collection, spaceId, field, value);
	}

	public static void add(Space space) throws ModelException {
		Model.insert(collection, space);

		// also add this space to the user's search index
		try {
			Search.add(space.owner, "space", space._id, space.name);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId ownerId, ObjectId spaceId) throws ModelException {
		// find order first
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", spaceId).get();
		Space space = get(properties, new ChainedSet<String>().add("order").get());

		// decrement all order fields greater than the removed space
		try {
			OrderOperations.decrement(collection, ownerId, space.order, 0);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}

		// also remove from search index
		Search.delete(ownerId, "space", spaceId);
		Model.delete(Space.class, collection, properties);
	}

	public static int getMaxOrder(ObjectId ownerId) {
		return OrderOperations.getMax(collection, ownerId);
	}

}
