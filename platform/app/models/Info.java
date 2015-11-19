package models;

import models.enums.InfoType;
import models.enums.Visibility;

/**
 * Stores a piece of information about a study 
 * Currently not used
 *
 */
public class Info {

	/**
	 * the type of information that is stored
	 */
	public InfoType type;
	
	/**
	 * the public label for this piece of information
	 */	
	public String label;
	
	/**
	 * the actual value 
	 */
    public String value;
    
    /**
     * the level of visibility of this piece of information
     */
    public Visibility visibility;
    
    /**
     * the display order 
     */
	public int order;
}
