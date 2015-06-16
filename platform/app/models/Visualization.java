package models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

public class Visualization extends Plugin implements Comparable<Visualization> {

	private static final String collection = "visualizations";

	public String url;
	public String previewUrl;
	public String defaultSpaceName;
	public String defaultSpaceContext;
	public List<FilterRule> defaultRules;

	@Override
	public int compareTo(Visualization other) {
		if (this.name != null && other.name != null) {
			return this.name.compareTo(other.name);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(Visualization.class, collection, properties);
	}

	public static Visualization get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Visualization.class, collection, properties, fields);
	}
	
	public static Visualization getById(ObjectId id, Set<String> fields) throws ModelException {
		return Model.get(Visualization.class, collection, CMaps.map("_id", id), fields);
	}

	public static Set<Visualization> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Visualization.class, collection, properties, fields);
	}

	public static void set(ObjectId visualizationId, String field, Object value) throws ModelException {
		Model.set(Visualization.class, collection, visualizationId, field, value);
	}

	public static void add(Visualization visualization) throws ModelException {
		Model.insert(collection, visualization);

		// add to search index
		try {
			Search.add(Type.VISUALIZATION, visualization._id, visualization.name, visualization.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId visualizationId) throws ModelException {
		// remove from search index
		Search.delete(Type.VISUALIZATION, visualizationId);

		// TODO only hide or remove from all users (including deleting their spaces associated with it)?
		Model.delete(Visualization.class, collection, new ChainedMap<String, ObjectId>().put("_id", visualizationId).get());
	}

}
