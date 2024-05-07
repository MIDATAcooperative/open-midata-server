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

package utils.access.pseudo;

import java.util.Calendar;

import models.Record;

public class RemoveTimeFilter extends ProjectDataFilterBase {

	@Override
	public boolean pseudonymize(String field, Object value) {
		return (value instanceof String) && ((String) value).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
	}

	@Override
	public Object result(String field, Object value) {
		String v = (String) value;
		int i = v.indexOf("T");
		return v.substring(0, i);
	}

	@Override
	public void pseudonymizeMeta(Record rec) {
		Calendar cal = Calendar.getInstance();
		if (rec.created != null) {
			cal.setTime(rec.created);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			rec.created = cal.getTime();
		}
		if (rec.lastUpdated != null) {
			cal.setTime(rec.lastUpdated);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			rec.lastUpdated = cal.getTime();
		}		
		
	}

	
}
