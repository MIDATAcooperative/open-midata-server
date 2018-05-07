package utils.access.index;

import java.io.Serializable;

import models.MidataId;

/**
 * Stores an index match
 *
 */
public class IndexMatch implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5753238851931181026L;

	/**
	 * id of record included in result
	 */
	public MidataId recordId;
	
	/**
	 * aps that can be checked if record is still accessible
	 */
	public MidataId apsId;
	
	public IndexMatch() {}
	
	public IndexMatch(String rec, String aps) {
		this.recordId = MidataId.from(rec);
		this.apsId = MidataId.from(aps);
	}
	
	public IndexMatch(MidataId rec, MidataId aps) {
		this.recordId = rec;
		this.apsId = aps;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IndexMatch) {
			return ((IndexMatch) obj).recordId.equals(recordId) && ((IndexMatch) obj).apsId.equals(apsId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return recordId.hashCode();
	}
	
	
}
