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

package models;

import java.util.Map;
import java.util.Set;

import utils.exceptions.InternalServerException;

public class Loinc extends Model  {

	  private static String collection = "loinc";
	  
	  /**
	   * Loinc code
	   */
	  public String LOINC_NUM;
	  
	  /**
	   * Loinc long name
	   */
	  public String LONG_COMMON_NAME;
	  
	  /**
	   * Loinc short name
	   */
	  public String SHORTNAME;
	  	 
	
	  /**
	   * returns all coding entries matching the given criteria.
	   * @param properties key-value map with criteria
	   * @param fields set of field names to return
	   * @return set of matching coding entries
	   * @throws InternalServerException
	   */
	  public static Set<Loinc> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
			return Model.getAll(Loinc.class, collection, properties, fields);
	  }
	  
}
