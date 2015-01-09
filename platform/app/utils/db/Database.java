package utils.db;

public abstract class Database {

	protected abstract void openConnection() throws DatabaseException;
	
	protected abstract void close();
	
	protected abstract void destroy();
}
