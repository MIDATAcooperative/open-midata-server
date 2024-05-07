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

import org.bson.BasicBSONObject;

import models.Record;
import utils.access.DBRecord;

public class RemovePractitionerFilter extends ProjectDataFilterBase {

	@Override
	public void pseudonymizeObject(DBRecord rec, BasicBSONObject obj) {
		if (obj.containsField("reference") && obj.get("reference").toString().startsWith("Practitioner/")) {
			obj.remove("reference");
			obj.put("display", "Removed Practitioner");
		}
		
	
	}
	
	@Override
	public void pseudonymizeMeta(Record rec) {
		if (!rec.creator.equals(rec.owner)) rec.creator = null;
		
		
	}

	
}
