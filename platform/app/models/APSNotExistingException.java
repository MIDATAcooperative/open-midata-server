package models;

import org.bson.types.ObjectId;

public class APSNotExistingException extends ModelException {

	private ObjectId aps;
	
	public APSNotExistingException(ObjectId aps, String msg) {	
		super(msg);
		this.aps = aps;		
	}
	
	public ObjectId getAps() {
		return aps;
	}
	
	

}
