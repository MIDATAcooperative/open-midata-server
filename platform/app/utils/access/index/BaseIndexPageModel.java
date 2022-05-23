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
import utils.access.EncryptionUtils;
import utils.collections.CMaps;
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public interface BaseIndexPageModel {

	public long getVersion();
	
	public void setLockTime(long lockTime);
	
	public long getLockTime();
	
	public void setEnc(byte[] enc);
	
	public byte[] getEnc();
	
	public void update() throws InternalServerException, LostUpdateException;
			
	public void updateLock() throws InternalServerException, LostUpdateException;		
	
	public MidataId getId();
	
	public BaseIndexPageModel reload() throws InternalServerException;
	
	public BaseIndexPageModel loadChildById(MidataId id) throws InternalServerException;
	
	public Set<? extends BaseIndexPageModel> getMultipleById(Set<MidataId> pageIds) throws InternalServerException;
	
	public default byte[] encrypt(byte[] key, byte[] data) throws InternalServerException {
		return EncryptionUtils.encrypt(key, data);
	}
	
	public default byte[] decrypt(byte[] key, byte[] data) throws InternalServerException {
		return EncryptionUtils.decrypt(key, data);
	}
	
	public void add() throws InternalServerException;
}
