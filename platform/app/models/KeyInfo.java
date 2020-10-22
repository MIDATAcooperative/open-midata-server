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

/**
 * data model class for storing private keys 
 *
 */
public class KeyInfo extends Model {

	protected @NotMaterialized static final String collection = "keys";
	
	/**
	 * the private key
	 */
	public byte[] privateKey;
	
	/**
	 * the type of the private key.
	 * 0 = plain private key
	 * 1 = passphrase protected private key
	 */
	public int type;
	
	public static KeyInfo getById(MidataId id) throws InternalServerException {
		return Model.get(KeyInfo.class, collection, CMaps.map("_id", id), Sets.create("privateKey", "type"));
	}
	
	public static void add(KeyInfo keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
	
	public static void update(KeyInfo keyinfo) throws InternalServerException {
		Model.upsert(collection, keyinfo);
	}
	
	public static void delete(MidataId id) throws InternalServerException {
		Model.delete(KeyInfo.class, collection, CMaps.map("_id", id));
	}
}
