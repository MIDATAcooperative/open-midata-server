package models;

import org.bson.types.ObjectId;

import utils.exceptions.ModelException;

public class APSNotExistingException extends ModelException {

	private ObjectId aps;
	
	public APSNotExistingException(ObjectId aps, String msg) {	
		super("error.internal.aps", msg);
		this.aps = aps;		
	}
	
	public ObjectId getAps() {
		return aps;
	}
	
	

}
