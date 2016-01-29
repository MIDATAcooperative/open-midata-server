package utils.access;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.enums.APSSecurityLevel;

import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class APSListImplementation extends APSImplementation {

	public APSListImplementation(EncryptedAPS eaps) {
		super(eaps);		
	}
		

	@Override
	public long getLastChanged() throws AppException {
		// TODO Auto-generated method stub
		return 0;
	}
		
	
	@Override
	protected boolean lookupSingle(DBRecord input, Query q) throws AppException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addPermission(DBRecord record, boolean withOwner) throws AppException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean removePermission(DBRecord record) throws AppException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removePermission(Collection<DBRecord> records) throws AppException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<DBRecord> query(Query q) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

}
