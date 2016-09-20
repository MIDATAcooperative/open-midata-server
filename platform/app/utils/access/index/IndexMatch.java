package utils.access.index;

import models.MidataId;

/**
 * Stores an index match
 *
 */
public class IndexMatch {

	/**
	 * id of record included in result
	 */
	public MidataId recordId;
	
	/**
	 * aps that can be checked if record is still accessible
	 */
	public MidataId apsId;
}
