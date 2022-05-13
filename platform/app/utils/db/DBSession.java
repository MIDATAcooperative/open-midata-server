package utils.db;

import com.mongodb.client.ClientSession;

public class DBSession implements AutoCloseable {

	private ClientSession clientSession;
	private String collection;
	
	public DBSession(ClientSession clientSession, String collection) {
		this.clientSession = clientSession;
		this.collection = collection;
	}

	@Override
	public void close() {
		clientSession.close();
		DBLayer.clearSession();		
	}
	
	public void commit() {
		DBLayer.commitTransaction(collection);
	}
}
