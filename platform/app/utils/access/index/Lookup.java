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

import utils.access.op.Condition;

public class Lookup extends BaseLookup<IndexKey> {

	private Condition[] condition;
	
	public Lookup(Condition[] condition) {
		this.condition = condition;
	}
	
	public boolean conditionCompare(IndexKey inkey) {
		Comparable[] idxKey = inkey.getKey();
		for (int i=0;i<condition.length;i++) {
			if (!condition[i].satisfiedBy(idxKey[i])) return false;
		}		
		return true;
	}
	
	public boolean conditionCompare(IndexKey lk, IndexKey hk) {
		Comparable[] lowkey = lk == null ? null :lk.getKey();
		Comparable[] highkey = hk == null ? null : hk.getKey();
		for (int i=0;i<condition.length;i++) {
			if (!condition[i].isInBounds(lowkey==null ? null : lowkey[i],  highkey == null ? null: highkey[i]))  return false;
		}		
		return true;
	}
	
	public String toString() {
		return condition.toString();
	}
}
