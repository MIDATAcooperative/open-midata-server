package utils.auth;

import utils.exceptions.InternalServerException;

/**
 * Thrown if an account or APS does not support encryption and encryption is necessary
 *
 */
public class EncryptionNotSupportedException extends InternalServerException {
	
	public EncryptionNotSupportedException(String msg) {
		super("error.encryption", msg);
	}

	private static final long serialVersionUID = 285728245833349692L;

}
