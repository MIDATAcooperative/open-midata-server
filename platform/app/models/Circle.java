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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import models.enums.ConsentStatus;
import models.enums.ConsentType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DatabaseException;
import utils.db.OrderOperations;
import utils.exceptions.InternalServerException;

/**
 * Data model for "Circles". Circles are consents between members.
 *
 */
public class Circle extends Consent {
	
	/**
	 * display order field
	 */
	public int order;	

	/*
	@Override
	public int compareTo(Circle other) {
		if (this.order > 0 && other.order > 0) {
			return this.order - other.order;
		} else {
			return super.compareTo(other);
		}
	}*/
	
	public Circle() {
		this.type = ConsentType.CIRCLE;
	}
/*
	public static boolean exists(Map<String, ? extends Object> properties) throws InternalServerException {
		return Model.exists(Circle.class, collection, properties);
	}
	*/	
/*
	public static Circle get(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.get(Circle.class, collection, properties, fields);
	}
	*/
	public static Circle getByIdAndOwner(MidataId circleId, MidataId ownerId, Set<String> fields) throws InternalServerException {
		return Model.get(Circle.class, collection, CMaps.map("_id", circleId).map("owner", ownerId).map("status", NOT_DELETED), fields);
	}


	public static Set<Circle> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(Circle.class, collection, properties, fields);
	}
	
	public static Set<Circle> getAllByOwner(MidataId owner) throws InternalServerException {
		return Model.getAll(Circle.class, collection, CMaps.map("owner", owner).map("type",  Sets.create(ConsentType.CIRCLE, ConsentType.REPRESENTATIVE)).map("status", NOT_DELETED), Consent.ALL);
	}
	
	public static Set<Circle> getAllByMember(MidataId member) throws InternalServerException {
		return Model.getAll(Circle.class, collection, CMaps.map("authorized", member).map("type",  Sets.create(ConsentType.CIRCLE, ConsentType.REPRESENTATIVE)).map("status", NOT_DELETED), Consent.ALL);
	}
	
	public static Set<Circle> getAllActiveByMember(MidataId member) throws InternalServerException {
		return Model.getAll(Circle.class, collection, CMaps.map("authorized", member).map("type",  Sets.create(ConsentType.CIRCLE, ConsentType.REPRESENTATIVE)).map("status", EnumSet.of(ConsentStatus.ACTIVE, ConsentStatus.FROZEN, ConsentStatus.PRECONFIRMED)), Consent.SMALL);
	}

	public static void set(MidataId circleId, String field, Object value) throws InternalServerException {
		Model.set(Circle.class, collection, circleId, field, value);
	}

	public void add() throws InternalServerException {
		assertNonNullFields();
		Model.insert(collection, this);

		// also add this circle to the user's search index
		/*try {
			Search.add(this.owner, "circle", this._id, this.name);
		} catch (SearchException e) {
			throw new InternalServerException("error.internal", e);
		}*/
	}

	public static void delete(MidataId ownerId, MidataId circleId) throws InternalServerException {
		// find order first
		
		Circle circle = Circle.getByIdAndOwner(circleId, ownerId, Sets.create("order","dataupdate","status"));

        if (circle != null) {
			// decrement all order fields greater than the removed circle
			try {
				OrderOperations.decrement(collection, ownerId, circle.order, 0);
			} catch (DatabaseException e) {
				throw new InternalServerException("error.internal", e);
			}
			
			circle.setStatus(ConsentStatus.DELETED);
        }
		        
		//Model.delete(Circle.class, collection, CMaps.map("owner", ownerId).map("_id", circleId));
	}

	public static int getMaxOrder(MidataId ownerId) {
		return OrderOperations.getMax(collection, ownerId);
	}

}
