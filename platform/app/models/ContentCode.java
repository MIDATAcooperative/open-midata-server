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

package models;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import utils.codesystems.Codesystems;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Mapping from coded measures to labels. FHIR compatible * 
 *
 */
public class ContentCode extends Model  {

	  private static String collection = "coding";
	  
	  /**
	   * The coding system used for this entry
	   */
	  public String system;
	  
	  /**
	   * Optional: The version of the coding system
	   */
	  public String version;
	  
	  /**
	   * The code
	   */
	  public String code;
	  
	  /**
	   * A default label for the given code
	   */
	  public String display;	  
	  
	  /**
	   * The "content" value (ContentInfo class) MIDATA uses to handle this content code
	   */
	  public String content;
	  
	  public @NotMaterialized static Map<String, String> contentForSystemCode = new ConcurrentHashMap<String, String>();
	  
	  public @NotMaterialized static Codesystems codesystems = new Codesystems();
	  	
	  /**
	   * returns all coding entries matching the given criteria.
	   * @param properties key-value map with criteria
	   * @param fields set of field names to return
	   * @return set of matching coding entries
	   * @throws InternalServerException
	   */
	  public static Set<ContentCode> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
			return Model.getAll(ContentCode.class, collection, properties, fields);
	  }
	  
	  /**
	   * returns the MIDATA content field value for a system+code pair
	   * @param systemCode the system plus code pair to be looked up
	   * @return the content field value used by MIDATA or null if the code could not be found
	   * @throws InternalServerException
	   */
	  public static String getContentForSystemCode(String systemCode) throws InternalServerException {
		  String fast = contentForSystemCode.get(systemCode);
		  if (fast != null) return fast;
		  
		  int p = systemCode.indexOf(' ');
		  if (p<0) return null;
		  String system = systemCode.substring(0, p);
		  String code = systemCode.substring(p+1);
		  
		  String resultStr = codesystems.getContentForSystemCode(system, code);
		  if (resultStr != null) return resultStr;
		  		  
		  ContentCode result = Model.get(ContentCode.class, collection, CMaps.map("system", system).map("code", code), Sets.create("content"));
		  if (result != null) {
			  contentForSystemCode.put(systemCode, result.content);
			  return result.content;
		  }
		  
		  return null;
	  }
	  
	  /**
	   * Lookup ContentCode by system+" "+code string. Splits string into system and code
	   * @param systemCode
	   * @return ContentCode
	   * @throws InternalServerException
	   */
	  public static ContentCode getBySystemCode(String systemCode) throws InternalServerException {
		  int p = systemCode.indexOf(' ');
		  String system = systemCode.substring(0, p);
		  String code = systemCode.substring(p+1);
		  
		  ContentCode result = Model.get(ContentCode.class, collection, CMaps.map("system", system).map("code", code), Sets.create("content"));
		  return result;
	  }
	  
	  /**
	   * Lookup ContentCode by system and code
	   * @param system
	   * @param code
	   * @return
	   * @throws InternalServerException
	   */
	  public static ContentCode getBySystemCode(String system, String code) throws InternalServerException {
		  ContentCode result = Model.get(ContentCode.class, collection, CMaps.map("system", system).map("code", code), Sets.create("content"));
		  return result;
	  }
	  
	  public static void add(ContentCode cc) throws InternalServerException {
		  Model.insert(collection, cc);
	  }
	  
	  public static void upsert(ContentCode cc) throws InternalServerException {
		  Model.upsert(collection, cc);
	  }
	  
	  public static void delete(MidataId ccId) throws InternalServerException {			
		  Model.delete(ContentCode.class, collection, CMaps.map("_id", ccId));
	  }

	  public static void reset() {
		  contentForSystemCode.clear();
	  }
	  	  	 
}
