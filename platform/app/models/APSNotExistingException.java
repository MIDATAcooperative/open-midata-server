package models;

import org.bson.types.ObjectId;

import utils.exceptions.InternalServerException;

/**
 * Exception that is thrown if the access package tries to use an APS that does not exist.
 *
 */
public class APSNotExistingException extends InternalServerException {
	
	private static final long serialVersionUID = 1L;
		
	private ObjectId aps;
	
	public APSNotExistingException(ObjectId aps, String msg) {	
		super("error.internal.aps", msg);
		this.aps = aps;		
	}
	
	public ObjectId getAps() {
		return aps;
	}
	
	

}
