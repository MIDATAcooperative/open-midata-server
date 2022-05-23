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
import utils.db.LostUpdateException;
import utils.exceptions.InternalServerException;

public class TimestampIndexPageModel implements BaseIndexPageModel {

	private IndexDefinition def;
	
	public TimestampIndexPageModel(IndexDefinition def) {
		this.def = def;
	}
		
	@Override
	public long getVersion() {
		return def.version;
	}

	@Override
	public void setLockTime(long lockTime) {
		def.lockTime = lockTime;		
	}

	@Override
	public long getLockTime() {
		return def.lockTime;
	}

	@Override
	public void setEnc(byte[] enc) {
		def.encTs = enc;		
	}

	@Override
	public byte[] getEnc() {
		return def.encTs;
	}

	@Override
	public void update() throws InternalServerException, LostUpdateException {
		def.updateTs();		
	}

	@Override
	public void updateLock() throws InternalServerException, LostUpdateException {
		def.updateLock();		
	}

	@Override
	public MidataId getId() {
		return def._id;
	}

	@Override
	public BaseIndexPageModel reload() throws InternalServerException {
		return new TimestampIndexPageModel(IndexDefinition.getById(def._id));
	}

	@Override
	public BaseIndexPageModel loadChildById(MidataId id) throws InternalServerException {
		return IndexPageModel.getById(id);
	}

	@Override
	public Set<? extends BaseIndexPageModel> getMultipleById(Set<MidataId> pageIds) throws InternalServerException {
		return IndexPageModel.getMultipleByIdS(pageIds);
	}

	@Override
	public void add() throws InternalServerException {
		def.add();			
	}

	
}
