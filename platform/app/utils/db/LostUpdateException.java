package utils.db;

/**
 * Exception is thrown if a database change would cause data to be lost due to concurrent updates
 *
 */
public class LostUpdateException extends Exception {

	/**
	 * UID for serialization
	 */
	private static final long serialVersionUID = 1L;

	public LostUpdateException() { super(); };
}
