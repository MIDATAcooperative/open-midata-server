/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.NotMaterialized;
import utils.db.OrderOperations;
import utils.exceptions.InternalServerException;
import utils.messaging.SubscriptionManager;

/**
 * data model class for a "Space".
 * 
 * A Space is a tile in the frontend of a user that belongs to one plugin. 
 * Each space has its own access permissions.
 *
 */
@JsonFilter("Space")
public class Space extends Model implements Comparable<Space>, Serializable {

	protected @NotMaterialized static final String collection = "spaces";
	/**
	 * constant set containing all fields of this class
	 */
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "name","owner", "visualization", "type", "order", "type", "context", "autoShare", "licence");

	private @NotMaterialized static final long serialVersionUID = 6535855157383731147L;
	
	/**
	 * the name of the space.
	 * 
	 * This is displayed as title for the tile in the dashboard
	 */
	public String name;
	
	/**
	 * The id of the owner of the space.
	 */
	public MidataId owner;
	
	/**
	 * The id of the plugin that is used for displaying the contents of this space.
	 */
	public MidataId visualization;
	
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
	 * automatically run import (deprecated. now use FHIR Subscription)
	 */
	public @NotMaterialized boolean autoImport;
	
	/**
	 * the filter query that is applied to the users main APS when querying data using this space.
	 * 
	 * This field is not directly stored in the database but in the APS that belongs to this space.
	 */
	public @NotMaterialized Map<String, Object> query;
	
	/**
	 * Licence used for access
	 */
	public MidataId licence;
	

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
	
	public static boolean existsByNameAndOwner(String name, MidataId ownerId) throws InternalServerException {
		return Model.exists(Space.class, collection, CMaps.map("name", name).map("owner", ownerId));
	}

	public static Space get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, properties, fields);
	}
	
	public static Space getByIdAndOwner(MidataId spaceId, MidataId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, CMaps.map("_id", spaceId).map("owner", ownerId), fields);
	}
	
	public static Space getByOwnerVisualizationContext(MidataId ownerId, MidataId visualizationId, String context, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, CMaps.map("owner", ownerId).map("visualization", visualizationId).map("context", context), fields);
	}
	
	public static Set<Space> getByOwnerVisualization(MidataId ownerId, MidataId visualizationId, Set<String> fields) throws InternalServerException {
		return Model.getAll(Space.class, collection, CMaps.map("owner", ownerId).map("visualization", visualizationId), fields);
	}
	
	public static Space getByOwnerSpecialContext(MidataId ownerId, String context, Set<String> fields) throws InternalServerException {
		return Model.get(Space.class, collection, CMaps.map("owner", ownerId).map("context", context), fields);
	}
	
	public static Set<Space> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Space.class, collection, properties, fields);
	}
	
	public static Set<Space> getAllByOwner(MidataId owner, Set<String> fields) throws InternalServerException {
		return Model.getAll(Space.class, collection, CMaps.map("owner", owner), fields);
	}

	public static void set(MidataId spaceId, String field, Object value) throws InternalServerException {
		Model.set(Space.class, collection, spaceId, field, value);
	}

	public static void add(Space space) throws InternalServerException {
		Model.insert(collection, space);
		
	}

	public static void delete(MidataId ownerId, MidataId spaceId) throws InternalServerException {
		// find order first
		Map<String, Object> properties = CMaps.map("_id", spaceId);
		Space space = get(properties, Sets.create("order"));
        if (space != null) {
			// decrement all order fields greater than the removed space
			try {
				OrderOperations.decrement(collection, ownerId, space.order, 0);
			} catch (DatabaseException e) {
				throw new InternalServerException("error.internal", e);
			}
        }
		SubscriptionManager.deactivateSubscriptions(ownerId, spaceId);
		Model.delete(Space.class, collection, properties);
	}

	public static int getMaxOrder(MidataId ownerId) {
		return OrderOperations.getMax(collection, ownerId);
	}

}
