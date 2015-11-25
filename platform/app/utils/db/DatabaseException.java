package utils.db;

/**
 * Exception thrown on database errors
 *
 */
public class DatabaseException extends Exception {

	private static final long serialVersionUID = 1L;

	public DatabaseException(Throwable cause) {
		super(cause);
	}

}
