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

import utils.AccessLog;

public class HistoryLookup extends BaseLookup<HistoryIndexKey>{

	private long minTs;
	private boolean deletes;
	
	public HistoryLookup(long minTs, boolean deletes) {
		this.minTs = minTs;
		this.deletes = deletes;
	}
	
	@Override
	public boolean conditionCompare(HistoryIndexKey inKey) {		
		return inKey.getTs() >= minTs && inKey.isDelete() == deletes;		
	}

	@Override
	public boolean conditionCompare(HistoryIndexKey lk, HistoryIndexKey hk) {
		return hk == null || hk.getTs() >= minTs;		
	} 
	
	@Override
	public String toString() {
		return "aps-history({ minTs="+minTs+" del="+deletes+" })";	    		
	}

}