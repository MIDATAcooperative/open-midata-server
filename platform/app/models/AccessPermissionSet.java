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
import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Data model class for an access permission set.
 *
 */
public class AccessPermissionSet extends Model {

	private static final String collection = "aps";
	public @NotMaterialized static final Set<String> ALL_FIELDS = Sets.create("keys", "version", "direct" ,"permissions", "encrypted", "security", "unmerged", "consent");
	
	/**
	 * security level of this APS.
	 * NONE : APS not encrypted, records not encrypted
	 * MEDIUM : APS is encrypted and all containing records are encrypted with the APS key
	 * HIGH : APS is encrypted and each record is encrypted with its own key
	 */
	public APSSecurityLevel security = APSSecurityLevel.NONE;
	
	/**
	 * timestamp of last change
	 */
	public long version;
	
	/**
	 * which format does the APS have
	 */
	public int format;
	
	/**
	 * key table. one entry for each entity that has access to this APS.
	 * map keys are the object ids of the entities and the word "owner" for the owner of the APS.
	 * each map value is the RSA encrypted AES key of this APS.
	 */
	public Map<String, byte[]> keys;
	
	/**
	 * the encrypted body of this APS
	 */
	public byte[] encrypted;
	
	/**
	 * this APS belongs to a consent
	 */
	public boolean consent;
	
	/**
	 * the unencrypted body of this APS.
	 */
	public Map<String, Object> permissions;
	
	/**
	 * additional blocks of data for this APS that need to be merged into this part (the main part)
	 */
	public List<AccessPermissionSet> unmerged;
	
	public static void add(AccessPermissionSet aps) throws InternalServerException {
		Model.insert(collection, aps);	
	}
	
	public static AccessPermissionSet getById(MidataId id) throws InternalServerException {
		return Model.get(AccessPermissionSet.class, collection, CMaps.map("_id", id), ALL_FIELDS);
	}
	
	public static Set<AccessPermissionSet> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(AccessPermissionSet.class, collection, properties, fields);
	}
		
	
	public void updatePermissions() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "permissions", "unmerged");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateEncrypted() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "encrypted", "unmerged");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateAll() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "encrypted", "keys", "unmerged", "format");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal_db", e);
		}
	}
	
	public void updateKeys() throws LostUpdateException, InternalServerException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "keys");
		} catch (DatabaseException e) {
				throw new InternalServerException("error.internal_db", e);
		}		
	}
	
	public void updateVersionOnly() throws LostUpdateException, InternalServerException {
		try {
		   DBLayer.secureUpdate(this, collection, "version");
		} catch (DatabaseException e) {
				throw new InternalServerException("error.internal_db", e);
		}		
	}
	
	public static void delete(MidataId appsId) throws InternalServerException {	
		Model.delete(AccessPermissionSet.class, collection, new ChainedMap<String, MidataId>().put("_id", appsId).get());
	}
	
	public static void setConsent(MidataId apsId) throws InternalServerException {
		Model.set(AccessPermissionSet.class, collection, apsId, "consent", true);
	}
}
