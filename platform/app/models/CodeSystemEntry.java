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

import org.bson.types.ObjectId;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class CodeSystemEntry extends Model  {

	  private static String collection = "csentries";
	  public @NotMaterialized static final Set<String> ALL = Sets.create("system", "systemDisplay", "version", "code", "language", "display", "deleted", "lastUpdated", "fhirResource");
	  
	  /**
	   * The coding system used for this entry
	   */
	  public String system;
	  
	  /**
	   * the display for the system
	   */
	  public String systemDisplay;
	  
	  /**
	   * Optional: The version of the coding system
	   */
	  public String version;
	  
	  /**
	   * The code
	   */
	  public String code;
	  
	  /**
	   * A label for the given code
	   */
	  public String display;	 
	  
	  /**
	   * the language of the display value
	   */
	  public String language;
	  
	  /**
	   * has this entry been deleted?
	   */
	  public boolean deleted;
	  
	  /**
	   * when was this entry last updated
	   */
	  public long lastUpdated;
	  
	  /**
	   * to which FHIR resource this belongs (optional)
	   */
	  public MidataId fhirResource;
	  
	  	
	  /**
	   * returns all coding entries matching the given criteria.
	   * @param properties key-value map with criteria
	   * @param fields set of field names to return
	   * @return set of matching coding entries
	   * @throws InternalServerException
	   */
	  public static Set<CodeSystemEntry> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
			return Model.getAll(CodeSystemEntry.class, collection, properties, fields);
	  }
	  
	  /**
	   * Lookup ContentCode by system and code
	   * @param system
	   * @param code
	   * @return
	   * @throws InternalServerException
	   */
	  public static CodeSystemEntry getBySystemCodeLanguage(String system, String code, String language) throws InternalServerException {
		  CodeSystemEntry result = Model.get(CodeSystemEntry.class, collection, CMaps.map("system", system).map("code", code).map("language", language).map("deleted", CMaps.map("$ne", true)), ALL);
		  return result;
	  }
	  
	  public static CodeSystemEntry getBySystemCodeVersionLanguage(String system, String code, String version, String language) throws InternalServerException {
		  CodeSystemEntry result = Model.get(CodeSystemEntry.class, collection, CMaps.map("system", system).map("code", code).map("version", version).map("language", language).map("deleted", CMaps.map("$ne", true)), ALL);
		  return result;
	  }
	  
	  public static Set<CodeSystemEntry> lookup(String system, String code, String version, String language) throws InternalServerException {
		  return Model.getAll(CodeSystemEntry.class, collection, CMaps.mapNotEmpty("system", system).mapNotEmpty("code", code).mapNotEmpty("version", version).mapNotEmpty("language", language).map("deleted", CMaps.map("$ne", true)), ALL);
	  }
	  
	  public boolean exists() throws InternalServerException {
		  return Model.exists(CodeSystemEntry.class, collection, CMaps.map("system", system).map("code", code).map("_id", CMaps.map("$ne", _id)).map("deleted", CMaps.map("$ne", true)));
	  }
	  
	  public static void add(CodeSystemEntry cc) throws InternalServerException {
		  Model.insert(collection, cc);
	  }
	  
	  public static void upsert(CodeSystemEntry cc) throws InternalServerException {
		  Model.upsert(collection, cc);
	  }
	  
	  public static void delete(MidataId ccId) throws InternalServerException {			
		  Model.set(ContentCode.class, collection, ccId, "lastUpdated", System.currentTimeMillis());
		  Model.set(ContentCode.class, collection, ccId, "deleted", true);
	  }

	  	  	 
}
