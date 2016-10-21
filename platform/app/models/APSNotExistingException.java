package models;

import utils.exceptions.InternalServerException;

/**
 * Exception that is thrown if the access package tries to use an APS that does not exist.
 *
 */
public class APSNotExistingException extends InternalServerException {
	
	private static final long serialVersionUID = 1L;
		
	private MidataId aps;
	
	public APSNotExistingException(MidataId aps, String msg) {	
		super("error.internal.aps", msg);
		this.aps = aps;		
	}
	
	public MidataId getAps() {
		return aps;
	}
	
	

}
