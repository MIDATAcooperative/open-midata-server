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

import java.util.Set;

import utils.collections.CMaps;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class ConsentReshare extends Model {

	protected @NotMaterialized static final String collection = "consentreshare";
	
	/**
	 * public key for consent signatures for consent chaining
	 */
	public byte[] publicKey;

	public static ConsentReshare getById(MidataId consentId, Set<String> fields) throws InternalServerException {
		return Model.get(ConsentReshare.class, collection, CMaps.map("_id", consentId), fields);
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
}
