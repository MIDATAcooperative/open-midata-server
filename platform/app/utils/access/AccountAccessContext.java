package utils.access;

import models.MidataId;
import models.Record;
import utils.exceptions.AppException;

public class AccountAccessContext extends AccessContext {

	public AccountAccessContext(APSCache cache, AccessContext parent) {
		super(cache, parent);
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (parent != null) return parent.mayCreateRecord(record);
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		if (parent != null) return parent.mayUpdateRecord(stored, newVersion);
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		if (parent != null) return parent.mustPseudonymize();
		return false;
	}

	@Override
	public MidataId getTargetAps() {		
		return cache.getAccountOwner();
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public String getOwnerName() {		
		return null;
	}

	@Override
	public MidataId getOwner() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getSelf() {
		return parent != null ? parent.getSelf() : cache.getAccountOwner();
	}

	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return true;
	}

	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}

	@Override
	public String toString() {
		return "account("+getOwner()+" "+parentString()+")";
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}
	
	

}
