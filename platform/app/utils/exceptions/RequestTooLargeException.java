package utils.exceptions;

public class RequestTooLargeException extends RuntimeException {

	public RequestTooLargeException(String localeKey, String msg) {
		super(msg);		
	}

}
