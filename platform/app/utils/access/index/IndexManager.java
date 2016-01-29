package utils.access.index;

import java.util.Collection;

/**
 * Manages indexes on encrypted data records.
 * Allows creation of new indexes, query for available indexes
 *
 */
public class IndexManager {

	
	/**
	 * update index:
	 * search all aps(consents) if version newer than index root
	 * for each match: 
	 *   search for streams created after index root -> add stream to index
	 *   standard query for records changed after index root ????
	 *   
	 * search all included streams if version is newer than index root
	 * for each match:
	 *   query for records created after index root -> add
	 *   query for records updated/deleted after index root -> update/delete
	 *   
	 * update index root version
	 */
	
	
	
	/**
	 * 
	 * @param idx
	 * @param values
	 * @return
	 */
	public Collection<IndexMatch> queryIndex(IndexDefinition idx, Object[] values) {
		return null;
	}
}
