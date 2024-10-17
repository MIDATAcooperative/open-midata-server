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

import java.util.Map;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import utils.access.DBRecord;
import utils.access.Feature_Pseudonymization;
import utils.exceptions.AppException;
import models.Record;

public class ProjectDataFilterBase {

	public void pseudonymizeAll(DBRecord rec, Object base) throws AppException {
	    if (base == null) return;
        if (base instanceof BasicBSONList) {       
            BasicBSONList lst = (BasicBSONList) base;
            for (int i=0;i<lst.size();i++) {
                Object entry = lst.get(i);
                pseudonymizeAll(rec, entry);
            }
	   } else if (base instanceof BasicBSONObject) {
            BasicBSONObject obj = (BasicBSONObject) base;
            pseudonymizeObject(rec, obj);
            for (Object v : obj.values()) pseudonymizeAll(rec, v);                    
        }       
    }
	
	public void pseudonymizeObject(DBRecord rec, BasicBSONObject obj) {
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			if (pseudonymize(entry.getKey(), entry.getValue())) {
				entry.setValue(result(entry.getKey(), entry.getValue()));
			}
		}
	}
	
	public boolean pseudonymize(String field, Object value) {
		return false;
	}
	
	public Object result(String field, Object value) {
		return value;
	}

	public void pseudonymizeMeta(Record rec) {
		
		
	}
}
