package models;

import java.util.Map;
import java.util.Set;

import utils.exceptions.InternalServerException;

/**
 * Mapping from coded measures to labels. FHIR compatible * 
 *
 */
public class Coding extends Model  {

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
	   * returns all coding entries matching the given criteria.
	   * @param properties key-value map with criteria
	   * @param fields set of field names to return
	   * @return set of matching coding entries
	   * @throws InternalServerException
	   */
	  public static Set<Coding> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
			return Model.getAll(Coding.class, collection, properties, fields);
	  }
	  
}
