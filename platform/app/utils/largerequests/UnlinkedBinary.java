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

package utils.largerequests;

import java.util.List;
import java.util.Set;

import models.AccessPermissionSet;
import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * A binary file that has been uploaded but is not linked from any record
 *
 */
public class UnlinkedBinary extends Model {
	
	private static final String collection = "unlinkedbinaries";
	public @NotMaterialized static final long EXPIRATION = 1000 * 60 * 60 * 8; 
	public @NotMaterialized static final Set<String> ALL_FIELDS = Sets.create("_id", "created", "owner");
	

	/* Id is same as id of file */
	
	/**
	 * Time of creation
	 */
	public long created;
	
	/**
	 * Owner of file
	 */
	public MidataId owner;
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static UnlinkedBinary getById(MidataId id) throws InternalServerException {
		return Model.get(UnlinkedBinary.class, collection, CMaps.map("_id", id), ALL_FIELDS);
	}
	
	public void delete() throws InternalServerException {
		Model.delete(UnlinkedBinary.class, collection, CMaps.map("_id", this._id));
	}
	
	public static List<UnlinkedBinary> getExpired() throws InternalServerException {
		return Model.getAllList(UnlinkedBinary.class, collection, CMaps.map("created", CMaps.map("$lt", System.currentTimeMillis() - EXPIRATION)), ALL_FIELDS, 1000);
	}
	
	public boolean isExpired() {
		return this.created < System.currentTimeMillis() - EXPIRATION;
	}
}
