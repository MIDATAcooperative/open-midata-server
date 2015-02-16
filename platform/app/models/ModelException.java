package models;

public class ModelException extends Exception {

	private static final long serialVersionUID = 1L;

	public ModelException(Throwable cause) {
		super(cause);
	}
	
	public ModelException(String msg) {
		super(msg);
	}

}
