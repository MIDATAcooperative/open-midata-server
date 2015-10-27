package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;


import utils.auth.EncryptionNotSupportedException;
import utils.auth.KeyManager;
import utils.db.LostUpdateException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import models.Record;
import models.RecordsInfo;
import models.enums.APSSecurityLevel;

/**
 * abstract interface for an access permission set
 *
 */
public abstract class APS extends Feature {

	public final static String QUERY = "_query";
	
	public abstract ObjectId getId();
	
	public abstract boolean isAccessible() throws AppException;
		
	public abstract APSSecurityLevel getSecurityLevel() throws InternalServerException;
	
	public abstract void provideRecordKey(Record record) throws AppException;
	
	public abstract void addAccess(Set<ObjectId> targets) throws AppException,EncryptionNotSupportedException;

	public abstract void addAccess(ObjectId target, byte[] publickey) throws AppException,EncryptionNotSupportedException;
	
	public abstract void removeAccess(Set<ObjectId> targets) throws InternalServerException;
	
	public abstract void setMeta(String key, Map<String, Object> data) throws AppException;
	
	public abstract void removeMeta(String key) throws AppException;
		
	public abstract BasicBSONObject getMeta(String key) throws AppException;
			
	protected abstract boolean lookupSingle(Record input, Query q) throws AppException;
							
	public abstract void addPermission(Record record, boolean withOwner) throws AppException;
		
	public abstract void addPermission(Collection<Record> records, boolean withOwner) throws AppException;
		
			
	public abstract boolean removePermission(Record record) throws AppException;
		
	public abstract void removePermission(Collection<Record> records) throws AppException;

	@Override
	protected List<Record> postProcess(List<Record> records, Query q)
			throws InternalServerException {			
		return records;
	}

	@Override
	protected List<Record> lookup(List<Record> input, Query q)
			throws AppException {
		List<Record> filtered = new ArrayList<Record>(input.size());
		for (Record record : input) { 
			if (lookupSingle(record, q)) { filtered.add(record); }			
		}
		return filtered;
	}
		
			
}
