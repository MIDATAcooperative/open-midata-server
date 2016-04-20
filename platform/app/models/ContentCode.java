package models;

import java.util.Map;
import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
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
		  int p = systemCode.indexOf(' ');
		  if (p<0) return null;
		  String system = systemCode.substring(0, p);
		  String code = systemCode.substring(p+1);
		  
		  ContentCode result = Model.get(ContentCode.class, collection, CMaps.map("system", system).map("code", code), Sets.create("content"));
		  if (result != null) return result.content;
		  
		  return null;
	  }
	  
	  public static ContentCode getBySystemCode(String systemCode) throws InternalServerException {
		  int p = systemCode.indexOf(' ');
		  String system = systemCode.substring(0, p);
		  String code = systemCode.substring(p);
		  
		  ContentCode result = Model.get(ContentCode.class, collection, CMaps.map("system", system).map("code", code), Sets.create("content"));
		  return result;
	  }
	  
	  public static void add(ContentCode cc) throws InternalServerException {
		  Model.insert(collection, cc);
	  }
	  
	  public static void upsert(ContentCode cc) throws InternalServerException {
		  Model.upsert(collection, cc);
	  }
	  	  	 
}
