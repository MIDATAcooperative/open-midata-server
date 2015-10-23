package utils.exceptions;

public class BadRequestException extends AppException {	
	
	private static final long serialVersionUID = 1L;

	public BadRequestException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public BadRequestException(String localeKey, String msg) {
		super(localeKey, msg);
	}
}
