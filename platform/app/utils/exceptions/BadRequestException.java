package utils.exceptions;

/**
 * exception that is thrown if an error occurs because a request with illegal parameters has been issued.
 *
 */
public class BadRequestException extends AppException {	
	
	private static final long serialVersionUID = 1L;
	
	private int statusCode = 400;

	public BadRequestException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public BadRequestException(String localeKey, String msg) {
		super(localeKey, msg);
	}
	
	public BadRequestException(String localeKey, String msg, int statusCode) {
		super(localeKey, msg);
		this.statusCode = statusCode;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}
