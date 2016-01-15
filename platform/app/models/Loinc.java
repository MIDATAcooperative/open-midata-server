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
