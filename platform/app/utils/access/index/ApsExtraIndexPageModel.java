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

package utils.access.index;

import java.util.Set;

import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.DBLayer;
import utils.db.DatabaseException;
import utils.db.LostUpdateException;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class ApsExtraIndexPageModel extends Model implements BaseIndexPageModel {

	protected @NotMaterialized static final String collection = "apsextra";
	protected @NotMaterialized final static Set<String> ALL_PAGE = Sets.create("version", "enc", "lockTime", "creation", "rev");
	
	/**
	 * Last updated version number to prevent lost updates
	 */
	public long version;
	
	/**
	 * Timestamp that is set to lock the index for longer index operations
	 */
	public long lockTime;
	
	/**
	 * software revision number
	 */
	public int rev;
			
	/**
	 * encrypted data
	 */
	public byte[] enc;
	
	/**
	 * Unencrypted data of index
	 */
	//public @NotMaterialized BSONObject unencrypted;
	
					
	public static void add(ApsExtraIndexPageModel def) throws InternalServerException {
		Model.insert(collection, def);				
	}
	
	public MidataId getId() {
		return _id;
	}
	
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getLockTime() {
		return lockTime;
	}

	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}

	public byte[] getEnc() {
		return enc;
	}

	public void setEnc(byte[] enc) {
		this.enc = enc;
	}

	public static ApsExtraIndexPageModel getById(MidataId pageId) throws InternalServerException {
		return Model.get(ApsExtraIndexPageModel.class, collection, CMaps.map("_id", pageId), ALL_PAGE);
	}
	
	@Override
	public Set<? extends BaseIndexPageModel> getMultipleById(Set<MidataId> pageIds) throws InternalServerException {
		return Model.getAll(ApsExtraIndexPageModel.class, collection, CMaps.map("_id", pageIds), ALL_PAGE);
	}
	
	public static Set<? extends BaseIndexPageModel> getMultipleByIdS(Set<MidataId> pageIds) throws InternalServerException {
		return Model.getAll(ApsExtraIndexPageModel.class, collection, CMaps.map("_id", pageIds), ALL_PAGE);
	}
	
	
	
	public void update() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "lockTime", "enc");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal.db", e);
		}
	}
	
	public void updateLock() throws InternalServerException, LostUpdateException {
		try {
		   DBLayer.secureUpdate(this, collection, "version", "lockTime");
		} catch (DatabaseException e) {
			throw new InternalServerException("error.internal.db", e);
		}
	}
	
	public static long count() throws AppException {
		return Model.count(ApsExtraIndexPageModel.class, collection, CMaps.map());
	}
	
	public BaseIndexPageModel reload() throws InternalServerException {
		return getById(_id);
	}
	
	@Override
	public BaseIndexPageModel loadChildById(MidataId id) throws InternalServerException {
		return ApsExtraIndexPageModel.getById(id);
	}
	
	@Override
	public void add() throws InternalServerException {
		Model.insert(collection, this);		
	}
	
	// array of { key : array , entries : [ { rec :   , consent :  } ] or page : IndexPageId } 
}
