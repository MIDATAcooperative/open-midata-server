package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import scala.NotImplementedError;
import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.Record;
import models.enums.APSSecurityLevel;

/**
 * query a preselected list of records in memory
 *
 */
public class Feature_InMemoryQuery extends APS {
	
	private List<DBRecord> contents;
	private ObjectId me = new ObjectId();
	
	public Feature_InMemoryQuery(List<DBRecord> contents) {
		this.contents = contents;
	}
			
	@Override
	protected List<DBRecord> lookup(List<DBRecord> input, Query q)
			throws InternalServerException {
		List<DBRecord> result = new ArrayList<DBRecord>();
		for (DBRecord record : input) {
			if (contents.contains(record)) result.add(record);
		}
		return result;
	}

	@Override
	protected List<DBRecord> query(Query q) throws InternalServerException {	
		return contents;
	}

	@Override
	protected List<DBRecord> postProcess(List<DBRecord> records, Query q)
			throws InternalServerException {
		return records;
	}

	@Override
	public ObjectId getId() {
		return me;
	}

	@Override
	public boolean isReady() throws AppException {
		return true;
	}

	@Override
	public boolean isAccessible() throws AppException {
		return true;
	}

	@Override
	public void touch() throws AppException {				
	}

	@Override
	public long getLastChanged() throws AppException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public APSSecurityLevel getSecurityLevel() throws InternalServerException {		
		return APSSecurityLevel.HIGH;
	}

	@Override
	public void provideRecordKey(DBRecord record) throws AppException {
		throw new NotImplementedError();
	}

	@Override
	public void addAccess(Set<ObjectId> targets) throws AppException, EncryptionNotSupportedException {
		throw new NotImplementedError();
	}

	@Override
	public void addAccess(ObjectId target, byte[] publickey) throws AppException, EncryptionNotSupportedException {
		throw new NotImplementedError();
	}

	@Override
	public void removeAccess(Set<ObjectId> targets) throws InternalServerException {
		throw new NotImplementedError();
	}

	@Override
	public void setMeta(String key, Map<String, Object> data) throws AppException {
		throw new NotImplementedError();
		
	}

	@Override
	public void removeMeta(String key) throws AppException {
		throw new NotImplementedError();
	}

	@Override
	public BasicBSONObject getMeta(String key) throws AppException {		
		return null;
	}

	@Override
	protected boolean lookupSingle(DBRecord input, Query q) throws AppException {		
		for (DBRecord record : contents) {
			if (record._id.equals(input._id)) return true;
		}
		return false;
	}

	@Override
	public void addPermission(DBRecord record, boolean withOwner) throws AppException {
		throw new NotImplementedError();
		
	}

	@Override
	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		throw new NotImplementedError();
		
	}

	@Override
	public boolean removePermission(DBRecord record) throws AppException {
		throw new NotImplementedError();		
	}

	@Override
	public void removePermission(Collection<DBRecord> records) throws AppException {
		throw new NotImplementedError();		
	}

	@Override
	public List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException {
		throw new NotImplementedError();
	}

	@Override
	public void clearPermissions() throws AppException {
		throw new NotImplementedError();		
	}
	
	

}
