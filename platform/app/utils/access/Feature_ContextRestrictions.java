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

import java.util.Map;

import utils.exceptions.AppException;

public class Feature_ContextRestrictions extends Feature {

    private Feature next;
	
	public Feature_ContextRestrictions(Feature next) {
		this.next = next;
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {		
		Map<String, Object> restrictions = q.getContext().getQueryRestrictions();
		if (restrictions != null) {
			return QueryEngine.combineIterator(q, "context-restrictions", restrictions, next);		
		} else return next.iterator(q);
	}

}
