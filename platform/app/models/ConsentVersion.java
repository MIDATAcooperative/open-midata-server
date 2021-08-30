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

import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class ConsentVersion extends Model {

	private static final String collection = "vconsents";
	
	public @NotMaterialized final static Set<String> ALL = Sets.create("_id", "version", "fhirConsent");
	
	@NotMaterialized
	public String version;
	
	public BSONObject fhirConsent;
	
	/**
	 * getter for _id field
	 * @return
	 */
	public Object to_db_id() {
		return new BasicBSONObject("_id", _id.toObjectId()).append("version", version);
	}
	
	/**
	 * setter for _id field
	 * @param _id value of _id field
	 */
	public void set_id(Object _id) {
		BasicBSONObject obj = (BasicBSONObject) _id;
		this._id = new MidataId(obj.get("_id").toString());
		this.version = (String) obj.getString("version");
	}
	
	@Override
	public boolean equals(Object other) {
		if (other != null && this.getClass().equals(other.getClass())) {
			ConsentVersion otherModel = (ConsentVersion) other;
			return _id.equals(otherModel._id) && version.equals(otherModel.version);
		}
		return false;
	}
				
	@Override
	public int hashCode() {
		return _id.hashCode() + version.hashCode();
	}
	
	/**
	 * Fallback method for comparison of models.
	 */
	public int compareTo(ConsentVersion other) {
		int r = this._id.compareTo(other._id);
		return r == 0 ? this.version.compareTo(other.version) : r;
	}
	
	public static void add(ConsentVersion record) throws InternalServerException {
		Model.upsert(collection, record);	
	}
	
	public static List<ConsentVersion> getAllById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.getAllList(ConsentVersion.class, collection, CMaps.map("_id._id", id), fields, 2000);
	}
	
	public static Set<ConsentVersion> getAllById(Set<MidataId> id, Set<String> fields) throws InternalServerException {
		return Model.getAll(ConsentVersion.class, collection, CMaps.map("_id._id", id), fields);
	}
	
	public static ConsentVersion getByIdAndVersion(MidataId id, String version, Set<String> fields) throws InternalServerException {
		return Model.get(ConsentVersion.class, collection, CMaps.map("_id", CMaps.map("_id", id).map("version", version)), fields);
	}
	
	
}
