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

import java.util.Date;

import org.bson.BasicBSONObject;

import controllers.Circles;
import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_ConsentRestrictions extends Feature {

	private Feature next;
	
	public Feature_ConsentRestrictions(Feature next) {
		this.next = next;
	}

	public static boolean hasFilter(Query q) throws AppException {
		BasicBSONObject filter = q.getCache().getAPS(q.getApsId()).getMeta("_filter");
		if (filter != null) {			
		  if (filter.containsField("valid-until")) {
			  Date until = filter.getDate("valid-until");
			  if (until.before(new Date(System.currentTimeMillis()))) {
				  return true;
			  }
		  }
		  Date historyDate = filter.getDate("history-date");
		  if (historyDate != null && historyDate.after(new Date())) filter.remove("history-date");
		  if (!filter.isEmpty())  return true;
			 		
		}
		return false;
	}
	

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		BasicBSONObject filter = q.getCache().getAPS(q.getApsId()).getMeta("_filter");
		if (filter != null) {			
		  if (filter.containsField("valid-until")) {
			  Date until = filter.getDate("valid-until");
			  if (until.before(new Date(System.currentTimeMillis()))) {
				  AccessLog.log("consent not valid anymore");			
				  Circles.consentExpired(q.getCache().getExecutor(), q.getApsId());
				  return ProcessingTools.empty();
			  }
		  }
		  Date historyDate = filter.getDate("history-date");
		  if (historyDate != null && historyDate.after(new Date())) filter.remove("history-date");
		  if (!filter.isEmpty()) {
			 AccessLog.log("Applying consent filter");
			 return QueryEngine.combineIterator(q, "consent-filter", filter.toMap(), new Feature_ProcessFilters(next));
		  }			
		} 
		return next.iterator(q);
	}
	
	
}
