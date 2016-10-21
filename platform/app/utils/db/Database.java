package utils.db;

/**
 * Base class for database implementations
 *
 */
public abstract class Database {

	/**
	 * open connection to database
	 * @throws DatabaseException
	 */
	protected abstract void openConnection() throws DatabaseException;
	
	/**
	 * close connection to database
	 */
	protected abstract void close();
		
}
