package utils.db;

/**
 * Exception is thrown if a database change would cause data to be lost due to concurrent updates
 *
 */
public class LostUpdateException extends Exception {

	public LostUpdateException() { super(); };
}
