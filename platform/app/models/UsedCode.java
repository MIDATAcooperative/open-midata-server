package models;

import utils.collections.CMaps;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class UsedCode extends Model {	
	
	@NotMaterialized
	private final static String collection = "usedcodes";
	
	@NotMaterialized
	private String code;
	
	public Object to_db_id() {
		return code;
	}
	
	/**
	 * setter for _id field
	 * @param _id value of _id field
	 */
	public void set_id(Object _id) {		
		this.code = _id.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			UsedCode otherModel = (UsedCode) other;
			return code.equals(otherModel.code);
		}
		return false;
	}
	
	
	
	@Override
	public int hashCode() {
		return code == null ? 0 : code.hashCode();
	}

	/**
	 * Fallback method for comparison of models.
	 */
	public int compareTo(Model other) {
		return this.code.compareTo(((UsedCode) other).code);
	}
	
	public static boolean use(String code) throws InternalServerException {
		UsedCode usedcode = new UsedCode();
		usedcode.code = code;
		if (!Model.exists(UsedCode.class, collection, CMaps.map("_id", code))) {
			try {
			  Model.insert(collection, usedcode);			  
			  return true;
			} catch (InternalServerException e) {
				return false;
			}
		}
		return false;
		
	}
}
