package utils.exceptions;

public class AppException extends Exception {

	private String localeKey;
	
	public AppException(String localeKey, Throwable cause) {
		super(cause);
		this.localeKey = localeKey;
	}
	
	public AppException(String localeKey, String msg) {
		super(msg);
		this.localeKey = localeKey;		
	}
	
	public String getLocaleKey() {
		return localeKey;
	}
}
