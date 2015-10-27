package utils.auth;

import utils.exceptions.InternalServerException;

public class EncryptionNotSupportedException extends InternalServerException {
	
	public EncryptionNotSupportedException(String msg) {
		super("error.encryption", msg);
	}

	private static final long serialVersionUID = 285728245833349692L;

}
