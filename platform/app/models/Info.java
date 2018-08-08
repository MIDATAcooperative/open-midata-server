package models;

import java.util.Map;

import models.enums.InfoType;
import models.enums.Visibility;

/**
 * Stores a piece of information about a study 
 *
 */
public class Info implements JsonSerializable {

	/**
	 * the type of information that is stored
	 */
	public InfoType type;
	
	/**
	 * the actual value (localized)
	 */
    public Map<String, String> value;
    
    /**
     * the level of visibility of this piece of information
     */
    public Visibility visibility;
        
}
