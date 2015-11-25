package utils.db;

/**
 * Exception throws if conversion between BSON and data model classes fails
 *
 */
public class DatabaseConversionException extends Exception {

	private static final long serialVersionUID = 1L;

	public DatabaseConversionException(Throwable cause) {
		super(cause);
	}

}