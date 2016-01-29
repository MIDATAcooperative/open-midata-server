package utils.access.index;

import models.Model;

/**
 * Data model of an index definition 
 *
 */
public class IndexDefinition extends Model {

	public String owner;
	
	/**
	 * Is this index only about records of the owner?
	 */
	public boolean selfOnly;
	
	/**
	 * For which record format does this index apply?
	 */
	public String format;
	
	/**
	 * Which fields are included in the index?
	 */
	public String[] fields;
}
