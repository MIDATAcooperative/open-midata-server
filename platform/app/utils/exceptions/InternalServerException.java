package utils.exceptions;

public class InternalServerException extends AppException {

	private static final long serialVersionUID = 1L;

	public InternalServerException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public InternalServerException(String localeKey, String msg) {
		super(localeKey, msg);
	}

}
