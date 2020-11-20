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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bson.BasicBSONObject;

import utils.exceptions.AppException;

/**
 * access permission sets may contain a blacklist of record IDs that are never contained in this APS.
 * (makes only sense with query based access permission sets)
 *
 */
public class Feature_BlackList extends Feature {

    private Feature next;
    private Set<String> blacklist; 
	
	public Feature_BlackList(APS target, Feature next) throws AppException {
		this.next = next;
		initBlacklist(target);
	}
	
	private void initBlacklist(APS target) throws AppException {
		blacklist = new HashSet<String>();
		BasicBSONObject list = target.getMeta("_exclude");
		if (list != null) {
			Collection idlist = (Collection) list.get("ids");
			for (Object id : idlist) {
				blacklist.add(id.toString());
			}
		}
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (blacklist.isEmpty()) return next.iterator(q);
		return new BlackListIterator(next.iterator(q));
		
		
	}
		
	class BlackListIterator implements DBIterator<DBRecord> {
		BlackListIterator(DBIterator<DBRecord> chain) {
			this.chain = chain;
		}
		
		private DBRecord next;
		private DBIterator<DBRecord> chain;

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord result = next;
			next = null;
			while (next == null && chain.hasNext()) {
				DBRecord record = chain.next();
				if (!blacklist.contains(record._id.toString())) next = record;	
			}			
			return result;
		}	
		
		@Override
		public String toString() {
			return "blacklist("+chain.toString()+")";
		}
		
	}
		
}
