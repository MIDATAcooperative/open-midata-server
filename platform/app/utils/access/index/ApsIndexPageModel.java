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

import java.util.Map;
import java.util.Set;

import models.MidataId;
import scala.NotImplementedError;
import utils.access.EncryptedAPS;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class ApsIndexPageModel implements BaseIndexPageModel {

	private EncryptedAPS aps;
	
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
	
	public ApsIndexPageModel(EncryptedAPS aps) throws AppException {
		this.aps = aps;
		Map<String, Object> data = aps.getPermissions();
		if (data.containsKey("idxEnc")) {
			lockTime = (long) data.get("idxLockTime");
			enc = (byte[]) data.get("idxEnc");
			rev = (int) data.get("idxRev");	
		}
	}
		
	@Override
	public long getVersion() {
		try {
		  return aps.getVersion();
		} catch (AppException e) { return 0; }
	}

	@Override
	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}

	@Override
	public long getLockTime() {
		return lockTime;
	}

	@Override
	public void setEnc(byte[] enc) {
		this.enc = enc;		
	}

	@Override
	public byte[] getEnc() {
		return this.enc;
	}

	@Override
	public void update() throws InternalServerException, LostUpdateException {
		try {
			Map<String, Object> data = aps.getPermissions();
			data.put("idxLockTime", lockTime);
			data.put("idxEnc", enc);
			data.put("idxRev", rev);
			aps.savePermissions();
		} catch (AppException e) {
			throw new InternalServerException("error.internal", e);
		}
	}

	@Override
	public void updateLock() throws InternalServerException, LostUpdateException {
		try {
			Map<String, Object> data = aps.getPermissions();
			data.put("idxLockTime", lockTime);
			aps.savePermissions();
		} catch (AppException e) {
			throw new InternalServerException("error.internal", e);
		}
	}

	@Override
	public MidataId getId() {
		return aps.getId();
	}

	@Override
	public BaseIndexPageModel reload() throws InternalServerException {
		aps.reload();
		try {
			return new ApsIndexPageModel(aps);
		} catch (AppException e) {
			throw new InternalServerException("error.internal", e);
		}
	}
	
	@Override
	public BaseIndexPageModel loadChildById(MidataId id) throws InternalServerException {
		return ApsExtraIndexPageModel.getById(id);
	}
	
	@Override
	public Set<? extends BaseIndexPageModel> getMultipleById(Set<MidataId> pageIds) throws InternalServerException {
		return ApsExtraIndexPageModel.getMultipleByIdS(pageIds);
	}
	
	@Override
	public byte[] encrypt(byte[] key, byte[] data) throws InternalServerException {
		return data; // APS is already encrypted, no double encryption with same key
	}
	
	@Override
	public byte[] decrypt(byte[] key, byte[] data) throws InternalServerException {
		return data; // APS is already encrypted, no double encryption with same key
	}
	
	@Override
	public void add() throws InternalServerException {
		// Is stored in APS. Cannot happen.
		throw new NotImplementedError();
		
	}

}
