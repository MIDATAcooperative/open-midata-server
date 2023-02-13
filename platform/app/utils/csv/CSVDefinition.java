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

package utils.csv;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import models.MidataId;
import models.Model;
import models.Research;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class CSVDefinition extends Model {

	private static final String collection = "csvdef";
	 
	@NotMaterialized
	public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "jsonDefinition", "names")); 
	
	public String jsonDefinition;
	
	public List<String> names;
	
	public static void add(CSVDefinition csv) throws InternalServerException {
		Model.upsert(collection, csv);
	 }
	
	public static CSVDefinition getById(MidataId csvid, Set<String> fields) throws InternalServerException {	    
		return Model.get(CSVDefinition.class, collection, CMaps.map("_id", csvid), fields);
    }
	
}
