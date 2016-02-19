package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.NotMaterialized;
import utils.db.OrderOperations;
import utils.exceptions.InternalServerException;
import utils.search.Search;
import utils.search.SearchException;

/**
 * data model class for a "Space".
 * 
 * A Space is a tile in the frontend of a user that belongs to one plugin. 
 * Each space has its own access permissions.
 *
 */
@JsonFilter("Space")
public class Space extends Model implements Comparable<Space> {

	protected @NotMaterialized static final String collection = "spaces";
	/**
	 * constant set containing all fields of this class
	 */
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "name","owner", "visualization", "type", "order", "type", "context", "autoShare");

	/**
	 * the name of the space.
	 * 
	 * This is displayed as title for the tile in the dashboard
	 */
	public String name;
	
	/**
	 * The id of the owner of the space.
	 */
	public ObjectId owner;
	
	/**
	 * The id of the plugin that is used for displaying the contents of this space.
	 */
	public ObjectId visualization;
	
	/**
	 * The id of the input form/importer that is used with this space. May be removed in the future.
	 */
	public String type;
	
	/**
	 * The display order 
	 */
	public int order;
	
	/**
	 * The internal name of the dashboard where this space is displayed
	 */
	public String context;
	
	/**
	 * a list of consent ids into which new records created in this space are automatically shared
	 */
	public Set<ObjectId> autoShare;
	
	/**
	 * automatically run import
	 */
	public boolean autoImport;
	
	/**
	 * the filter query that is applied to the users main APS when querying data using this space.
	 * 
	 * This field is not directly stored in the database but in the APS that belongs to this space.
	 */
	public @NotMaterialized Map<String, Object> query;
	

	@Override
	public int compareTo(Space other) {
		if (this.order > 0 && other.order > 0) {
			return this.order - other.order;
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(Space.class, collection, properties);
	}
	
	public static boolean existsByNameAndOwner(String name, ObjectId ownerId) throws InternalServerException {
		return Model.exists(Space.class, collection, CMaps.map("name", name).map("owner", ownerId));
	}

	public static Space get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, properties, fields);
	}
	
	public static Space getByIdAndOwner(ObjectId spaceId, ObjectId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, CMaps.map("_id", spaceId).map("owner", ownerId), fields);
	}
	
	public static Space getByOwnerVisualizationContext(ObjectId ownerId, ObjectId visualizationId, String context, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, CMaps.map("owner", ownerId).map("visualization", visualizationId).map("context", context), fields);
	}
	
	public static Space getByOwnerSpecialContext(ObjectId ownerId, String context, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, CMaps.map("owner", ownerId).map("context", context), fields);
	}
	
	public static Set<Space> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Space.class, collection, properties, fields);
	}
	
	public static Set<Space> getAllByOwner(ObjectId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(Space.class, collection, CMaps.map("owner", owner), fields);
	}

	public static void set(ObjectId spaceId, String field, Object value) throws InternalServerException {
		Model.set(Space.class, collection, spaceId, field, value);
	}

	public static void add(Space space) throws InternalServerException {
		Model.insert(collection, space);
		
	}

	public static void delete(ObjectId ownerId, ObjectId spaceId) throws InternalServerException {
		// find order first
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", spaceId).get();
		Space space = get(properties, new ChainedSet<String>().add("order").get());

		// decrement all order fields greater than the removed space
		try {
			OrderOperations.decrement(collection, ownerId, space.order, 0);
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal", e);
		}
		
		Model.delete(Space.class, collection, properties);
	}

	public static int getMaxOrder(ObjectId ownerId) {
		return OrderOperations.getMax(collection, ownerId);
	}

}
