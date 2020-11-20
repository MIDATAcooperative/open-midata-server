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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.MidataId;
import utils.AccessLog;
import utils.collections.CMaps;
import utils.exceptions.AppException;

public class Feature_Pagination extends Feature {

	private Feature next;
	
	public Feature_Pagination(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		
		if (q.restrictedBy("from")) {
			MidataId from = q.getFrom();
			MidataId fromOwner = q.getFromOwner();
			
			Map<String, Object> props = new HashMap<String, Object>(q.getProperties());			
			props.put("limit", null);
			
			List<DBRecord> findFrom = next.query(new Query(q, "pagination-from", CMaps.map("_id", from).mapNotEmpty("owner", fromOwner)));
			DBRecord fromRecord = null;
			if (findFrom.size() == 1) {
				fromRecord = findFrom.get(0);				
			} else return ProcessingTools.empty();
			
			//AccessLog.logBeginPath("skip-until", "fromRecord="+fromRecord._id);
			DBIterator<DBRecord> result = next.iterator(new Query(q, "pagination-skip", props).setFromRecord(fromRecord));
			boolean foundFrom = false;
			
			while (!foundFrom && result.hasNext()) { DBRecord rec = result.next();if (rec._id.equals(from)) foundFrom = true; }
			
			AccessLog.log("foundFrom="+foundFrom);
			if (!foundFrom) return ProcessingTools.empty();
			if (!result.hasNext()) return ProcessingTools.dbiterator("singleton()", Collections.singletonList(fromRecord).iterator());
			
			return ProcessingTools.limit(q.getProperties(), new PaginationIterator(fromRecord, result));
		}
		
		if (q.restrictedBy("skip")) {
			int skip = (Integer) q.getProperties().get("skip");
			
			//AccessLog.logBeginPath("skip("+skip+")", null);
			Map<String, Object> props = new HashMap<String, Object>(q.getProperties());
			props.put("skip", null);									
			DBIterator<DBRecord> result = next.iterator(new Query(q, "pagination-skip", props));
			int current = 0;
			while ( current < skip && result.hasNext()) { result.next();current++; }
			//AccessLog.logEndPath(null);
			if (!result.hasNext()) return ProcessingTools.empty();
			
			return ProcessingTools.limit(q.getProperties(), result);
		}				
		
		return ProcessingTools.limit(q.getProperties(), next.iterator(q));
	}
	
	class PaginationIterator implements DBIterator<DBRecord> {

		private DBRecord first;
		private DBIterator<DBRecord> chain;
		
		public PaginationIterator(DBRecord first, DBIterator<DBRecord> chain) {
			this.first = first;
			this.chain = chain;
		}
		
		@Override
		public boolean hasNext() throws AppException {
			return first != null || chain.hasNext();
		}

		@Override
		public DBRecord next() throws AppException {
			if (first != null) {
				DBRecord result = first;
				first = null;
				return result;
			}
			return chain.next();
		}

		@Override
		public String toString() {
			return "paginate("+chain.toString()+")";
		}
		
		
		
	}
	
	
}
