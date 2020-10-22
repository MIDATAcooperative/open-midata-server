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

package utils.access;

import java.util.Collection;
import java.util.Map;

import utils.exceptions.AppException;

public class Feature_Or extends Feature {

	private Feature next;
	
	public Feature_Or(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.getProperties().containsKey("$or")) {
			  
			  // TODO overlapping ORs
			  q.setFromRecord(null);
			
	      	  Feature qm = new Feature_Sort(next);
	      	  Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) q.getProperties().get("$or");      	  
	      	  return ProcessingTools.noDuplicates(ProcessingTools.multiQuery(qm, q, ProcessingTools.dbiterator("", col.iterator())));      	  
	    } return next.iterator(q);
	}
	
	
}
