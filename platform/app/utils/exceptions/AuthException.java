package utils.exceptions;

import models.enums.SubUserRole;

/**
 * exception that is thrown if the user is not authorized to do the current action or read requested data
 *
 */
public class AuthException extends AppException {

	private static final long serialVersionUID = 1L;
	private SubUserRole requiredSubUserRole = null;
	

	public AuthException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public AuthException(String localeKey, String msg) {
		super(localeKey, msg);
	}
	
	public AuthException(String localeKey, String msg, SubUserRole required) {
		super(localeKey, msg);
		this.requiredSubUserRole = required;
	}
	
	public SubUserRole getRequiredSubUserRole() {
		return requiredSubUserRole;
	}

}