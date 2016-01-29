package utils.access.index;

import models.Model;

/**
 * Data model for an index page
 *
 */
public class IndexPageModel extends Model {

	/**
	 * Last updated version number to prevent lost updates
	 */
	public long version;
	
	// array of { key : array , entries : [ { rec :   , consent :  } ] or page : IndexPageId } 
}
