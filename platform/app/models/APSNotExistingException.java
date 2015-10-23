package models;

import org.bson.types.ObjectId;

import utils.exceptions.InternalServerException;

public class APSNotExistingException extends InternalServerException {

	private ObjectId aps;
	
	public APSNotExistingException(ObjectId aps, String msg) {	
		super("error.internal.aps", msg);
		this.aps = aps;		
	}
	
	public ObjectId getAps() {
		return aps;
	}
	
	

}
