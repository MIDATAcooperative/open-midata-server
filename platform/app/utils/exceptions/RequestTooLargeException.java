package utils.exceptions;

public class RequestTooLargeException extends AppException {

	public RequestTooLargeException(String localeKey, String msg) {
		super(localeKey, msg);		
	}

}
