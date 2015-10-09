package utils.exceptions;

public class AuthException extends AppException {

	private static final long serialVersionUID = 1L;

	public AuthException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public AuthException(String localeKey, String msg) {
		super(localeKey, msg);
	}

}