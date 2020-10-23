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

package utils.access.index;

import models.MidataId;
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
}
