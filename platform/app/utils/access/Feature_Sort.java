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

package utils.access;

import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_Sort extends Feature {
	
    private Feature next;
	
	public Feature_Sort(Feature next) {
		this.next = next;
	}
		

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("sort")) {
			q.setFromRecord(null);
			
			if (q.restrictedBy("_id") && q.restrictedBy("history") && q.getRestrictionOrNull("_id").size()==1 && q.getStringRestriction("sort").equals("lastUpdated desc")) {
				AccessLog.log("Result is already sorted by lastUpdated.");
				return next.iterator(q); 
			}
			
			return ProcessingTools.sort(q.getProperties(), next.iterator(q));
			
		} else return next.iterator(q);		
	}

}
