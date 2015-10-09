package utils.exceptions;

public class ModelException extends AppException {

	private static final long serialVersionUID = 1L;

	public ModelException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public ModelException(String localeKey, String msg) {
		super(localeKey, msg);
	}

}
