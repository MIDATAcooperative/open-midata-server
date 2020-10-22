/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class KeyInfoExtern extends Model {

	protected @NotMaterialized static final String collection = "keysext";
	
	/**
	 * the private key
	 */
	public String privateKey;
		
	
	public static KeyInfoExtern getById(MidataId id) throws InternalServerException {
		return Model.get(KeyInfoExtern.class, collection, CMaps.map("_id", id), Sets.create("privateKey"));
	}
	
	public static void add(KeyInfoExtern keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
	
	public static void update(KeyInfoExtern keyinfo) throws InternalServerException {
		Model.upsert(collection, keyinfo);
	}
	
	public static void delete(MidataId id) throws InternalServerException {
		Model.delete(KeyInfoExtern.class, collection, CMaps.map("_id", id));
	}
}
