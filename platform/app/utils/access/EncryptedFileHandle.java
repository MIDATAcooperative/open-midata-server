package utils.access;

import com.mongodb.MongoGridFSException;

import models.MidataId;
import utils.db.FileStorage;

public class EncryptedFileHandle {

	private MidataId id;
	private byte[] key;
	private long length;
	
	EncryptedFileHandle(MidataId id, byte[] key, long length) {
		this.id = id;
		this.key = key;
		this.length = length;
	}

	protected MidataId getId() {
		return id;
	}

	protected byte[] getKey() {
		return key;
	}

	public long getLength() {
		return length;
	}
	
	public void removeAfterFailure() {
		try {
		  FileStorage.delete(id.toObjectId());
		} catch (MongoGridFSException e) {}
	}
	
	
}
