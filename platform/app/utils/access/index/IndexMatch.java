package utils.access.index;

import org.bson.types.ObjectId;

/**
 * Stores an index match
 *
 */
public class IndexMatch {

	/**
	 * id of record included in result
	 */
	public ObjectId recordId;
	
	/**
	 * aps that can be checked if record is still accessible
	 */
	public ObjectId apsId;
}
