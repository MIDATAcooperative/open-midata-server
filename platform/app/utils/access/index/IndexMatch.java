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

import java.io.Serializable;

import models.MidataId;

/**
 * Stores an index match
 *
 */
public class IndexMatch implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5753238851931181026L;

	/**
	 * id of record included in result
	 */
	public MidataId recordId;
	
	/**
	 * aps that can be checked if record is still accessible
	 */
	public MidataId apsId;
	
	public IndexMatch() {}
	
	public IndexMatch(String rec, String aps) {
		this.recordId = MidataId.from(rec);
		this.apsId = MidataId.from(aps);
	}
	
	public IndexMatch(MidataId rec, MidataId aps) {
		this.recordId = rec;
		this.apsId = aps;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IndexMatch) {
			return ((IndexMatch) obj).recordId.equals(recordId) && ((IndexMatch) obj).apsId.equals(apsId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return recordId.hashCode();
	}
	
	
}
